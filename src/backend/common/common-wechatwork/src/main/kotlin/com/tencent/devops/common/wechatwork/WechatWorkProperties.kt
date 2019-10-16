package com.tencent.devops.common.wechatwork

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class WechatWorkProperties {
    @Value("\${wechatWork.dev.corpId:#{null}}")
    val devCorpId: String? = null
    @Value("\${wechatWork.dev.serviceId:#{null}}")
    val devServiceId: String? = null
    @Value("\${wechatWork.dev.secret:#{null}}")
    val devSecret: String? = null
    @Value("\${wechatWork.dev.token:#{null}}")
    val devToken: String? = null
    @Value("\${wechatWork.dev.aesKey:#{null}}")
    val devAesKey: String? = null
    @Value("\${wechatWork.dev.url:#{null}}")
    val devUrl: String? = null
    @Value("\${wechatWork.prod.corpId:#{null}}")
    val prodCorpId: String? = null
    @Value("\${wechatWork.prod.serviceId:#{null}}")
    val prodServiceId: String? = null
    @Value("\${wechatWork.prod.secret:#{null}}")
    val prodSecret: String? = null
    @Value("\${wechatWork.prod.token:#{null}}")
    val prodToken: String? = null
    @Value("\${wechatWork.prod.aesKey:#{null}}")
    val prodAesKey: String? = null
    @Value("\${wechatWork.prod.url:#{null}}")
    val prodUrl: String? = null
}
