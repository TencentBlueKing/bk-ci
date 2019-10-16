package com.tencent.devops.support.model.approval

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
/**
 * MOA审批结单参数
 * @author: carlyin
 * @since: 2019-09-03
 * @version: $Revision$ $Date$ $LastChangedBy$
 *.
 */
@ApiModel("MOA审批结单参数")
open class CreateEsbMoaCompleteParam(
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
    @ApiModelProperty("任务ID", required = true)
    @JsonProperty("task_id")
    val taskId: String
)