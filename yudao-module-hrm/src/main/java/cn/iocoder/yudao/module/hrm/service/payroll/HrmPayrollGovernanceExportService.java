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
    /** Long text uses continuation columns in the SAME record, preserving row counts and every character. */
    static List<String> chunks(String text){List<String> out=new ArrayList<>();for(int at=0;at<text.length();){int end=Math.min(at+600,text.length());if(end<text.length()&&Character.isHighSurrogate(text.charAt(end-1)))end--;out.add(text.substring(at,end));at=end;}if(out.isEmpty())out.add("");return out;}
    public static byte[] workbook(Map<String,Object> projected,Long exporter) throws IOException {
        try(XSSFWorkbook book=new XSSFWorkbook();ByteArrayOutputStream output=new ByteArrayOutputStream()){
            Font bodyFont=book.createFont();bodyFont.setFontName("Microsoft YaHei");bodyFont.setFontHeightInPoints((short)11);
            CellStyle body=book.createCellStyle();body.setFont(bodyFont);body.setWrapText(true);body.setVerticalAlignment(VerticalAlignment.TOP);
            Font headingFont=book.createFont();headingFont.setFontName("Microsoft YaHei");headingFont.setFontHeightInPoints((short)11);headingFont.setBold(true);headingFont.setColor(IndexedColors.WHITE.getIndex());
            CellStyle heading=book.createCellStyle();heading.cloneStyleFrom(body);heading.setFont(headingFont);heading.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());heading.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Map<String,List<Map<String,Object>>> tables=(Map<String,List<Map<String,Object>>>)projected.get("sheets");
            for(String title:HrmPayrollGovernanceProjectionService.SHEETS){
                List<Map<String,Object>> records=new ArrayList<>(tables.get(title));if(title.equals(HrmPayrollGovernanceProjectionService.SHEETS.get(0)))records.add(map("记录类型","SUMMARY","记录号","EXPORT-METADATA","本次导出人ID",String.valueOf(exporter),"本次生成时间",now(),"文件完整性",projected.get("label"),"长文本说明","长文本在同一记录的续列保存，可横向查看；不截断正文，不增加第七张工作表。"));
                List<Map<String,Object>> rows=new ArrayList<>();LinkedHashSet<String> columns=new LinkedHashSet<>(Arrays.asList("记录类型","记录号"));
                for(Map<String,Object> record:records){Map<String,Object> expanded=new LinkedHashMap<>();for(Map.Entry<String,Object> field:record.entrySet()){
                    Object value=field.getValue();if(value instanceof Number||value instanceof Boolean||value==null)expanded.put(field.getKey(),value);
                    else{String text=value instanceof String?(String)value:JsonUtils.toJsonString(value);List<String> parts=chunks(text);for(int n=0;n<parts.size();n++)expanded.put(field.getKey()+(n==0?"":"（续"+(n+1)+"）"),parts.get(n));}
                }rows.add(expanded);columns.addAll(expanded.keySet());}
                Sheet sheet=book.createSheet(title);sheet.createFreezePane(2,1);sheet.setDefaultRowHeightInPoints(36);Row header=sheet.createRow(0);header.setHeightInPoints(34);int col=0;
                for(String name:columns){Cell cell=header.createCell(col);cell.setCellValue(name);cell.setCellStyle(heading);sheet.setColumnWidth(col,Math.min(255*256,(name.matches(".*(正文|闭包|字段|规则|场景|条件|事实|统计|范围|依据|处理|历史|供给|说明).*" )?60:24)*256));col++;}
                int rowNo=1;for(Map<String,Object> record:rows){Row row=sheet.createRow(rowNo++);int height=48;col=0;for(String name:columns){Cell cell=row.createCell(col++);Object value=record.get(name);cell.setCellStyle(body);if(value instanceof Number)cell.setCellValue(((Number)value).doubleValue());else if(value instanceof Boolean)cell.setCellValue((Boolean)value);else if(value!=null){String text=String.valueOf(value);cell.setCellValue(text);height=Math.max(height,(text.length()/25+2)*14);} }row.setHeightInPoints(Math.min(409,height));}
                if(!rows.isEmpty())sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0,rows.size(),0,columns.size()-1));sheet.setHorizontallyCenter(false);sheet.setFitToPage(false);
            }
            book.write(output);return output.toByteArray();
        }
    }
}
