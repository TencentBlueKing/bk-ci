package com.tencent.devops.prebuild.dao

import com.tencent.devops.model.prebuild.tables.TWebideIdeinfo
import com.tencent.devops.model.prebuild.tables.records.TWebideIdeinfoRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class WebIDEStatusDao {
    fun create(
        dslContext: DSLContext,
        owner: String,
        ip: String,
        machineType: String,
        agentStatus: Int,
        ideStatus: Int,
        ideVersion: String,
        serverCreateTime: Long,
        pipelineId: String
    ) {
        with(TWebideIdeinfo.T_WEBIDE_IDEINFO) {
            dslContext.insertInto(
            this,
            OWNER,
            IP,
            SERVER_TYPE,
            AGENT_STATUS,
            IDE_STATUS,
            IDE_VERSION,
            SERVER_CREATE_TIME,
            PIPELINE_ID
            ).values(
                    owner,
                    ip,
                    machineType,
                    agentStatus,
                    ideStatus,
                    ideVersion,
                    serverCreateTime,
                    pipelineId
            ).execute()
        }
    }

    fun get(dslContext: DSLContext, owner: String): List<TWebideIdeinfoRecord> {
        with(TWebideIdeinfo.T_WEBIDE_IDEINFO) {
            return dslContext.selectFrom(this)
                    .where(OWNER.eq(owner))
                    .fetch()
        }
    }

    fun getByIp(dslContext: DSLContext, owner: String, ip: String): TWebideIdeinfoRecord {
        with(TWebideIdeinfo.T_WEBIDE_IDEINFO) {
            return dslContext.selectFrom(this)
                    .where(OWNER.eq(owner))
                    .and(IP.eq(ip))
                    .fetchOne()
        }
    }

    fun del(dslContext: DSLContext, owner: String, ip: String) {
        with(TWebideIdeinfo.T_WEBIDE_IDEINFO) {
            dslContext.deleteFrom(this)
                    .where(OWNER.eq(owner))
                    .and(IP.eq(ip))
                    .execute()
        }
    }

    fun update(dslContext: DSLContext, owner: String, ip: String, agentStatus: Int, ideStatus: Int, ideVersion: String) {
        with(TWebideIdeinfo.T_WEBIDE_IDEINFO) {
            dslContext.update(this)
                    .set(AGENT_STATUS, agentStatus)
                    .set(IDE_STATUS, ideStatus)
                    .set(IDE_VERSION, ideVersion)
                    .where(OWNER.eq(owner))
                    .and(IP.eq(ip))
                    .execute()
        }
    }

    fun updatePipelineId(dslContext: DSLContext, owner: String, ip: String, pipelineId: String) {
        with(TWebideIdeinfo.T_WEBIDE_IDEINFO) {
            dslContext.update(this)
                    .set(PIPELINE_ID, pipelineId)
                    .where(OWNER.eq(owner))
                    .and(IP.eq(ip))
                    .execute()
        }
    }

    fun updateIDEHeartBeat(dslContext: DSLContext, owner: String, ip: String) {
        with(TWebideIdeinfo.T_WEBIDE_IDEINFO) {
            dslContext.update(this)
                    .set(IDE_LAST_UPDATE, System.currentTimeMillis())
                    .where(OWNER.eq(owner))
                    .and(IP.eq(ip))
                    .execute()
        }
    }
}