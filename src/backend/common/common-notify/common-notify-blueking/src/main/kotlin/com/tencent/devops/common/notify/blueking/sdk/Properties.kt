/*
 *  Copyright (c) 2017 . Tencent 蓝鲸智云(BlueKing)
 */

package com.tencent.devops.common.notify.blueking.sdk

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * 蓝鲸关键配置项
 */
@Component
class Properties {

    @Value("\${app.code:#{null}}")
    val appCode: String? = null
    @Value("\${app.secret:#{null}}")
    val appSecret: String? = null

    @Value("\${bk.paas.host:#{null}}")
    val bkHost: String? = null
}
