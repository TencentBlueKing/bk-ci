package com.tencent.devops.environment.dao.thirdpartyagent

import com.tencent.devops.environment.model.AgentInstallSession
import com.tencent.devops.environment.model.AgentInstallSessionMode
import com.tencent.devops.environment.model.AgentInstallSessionNode
import com.tencent.devops.environment.model.AgentInstallSessionNodeStatus
import com.tencent.devops.environment.model.AgentInstallSessionStatus
import com.tencent.devops.environment.model.AgentInstallSessionTag
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class AgentInstallSessionDao {

    fun getById(dslContext: DSLContext, sessionId: String): AgentInstallSession? =
        selectSessions(dslContext)
            .where(SessionTable.ID.eq(sessionId))
            .fetchOne(::mapSession)

    fun getById(
        dslContext: DSLContext,
        projectId: String,
        sessionId: String
    ): AgentInstallSession? = selectSessions(dslContext)
        .where(SessionTable.ID.eq(sessionId))
        .and(SessionTable.PROJECT_ID.eq(projectId))
        .fetchOne(::mapSession)

    fun listByProject(
        dslContext: DSLContext,
        projectId: String,
        offset: Int,
        limit: Int
    ): List<AgentInstallSession> = selectSessions(dslContext)
        .where(SessionTable.PROJECT_ID.eq(projectId))
        .orderBy(SessionTable.CREATED_TIME.desc(), SessionTable.ID.desc())
        .limit(offset, limit)
        .fetch(::mapSession)

    fun countByProject(dslContext: DSLContext, projectId: String): Long = dslContext
        .selectCount()
        .from(SessionTable.TABLE)
        .where(SessionTable.PROJECT_ID.eq(projectId))
        .fetchOne(0, Long::class.java) ?: 0L

    fun findActiveByFingerprint(
        dslContext: DSLContext,
        projectId: String,
        createdBy: String,
        mode: AgentInstallSessionMode,
        targetAgentId: Long?,
        targetNodeId: Long?,
        configFingerprint: String,
        now: LocalDateTime
    ): AgentInstallSession? = selectSessions(dslContext)
        .where(SessionTable.PROJECT_ID.eq(projectId))
        .and(SessionTable.CREATED_BY.eq(createdBy))
        .and(SessionTable.MODE.eq(mode.name))
        .and(nullableLongCondition(SessionTable.TARGET_AGENT_ID, targetAgentId))
        .and(nullableLongCondition(SessionTable.TARGET_NODE_ID, targetNodeId))
        .and(SessionTable.CONFIG_FINGERPRINT.eq(configFingerprint))
        .and(SessionTable.STATUS.eq(AgentInstallSessionStatus.ACTIVE.name))
        .and(SessionTable.EXPIRED_TIME.gt(now))
        .orderBy(SessionTable.CREATED_TIME.desc())
        .limit(1)
        .fetchOne(::mapSession)

    fun getByTokenHash(dslContext: DSLContext, tokenHash: String): AgentInstallSession? =
        selectSessions(dslContext)
            .where(SessionTable.TOKEN_HASH.eq(tokenHash))
            .fetchOne(::mapSession)

    fun create(
        dslContext: DSLContext,
        session: AgentInstallSession,
        tags: Collection<AgentInstallSessionTag>
    ) {
        require(session.parallelTaskCount >= 0) { "parallelTaskCount must not be negative" }
        require(tags.all { it.sessionId == session.id }) { "tag sessionId must match session id" }

        dslContext.transaction { configuration ->
            val tx = DSL.using(configuration)
            insertSession(tx, session)
            tags.forEach { insertTag(tx, it) }
        }
    }

    fun listTags(dslContext: DSLContext, sessionId: String): List<AgentInstallSessionTag> =
        dslContext.select(*SessionTagTable.ALL_FIELDS)
            .from(SessionTagTable.TABLE)
            .where(SessionTagTable.SESSION_ID.eq(sessionId))
            .orderBy(SessionTagTable.TAG_KEY_ID, SessionTagTable.TAG_VALUE_ID)
            .fetch(::mapTag)

    fun markExpired(dslContext: DSLContext, sessionId: String, updatedTime: LocalDateTime): Boolean =
        dslContext.update(SessionTable.TABLE)
            .set(SessionTable.STATUS, AgentInstallSessionStatus.EXPIRED.name)
            .set(SessionTable.UPDATED_TIME, updatedTime)
            .where(SessionTable.ID.eq(sessionId))
            .and(SessionTable.STATUS.eq(AgentInstallSessionStatus.ACTIVE.name))
            .execute() == 1

    fun expireActiveSessions(dslContext: DSLContext, now: LocalDateTime): Int =
        dslContext.update(SessionTable.TABLE)
            .set(SessionTable.STATUS, AgentInstallSessionStatus.EXPIRED.name)
            .set(SessionTable.UPDATED_TIME, now)
            .where(SessionTable.STATUS.eq(AgentInstallSessionStatus.ACTIVE.name))
            .and(SessionTable.EXPIRED_TIME.le(now))
            .execute()

    fun revoke(dslContext: DSLContext, sessionId: String, updatedTime: LocalDateTime): Boolean =
        dslContext.update(SessionTable.TABLE)
            .set(SessionTable.STATUS, AgentInstallSessionStatus.REVOKED.name)
            .set(SessionTable.UPDATED_TIME, updatedTime)
            .where(SessionTable.ID.eq(sessionId))
            .and(SessionTable.STATUS.eq(AgentInstallSessionStatus.ACTIVE.name))
            .execute() == 1

    fun bindSessionAgent(dslContext: DSLContext, node: AgentInstallSessionNode): Boolean =
        dslContext.insertInto(
            SessionNodeTable.TABLE,
            SessionNodeTable.SESSION_ID,
            SessionNodeTable.AGENT_ID,
            SessionNodeTable.NODE_ID,
            SessionNodeTable.STATUS,
            SessionNodeTable.HOSTNAME,
            SessionNodeTable.IP,
            SessionNodeTable.ERROR_MESSAGE,
            SessionNodeTable.STARTED_TIME,
            SessionNodeTable.FINISHED_TIME,
            SessionNodeTable.AGENT_VERSION,
            SessionNodeTable.CREATED_TIME,
            SessionNodeTable.UPDATED_TIME
        ).values(
            node.sessionId,
            node.agentId,
            node.nodeId,
            node.status.name,
            node.hostname,
            node.ip,
            node.errorMessage,
            node.startedTime,
            node.finishedTime,
            node.agentVersion,
            node.createdTime,
            node.updatedTime
        ).onDuplicateKeyIgnore()
            .execute() == 1

    fun listNodes(dslContext: DSLContext, sessionId: String): List<AgentInstallSessionNode> =
        selectNodes(dslContext)
            .where(SessionNodeTable.SESSION_ID.eq(sessionId))
            .orderBy(SessionNodeTable.ID)
            .fetch(::mapNode)

    fun findUnfinishedByAgentId(
        dslContext: DSLContext,
        agentId: Long,
        now: LocalDateTime
    ): AgentInstallSessionNode? = selectNodes(dslContext)
        .where(SessionNodeTable.AGENT_ID.eq(agentId))
        .and(SessionNodeTable.STATUS.notIn(FINISHED_NODE_STATUSES))
        .and(SessionNodeTable.CREATED_TIME.le(now))
        .orderBy(SessionNodeTable.CREATED_TIME.desc(), SessionNodeTable.ID.desc())
        .limit(1)
        .fetchOne(::mapNode)

    fun compareAndSetNodeStatus(
        dslContext: DSLContext,
        sessionId: String,
        agentId: Long,
        expectedStatus: AgentInstallSessionNodeStatus,
        newStatus: AgentInstallSessionNodeStatus,
        updatedTime: LocalDateTime
    ): Boolean {
        val update = dslContext.update(SessionNodeTable.TABLE)
            .set(SessionNodeTable.STATUS, newStatus.name)
            .set(SessionNodeTable.UPDATED_TIME, updatedTime)
        if (newStatus == AgentInstallSessionNodeStatus.INSTALLING) {
            update.set(SessionNodeTable.STARTED_TIME, updatedTime)
        }
        if (newStatus.isFinished()) {
            update.set(SessionNodeTable.FINISHED_TIME, updatedTime)
        } else {
            update.setNull(SessionNodeTable.FINISHED_TIME)
            update.setNull(SessionNodeTable.ERROR_MESSAGE)
        }
        return update.where(SessionNodeTable.SESSION_ID.eq(sessionId))
            .and(SessionNodeTable.AGENT_ID.eq(agentId))
            .and(SessionNodeTable.STATUS.eq(expectedStatus.name))
            .execute() == 1
    }

    fun updateNodeStatus(
        dslContext: DSLContext,
        sessionId: String,
        agentId: Long,
        status: AgentInstallSessionNodeStatus,
        updatedTime: LocalDateTime
    ): Boolean {
        val update = dslContext.update(SessionNodeTable.TABLE)
            .set(SessionNodeTable.STATUS, status.name)
            .set(SessionNodeTable.UPDATED_TIME, updatedTime)
        if (status == AgentInstallSessionNodeStatus.INSTALLING) {
            update.set(SessionNodeTable.STARTED_TIME, updatedTime)
        }
        if (status.isFinished()) {
            update.set(SessionNodeTable.FINISHED_TIME, updatedTime)
        } else {
            update.setNull(SessionNodeTable.FINISHED_TIME)
            update.setNull(SessionNodeTable.ERROR_MESSAGE)
        }
        return update.where(SessionNodeTable.SESSION_ID.eq(sessionId))
            .and(SessionNodeTable.AGENT_ID.eq(agentId))
            .execute() == 1
    }

    fun updateNodeDetails(
        dslContext: DSLContext,
        sessionId: String,
        agentId: Long,
        nodeId: Long?,
        hostname: String?,
        ip: String?,
        errorMessage: String?,
        agentVersion: String?,
        updatedTime: LocalDateTime
    ): Boolean = dslContext.update(SessionNodeTable.TABLE)
        .set(SessionNodeTable.NODE_ID, nodeId)
        .set(SessionNodeTable.HOSTNAME, hostname)
        .set(SessionNodeTable.IP, ip)
        .set(SessionNodeTable.ERROR_MESSAGE, errorMessage)
        .set(SessionNodeTable.AGENT_VERSION, agentVersion)
        .set(SessionNodeTable.UPDATED_TIME, updatedTime)
        .where(SessionNodeTable.SESSION_ID.eq(sessionId))
        .and(SessionNodeTable.AGENT_ID.eq(agentId))
        .execute() == 1

    private fun selectSessions(dslContext: DSLContext) = dslContext.select(*SessionTable.ALL_FIELDS)
        .from(SessionTable.TABLE)

    private fun selectNodes(dslContext: DSLContext) = dslContext.select(*SessionNodeTable.ALL_FIELDS)
        .from(SessionNodeTable.TABLE)

    private fun insertSession(dslContext: DSLContext, session: AgentInstallSession) {
        dslContext.insertInto(
            SessionTable.TABLE,
            SessionTable.ID,
            SessionTable.PROJECT_ID,
            SessionTable.MODE,
            SessionTable.CREATED_BY,
            SessionTable.OS,
            SessionTable.ZONE,
            SessionTable.GATEWAY,
            SessionTable.FILE_GATEWAY,
            SessionTable.INSTALL_TYPE,
            SessionTable.AGENT_TYPE,
            SessionTable.LOGIN_NAME,
            SessionTable.LOGIN_PASSWORD_CIPHER,
            SessionTable.PARALLEL_TASK_COUNT,
            SessionTable.TARGET_AGENT_ID,
            SessionTable.TARGET_NODE_ID,
            SessionTable.CONFIG_FINGERPRINT,
            SessionTable.TOKEN_HASH,
            SessionTable.TOKEN_CIPHER,
            SessionTable.STATUS,
            SessionTable.EXPIRED_TIME,
            SessionTable.PREVIOUS_SESSION_ID,
            SessionTable.CREATED_TIME,
            SessionTable.UPDATED_TIME
        ).values(
            session.id,
            session.projectId,
            session.mode.name,
            session.createdBy,
            session.os,
            session.zone,
            session.gateway,
            session.fileGateway,
            session.installType,
            session.agentType,
            session.loginName,
            session.loginPasswordCipher,
            session.parallelTaskCount,
            session.targetAgentId,
            session.targetNodeId,
            session.configFingerprint,
            session.tokenHash,
            session.tokenCipher,
            session.status.name,
            session.expiredTime,
            session.previousSessionId,
            session.createdTime,
            session.updatedTime
        ).execute()
    }

    private fun insertTag(dslContext: DSLContext, tag: AgentInstallSessionTag) {
        dslContext.insertInto(
            SessionTagTable.TABLE,
            SessionTagTable.SESSION_ID,
            SessionTagTable.TAG_KEY_ID,
            SessionTagTable.TAG_VALUE_ID,
            SessionTagTable.TAG_KEY_NAME,
            SessionTagTable.TAG_VALUE_NAME
        ).values(
            tag.sessionId,
            tag.tagKeyId,
            tag.tagValueId,
            tag.tagKeyName,
            tag.tagValueName
        ).execute()
    }

    private fun mapSession(record: Record) = AgentInstallSession(
        id = record.get(SessionTable.ID)!!,
        projectId = record.get(SessionTable.PROJECT_ID)!!,
        mode = AgentInstallSessionMode.valueOf(record.get(SessionTable.MODE)!!),
        createdBy = record.get(SessionTable.CREATED_BY)!!,
        os = record.get(SessionTable.OS)!!,
        zone = record.get(SessionTable.ZONE),
        gateway = record.get(SessionTable.GATEWAY)!!,
        fileGateway = record.get(SessionTable.FILE_GATEWAY),
        installType = record.get(SessionTable.INSTALL_TYPE)!!,
        agentType = record.get(SessionTable.AGENT_TYPE)!!,
        loginName = record.get(SessionTable.LOGIN_NAME),
        loginPasswordCipher = record.get(SessionTable.LOGIN_PASSWORD_CIPHER),
        parallelTaskCount = record.get(SessionTable.PARALLEL_TASK_COUNT)!!,
        targetAgentId = record.get(SessionTable.TARGET_AGENT_ID),
        targetNodeId = record.get(SessionTable.TARGET_NODE_ID),
        configFingerprint = record.get(SessionTable.CONFIG_FINGERPRINT)!!,
        tokenHash = record.get(SessionTable.TOKEN_HASH)!!,
        tokenCipher = record.get(SessionTable.TOKEN_CIPHER)!!,
        status = AgentInstallSessionStatus.valueOf(record.get(SessionTable.STATUS)!!),
        expiredTime = record.get(SessionTable.EXPIRED_TIME)!!,
        previousSessionId = record.get(SessionTable.PREVIOUS_SESSION_ID),
        createdTime = record.get(SessionTable.CREATED_TIME)!!,
        updatedTime = record.get(SessionTable.UPDATED_TIME)!!
    )

    private fun mapTag(record: Record) = AgentInstallSessionTag(
        sessionId = record.get(SessionTagTable.SESSION_ID)!!,
        tagKeyId = record.get(SessionTagTable.TAG_KEY_ID)!!,
        tagValueId = record.get(SessionTagTable.TAG_VALUE_ID)!!,
        tagKeyName = record.get(SessionTagTable.TAG_KEY_NAME)!!,
        tagValueName = record.get(SessionTagTable.TAG_VALUE_NAME)!!
    )

    private fun mapNode(record: Record) = AgentInstallSessionNode(
        id = record.get(SessionNodeTable.ID),
        sessionId = record.get(SessionNodeTable.SESSION_ID)!!,
        agentId = record.get(SessionNodeTable.AGENT_ID)!!,
        nodeId = record.get(SessionNodeTable.NODE_ID),
        status = AgentInstallSessionNodeStatus.valueOf(record.get(SessionNodeTable.STATUS)!!),
        hostname = record.get(SessionNodeTable.HOSTNAME),
        ip = record.get(SessionNodeTable.IP),
        errorMessage = record.get(SessionNodeTable.ERROR_MESSAGE),
        startedTime = record.get(SessionNodeTable.STARTED_TIME),
        finishedTime = record.get(SessionNodeTable.FINISHED_TIME),
        agentVersion = record.get(SessionNodeTable.AGENT_VERSION),
        createdTime = record.get(SessionNodeTable.CREATED_TIME)!!,
        updatedTime = record.get(SessionNodeTable.UPDATED_TIME)!!
    )

    private fun nullableLongCondition(field: Field<Long>, value: Long?): Condition =
        value?.let(field::eq) ?: field.isNull

    private companion object {
        val FINISHED_NODE_STATUSES = listOf(AgentInstallSessionNodeStatus.SUCCEEDED.name)
    }
}

