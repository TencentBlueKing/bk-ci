package com.tencent.devops.environment.permission.service.impl

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.code.EnvironmentAuthServiceCode
import com.tencent.devops.environment.dao.EnvDao
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.permission.AbstractEnvironmentPermissionService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory

class V3EnvironmentPermissionService constructor(
    private val dslContext: DSLContext,
    private val envDao: EnvDao,
    private val nodeDao: NodeDao,
    authResourceApi: AuthResourceApi,
    authPermissionApi: AuthPermissionApi,
    environmentAuthServiceCode: EnvironmentAuthServiceCode
) : AbstractEnvironmentPermissionService(
    authResourceApi = authResourceApi,
    authPermissionApi = authPermissionApi,
    environmentAuthServiceCode = environmentAuthServiceCode
) {
    override fun supplierForEnvFakePermission(projectId: String): () -> MutableList<String> {
        return { mutableListOf() }
    }

    override fun supplierForNodeFakePermission(projectId: String): () -> MutableList<String> {
        return { mutableListOf() }
    }

    // 解密后
    override fun listEnvByPermission(userId: String, projectId: String, permission: AuthPermission): Set<Long> {
        val resourceInstances = authPermissionApi.getUserResourceByPermission(
            user = userId,
            serviceCode = environmentAuthServiceCode,
            resourceType = envResourceType,
            projectCode = projectId,
            permission = permission,
            supplier = supplierForEnvFakePermission(projectId)
        )

        return getAllEnvInstance(resourceInstances, projectId, userId).map { HashUtil.decodeIdToLong(it) }.toSet()
    }

    // 加密
    override fun listEnvByPermissions(
        userId: String,
        projectId: String,
        permissions: Set<AuthPermission>
    ): Map<AuthPermission, List<String>> {
        val instanceResourcesMap = authPermissionApi.getUserResourcesByPermissions(
            user = userId,
            serviceCode = environmentAuthServiceCode,
            resourceType = envResourceType,
            projectCode = projectId,
            permissions = permissions,
            supplier = supplierForEnvFakePermission(projectId)
        )
        val instanceMap = mutableMapOf<AuthPermission, List<String>>()
        instanceResourcesMap.forEach { (key, value) ->
            val envs = getAllEnvInstance(value, projectId, userId).toList()
            instanceMap[key] = envs.map { it }
        }
        logger.info("listEnvByPermissions v3Impl [$userId] [$projectId] [$instanceMap]")
        return instanceMap
    }

    // 解密后
    override fun listNodeByPermission(userId: String, projectId: String, permission: AuthPermission): Set<Long> {
        val resourceInstances = authPermissionApi.getUserResourceByPermission(
            user = userId,
            serviceCode = environmentAuthServiceCode,
            resourceType = nodeResourceType,
            projectCode = projectId,
            permission = permission,
            supplier = supplierForEnvFakePermission(projectId)
        )

        return getAllNodeInstance(resourceInstances, projectId, userId).map { HashUtil.decodeIdToLong(it) }.toSet()
    }

    // 加密
    override fun listNodeByPermissions(
        userId: String,
        projectId: String,
        permissions: Set<AuthPermission>
    ): Map<AuthPermission, List<String>> {
        val instanceResourcesMap = authPermissionApi.getUserResourcesByPermissions(
            user = userId,
            serviceCode = environmentAuthServiceCode,
            resourceType = nodeResourceType,
            projectCode = projectId,
            permissions = permissions,
            supplier = supplierForEnvFakePermission(projectId)
        )
        val instanceMap = mutableMapOf<AuthPermission, List<String>>()
        instanceResourcesMap.forEach { (key, value) ->
            val nodes = getAllNodeInstance(value, projectId, userId).toList().map {
                logger.info("listNodeByPermissions v3Impl [$it] [${HashUtil.decodeIdToLong(it)}")
                it
            }
            logger.info("listNodeByPermissions v3Impl [$nodes] ")
            instanceMap[key] = nodes
        }
        logger.info("listNodeByPermissions v3Impl [$userId] [$projectId] [$instanceMap]")
        return instanceMap
    }

    // 拿到的数据统一为加密后的id
    private fun getAllNodeInstance(resourceCodeList: List<String>, projectId: String, userId: String): Set<String> {
        val instanceIds = mutableSetOf<String>()
        if (resourceCodeList.contains("*")) {
            logger.info("node getResourceInstance impl, user[$userId], projectId[$projectId], resourceCodeList[$resourceCodeList]")
            val repositoryInfos = nodeDao.listNodes(dslContext, projectId)
            repositoryInfos.map {
                instanceIds.add(HashUtil.encodeLongId(it.nodeId))
            }
            return instanceIds
        }
        resourceCodeList.map { instanceIds.add(it) }
        return instanceIds
    }

    // 拿到的数据统一为加密后的id
    private fun getAllEnvInstance(resourceCodeList: List<String>, projectId: String, userId: String): Set<String> {
        val instanceIds = mutableSetOf<String>()

        if (resourceCodeList.contains("*")) {
            logger.info("env getResourceInstance impl, user[$userId], projectId[$projectId], resourceCodeList[$resourceCodeList]")
            val repositoryInfos = envDao.list(dslContext, projectId)
            repositoryInfos.map {
                instanceIds.add(HashUtil.encodeLongId(it.envId))
            }
            return instanceIds
        }
        resourceCodeList.map { instanceIds.add(it) }
        return instanceIds
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}