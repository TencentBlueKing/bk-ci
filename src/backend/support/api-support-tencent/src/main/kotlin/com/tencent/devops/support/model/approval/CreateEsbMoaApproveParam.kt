package com.tencent.devops.support.model.approval

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
/**
 * ESB创建MOA审批单参数
 * @author: carlyin
 * @since: 2019-09-03
 * @version: $Revision$ $Date$ $LastChangedBy$
 *.
 */
@ApiModel("ESB创建MOA审批单参数")
open class CreateEsbMoaApproveParam(
    @ApiModelProperty("app标识", required = true)
    @JsonProperty("app_code")
    val appCode: String = "",
    @ApiModelProperty("app私密key", required = true)
    @JsonProperty("app_secret")
    val appSecret: String = "",
    @ApiModelProperty("用户access_token", required = false)
    @JsonProperty("access_token")
    var accessToken: String? = null,
    @ApiModelProperty("内部版用户登录态", required = false)
    @JsonProperty("bk_ticket")
    var bkTicket: String? = null,
    @ApiModelProperty("操作者RTX英文名", required = false)
    @JsonProperty("operator")
    var operator: String? = null,
    @ApiModelProperty("审批人，多个以逗号分隔", required = true)
    @JsonProperty("verifier")
    val verifier: String,
    @ApiModelProperty("消息内容", required = true)
    @JsonProperty("title")
    val title: String,
    @ApiModelProperty("任务ID", required = true)
    @JsonProperty("taskid")
    val taskId: String,
    @ApiModelProperty("申请时间", required = true)
    @JsonProperty("start_date")
    val startDate: String,
    @ApiModelProperty("回调URL", required = true)
    @JsonProperty("back_url")
    val backUrl: String,
    @ApiModelProperty("系统URL，用于用户审核时跳转系统查看", required = false)
    @JsonProperty("sys_url")
    val sysUrl: String? = null
)