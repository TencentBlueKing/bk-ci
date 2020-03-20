package com.tencent.devops.dispatch.dao

import com.tencent.devops.model.dispatch.tables.TDispatchPipelineDockerIpInfo
import com.tencent.devops.model.dispatch.tables.records.TDispatchPipelineDockerIpInfoRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineDockerIPInfoDao {
    fun create(
        dslContext: DSLContext,
        idcIp: String,
        capacity: Int,
        used: Int,
        enable: Boolean,
        grayEnv: Boolean
    ) {
        with(TDispatchPipelineDockerIpInfo.T_DISPATCH_PIPELINE_DOCKER_IP_INFO) {
            dslContext.insertInto(
                this,
                DOCKER_IP,
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
                LocalDateTime.now(),
                LocalDateTime.now()
            ).execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        idcIp: String,
        capacity: Int,
        used: Int,
        cpuLoad: Int,
        memLoad: Int,
        diskLoad: Int,
        enable: Boolean
    ) {
        with(TDispatchPipelineDockerIpInfo.T_DISPATCH_PIPELINE_DOCKER_IP_INFO) {
            dslContext.update(this)
                .set(CAPACITY, capacity)
                .set(USED_NUM, used)
                .set(CPU_LOAD, cpuLoad)
                .set(MEM_LOAD, memLoad)
                .set(DISK_LOAD, diskLoad)
                .set(ENABLE, enable)
                .set(GMT_MODIFIED, LocalDateTime.now())
                .where(DOCKER_IP.eq(idcIp))
                .execute()
        }
    }

    fun updateDockerIpStatus(
        dslContext: DSLContext,
        id: Long,
        enable: Boolean
    ) {
        with(TDispatchPipelineDockerIpInfo.T_DISPATCH_PIPELINE_DOCKER_IP_INFO) {
            dslContext.update(this)
                .set(ENABLE, enable)
                .set(GMT_MODIFIED, LocalDateTime.now())
                .where(ID.eq(id))
                .execute()
        }
    }

    fun getDockerIpList(
        dslContext: DSLContext,
        page: Int,
        pageSize: Int
    ): Result<TDispatchPipelineDockerIpInfoRecord> {
        with(TDispatchPipelineDockerIpInfo.T_DISPATCH_PIPELINE_DOCKER_IP_INFO) {
            return dslContext.selectFrom(this)
                .limit(pageSize).offset((page - 1) * pageSize)
                .fetch()
        }
    }

    fun getDockerIpInfo(
        dslContext: DSLContext,
        dockerIp: String
    ): TDispatchPipelineDockerIpInfoRecord {
        with(TDispatchPipelineDockerIpInfo.T_DISPATCH_PIPELINE_DOCKER_IP_INFO) {
            return dslContext.selectFrom(this)
                .where(DOCKER_IP.eq(dockerIp))
                .fetchOne()
        }
    }

    fun getDockerIpCount(
        dslContext: DSLContext
    ): Long {
        with(TDispatchPipelineDockerIpInfo.T_DISPATCH_PIPELINE_DOCKER_IP_INFO) {
            return dslContext.selectCount()
                .from(this)
                .fetchOne(0, Long::class.java)
        }
    }

    fun getEnableDockerIpList(
        dslContext: DSLContext,
        grayEnv: Boolean,
        cpuLoad: Int,
        memLoad: Int,
        diskLoad: Int
    ): Result<TDispatchPipelineDockerIpInfoRecord> {
        with(TDispatchPipelineDockerIpInfo.T_DISPATCH_PIPELINE_DOCKER_IP_INFO) {
            return dslContext.selectFrom(this)
                .where(ENABLE.eq(true))
                .and(GRAY_ENV.eq(grayEnv))
                .and(CPU_LOAD.lessOrEqual(cpuLoad))
                .and(MEM_LOAD.lessOrEqual(memLoad))
                .and(DISK_LOAD.lessOrEqual(diskLoad))
                .orderBy(DISK_LOAD.asc())
                .fetch()
        }
    }

    fun delete(
        dslContext: DSLContext,
        ipInfoId: Long
    ): Int {
        return with(TDispatchPipelineDockerIpInfo.T_DISPATCH_PIPELINE_DOCKER_IP_INFO) {
            dslContext.delete(this)
                .where(ID.eq(ipInfoId))
                .execute()
        }
    }
}