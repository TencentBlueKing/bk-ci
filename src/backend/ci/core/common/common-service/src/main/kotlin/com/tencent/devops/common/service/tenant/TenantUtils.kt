package com.tencent.devops.common.service.tenant

import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.core.env.get

class TenantUtils : ApplicationContextAware, InitializingBean {
    private var applicationContext: ApplicationContext? = null


    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
    }

    override fun afterPropertiesSet() {
        enableMultiTenantMode =
            applicationContext?.environment?.get("bk.enableMultiTenantMode") == "true"
    }

    companion object {
        private var enableMultiTenantMode: Boolean = false
        private const val DEFAULT_TENANT_ID = "default"
        fun isMultiTenantMode(): Boolean {
            return enableMultiTenantMode
        }

        fun getTenantId(tenantId: String? = null): String {
            return if (enableMultiTenantMode && !tenantId.isNullOrBlank()) {
                tenantId
            } else {
                DEFAULT_TENANT_ID
            }
        }

        fun parseEnglishName(tenantId: String? = null, tenantEnglishName: String): String {
            return if (enableMultiTenantMode && !tenantId.isNullOrBlank()) {
                "$tenantId.$tenantEnglishName"
            } else {
                tenantEnglishName
            }
        }
    }
}