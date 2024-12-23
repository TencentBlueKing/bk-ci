package com.tencent.devops.notify.wework.pojo

import com.fasterxml.jackson.annotation.JsonProperty

@Suppress("UnnecessaryAbstractClass")
abstract class AbstractSendMessageRequest(
    /**
     *  企业应用的id，整型。企业内部开发，可在应用的设置页面查看；第三方服务商，可通过接口 获取企业授权信息 获取该参数值
     */
    @JsonProperty("agentid")
    @get:JsonProperty("agentid")
    open val agentId: Int,
    @JsonProperty("duplicate_check_interval")
    /**
     *  表示是否重复消息检查的时间间隔，默认1800s，最大不超过4小时
     */
    open val duplicateCheckInterval: Int? = null,
    /**
     *  表示是否开启重复消息检查，0表示否，1表示是，默认0
     */
    @JsonProperty("enable_duplicate_check")
    @get:JsonProperty("enable_duplicate_check")
    open val enableDuplicateCheck: Int? = null,
    @JsonProperty("msgtype")
    @get:JsonProperty("msgtype")
    open val msgType: String,
    /**
     *  表示是否是保密消息，0表示可对外分享，1表示不能分享且内容显示水印，默认为0
     */
    @JsonProperty("safe")
    @get:JsonProperty("safe")
    open val safe: Int? = null,
    /**
     *  指定接收消息的部门，部门ID列表，多个接收者用‘|’分隔，最多支持100个。
     *  当touser为”@all”时忽略本参数
     */
    @JsonProperty("toparty")
    @get:JsonProperty("toparty")
    open val toParty: String = "",
    /**
     * 指定接收消息的标签，标签ID列表，多个接收者用‘|’分隔，最多支持100个。
     * 当touser为”@all”时忽略本参数
     */
    @JsonProperty("totag")
    @get:JsonProperty("totag")
    open val toTag: String = "",
    /**
     *  指定接收消息的成员，成员ID列表（多个接收者用‘|’分隔，最多支持1000个）。
     *  特殊情况：指定为”@all”，则向该企业应用的全部成员发送
     */
    @JsonProperty("touser")
    @get:JsonProperty("touser")
    open val toUser: String = ""
) {
    data class MediaMessageContent(
        /**
         *  媒体文件id，可以调用上传临时素材接口获取
         */
        @JsonProperty("media_id")
        @get:JsonProperty("media_id")
        val mediaId: String,
        /**
         *  视频消息的标题，不超过128个字节，超过会自动截断，视频才有
         */
        @JsonProperty("description")
        @get:JsonProperty("description")
        var description: String? = null,
        /**
         *  视频消息的描述，不超过512个字节，超过会自动截断，视频才有
         */
        @JsonProperty("title")
        @get:JsonProperty("title")
        var title: String? = null
    )
}
