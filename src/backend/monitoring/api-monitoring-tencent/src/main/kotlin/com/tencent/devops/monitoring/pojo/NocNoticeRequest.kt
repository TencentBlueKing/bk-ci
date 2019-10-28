package com.tencent.devops.monitoring.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("noc语音告警通知请求体")
data class NocNoticeRequest(
    @ApiModelProperty("app标识", required = true)
    @JsonProperty("app_code")
    val appCode: String,
    @ApiModelProperty("app私密key", required = true)
    @JsonProperty("app_secret")
    val appSecret: String,
    @ApiModelProperty("操作者RTX英文名", required = true)
    @JsonProperty("operator")
    var operator: String,
    @ApiModelProperty("父流程的ProcessName", required = false)
    @JsonProperty("parent_process_name")
    val parentProcessName: String? = null,
    @ApiModelProperty("父流程的InstanceId", required = false)
    @JsonProperty("parent_instance_id")
    val parentInstanceId: String? = null,
    @ApiModelProperty("自动语音读字信息", required = false)
    @JsonProperty("auto_read_message")
    val autoReadMessage: String? = null,
    @ApiModelProperty("自动语音电话自定义回复选项", required = false)
    @JsonProperty("key_options")
    val keyOptions: Map<String, String>? = null,
    @ApiModelProperty("任务头描述", required = false)
    @JsonProperty("head_desc")
    val headDesc: String? = null,
    @ApiModelProperty("需要展示的业务故障数据", required = true)
    @JsonProperty("busi_data_list")
    val busiDataList: List<NocNoticeBusData>,
    @ApiModelProperty("待通知的用户列表", required = true)
    @JsonProperty("user_list_information")
    val userInfoList: List<NocNoticeUserInfo>,
    @ApiModelProperty("具体通知给用户的信息", required = false)
    @JsonProperty("notice_information")
    val noticeInformation: String? = null,
    @ApiModelProperty("任务最下方提供的附加注解信息", required = false)
    @JsonProperty("append_comment")
    val appendComment: String? = null
)