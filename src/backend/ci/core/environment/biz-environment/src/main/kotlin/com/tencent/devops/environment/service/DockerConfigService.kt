package com.tencent.devops.environment.service

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.pipeline.type.agent.AgentDockerConfig
import com.tencent.devops.environment.dao.DockerConfigDao
import com.tencent.devops.environment.dao.EnvDao
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.BatchDockerConfigRequest
import com.tencent.devops.environment.pojo.DockerConfigRequest
import com.tencent.devops.environment.pojo.DockerConfigScope
import com.tencent.devops.environment.pojo.enums.EnvType
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class DockerConfigService @Autowired constructor(
    private val context: DSLContext,
    private val dao: DockerConfigDao,
    private val nodeDao: NodeDao,
    private val envDao: EnvDao,
    private val permissions: EnvironmentPermissionService
) {
    fun forAgents(projectId: String, nodeIds: Collection<Long>, envId: Long? = null): Map<Long, AgentDockerConfig> {
        val nodes = dao.list(context, DockerConfigScope.NODE, projectId, nodeIds)
        if (envId == null) return nodes
        val env = dao.list(context, DockerConfigScope.ENV, projectId, listOf(envId))[envId] ?: AgentDockerConfig()
        // An environment without limits intentionally DOES NOT inherit the selected node's limits.
        return nodeIds.associateWith { env.copy(containerRuntime = nodes[it]?.containerRuntime ?: "docker") }
    }

    fun get(userId: String, projectId: String, scope: DockerConfigScope, id: Long): AgentDockerConfig {
        checkPermission(userId, projectId, scope, id, AuthPermission.VIEW)
        return dao.list(context, scope, projectId, listOf(id))[id] ?: AgentDockerConfig()
    }

    fun save(userId: String, projectId: String, scope: DockerConfigScope, id: Long, request: DockerConfigRequest) {
        request.validate()
        checkPermission(userId, projectId, scope, id, AuthPermission.EDIT)
        context.transaction { configuration ->
            val tx = DSL.using(configuration)
            checkScope(tx, projectId, scope, id, request)
            apply(tx, userId, projectId, scope, id, request)
        }
    }

    fun batch(userId: String, projectId: String, request: BatchDockerConfigRequest): Int {
        require(request.nodeIds.isNotEmpty() && request.nodeIds.size <= 500) { "请选择 1 到 500 个节点" }
        val config = request.config
        require(config.cpus != null || config.memory != null) { "请至少修改一项" }
        require(config.containerRuntime == null) { "不支持批量修改容器引擎" }
        config.validate()
        val ids = request.nodeIds.map(HashUtil::decodeIdToLong).sorted()
        ids.forEach { checkPermission(userId, projectId, DockerConfigScope.NODE, it, AuthPermission.EDIT) }
        return context.transactionResult { configuration ->
            val tx = DSL.using(configuration)
            val eligible = ids.filter { id ->
                val node = nodeDao.get(tx, projectId, id)
                node != null && node.nodeType == NodeType.THIRDPARTY.name &&
                    node.osName.equals("LINUX", true) && node.nodeStatus != NodeStatus.DELETED.name
            }
            require(eligible.isNotEmpty()) { "未选择 Linux 构建节点，不支持修改" }
            eligible.forEach {
                checkScope(tx, projectId, DockerConfigScope.NODE, it, config)
                apply(tx, userId, projectId, DockerConfigScope.NODE, it, config)
            }
            eligible.size
        }
    }

    private fun apply(
        tx: DSLContext, userId: String, projectId: String, scope: DockerConfigScope,
        id: Long, request: DockerConfigRequest
    ) {
        val old = dao.list(tx, scope, projectId, listOf(id))[id] ?: AgentDockerConfig()
        dao.save(tx, scope, projectId, id, AgentDockerConfig(
            if (request.cpus == null) old.cpus else request.cpus?.takeIf { it.isNotBlank() },
            if (request.memory == null) old.memory else request.memory?.takeIf { it.isNotBlank() },
            request.containerRuntime ?: old.containerRuntime
        ), userId)
    }

    private fun checkPermission(
        userId: String, projectId: String, scope: DockerConfigScope, id: Long, permission: AuthPermission
    ) {
        val allowed = if (scope == DockerConfigScope.ENV) {
            envDao.get(context, projectId, id)
            permissions.checkEnvPermission(userId, projectId, id, permission)
        } else {
            val node = nodeDao.get(context, projectId, id) ?: throw InvalidParamException("Node not found")
            permissions.checkNodePermission(userId, projectId, id, permission,
                if (node.nodeType == NodeType.CREATE.name) AuthResourceType.CREATIVE_STREAM_NODE
                else AuthResourceType.ENVIRONMENT_ENV_NODE)
        }
        if (!allowed) throw PermissionForbiddenException("No permission to access container configuration")
    }

    private fun checkScope(
        tx: DSLContext, projectId: String, scope: DockerConfigScope, id: Long, request: DockerConfigRequest
    ) {
        // Lock the parent even when no optional row exists. Serializes partial updates and deletes.
        val table = if (scope == DockerConfigScope.ENV) "T_ENV" else "T_NODE"
        val key = if (scope == DockerConfigScope.ENV) "ENV_ID" else "NODE_ID"
        tx.selectFrom(DSL.table(DSL.name(table)))
            .where(DSL.field(DSL.name(key), Long::class.java).eq(id))
            .and(DSL.field(DSL.name("PROJECT_ID"), String::class.java).eq(projectId)).forUpdate().fetchOne()
            ?: throw InvalidParamException("Resource not found")
        if (scope == DockerConfigScope.ENV) {
            require(envDao.get(tx, projectId, id).envType == EnvType.BUILD.name) { "Only build environments supported" }
            require(request.containerRuntime == null) { "Container runtime is a node setting" }
        } else {
            val node = nodeDao.get(tx, projectId, id) ?: throw InvalidParamException("Node not found")
            require(node.nodeStatus != NodeStatus.DELETED.name && node.osName.equals("LINUX", true) &&
                node.nodeType in setOf(NodeType.THIRDPARTY.name, NodeType.CREATE.name)) { "Only Linux nodes supported" }
            if (node.nodeType == NodeType.CREATE.name) {
                require(request.cpus == null && request.memory == null) {
                    "Creation nodes only support runtime settings"
                }
            }
        }
    }

    @Scheduled(initialDelay = 60000, fixedDelay = 60000)
    fun cleanDeletedResources() {
        // Bound deletes on sparse tables. All identifiers below are internal constants, never user input.
        listOf(
            Triple("T_NODE_DOCKER_CONFIG", "T_NODE", "NODE_ID"),
            Triple("T_ENV_DOCKER_CONFIG", "T_ENV", "ENV_ID")
        ).forEach { (config, parent, key) ->
            val active = if (key == "NODE_ID") "p.NODE_STATUS <> 'DELETED'" else "p.IS_DELETED = 0"
            context.execute(
                """DELETE FROM $config WHERE NOT EXISTS (
                    SELECT 1 FROM $parent p WHERE p.$key = $config.RESOURCE_ID
                    AND p.PROJECT_ID = $config.PROJECT_ID AND $active
                ) LIMIT 500""".trimIndent()
            )
        }
    }
}
