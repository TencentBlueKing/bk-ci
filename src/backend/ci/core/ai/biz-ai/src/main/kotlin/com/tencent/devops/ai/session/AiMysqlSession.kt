package com.tencent.devops.ai.session

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.tencent.devops.ai.dao.AiAgentStateDao
import io.agentscope.core.session.Session
import io.agentscope.core.state.SessionKey
import io.agentscope.core.state.SimpleSessionKey
import io.agentscope.core.state.State
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import java.util.Optional

/**
 * 基于 MySQL 的 Session 存储实现，
 * 用于持久化智能体的对话记忆状态。
 */
class AiMysqlSession(
    private val dslContext: DSLContext,
    private val agentStateDao: AiAgentStateDao
) : Session {

    private val objectMapper = ObjectMapper()
        .registerModule(KotlinModule.Builder().build())
        .registerModule(JavaTimeModule())

    override fun save(
        sessionKey: SessionKey,
        key: String,
        value: State
    ) {
        val sid = sessionKey.toIdentifier()
        val json = objectMapper.writeValueAsString(value)
        logger.debug(
            "[MysqlSession] save: sid={}, key={}, type={}",
            sid, key, value.javaClass.simpleName
        )
        agentStateDao.upsert(dslContext, sid, key, 0, json)
        logger.info(
            "[MysqlSession] Saved state: sid={}, key={}, " +
                "dataLength={}",
            sid, key, json.length
        )
    }

    override fun save(
        sessionKey: SessionKey,
        key: String,
        values: List<State>
    ) {
        val sid = sessionKey.toIdentifier()
        logger.debug(
            "[MysqlSession] saveList: sid={}, key={}, " +
                "count={}",
            sid, key, values.size
        )
        dslContext.transaction { config ->
            val ctx = DSL.using(config)
            agentStateDao.deleteBySessionAndKey(ctx, sid, key)
            if (values.isNotEmpty()) {
                val jsonList = values.map {
                    objectMapper.writeValueAsString(it)
                }
                agentStateDao.batchInsert(
                    ctx, sid, key, jsonList
                )
            }
        }
        if (values.isNotEmpty()) {
            logger.info(
                "[MysqlSession] Saved {} state items: " +
                    "sid={}, key={}",
                values.size, sid, key
            )
        }
    }

    override fun <T : State> get(
        sessionKey: SessionKey,
        key: String,
        type: Class<T>
    ): Optional<T> {
        val sid = sessionKey.toIdentifier()
        val record = agentStateDao.getBySessionKeyAndIndex(
            dslContext, sid, key, 0
        )
        if (record == null) {
            logger.debug(
                "[MysqlSession] get: not found, " +
                    "sid={}, key={}",
                sid, key
            )
            return Optional.empty()
        }
        val value = objectMapper.readValue(
            record.stateData, type
        )
        logger.debug(
            "[MysqlSession] get: found, sid={}, key={}, " +
                "type={}",
            sid, key, type.simpleName
        )
        return Optional.ofNullable(value)
    }

    override fun <T : State> getList(
        sessionKey: SessionKey,
        key: String,
        itemType: Class<T>
    ): List<T> {
        val sid = sessionKey.toIdentifier()
        val result = agentStateDao
            .listBySessionAndKey(dslContext, sid, key)
            .map { record ->
                objectMapper.readValue(
                    record.stateData, itemType
                )
            }
        logger.debug(
            "[MysqlSession] getList: sid={}, key={}, " +
                "count={}",
            sid, key, result.size
        )
        return result
    }

    override fun exists(sessionKey: SessionKey): Boolean {
        val sid = sessionKey.toIdentifier()
        val found = agentStateDao.countBySession(
            dslContext, sid
        ) > 0
        logger.debug(
            "[MysqlSession] exists: sid={}, result={}",
            sid, found
        )
        return found
    }

    override fun delete(sessionKey: SessionKey) {
        val sid = sessionKey.toIdentifier()
        val count = agentStateDao.deleteBySession(
            dslContext, sid
        )
        logger.info(
            "[MysqlSession] Deleted state: sid={}, " +
                "rowsAffected={}",
            sid, count
        )
    }

    override fun listSessionKeys(): Set<SessionKey> {
        val keys = agentStateDao
            .listDistinctSessionIds(dslContext)
            .map { SimpleSessionKey.of(it) }
            .toSet()
        logger.debug(
            "[MysqlSession] listSessionKeys: count={}",
            keys.size
        )
        return keys
    }

    override fun close() {
        logger.debug("[MysqlSession] close() called")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(
            AiMysqlSession::class.java
        )
    }
}
