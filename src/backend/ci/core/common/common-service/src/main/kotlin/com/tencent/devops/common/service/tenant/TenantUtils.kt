package com.tencent.devops.common.service.tenant

import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

class TenantUtils : ApplicationContextAware, InitializingBean {
    private var applicationContext: ApplicationContext? = null


    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
    }

    override fun afterPropertiesSet() {
        enableMultiTenantMode =
            applicationContext!!.environment.getProperty("bk.enableMultiTenantMode", Boolean::class.java) == true
    }

    companion object {
        private var enableMultiTenantMode: Boolean = false
        private const val DEFAULT_TENANT_ID = "default"
        fun isMultiTenantMode(): Boolean {
            return enableMultiTenantMode
        }

        fun getTenantId(tenantId: String? = null): String {
            return if (enableMultiTenantMode) {
                tenantId ?: DEFAULT_TENANT_ID
            } else {
                DEFAULT_TENANT_ID
            }
        }
    }
}