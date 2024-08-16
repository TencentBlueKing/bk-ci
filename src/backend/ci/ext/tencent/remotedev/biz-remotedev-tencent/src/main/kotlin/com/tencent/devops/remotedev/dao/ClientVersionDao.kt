package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.remotedev.tables.TClientVersion
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class ClientVersionDao {

    /**
     * 查询项目下镜像模板信息
     */
    fun fetchAll(
        dslContext: DSLContext
    ): List<Triple<String/*ip*/, String/*user*/, String/*version*/>> {
        return with(TClientVersion.T_CLIENT_VERSION) {
            dslContext.select(IP, USER, VERSION).from(this).skipCheck()
                .fetch().map { Triple(it.value1(), it.value2(), it.value3()) }
        }
    }

    fun fetch(dslContext: DSLContext, ip: String, userId: String): String? {
        return with(TClientVersion.T_CLIENT_VERSION) {
            dslContext.selectFrom(this).where(IP.eq(ip).and(USER.eq(userId))).fetchAny()?.version
        }
    }

    /**
     * 新增项目镜像
     */
    fun create(
        dslContext: DSLContext,
        ip: String,
        userId: String,
        version: String,
        macAddress: String
    ): Int {
        return with(TClientVersion.T_CLIENT_VERSION) {
            dslContext.insertInto(
                this,
                IP,
                USER,
                VERSION,
                MAC_ADDRESS
            ).values(
                ip,
                userId,
                version,
                macAddress
            ).onDuplicateKeyIgnore().execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        ip: String,
        userId: String,
        version: String,
        lastVersion: String,
        macAddress: String
    ): Int {
        return with(TClientVersion.T_CLIENT_VERSION) {
            dslContext.update(this)
                .set(VERSION, version)
                .set(LAST_UPDATE_TIME, LocalDateTime.now())
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(LAST_VERSION, lastVersion)
                .set(MAC_ADDRESS, macAddress)
                .where(USER.eq(userId).and(IP.eq(ip)).and(VERSION.eq(lastVersion))).execute()
        }
    }

    fun updateTime(
        dslContext: DSLContext,
        ip: String,
        macAddress: String,
        userId: String,
        lastVersion: String
    ) {
        return with(TClientVersion.T_CLIENT_VERSION) {
            dslContext.update(this)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(MAC_ADDRESS, macAddress)
                .where(USER.eq(userId).and(IP.eq(ip)).and(VERSION.eq(lastVersion))).execute()
        }
    }
}
