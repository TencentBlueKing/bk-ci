package com.tencent.devops.repository.service.impl

import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.CodeAuthServiceCode
import com.tencent.devops.repository.dao.RepositoryDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory

class V3RepositoryPermissionService constructor(
    private val dslContext: DSLContext,
    private val repositoryDao: RepositoryDao,
    private val authResourceApi: AuthResourceApi,
    private val authPermissionApi: AuthPermissionApi,
    private val codeAuthServiceCode: CodeAuthServiceCode
) : AbstractRepositoryPermissionService(
    authResourceApi = authResourceApi,
    authPermissionApi = authPermissionApi,
    codeAuthServiceCode = codeAuthServiceCode
) {

    override fun supplierForFakePermission(projectId: String): () -> MutableList<String> {
        return { mutableListOf() }
    }

    override fun filterRepository(userId: String, projectId: String, authPermission: AuthPermission): List<Long> {
        val resourceCodeList = authPermissionApi.getUserResourceByPermission(
            user = userId,
            serviceCode = codeAuthServiceCode,
            resourceType = AuthResourceType.CODE_REPERTORY,
            projectCode = projectId,
            permission = authPermission,
            supplier = supplierForFakePermission(projectId)
        )

        if (resourceCodeList.contains("*")) {
            return getAllInstance(resourceCodeList, projectId, userId)
        }
        return resourceCodeList.map { it.toLong() }
    }

    override fun filterRepositories(
        userId: String,
        projectId: String,
        authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<Long>> {
        val permissionResourcesMap = authPermissionApi.getUserResourcesByPermissions(
            user = userId,
            serviceCode = codeAuthServiceCode,
            resourceType = AuthResourceType.CODE_REPERTORY,
            projectCode = projectId,
            permissions = authPermissions,
            supplier = supplierForFakePermission(projectId)
        )
        val instanceMap = mutableMapOf<AuthPermission, List<Long>>()

        permissionResourcesMap.forEach { (key, value) ->
            instanceMap[key] = getAllInstance(value, projectId, userId)
        }
        return instanceMap
    }

    private fun getAllInstance(resourceCodeList: List<String>, projectId: String, userId: String): List<Long> {
        if (resourceCodeList.contains("*")) {
            logger.info("repositories getResourceInstance impl, user[$userId], projectId[$projectId], resourceCodeList[$resourceCodeList]")
            val instanceIds = mutableListOf<Long>()
            val repositoryInfos = repositoryDao.listByProject(dslContext, projectId, null)
            repositoryInfos.map {
                instanceIds.add(it.repositoryId)
            }
            return instanceIds
        }
        return resourceCodeList.map { it.toLong() }
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}