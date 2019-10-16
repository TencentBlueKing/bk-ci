package com.tencent.devops.environment.service.node

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.BkAuthPermission
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.dao.EnvDao
import com.tencent.devops.environment.dao.EnvNodeDao
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.EnvCreateInfo
import com.tencent.devops.environment.pojo.EnvironmentId
import com.tencent.devops.environment.pojo.enums.NodeSource
import com.tencent.devops.model.environment.tables.records.TEnvNodeRecord
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class BkCmdbEnvCreatorImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val envDao: EnvDao,
    private val nodeDao: NodeDao,
    private val envNodeDao: EnvNodeDao,
    private val environmentPermissionService: EnvironmentPermissionService
) : EnvCreator {

    override fun id(): String {
        return NodeSource.CMDB.name
    }

    override fun createEnv(projectId: String, userId: String, envCreateInfo: EnvCreateInfo): EnvironmentId {

        val nodeLongIds = envCreateInfo.nodeHashIds!!.map { HashUtil.decodeIdToLong(it) }
        // 检查 node 权限
        val canUseNodeIds =
            environmentPermissionService.listNodeByPermission(userId, projectId, BkAuthPermission.USE)
        val unauthorizedNodeIds = nodeLongIds.filterNot { canUseNodeIds.contains(it) }
        if (unauthorizedNodeIds.isNotEmpty()) {
            val nodeIdStr = unauthorizedNodeIds.joinToString(",") { HashUtil.encodeLongId(it) }
            throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_INSUFFICIENT_PERMISSIONS,
                defaultMessage = "节点权限不足 [$nodeIdStr]",
                params = arrayOf(nodeIdStr)
            )
        }

        // 检查 node 是否存在
        val existNodes = nodeDao.listByIds(dslContext, projectId, nodeLongIds)
        val existNodeIds = existNodes.map { it.nodeId }.toSet()
        val notExistNodeIds = nodeLongIds.filterNot { existNodeIds.contains(it) }
        if (notExistNodeIds.isNotEmpty()) {
            val nodeIdStr = notExistNodeIds.joinToString(",") { HashUtil.encodeLongId(it) }
            throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_NOT_EXISTS,
                defaultMessage = "节点不存在 [$nodeIdStr]",
                params = arrayOf(nodeIdStr)
            )
        }

        var envId = 0L
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            envId = envDao.create(
                context, userId, projectId, envCreateInfo.name, envCreateInfo.desc,
                envCreateInfo.envType.name, ObjectMapper().writeValueAsString(envCreateInfo.envVars)
            )
            val envNodeList = nodeLongIds.map { TEnvNodeRecord(envId, it, projectId) }
            envNodeDao.batchStoreEnvNode(context, envNodeList)
            environmentPermissionService.createEnv(userId, projectId, envId, envCreateInfo.name)
        }
        return EnvironmentId(HashUtil.encodeLongId(envId))
    }
}