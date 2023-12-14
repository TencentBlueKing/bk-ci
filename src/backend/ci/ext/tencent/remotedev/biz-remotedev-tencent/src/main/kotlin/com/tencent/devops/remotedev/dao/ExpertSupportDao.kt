package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.model.remotedev.tables.TRemotedevExpertSupport
import com.tencent.devops.model.remotedev.tables.TRemotedevExpertSupportConfig
import com.tencent.devops.model.remotedev.tables.records.TRemotedevExpertSupportConfigRecord
import com.tencent.devops.model.remotedev.tables.records.TRemotedevExpertSupportRecord
import com.tencent.devops.remotedev.pojo.expert.ExpertSupportConfigType
import com.tencent.devops.remotedev.pojo.expert.ExpertSupportStatus
import org.jooq.DSLContext
import org.jooq.DatePart
import org.jooq.Field
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
class ExpertSupportDao {
    fun addSupport(
        dslContext: DSLContext,
        projectId: String,
        hostIp: String,
        creator: String,
        status: ExpertSupportStatus,
        content: String,
        workspaceName: String,
        city: String,
        machineType: String
    ): Long {
        with(TRemotedevExpertSupport.T_REMOTEDEV_EXPERT_SUPPORT) {
            return dslContext.insertInto(
                this,
                PROJECT_ID,
                HOST_IP,
                WORKSPACE_NAME,
                CREATOR,
                SUPPORTER,
                STATUS,
                CONTENT,
                CITY,
                MACHINE_TYPE
            ).values(
                projectId,
                hostIp,
                workspaceName,
                creator,
                null,
                status.name,
                content,
                city,
                machineType
            ).returning(ID)
                .fetchOne()!!.id
        }
    }

    fun updateSupport(
        dslContext: DSLContext,
        id: Long,
        status: ExpertSupportStatus,
        supporter: Set<String>?
    ) {
        with(TRemotedevExpertSupport.T_REMOTEDEV_EXPERT_SUPPORT) {
            val sql = dslContext.update(this)
                .set(STATUS, status.name)
            if (supporter != null) {
                sql.set(SUPPORTER, JsonUtil.toJson(supporter, formatted = false))
            }
            sql.set(UPDATE_TIME, LocalDateTime.now())
            sql.where(ID.eq(id))
                .execute()
        }
    }

    fun timestampDiff(part: DatePart, t1: Field<Timestamp>): Field<Int> {
        return DSL.field(
            "timestampdiff({0}, {1}, NOW())",
            Int::class.java, DSL.keyword(part.toSQL()), t1
        )
    }

    fun fetchSupports(
        dslContext: DSLContext,
        projectId: String,
        hostIp: String,
        creator: String,
        status: ExpertSupportStatus,
        content: String? = null,
        internalTime: Int? = null
    ): List<TRemotedevExpertSupportRecord> {
        with(TRemotedevExpertSupport.T_REMOTEDEV_EXPERT_SUPPORT) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(HOST_IP.eq(hostIp))
                .and(CREATOR.eq(creator))
                .and(STATUS.eq(status.name))
                .let { if (content != null) it.and(CONTENT.eq(content)) else it }
                .let {
                    if (internalTime != null) {
                        it.and(
                            timestampDiff(DatePart.SECOND, CREATE_TIME.cast(java.sql.Timestamp::class.java))
                                .lessOrEqual(internalTime)
                        )
                    } else {
                        it
                    }
                }
                .orderBy(CREATE_TIME.desc())
                .fetch()
        }
    }

    fun fetchSupByWorkspaceName(
        dslContext: DSLContext,
        workspaceNames: Set<String>
    ): List<TRemotedevExpertSupportRecord> {
        with(TRemotedevExpertSupport.T_REMOTEDEV_EXPERT_SUPPORT) {
            return dslContext.selectFrom(this)
                .where(WORKSPACE_NAME.`in`(workspaceNames))
                .and(STATUS.ne(ExpertSupportStatus.DONE.name))
                .fetch()
        }
    }

    fun getSup(
        dslContext: DSLContext,
        id: Long
    ): TRemotedevExpertSupportRecord? {
        with(TRemotedevExpertSupport.T_REMOTEDEV_EXPERT_SUPPORT) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchAny()
        }
    }

    // ---- config -----
    fun addExpertSupportConfig(
        dslContext: DSLContext,
        type: ExpertSupportConfigType,
        content: String
    ) {
        with(TRemotedevExpertSupportConfig.T_REMOTEDEV_EXPERT_SUPPORT_CONFIG) {
            dslContext.insertInto(
                this,
                TYPE,
                CONTENT
            ).values(
                type.name,
                content
            ).execute()
        }
    }

    fun deleteExpertSupportConfig(
        dslContext: DSLContext,
        id: Long
    ) {
        with(TRemotedevExpertSupportConfig.T_REMOTEDEV_EXPERT_SUPPORT_CONFIG) {
            dslContext.deleteFrom(this).where(ID.eq(id)).execute()
        }
    }

    fun fetchExpertSupportConfig(
        dslContext: DSLContext,
        type: ExpertSupportConfigType
    ): List<TRemotedevExpertSupportConfigRecord> {
        with(TRemotedevExpertSupportConfig.T_REMOTEDEV_EXPERT_SUPPORT_CONFIG) {
            return dslContext.selectFrom(this).where(TYPE.eq(type.name)).fetch()
        }
    }
}
