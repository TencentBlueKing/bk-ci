/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.store.common.service.action.impl

import com.tencent.devops.common.api.auth.REFERER
import com.tencent.devops.common.api.util.ThreadLocalUtil
import com.tencent.devops.common.util.RegexUtils
import com.tencent.devops.common.web.utils.BkApiUtil
import javax.annotation.Priority
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Primary
@Component
@Priority(Int.MAX_VALUE)
@Suppress("UNUSED")
class TxFirstStoreHostDecorateImpl : AbstractStoreHostDecorateImpl() {

    @Value("\${bkrepo.staticRepoPrefixUrl:#{null}}")
    val staticRepoPrefixUrl: String? = null

    @Value("\${bkrepo.devxStaticRepoPrefixUrl:#{null}}")
    val devxStaticRepoPrefixUrl: String? = null

    override fun handleHostBus(str: String): String {
        // 获取请求来源
        val referer = BkApiUtil.getHttpServletRequest()?.getHeader(REFERER) ?: ThreadLocalUtil.get(REFERER)?.toString()
        val hostReplaceFlag = if (!referer.isNullOrBlank() && !devxStaticRepoPrefixUrl.isNullOrBlank()) {
            // 判断请求来源的域名是否是devx环境的域名
            val host = RegexUtils.splitDomainContextPath(devxStaticRepoPrefixUrl!!)!!.first
            referer.contains(host)
        } else {
            false
        }
        if (hostReplaceFlag && !staticRepoPrefixUrl.isNullOrBlank()) {
            // 进行域名替换
            return str.replace(staticRepoPrefixUrl!!, devxStaticRepoPrefixUrl!!)
        }
        return str
    }
}
