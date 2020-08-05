package com.tencent.bk.codecc.task.vo;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Set;

/**
 * 通知定制视图
 *
 * @version V1.0
 * @date 2019/11/25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("通知定制视图")
public class NotifyCustomVO extends CommonVO
{
    /**
     * rtx接收人类型：0-所有项目成员；1-接口人；2-自定义
     */
    private String rtxReceiverType;

    /**
     * rtx接收人列表，rtxReceiverType=2时，自定义的接收人保存在该字段
     */
    private Set<String> rtxReceiverList;

    /**
     * 邮件收件人类型：0-所有项目成员；1-接口人；2-自定义
     */
    private String emailReceiverType;

    /**
     * 邮件收件人列表，当emailReceiverType=2时，自定义的收件人保存在该字段
     */
    private Set<String> emailReceiverList;

    /**
     * 邮件抄送人列表
     */
    private Set<String> emailCCReceiverList;

    /**
     * 定时报告任务的状态，有效：1，暂停：2
     */
    private Integer reportStatus;

    /**
     * 定时报告的发送日期:
     */
    private Set<Integer> reportDate;

    /**
     * 定时报告的发送时间，小时位
     */
    private Integer reportTime;

    /**
     * 定时报告分钟，分钟位
     */
    private Integer reportMinute;


    /**
     * 即时报告状态，有效：1，暂停：2
     */
    private String instantReportStatus;

    /*---------群机器人通知start---------*/
    /**
     * 定时报告工具（即邮件需要发送哪些工具的数据报表）
     */
    private Set<String> reportTools;

    /**
     * 群机器人通知地址
     */
    private String botWebhookUrl;

    /**
     * 群机器人通知告警级别，多个级别直接相加
     */
    private Integer botRemindSeverity;

    /**
     * 群机器人通知工具列表
     */
    private Set<String> botRemaindTools;

    /**
     * 群机器人通知范围
     */
    private Integer botRemindRange;
    /*---------群机器人通知end---------*/
}
