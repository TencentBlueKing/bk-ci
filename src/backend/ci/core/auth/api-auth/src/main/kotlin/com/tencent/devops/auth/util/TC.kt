package com.tencent.devops.auth.util

import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.devops.auth.api.service.ServiceDeptResource
import com.tencent.devops.auth.pojo.vo.UserAndDeptInfoVo
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.tenant.TenantUtils
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * Tenant Converter的意思
 */
class TC : ApplicationContextAware, InitializingBean {
    private var applicationContext: ApplicationContext? = null

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
    }

    override fun afterPropertiesSet() {
        client = applicationContext!!.getBean(Client::class.java)
    }

    companion object {
        private var client: Client? = null

        /**
         * 用户ID转换名字
         */
        fun uid2Name(userId: String?, tenantId: String?): String {
            if (userId == null) {
                return "null"
            }
            if (tenantId == null) {
                return userId
            }
            val listUserInfos = listUserInfos(listOf(userId), tenantId)
            return if (listUserInfos.isEmpty()) {
                userId
            } else {
                listUserInfos[0].displayName
            }
        }

        private fun listUserInfos(
            memberIds: List<String>,
            tenantId: String?
        ): List<UserAndDeptInfoVo> {
            return if (TenantUtils.isMultiTenantMode()) {
                client!!.get(ServiceDeptResource::class).listUserInfos(memberIds, tenantId).data ?: emptyList()
            } else {
                memberIds.map {
                    UserAndDeptInfoVo(
                        id = 0,
                        name = it,
                        displayName = it,
                        type = ManagerScopesEnum.USER,
                        deptInfo = emptyList()
                    )
                }
            }
        }
    }
}