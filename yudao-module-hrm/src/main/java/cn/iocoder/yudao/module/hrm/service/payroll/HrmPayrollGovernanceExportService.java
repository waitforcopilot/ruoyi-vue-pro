package cn.iocoder.yudao.module.hrm.service.payroll;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollGovernanceVO.Job;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import javax.annotation.Resource;
import java.util.*;
import java.io.*;
import static cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollGovernanceStore.*;

@Service
@Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
@SuppressWarnings("unchecked")
public class HrmPayrollGovernanceExportService {
    @Resource private HrmPayrollGovernanceStore store;
    @Resource private HrmPayrollGovernanceRootService roots;
    @Resource private HrmPayrollGovernanceProjectionService projection;
    @Resource private HrmPayrollRequirementProjectGrantMapper grants;
    @Resource private HrmPayrollRequirementProjectMapper projects;
    private HrmPayrollGovernanceRecordDO job(Long project,Long id,Long actor){HrmPayrollGovernanceRecordDO j=store.record(id,"JOB");allowed(Objects.equals(j.getProjectId(),project)&&Objects.equals(id(store.data(j).get("actorId")),actor));return j;}
    private Map<String,Object> projected(HrmPayrollGovernanceRecordDO j,Long actor){Map<String,Object>d=store.data(j);roots.platform(actor,"export");List<Long> filter=null;if(d.get("filter")!=null){filter=new ArrayList<>();for(Object raw:(List<?>)d.get("filter"))filter.add(id(raw));}return projection.project(j.getProjectId(),id(d.get("releaseId")),actor,"EXPORT",filter);}
    private String stamp(HrmPayrollGovernanceRecordDO j,Map<String,Object> projected){
        List<HrmPayrollRequirementProjectGrantDO> currentGrants=grants.selectList(new LambdaQueryWrapperX<HrmPayrollRequirementProjectGrantDO>().eq(HrmPayrollRequirementProjectGrantDO::getProjectId,j.getProjectId()).orderByAsc(HrmPayrollRequirementProjectGrantDO::getId));
        List<HrmPayrollGovernanceRecordDO> controlled=new ArrayList<>(store.all(j.getProjectId(),"ROOT"));
        Set<Long> rootIds=new TreeSet<>();for(HrmPayrollRequirementProjectGrantDO grant:currentGrants)if(grant.getRootRecordId()!=null)rootIds.add(grant.getRootRecordId());
        Set<Long> present=new HashSet<>();for(HrmPayrollGovernanceRecordDO root:controlled)present.add(root.getId());
        for(Long rootId:rootIds)if(present.add(rootId))controlled.add(store.record(rootId,"ROOT"));
        controlled.sort(Comparator.comparing(HrmPayrollGovernanceRecordDO::getId));
        Set<Long> anchorIds=new HashSet<>();for(HrmPayrollGovernanceRecordDO root:controlled)anchorIds.add(id(store.data(root).get("anchorId")));
        List<HrmPayrollGovernanceRecordDO> anchors=new ArrayList<>();for(HrmPayrollGovernanceRecordDO anchor:store.all(0L,"ANCHOR"))if(anchorIds.contains(anchor.getId()))anchors.add(anchor);
        // Unrelated trust anchors must not invalidate a queued/ready download. Current
        // projection and the complete relevant authority chain are still rechecked.
        return digest(map("projection",projected,"project",projects.selectById(j.getProjectId()),"grants",currentGrants,"roots",controlled,"anchors",anchors));
    }
    public Long create(Job c,Long actor){
        store.lock();roots.platform(actor,"export");if(c.getRequirementIds()!=null)need(!c.getRequirementIds().isEmpty()&&c.getRequirementIds().size()<=10000&&c.getRequirementIds().stream().allMatch(x->x!=null&&x>0)&&new HashSet<>(c.getRequirementIds()).size()==c.getRequirementIds().size());Map<String,Object>p=projection.project(c.getProjectId(),c.getReleaseId(),actor,"EXPORT",c.getRequirementIds());
        HrmPayrollGovernanceRecordDO j=store.insert(c.getProjectId(),"JOB",UUID.randomUUID().toString(),map("actorId",actor,"releaseId",c.getReleaseId(),"filter",c.getRequirementIds(),"state","QUEUED","createdAt",now()));
        Map<String,Object>d=store.data(j);d.put("authorizationStamp",stamp(j,p));store.state(j,d);return j.getId();
    }
    public void generate(Long project,Long id,Long actor) throws IOException {
        store.lock();HrmPayrollGovernanceRecordDO j=job(project,id,actor);Map<String,Object>d=store.data(j);Map<String,Object>p=projected(j,actor);need(Objects.equals(d.get("authorizationStamp"),stamp(j,p)));need("QUEUED".equals(d.get("state")));
        byte[] bytes=workbook(p,actor);d.putAll(map("state","READY","generatedAt",now(),"fileDigest",digest(Base64.getEncoder().encodeToString(bytes)),"bytes",Base64.getEncoder().encodeToString(bytes)));store.state(j,d);
    }
    /** Called in a separate transaction after the generating transaction has rolled back. */
    public void failed(Long project,Long id,Long actor){store.lock();HrmPayrollGovernanceRecordDO j=job(project,id,actor);Map<String,Object>d=store.data(j);if("QUEUED".equals(d.get("state"))){d.putAll(map("state","FAILED","error","生成未完成：当前权限或版本已变化，请重新创建导出任务"));store.state(j,d);}}
    public Map<String,Object> status(Long project,Long id,Long actor){store.lock();HrmPayrollGovernanceRecordDO j=job(project,id,actor);Map<String,Object>d=store.data(j);Map<String,Object>p=projected(j,actor);need(Objects.equals(d.get("authorizationStamp"),stamp(j,p)));return map("id",j.getId(),"state",d.get("state"),"error",d.get("error"),"label",p.get("label"),"releaseId",d.get("releaseId"));}
    public byte[] claim(Long project,Long id,Long actor){store.lock();HrmPayrollGovernanceRecordDO j=job(project,id,actor);Map<String,Object>d=store.data(j);Map<String,Object>p=projected(j,actor);need("READY".equals(d.get("state"))&&Objects.equals(d.get("authorizationStamp"),stamp(j,p)));return Base64.getDecoder().decode(String.valueOf(d.get("bytes")));}
    /** Bounded detail rows keep all characters and stable parent references within the six sheets. */
    static List<String> chunks(String text){
        List<String> out=new ArrayList<>();
        for(int at=0;at<text.length();){
            int end=Math.min(at+180,text.length()),newlines=0;
            for(int n=at;n<end;n++)if(text.charAt(n)=='\n'&&++newlines==5){end=n+1;break;}
            if(end<text.length()&&Character.isHighSurrogate(text.charAt(end-1)))end--;
            out.add(text.substring(at,end));at=end;
        }
        if(out.isEmpty())out.add("");return out;
    }
    private static float detailHeight(String text){
        int lines=0;for(String line:text.split("\n",-1))lines+=Math.max(1,(int)Math.ceil(line.length()/34.0));
        return Math.max(30,Math.min(180,lines*15+12));
    }
    public static byte[] workbook(Map<String,Object> projected,Long exporter) throws IOException {
        try(XSSFWorkbook book=new XSSFWorkbook();ByteArrayOutputStream output=new ByteArrayOutputStream()){
            Font bodyFont=book.createFont();bodyFont.setFontName("Microsoft YaHei");bodyFont.setFontHeightInPoints((short)11);
            CellStyle body=book.createCellStyle();body.setFont(bodyFont);body.setWrapText(true);body.setVerticalAlignment(VerticalAlignment.TOP);
            Font headingFont=book.createFont();headingFont.setFontName("Microsoft YaHei");headingFont.setFontHeightInPoints((short)11);headingFont.setBold(true);headingFont.setColor(IndexedColors.WHITE.getIndex());
            CellStyle heading=book.createCellStyle();heading.cloneStyleFrom(body);heading.setFont(headingFont);heading.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());heading.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Map<String,List<Map<String,Object>>> tables=(Map<String,List<Map<String,Object>>>)projected.get("sheets");
            for(String title:HrmPayrollGovernanceProjectionService.SHEETS){
                List<Map<String,Object>> records=new ArrayList<>(tables.get(title));
                if(title.equals(HrmPayrollGovernanceProjectionService.SHEETS.get(0)))records.add(map("记录类型","SUMMARY","记录号","EXPORT-METADATA","本次导出人ID",String.valueOf(exporter),"本次生成时间",now(),"文件完整性",projected.get("label"),"长文本说明","每个业务记录后列出关联明细行。按所属记录及字段名筛选，按片段序号拼接可还原完整正文；统计中的需求数量不含明细行。"));
                List<String> columns=Arrays.asList("记录类型","记录号","所属记录","字段名","片段序号","片段总数","值类型","内容");
                Sheet sheet=book.createSheet(title);sheet.createFreezePane(3,1);sheet.setDefaultRowHeightInPoints(30);
                Row header=sheet.createRow(0);header.setHeightInPoints(30);
                int[] widths={14,24,24,28,9,9,10,72};
                for(int col=0;col<columns.size();col++){Cell cell=header.createCell(col);cell.setCellValue(columns.get(col));cell.setCellStyle(heading);sheet.setColumnWidth(col,widths[col]*256);}
                int rowNo=1;
                for(Map<String,Object> record:records){
                    String ref=String.valueOf(record.get("记录号"));
                    Row parent=sheet.createRow(rowNo++);parent.setHeightInPoints(30);
                    Object[] summary={record.get("记录类型"),ref,ref,"",null,null,"RECORD","本记录完整字段见下方明细；按所属记录筛选可单独审阅。"};
                    writeCells(parent,summary,body);
                    for(Map.Entry<String,Object> field:record.entrySet()){
                        Object value=field.getValue();String type=value==null?"NULL":value instanceof String?"TEXT":value instanceof Number?"NUMBER":value instanceof Boolean?"BOOLEAN":"JSON";
                        String text=value==null?"":value instanceof String?(String)value:JsonUtils.toJsonString(value);
                        List<String> parts=chunks(text);
                        for(int n=0;n<parts.size();n++){
                            if(rowNo>=1048576)throw new IOException("导出明细超过单表容量，请缩小筛选范围后重试");
                            Row detail=sheet.createRow(rowNo++);
                            detail.setHeightInPoints(detailHeight(parts.get(n)));
                            writeCells(detail,new Object[]{"DETAIL",ref+"/"+field.getKey()+"/"+(n+1),ref,field.getKey(),n+1,parts.size(),type,parts.get(n)},body);
                        }
                    }
                }
                if(rowNo>1)sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0,rowNo-1,0,7));
                sheet.getPrintSetup().setLandscape(true);sheet.getPrintSetup().setPaperSize(PrintSetup.A3_PAPERSIZE);sheet.setFitToPage(true);sheet.getPrintSetup().setFitWidth((short)1);sheet.getPrintSetup().setFitHeight((short)0);sheet.setRepeatingRows(new org.apache.poi.ss.util.CellRangeAddress(0,0,-1,-1));
            }
            book.write(output);return output.toByteArray();
        }
    }
    private static void writeCells(Row row,Object[] values,CellStyle style){
        for(int n=0;n<values.length;n++){Cell cell=row.createCell(n);cell.setCellStyle(style);Object v=values[n];if(v instanceof Number)cell.setCellValue(((Number)v).doubleValue());else if(v!=null)cell.setCellValue(String.valueOf(v));}
    }

}
