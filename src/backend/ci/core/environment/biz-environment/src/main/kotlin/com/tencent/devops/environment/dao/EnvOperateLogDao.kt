package com.tencent.devops.environment.dao;

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.environment.pojo.envOperate.EnvOperateContent
import com.tencent.devops.environment.pojo.envOperate.EnvOperateLog
import com.tencent.devops.environment.pojo.envOperate.EnvOperateName
import com.tencent.devops.environment.pojo.envOperate.EnvOperateOrigin
import com.tencent.devops.model.environment.tables.TEnvOperateLog
import com.tencent.devops.model.environment.tables.records.TEnvOperateLogRecord
import org.jooq.DSLContext
import org.jooq.JSON
import org.jooq.RecordMapper
import org.springframework.stereotype.Repository;

@Repository
class EnvOperateLogDao {
    fun addOperateLog(
        dslContext: DSLContext,
        projectId: String,
        envId: Long,
        operateOrigin: EnvOperateOrigin,
        operateName: EnvOperateName,
        operateContent: EnvOperateContent?,
        operator: String
    ) {
        with(TEnvOperateLog.T_ENV_OPERATE_LOG) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                ENV_ID,
                OPERATE_ORIGIN,
                OPERATE_NAME,
                OPERATE_CONTENT,
                CREATED_USER
            ).values(
                projectId,
                envId,
                operateOrigin.name,
                operateName.name,
                operateContent?.let {
                    JSON.json(JsonUtil.toJson(operateContent, false))
                },
                operator
            ).execute()
        }
    }

    fun deleteByIds(
        dslContext: DSLContext,
        ids: List<Long>,
    ) {
        if (ids.isEmpty()) return
        with(TEnvOperateLog.T_ENV_OPERATE_LOG) {
            dslContext.deleteFrom(this).where(ID.`in`(ids)).execute()
        }
    }

    fun getAllEnvIds(
        dslContext: DSLContext,
    ): List<Long> {
        with(TEnvOperateLog.T_ENV_OPERATE_LOG) {
            return dslContext.selectDistinct(ENV_ID).from(this).fetch(ENV_ID)
        }
    }

    fun getOldLogIds(
        dslContext: DSLContext,
        beforeDate: java.time.LocalDateTime,
    ): List<Long> {
        with(TEnvOperateLog.T_ENV_OPERATE_LOG) {
            return dslContext.select(ID).from(this)
                .where(CREATE_TIME.lt(beforeDate))
                .fetch(ID)
        }
    }

    fun getExcessLogIds(
        dslContext: DSLContext,
        envId: Long,
        keepCount: Int,
    ): List<Long> {
        with(TEnvOperateLog.T_ENV_OPERATE_LOG) {
            val totalCount =
                dslContext.selectCount().from(this).where(ENV_ID.eq(envId)).fetchOne(0, Int::class.java) ?: 0
            if (totalCount <= keepCount) return emptyList()
            return dslContext.select(ID).from(this)
                .where(ENV_ID.eq(envId))
                .orderBy(CREATE_TIME.asc())
                .limit(totalCount - keepCount)
                .fetch(ID)
        }
    }

    fun countOperateLog(
        dslContext: DSLContext,
        projectId: String,
        envId: Long,
        operator: String?
    ):Long {
        with(TEnvOperateLog.T_ENV_OPERATE_LOG) {
            val dsl = dslContext.selectCount().from(this).where(PROJECT_ID.eq(projectId)).and(ENV_ID.eq(envId))
            operator?.let { dsl.and(CREATED_USER.eq(operator)) }
            return dsl.fetchAny(0, Long::class.java) ?: 0L
        }
    }

    fun fetchOperateLog(
        dslContext: DSLContext,
        projectId: String,
        envId: Long,
        operator: String?,
        limit: Int,
        offset: Int
    ): List<EnvOperateLog> {
        with(TEnvOperateLog.T_ENV_OPERATE_LOG) {
            val dsl = dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId)).and(ENV_ID.eq(envId))
            operator?.let { dsl.and(CREATED_USER.eq(operator)) }
            return dsl.limit(limit).offset(offset).fetch(mapper)
        }
    }

    companion object {
        private val mapper = EnvOperateLogRecordMapper()
    }
}

class EnvOperateLogRecordMapper :
    RecordMapper<TEnvOperateLogRecord, EnvOperateLog> {
    override fun map(record: TEnvOperateLogRecord?): EnvOperateLog? {
        return record?.let {
            EnvOperateLog(
                id = it.id,
                projectId = it.projectId,
                envId = it.envId,
                operateOrigin = EnvOperateOrigin.valueOf(it.operateOrigin),
                operateName = EnvOperateName.valueOf(it.operateName),
                operateContent = it.operateContent?.let { c ->
                    JsonUtil.to(
                        c.data(),
                        object : TypeReference<EnvOperateContent>() {}
                    )
                },
                operator = it.createdUser,
                createTime = it.createTime
            )
        }
    }
}