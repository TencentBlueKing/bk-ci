package com.tencent.devops.process.dao

import com.tencent.devops.model.process.tables.TPipelineSubscription
import com.tencent.devops.model.process.tables.records.TPipelineSubscriptionRecord
import com.tencent.devops.process.pojo.SubscriptionType
import com.tencent.devops.process.pojo.pipeline.PipelineSubscription
import com.tencent.devops.process.pojo.pipeline.PipelineSubscriptionType
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PipelineSubscriptionDao {

    fun insert(dslContext: DSLContext, pipelineId: String, username: String, subscriptionTypes: List<PipelineSubscriptionType>, type: SubscriptionType) {
        with(TPipelineSubscription.T_PIPELINE_SUBSCRIPTION) {
            dslContext.insertInto(this,
                    PIPELINE_ID,
                    USERNAME,
                    SUBSCRIPTION_TYPE,
                    TYPE)
                    .values(pipelineId,
                            username,
                            subscriptionTypes.joinToString(","),
                            type.type)
                    .execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        id: Int,
        subscriptionTypes: List<PipelineSubscriptionType>,
        type: SubscriptionType
    ) =
            with(TPipelineSubscription.T_PIPELINE_SUBSCRIPTION) {
                dslContext.update(this)
                        .set(SUBSCRIPTION_TYPE, subscriptionTypes.joinToString(","))
                        .set(TYPE, type.type)
                        .where(ID.eq(id))
                        .execute()
    }

    fun delete(dslContext: DSLContext, pipelineId: String, username: String): Boolean {
        with(TPipelineSubscription.T_PIPELINE_SUBSCRIPTION) {
            return dslContext.deleteFrom(this)
                    .where(PIPELINE_ID.eq(pipelineId))
                    .and(USERNAME.eq(username))
                    .execute() == 1
        }
    }

    fun get(dslContext: DSLContext, pipelineId: String, username: String): TPipelineSubscriptionRecord? {
        with(TPipelineSubscription.T_PIPELINE_SUBSCRIPTION) {
            return dslContext.selectFrom(this)
                    .where(PIPELINE_ID.eq(pipelineId))
                    .and(USERNAME.eq(username))
                    .fetchOne()
        }
    }

    fun list(dslContext: DSLContext, pipelineId: String): List<TPipelineSubscriptionRecord> {
        with(TPipelineSubscription.T_PIPELINE_SUBSCRIPTION) {
            return dslContext.selectFrom(this)
                    .where(PIPELINE_ID.eq(pipelineId))
                    .fetch()
        }
    }

    fun convert(record: TPipelineSubscriptionRecord): PipelineSubscription {
        with(record) {
            return PipelineSubscription(pipelineId, username, convertSubscriptionTypes(subscriptionType),
                    if (type == null) {
                        SubscriptionType.ALL
                    } else {
                        SubscriptionType.toType(type)
                    }
            )
        }
    }

    private fun convertSubscriptionTypes(types: String?): List<PipelineSubscriptionType> {
        return if (types.isNullOrEmpty()) {
            listOf()
        } else {
            val tmp = types!!.split(",")
            tmp.map {
                PipelineSubscriptionType.valueOf(it)
            }.toList()
        }
    }
}