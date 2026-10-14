package cn.iocoder.yudao.module.system.api.mail;

import cn.iocoder.yudao.module.system.api.mail.dto.MailSendSingleToUserReqDTO;
import cn.iocoder.yudao.module.system.service.mail.MailSendService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

/**
 * 邮件发送 API 实现类
 *
 * @author wangjingyi
 */
@Service
@Validated
public class MailSendApiImpl implements MailSendApi {
    @Resource private cn.iocoder.yudao.module.system.service.mail.MailLogService logs;
    @Override public Integer getSendStatus(Long id) {
        cn.iocoder.yudao.module.system.dal.dataobject.mail.MailLogDO log=logs.getMailLog(id);
        return log==null ? null : log.getSendStatus();
    }


    @Resource
    private MailSendService mailSendService;

    @Override
    public Long sendSingleMailToAdmin(MailSendSingleToUserReqDTO reqDTO) {
        return mailSendService.sendSingleMailToAdmin(reqDTO.getUserId(),
                reqDTO.getToMails(), reqDTO.getCcMails(), reqDTO.getBccMails(),
                reqDTO.getTemplateCode(), reqDTO.getTemplateParams(), reqDTO.getAttachments());
    }

    @Override
    public Long sendSingleMailToMember(MailSendSingleToUserReqDTO reqDTO) {
        return mailSendService.sendSingleMailToMember(reqDTO.getUserId(),
                reqDTO.getToMails(), reqDTO.getCcMails(), reqDTO.getBccMails(),
                reqDTO.getTemplateCode(), reqDTO.getTemplateParams(), reqDTO.getAttachments());
    }

}
