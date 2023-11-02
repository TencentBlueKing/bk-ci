package com.tencent.devops.remotedev.dao

import com.tencent.devops.model.remotedev.tables.TRemotedevExpertSupport
import com.tencent.devops.model.remotedev.tables.TRemotedevExpertSupportConfig
import com.tencent.devops.model.remotedev.tables.records.TRemotedevExpertSupportConfigRecord
import com.tencent.devops.model.remotedev.tables.records.TRemotedevExpertSupportRecord
import com.tencent.devops.remotedev.pojo.expertSupport.ExpertSupportConfigType
import com.tencent.devops.remotedev.pojo.expertSupport.ExpertSupportStatus
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ExpertSupportDao {
    fun addSupport(
        dslContext: DSLContext,
        projectId: String,
        hostIp: String,
        creator: String,
        responder: String,
        status: ExpertSupportStatus,
        content: String,
        workspaceName: String
    ): Long {
        with(TRemotedevExpertSupport.T_REMOTEDEV_EXPERT_SUPPORT) {
            return dslContext.insertInto(
                this,
                PROJECT_ID,
                HOST_IP,
                WORKSPACE_NAME,
                CREATOR,
                RESPONDER,
                SUPPORTER,
                STATUS,
                CONTENT
            ).values(
                projectId,
                hostIp,
                workspaceName,
                creator,
                responder,
                null,
                status.name,
                content
            ).returning(ID)
                .fetchOne()!!.id
        }
    }

    fun updateSupport(
        dslContext: DSLContext,
        id: Long,
        status: ExpertSupportStatus,
        supporter: String?
    ) {
        with(TRemotedevExpertSupport.T_REMOTEDEV_EXPERT_SUPPORT) {
            val sql = dslContext.update(this)
                .set(STATUS, status.name)
            if (supporter != null) {
                sql.set(SUPPORTER, supporter)
            }
            sql.set(UPDATE_TIME, LocalDateTime.now())
            sql.where(ID.eq(id))
                .execute()
        }
    }

    fun fetchSupports(
        dslContext: DSLContext,
        projectId: String,
        hostIp: String,
        status: ExpertSupportStatus
    ): List<TRemotedevExpertSupportRecord> {
        with(TRemotedevExpertSupport.T_REMOTEDEV_EXPERT_SUPPORT) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(HOST_IP.eq(hostIp))
                .and(STATUS.eq(status.name))
                .fetch()
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