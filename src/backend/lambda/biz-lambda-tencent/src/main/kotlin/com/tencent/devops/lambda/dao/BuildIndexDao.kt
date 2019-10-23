package com.tencent.devops.lambda.dao

import com.tencent.devops.model.lambda.tables.TLambdaBuildIndices
import com.tencent.devops.model.lambda.tables.records.TLambdaBuildIndicesRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.sql.Timestamp

@Repository
class BuildIndexDao {

    fun create(
        dslContext: DSLContext,
        buildId: String,
        indexName: String
    ) {
        with(TLambdaBuildIndices.T_LAMBDA_BUILD_INDICES) {
            dslContext.insertInto(this,
                BUILD_ID,
                INDEX_NAME)
                .values(
                    buildId,
                    indexName
                )
                .onDuplicateKeyIgnore()
                .execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        buildId: String,
        startTime: Long,
        endTime: Long
    ) {
        with(TLambdaBuildIndices.T_LAMBDA_BUILD_INDICES) {
            dslContext.update(this)
                .set(START_TIME, Timestamp(startTime).toLocalDateTime())
                .set(END_TIME, Timestamp(endTime).toLocalDateTime())
                .where(BUILD_ID.eq(buildId))
                .execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        buildId: String
    ): TLambdaBuildIndicesRecord? {
        with(TLambdaBuildIndices.T_LAMBDA_BUILD_INDICES) {
            return dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .fetchOne()
        }
    }
}