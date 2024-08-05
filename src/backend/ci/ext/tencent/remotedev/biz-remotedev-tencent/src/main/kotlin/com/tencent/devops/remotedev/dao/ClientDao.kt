package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.remotedev.tables.TClient
import com.tencent.devops.model.remotedev.tables.records.TClientRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ClientDao {
    fun createOrUpdate(
        dslContext: DSLContext,
        macAddress: String,
        currentUserId: String,
        projectId: String,
        version: String,
        startVersion: String
    ) {
        with(TClient.T_CLIENT) {
            dslContext.insertInto(
                this,
                MAC_ADDRESS,
                CURRENT_USER,
                PROJECT_ID,
                VERSION,
                START_VERSION
            ).values(
                macAddress,
                currentUserId,
                projectId,
                version,
                startVersion
            ).onDuplicateKeyUpdate()
                .set(CURRENT_USER, currentUserId)
                .set(PROJECT_ID, projectId)
                .set(VERSION, version)
                .set(START_VERSION, startVersion)
                .set(UPDATE_TIME, LocalDateTime.now())
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
        dslContext: DSLContext
    ): List<TClientRecord> {
        with(TClient.T_CLIENT) {
            return dslContext.selectFrom(this).orderBy(UPDATE_TIME.desc()).skipCheck().fetch()
        }
    }
}