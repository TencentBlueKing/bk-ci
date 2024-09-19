package com.tencent.devops.remotedev.dao

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.remotedev.tables.TClientTips
import com.tencent.devops.model.remotedev.tables.records.TClientTipsRecord
import com.tencent.devops.remotedev.pojo.ClientTipsInfo
import org.jooq.DSLContext
import org.jooq.JSON
import org.jooq.RecordMapper
import org.springframework.stereotype.Repository

@Repository
class ClientTipsDao {
    fun create(
        dslContext: DSLContext,
        info: ClientTipsInfo
    ) {
        with(TClientTips.T_CLIENT_TIPS) {
            dslContext.insertInto(
                this,
                TITLE,
                CONTENT,
                WEIGHT,
                EFFECTIVE_PROJECTS,
                EFFECTIVE_USERS
            ).values(
                info.title,
                info.content,
                info.weight,
                if (info.effectiveProjects.isNullOrEmpty()) {
                    null
                } else {
                    JSON.json(JsonUtil.toJson(info.effectiveProjects!!, false))
                },
                if (info.effectiveUsers.isNullOrEmpty()) {
                    null
                } else {
                    JSON.json(JsonUtil.toJson(info.effectiveUsers!!, false))
                }
            ).execute()
        }
    }

    fun delete(dslContext: DSLContext, ids: Set<Long>) {
        with(TClientTips.T_CLIENT_TIPS) {
            dslContext.deleteFrom(this).where(ID.`in`(ids)).execute()
        }
    }

    fun update(dslContext: DSLContext, id: Long, info: ClientTipsInfo) {
        with(TClientTips.T_CLIENT_TIPS) {
            dslContext.update(this)
                .set(TITLE, info.title)
                .set(CONTENT, info.content)
                .set(WEIGHT, info.weight)
                .set(
                    EFFECTIVE_PROJECTS, if (info.effectiveProjects.isNullOrEmpty()) {
                        null
                    } else {
                        JSON.json(JsonUtil.toJson(info.effectiveProjects!!, false))
                    }
                ).set(
                    EFFECTIVE_USERS, if (info.effectiveUsers.isNullOrEmpty()) {
                        null
                    } else {
                        JSON.json(JsonUtil.toJson(info.effectiveUsers!!, false))
                    }
                ).where(ID.eq(id)).execute()
        }
    }

    fun fetchAll(
        dslContext: DSLContext
    ): List<ClientTipsRecordM> {
        with(TClientTips.T_CLIENT_TIPS) {
            return dslContext.selectFrom(this).skipCheck().fetch(mapper)
        }
    }

    class ClientTipsRecordJooqMapper : RecordMapper<TClientTipsRecord, ClientTipsRecordM> {
        override fun map(record: TClientTipsRecord?): ClientTipsRecordM? {
            return record?.run {
                ClientTipsRecordM(
                    title = title,
                    content = content,
                    weight = weight,
                    effectiveProjects = if (effectiveProjects == null) {
                        null
                    } else {
                        JsonUtil.to(effectiveProjects.data(), object : TypeReference<Set<String>>() {})
                    },
                    effectiveUsers = if (effectiveUsers == null) {
                        null
                    } else {
                        JsonUtil.to(effectiveUsers.data(), object : TypeReference<Set<String>>() {})
                    }
                )
            }
        }
    }

    companion object {
        val mapper = ClientTipsRecordJooqMapper()
    }
}

data class ClientTipsRecordM(
    val title: String,
    val content: String,
    val weight: Int,
    val effectiveProjects: Set<String>?,
    val effectiveUsers: Set<String>?
)