private object SessionTable {
    const val TABLE_NAME = "T_AGENT_INSTALL_SESSION"
    val TABLE = DSL.table(DSL.name(TABLE_NAME))
    val ID = field("ID", String::class.java)
    val PROJECT_ID = field("PROJECT_ID", String::class.java)
    val MODE = field("MODE", String::class.java)
    val CREATED_BY = field("CREATED_BY", String::class.java)
    val OS = field("OS", String::class.java)
    val ZONE = field("ZONE", String::class.java)
    val GATEWAY = field("GATEWAY", String::class.java)
    val FILE_GATEWAY = field("FILE_GATEWAY", String::class.java)
    val INSTALL_TYPE = field("INSTALL_TYPE", String::class.java)
    val AGENT_TYPE = field("AGENT_TYPE", String::class.java)
    val LOGIN_NAME = field("LOGIN_NAME", String::class.java)
    val LOGIN_PASSWORD_CIPHER = field("LOGIN_PASSWORD_CIPHER", String::class.java)
    val PARALLEL_TASK_COUNT = field("PARALLEL_TASK_COUNT", Int::class.java)
    val TARGET_AGENT_ID = field("TARGET_AGENT_ID", Long::class.java)
    val TARGET_NODE_ID = field("TARGET_NODE_ID", Long::class.java)
    val CONFIG_FINGERPRINT = field("CONFIG_FINGERPRINT", String::class.java)
    val TOKEN_HASH = field("TOKEN_HASH", String::class.java)
    val TOKEN_CIPHER = field("TOKEN_CIPHER", String::class.java)
    val STATUS = field("STATUS", String::class.java)
    val EXPIRED_TIME = field("EXPIRED_TIME", LocalDateTime::class.java)
    val PREVIOUS_SESSION_ID = field("PREVIOUS_SESSION_ID", String::class.java)
    val CREATED_TIME = field("CREATED_TIME", LocalDateTime::class.java)
    val UPDATED_TIME = field("UPDATED_TIME", LocalDateTime::class.java)
    val ALL_FIELDS = arrayOf<Field<*>>(
        ID, PROJECT_ID, MODE, CREATED_BY, OS, ZONE, GATEWAY, FILE_GATEWAY, INSTALL_TYPE, AGENT_TYPE,
        LOGIN_NAME, LOGIN_PASSWORD_CIPHER, PARALLEL_TASK_COUNT, TARGET_AGENT_ID, TARGET_NODE_ID,
        CONFIG_FINGERPRINT, TOKEN_HASH, TOKEN_CIPHER,
        STATUS, EXPIRED_TIME, PREVIOUS_SESSION_ID, CREATED_TIME, UPDATED_TIME
    )

