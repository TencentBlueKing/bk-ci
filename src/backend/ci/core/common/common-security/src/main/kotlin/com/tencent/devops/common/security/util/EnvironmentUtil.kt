package com.tencent.devops.common.security.util

import org.apache.commons.lang.StringUtils
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
    override fun setApplicationContext(applicationContext: ApplicationContext?) {
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
    }
}