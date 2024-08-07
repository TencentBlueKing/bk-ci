package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.remotedev.tables.TClient
import com.tencent.devops.model.remotedev.tables.records.TClientRecord
import org.jooq.DSLContext
import org.jooq.JSON
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ClientDao {
    fun createOrUpdate(
        dslContext: DSLContext,
        macAddress: String,
        currentUserId: String,
        currentProjectIds: Set<String>,
        version: String,
        startVersion: String
    ) {
        with(TClient.T_CLIENT) {
            dslContext.insertInto(
                this,
                MAC_ADDRESS,
                CURRENT_USER,
                CURRENT_PROJECT_IDS,
                VERSION,
                START_VERSION
            ).values(
                macAddress,
                currentUserId,
                JSON.json(JsonUtil.toJson(currentProjectIds, false)),
                version,
                startVersion
            ).onDuplicateKeyUpdate()
                .set(CURRENT_USER, currentUserId)
                .set(CURRENT_PROJECT_IDS, JSON.json(JsonUtil.toJson(currentProjectIds, false)))
                .set(VERSION, version)
                .set(START_VERSION, startVersion)
                .set(UPDATE_TIME, LocalDateTime.now())
                .execute()
        }
    }

    fun fetchVersionCount(
        dslContext: DSLContext,
        version: String
    ): Int {
        with(TClient.T_CLIENT) {
            return dslContext.selectCount().from(this).where(VERSION.eq(version)).fetchOne(0, Int::class.java)!!
        }
    }

    fun fetchStartVersionCount(
        dslContext: DSLContext,
        version: String
    ): Int {
        with(TClient.T_CLIENT) {
            return dslContext.selectCount().from(this).where(START_VERSION.eq(version)).fetchOne(0, Int::class.java)!!
        }
    }

    fun fetchAny(
        dslContext: DSLContext,
        macAddress: String
    ): TClientRecord? {
        with(TClient.T_CLIENT) {
            return dslContext.selectFrom(this).where(MAC_ADDRESS.eq(macAddress)).fetchAny()
        }
    }

    fun fetchAll(
        dslContext: DSLContext,
        lastUpdateBeforeDays: Int?
    ): List<TClientRecord> {
        with(TClient.T_CLIENT) {
            val dsl = dslContext.selectFrom(this)
            if (lastUpdateBeforeDays != null) {
                dsl.where(UPDATE_TIME.greaterOrEqual(LocalDateTime.now().minusDays(14)))
            }
            return dsl.orderBy(UPDATE_TIME.desc()).skipCheck().fetch()
        }
    }
}