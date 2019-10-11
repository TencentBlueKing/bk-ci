package com.tencent.devops.log.dao.v2

import com.tencent.devops.model.log.tables.TLogIndicesV2
import com.tencent.devops.model.log.tables.records.TLogIndicesV2Record
import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import sun.misc.MessageUtils.where
import java.time.LocalDateTime

@Repository
class IndexDaoV2 {

    fun create(
        dslContext: DSLContext,
        buildId: String,
        indexName: String,
        enable: Boolean
    ) {
        with(TLogIndicesV2.T_LOG_INDICES_V2) {
            val now = LocalDateTime.now()
            dslContext.insertInto(this,
                BUILD_ID,
                INDEX_NAME,
                LAST_LINE_NUM,
                CREATED_TIME,
                UPDATED_TIME,
                ENABLE
                )
                .values(
                    buildId,
                    indexName,
                    1,
                    now,
                    now,
                    enable
                )
                .onDuplicateKeyIgnore()
                .execute()
        }
    }

    fun getBuild(dslContext: DSLContext, buildId: String): TLogIndicesV2Record? {
        with(TLogIndicesV2.T_LOG_INDICES_V2) {
            return dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .fetchOne()
        }
    }

    fun getIndexName(
        dslContext: DSLContext,
        buildId: String
    ): String? {
        return getBuild(dslContext, buildId)?.indexName
    }

    fun updateLastLineNum(
        dslContext: DSLContext,
        buildId: String,
        size: Int
    ): Long? {
        with(TLogIndicesV2.T_LOG_INDICES_V2) {
            return dslContext.transactionResult { configuration ->
                val context = DSL.using(configuration)
                val record = context.selectFrom(this)
                    .where(BUILD_ID.eq(buildId))
                    .forUpdate()
                    .fetchOne() ?: return@transactionResult null
                if (record.lastLineNum == null || record.lastLineNum <= 0L) {
                    record.lastLineNum = 1L
                }
                val result = record.lastLineNum + size
                val updated = context.update(this)
                    .set(LAST_LINE_NUM, result)
                    .where(BUILD_ID.eq(buildId))
                    .execute()
                if (updated != 1) {
                    logger.warn("[$buildId|$size|$result|$updated] Fail to update the build last line num")
                }
                record.lastLineNum
            }
        }
    }

    fun listLatestBuilds(
        dslContext: DSLContext,
        limit: Int
    ): Result<TLogIndicesV2Record> {
        with(TLogIndicesV2.T_LOG_INDICES_V2) {
            return dslContext.selectFrom(this)
                .orderBy(ID.desc())
                .limit(limit)
                .fetch()
        }
    }

    fun delete(
        dslContext: DSLContext,
        buildIds: Set<String>
    ): Int {
        with(TLogIndicesV2.T_LOG_INDICES_V2) {
            return dslContext.deleteFrom(this)
                .where(BUILD_ID.`in`(buildIds))
                .execute()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(IndexDaoV2::class.java)
    }
}