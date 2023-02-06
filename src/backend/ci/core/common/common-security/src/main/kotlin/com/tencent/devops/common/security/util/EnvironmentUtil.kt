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

package com.tencent.devops.common.security.util

import org.apache.commons.lang3.StringUtils
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component

@Component
class EnvironmentUtil : ApplicationContextAware {

    /**
     * 实现ApplicationContextAware接口的回调方法，设置上下文环境
     */
    @Throws(BeansException::class)
    override fun setApplicationContext(applicationContext: ApplicationContext) {
        EnvironmentUtil.applicationContext = applicationContext
    }

    companion object {
        // Spring应用上下文环境
        private var applicationContext: ApplicationContext? = null

        // 正式环境
        private var isProdProfileActive: Boolean? = null

        /**
         * 获取activeProfile信息
         * @return String
         */
        fun getActiveProfile(): String {
            return StringUtils.join(applicationContext?.environment?.activeProfiles ?: arrayOf<String>(), ",")
        }

        /**
         * 获取applicationName信息
         * @return String
         */
        fun getApplicationName(): String {
            return applicationContext?.environment?.getProperty("spring.application.name") ?: ""
        }

        /**
         * 获取serverPort信息
         * @return Int
         */
        fun getServerPort(): Int {
            return applicationContext?.environment?.getProperty("server.port")?.toInt() ?: 0
        }

        /**
         * 是否为正式环境
         * @return Boolean
         */
        fun isProdProfileActive(): Boolean {
            if (isProdProfileActive != null) {
                return isProdProfileActive!!
            }
            isProdProfileActive = isProfileActive("prod")
            return isProdProfileActive!!
        }

        /**
         * 判断是否为某个环境
         * @param String 环境名称
         * @return Boolean
         */
        private fun isProfileActive(profile: String): Boolean {
            val activeProfiles: Array<String>? =
                applicationContext?.environment?.activeProfiles
            if (activeProfiles != null && activeProfiles.isNotEmpty()) {
                for (activeProfile in activeProfiles) {
                    if (activeProfile == profile) {
                        return true
                    }
                }
            }
            return false
        }

        /**
         * 获取gateway定义的devopsToken
         */
        fun gatewayDevopsToken(): String? {
            return applicationContext?.environment?.getProperty("auth.gateway.devopsToken")
        }
    }
}
