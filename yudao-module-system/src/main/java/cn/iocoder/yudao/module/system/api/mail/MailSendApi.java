package cn.iocoder.yudao.module.system.api.mail;

import cn.iocoder.yudao.module.system.api.mail.dto.MailSendSingleToUserReqDTO;

import javax.validation.Valid;

/**
 * 邮箱发送 API 接口
 *
 * @author 芋道源码
 */
public interface MailSendApi {
    /** Tenant-scoped gateway send status; null means no matching log. */
    Integer getSendStatus(Long logId);


    /**
     * 发送单条邮箱给 Admin 用户
     *
     * 在 mail 为空时，使用 userId 加载对应 Admin 的邮箱
     *
     * @param reqDTO 发送请求
     * @return 发送日志编号
     */
    Long sendSingleMailToAdmin(@Valid MailSendSingleToUserReqDTO reqDTO);

    /**
     * 发送单条邮箱给 Member 用户
     *
     * 在 mail 为空时，使用 userId 加载对应 Member 的邮箱
     *
     * @param reqDTO 发送请求
     * @return 发送日志编号
     */
    Long sendSingleMailToMember(@Valid MailSendSingleToUserReqDTO reqDTO);

}
