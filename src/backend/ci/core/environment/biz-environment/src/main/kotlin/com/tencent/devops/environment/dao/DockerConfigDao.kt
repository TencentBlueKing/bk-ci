package com.tencent.devops.environment.dao

import com.tencent.devops.common.pipeline.type.agent.AgentDockerConfig
import com.tencent.devops.environment.pojo.DockerConfigScope
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class DockerConfigDao {
    private fun table(scope: DockerConfigScope) = DSL.table(DSL.name(
        if (scope == DockerConfigScope.ENV) "T_ENV_DOCKER_CONFIG" else "T_NODE_DOCKER_CONFIG"
    ))
    private val project = DSL.field(DSL.name("PROJECT_ID"), String::class.java)
    private val id = DSL.field(DSL.name("RESOURCE_ID"), Long::class.java)
    private val cpus = DSL.field(DSL.name("CPUS"), String::class.java)
    private val memory = DSL.field(DSL.name("MEMORY"), String::class.java)
    private val runtime = DSL.field(DSL.name("CONTAINER_RUNTIME"), String::class.java)
    private val user = DSL.field(DSL.name("UPDATED_USER"), String::class.java)
    private val time = DSL.field(DSL.name("UPDATED_TIME"), LocalDateTime::class.java)

    fun list(
        context: DSLContext, scope: DockerConfigScope, projectId: String, ids: Collection<Long>
    ): Map<Long, AgentDockerConfig> {
        if (ids.isEmpty()) return emptyMap()
        return ids.chunked(500).flatMap { batch ->
            context.selectFrom(table(scope)).where(project.eq(projectId)).and(id.`in`(batch)).fetch()
        }.associate { row ->
            row.get(id)!! to AgentDockerConfig(
                row.get(cpus), row.get(memory),
                if (scope == DockerConfigScope.NODE) row.get(runtime) ?: "docker" else "docker"
            )
        }
    }

    fun save(
        context: DSLContext, scope: DockerConfigScope, projectId: String, resourceId: Long,
        config: AgentDockerConfig, userId: String
    ) {
        val target = table(scope)
        if (config.cpus == null && config.memory == null && config.containerRuntime == "docker") {
            context.deleteFrom(target).where(project.eq(projectId)).and(id.eq(resourceId)).execute()
            return
        }
        val values = linkedMapOf<org.jooq.Field<*>, Any?>(
            project to projectId, id to resourceId, cpus to config.cpus, memory to config.memory,
            user to userId, time to LocalDateTime.now()
        )
        if (scope == DockerConfigScope.NODE) values[runtime] = config.containerRuntime
        context.insertInto(target).set(values).onDuplicateKeyUpdate().set(values).execute()
    }
}