    private fun <T> field(fieldName: String, type: Class<T>) =
        DSL.field(DSL.name(TABLE_NAME, fieldName), type)
}

private object SessionTagTable {
    const val TABLE_NAME = "T_AGENT_INSTALL_SESSION_TAG"
    val TABLE = DSL.table(DSL.name(TABLE_NAME))
    val SESSION_ID = field("SESSION_ID", String::class.java)
    val TAG_KEY_ID = field("TAG_KEY_ID", Long::class.java)
    val TAG_VALUE_ID = field("TAG_VALUE_ID", Long::class.java)
    val TAG_KEY_NAME = field("TAG_KEY_NAME", String::class.java)
    val TAG_VALUE_NAME = field("TAG_VALUE_NAME", String::class.java)
    val ALL_FIELDS = arrayOf<Field<*>>(SESSION_ID, TAG_KEY_ID, TAG_VALUE_ID, TAG_KEY_NAME, TAG_VALUE_NAME)

    private fun <T> field(fieldName: String, type: Class<T>) =
        DSL.field(DSL.name(TABLE_NAME, fieldName), type)
}

private object SessionNodeTable {
    const val TABLE_NAME = "T_AGENT_INSTALL_SESSION_NODE"
    val TABLE = DSL.table(DSL.name(TABLE_NAME))
    val ID = field("ID", Long::class.java)
    val SESSION_ID = field("SESSION_ID", String::class.java)
    val AGENT_ID = field("AGENT_ID", Long::class.java)
    val NODE_ID = field("NODE_ID", Long::class.java)
    val STATUS = field("STATUS", String::class.java)
    val HOSTNAME = field("HOSTNAME", String::class.java)
    val IP = field("IP", String::class.java)
    val ERROR_MESSAGE = field("ERROR_MESSAGE", String::class.java)
    val STARTED_TIME = field("STARTED_TIME", LocalDateTime::class.java)
    val FINISHED_TIME = field("FINISHED_TIME", LocalDateTime::class.java)
    val AGENT_VERSION = field("AGENT_VERSION", String::class.java)
    val CREATED_TIME = field("CREATED_TIME", LocalDateTime::class.java)
    val UPDATED_TIME = field("UPDATED_TIME", LocalDateTime::class.java)
    val ALL_FIELDS = arrayOf<Field<*>>(
        ID, SESSION_ID, AGENT_ID, NODE_ID, STATUS, HOSTNAME, IP, ERROR_MESSAGE, STARTED_TIME, FINISHED_TIME,
        AGENT_VERSION, CREATED_TIME, UPDATED_TIME
    )

    private fun <T> field(fieldName: String, type: Class<T>) =
        DSL.field(DSL.name(TABLE_NAME, fieldName), type)
}
