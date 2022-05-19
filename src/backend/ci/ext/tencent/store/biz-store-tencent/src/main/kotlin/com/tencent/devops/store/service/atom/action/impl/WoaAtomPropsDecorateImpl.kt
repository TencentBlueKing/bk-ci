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

package com.tencent.devops.store.service.atom.action.impl

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import javax.annotation.Priority

@Priority(Int.MAX_VALUE - 1)
@Component
class WoaAtomPropsDecorateImpl : FirstAtomPropsDecorateImpl() {

    override fun decorateSpecial(obj: Map<String, Any>): Map<String, Any> {
        val replaceMap = getReplaceMapByRequest()
        if (replaceMap.isEmpty()) {
            return super.decorateSpecial(obj)
        }
        val (mutable, isChange) = replaceWoa(obj, replaceMap)
        return if (isChange) super.decorateSpecial(mutable) else super.decorateSpecial(obj)
    }

    @Suppress("NestedBlockDepth")
    private fun replaceWoa(
        obj: Map<String, Any>,
        replaceMap: Map<String, String>
    ): Pair<MutableMap<String, Any>, Boolean> {

        var isChange = false
        val mutable = obj.toMutableMap()

        if (replaceMap.isEmpty()) {
            return mutable to isChange
        }

        obj.forEach { (key, entry) ->
            if (key == "url" && entry is String) {
                val newUrl = replaceUrl(entry, replaceMap)
                if (newUrl != entry) {
                    isChange = true
                    LOG.info("$key replace $entry to $newUrl")
                    mutable[key] = newUrl
                }
            } else if (entry is List<*>) {
                entry.forEach {
                    if (it is Map<*, *>) {
                        try {
                            @Suppress("UNCHECKED_CAST")
                            val p = replaceWoa(it as Map<String, Any>, replaceMap)
                            if (p.second) {
                                isChange = true
                                mutable[key] = p.first
                            }
                        } catch (ignore: Exception) {
                            LOG.warn("BKSystemErrorMonitor| cast replaceWoa failed in $entry", ignore)
                        }
                    }
                }
            } else if (entry is Map<*, *>) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    val p = replaceWoa(entry as Map<String, Any>, replaceMap)
                    if (p.second) {
                        isChange = true
                        mutable[key] = p.first
                    }
                } catch (ignore: Exception) {
                    LOG.warn("BKSystemErrorMonitor| cast replaceWoa failed in $entry", ignore)
                }
            }
        }
        return mutable to isChange
    }

    private fun getReplaceMapByRequest(): Map<String, String> {
        val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        if (null != attributes) {
            val request = attributes.request
            request.getHeader("referer")?.let { referer ->
                val replaceMap = MAP.toMutableMap()
                if (referer.trim().startsWith("http://")) { // 如果此时用户访问的不是https，则不做https替换
                    replaceMap.remove("http://")
                }

                if (referer.contains(".oa.com")) { // 如果访问是oa.com 则不做woa替换
                    replaceMap.remove(".oa.com")
                }
                return replaceMap
            }
        }
        return MAP
    }

    companion object {
        private val MAP = mapOf("http://" to "https://", "api.devops" to "devops", ".oa.com" to ".woa.com")
        private val regex = Regex("((http[s]?://)([-a-z0-9A-Z]+\\.)+([w]?oa\\.com)).*")
        private val LOG = LoggerFactory.getLogger(WoaAtomPropsDecorateImpl::class.java)
    }

    fun replaceUrl(url: String, replaceMap: Map<String, String> = MAP): String {
        var result = url
        val matcher = regex.toPattern().matcher(url)
        while (matcher.find()) {
            var tmp = matcher.group(1)
            replaceMap.forEach { replace -> tmp = tmp.replace(replace.key, replace.value) }

            if (tmp != matcher.group(1)) {
                result = result.replaceFirst(matcher.group(1), tmp)
            }
        }
        return result
    }
}
