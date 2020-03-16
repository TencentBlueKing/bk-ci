package com.tencent.devops.dispatch.dao

import com.tencent.devops.model.dispatch.tables.TIdcIpInfo
import com.tencent.devops.model.dispatch.tables.records.TIdcIpInfoRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class DockerIPInfoDao {
    fun create(
        dslContext: DSLContext,
        idcIp: String,
        capacity: Int,
        used: Int,
        enable: Boolean,
        grayEnv: Boolean
    ) {
        with(TIdcIpInfo.T_IDC_IP_INFO) {
            dslContext.insertInto(
                this,
                IDC_IP,
                CAPACITY,
                USED_NUM,
                ENABLE,
                GRAY_ENV,
                GMT_CREATE,
                GMT_MODIFIED
            ).values(
                idcIp,
                capacity,
                used,
                enable,
                grayEnv,
                java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now()
            ).execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        idcIp: String,
        capacity: Int,
        used: Int,
        enable: Boolean
    ) {
        with(TIdcIpInfo.T_IDC_IP_INFO) {
            dslContext.update(this)
                .set(CAPACITY, capacity)
                .set(USED_NUM, used)
                .set(ENABLE, enable)
                .where(IDC_IP.eq(idcIp))
                .execute()
        }
    }

    fun updateIdcIpStatus(
        dslContext: DSLContext,
        id: Int,
        enable: Boolean
    ) {
        with(TIdcIpInfo.T_IDC_IP_INFO) {
            dslContext.update(this)
                .set(ENABLE, enable)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun getIdcIpList(
        dslContext: DSLContext,
        page: Int,
        pageSize: Int
    ): Result<TIdcIpInfoRecord> {
        with(TIdcIpInfo.T_IDC_IP_INFO) {
            return dslContext.selectFrom(this)
                .limit(pageSize).offset((page - 1) * pageSize)
                .fetch()
        }
    }

    fun getIdcIpCount(
        dslContext: DSLContext
    ): Long {
        with(TIdcIpInfo.T_IDC_IP_INFO) {
            return dslContext.selectCount()
                .from(this)
                .fetchOne(0, Long::class.java)
        }
    }

    fun getEnableIdcIpList(
        dslContext: DSLContext,
        grayEnv: Boolean
    ): Result<TIdcIpInfoRecord> {
        with(TIdcIpInfo.T_IDC_IP_INFO) {
            return dslContext.selectFrom(this)
                .where(ENABLE.eq(true))
                .and(GRAY_ENV.eq(grayEnv))
                .fetch()
        }
    }

    fun delete(
        dslContext: DSLContext,
        idcIpInfoId: Int
    ): Int {
        return with(TIdcIpInfo.T_IDC_IP_INFO) {
            dslContext.delete(this)
                .where(ID.eq(idcIpInfoId))
                .execute()
        }
    }
}