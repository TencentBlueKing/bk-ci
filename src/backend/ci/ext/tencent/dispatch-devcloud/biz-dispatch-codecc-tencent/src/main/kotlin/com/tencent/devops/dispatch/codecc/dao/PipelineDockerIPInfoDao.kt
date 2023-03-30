package com.tencent.devops.dispatch.codecc.dao

import com.tencent.devops.model.dispatch.codecc.tables.TDispatchPipelineDockerIpInfo
import com.tencent.devops.model.dispatch.codecc.tables.records.TDispatchPipelineDockerIpInfoRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Result
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineDockerIPInfoDao {

    private val logger = LoggerFactory.getLogger(PipelineDockerIPInfoDao::class.java)

    fun createOrUpdate(
        dslContext: DSLContext,
        dockerIp: String,
        dockerHostPort: Int,
        capacity: Int,
        used: Int,
        cpuLoad: Int,
        memLoad: Int,
        diskLoad: Int,
        diskIOLoad: Int,
        enable: Boolean,
        grayEnv: Boolean,
        specialOn: Boolean,
        clusterName: String
    ) {
        with(TDispatchPipelineDockerIpInfo.T_DISPATCH_PIPELINE_DOCKER_IP_INFO) {
            val preRecord = dslContext.selectFrom(this)
                .where(DOCKER_IP.eq(dockerIp))
                .fetchAny()
            if (preRecord != null) {
                dslContext.update(this)
                    .set(DOCKER_HOST_PORT, dockerHostPort)
                    .set(USED_NUM, used)
                    .set(CPU_LOAD, cpuLoad)
                    .set(MEM_LOAD, memLoad)
                    .set(DISK_LOAD, diskLoad)
                    .set(DISK_IO_LOAD, diskIOLoad)
                    .set(ENABLE, enable)
                    .set(CLUSTER_NAME, clusterName)
                    .set(GRAY_ENV, grayEnv)
                    .set(SPECIAL_ON, specialOn)
                    .set(GMT_MODIFIED, LocalDateTime.now())
                    .where(DOCKER_IP.eq(dockerIp))
                    .execute()
            } else {
                dslContext.insertInto(
                    this,
                    DOCKER_IP,
                    DOCKER_HOST_PORT,
                    CAPACITY,
                    USED_NUM,
                    CPU_LOAD,
                    MEM_LOAD,
                    DISK_LOAD,
                    DISK_IO_LOAD,
                    ENABLE,
                    CLUSTER_NAME,
                    GRAY_ENV,
                    SPECIAL_ON,
                    GMT_CREATE,
                    GMT_MODIFIED
                ).values(
                    dockerIp,
                    dockerHostPort,
                    capacity,
                    used,
                    cpuLoad,
                    memLoad,
                    diskLoad,
                    diskIOLoad,
                    enable,
                    clusterName,
                    grayEnv,
                    specialOn,
                    LocalDateTime.now(),
                    LocalDateTime.now()
                ).execute()
            }
        }
    }

    fun update(
        dslContext: DSLContext,
        dockerIp: String,
        dockerHostPort: Int,
        enable: Boolean,
        grayEnv: Boolean,
        specialOn: Boolean,
        clusterName: String
    ) {
        with(TDispatchPipelineDockerIpInfo.T_DISPATCH_PIPELINE_DOCKER_IP_INFO) {
            dslContext.update(this)
                .set(DOCKER_HOST_PORT, dockerHostPort)
                .set(ENABLE, enable)
                .set(GRAY_ENV, grayEnv)
                .set(SPECIAL_ON, specialOn)
                .set(CLUSTER_NAME, clusterName)
                .set(GMT_MODIFIED, LocalDateTime.now())
                .where(DOCKER_IP.eq(dockerIp))
                .execute()
        }
    }

    fun updateDockerIpStatus(
        dslContext: DSLContext,
        dockerIp: String,
        enable: Boolean
    ) {
        with(TDispatchPipelineDockerIpInfo.T_DISPATCH_PIPELINE_DOCKER_IP_INFO) {
            dslContext.update(this)
                .set(ENABLE, enable)
                .set(GMT_MODIFIED, LocalDateTime.now())
                .where(DOCKER_IP.eq(dockerIp))
                .execute()
        }
    }

    fun updateDockerHostPort(
        dslContext: DSLContext,
        dockerIp: String,
        enable: Boolean,
        dockerHostPort: Int
    ) {
        with(TDispatchPipelineDockerIpInfo.T_DISPATCH_PIPELINE_DOCKER_IP_INFO) {
            dslContext.update(this)
                .set(ENABLE, enable)
                .set(DOCKER_HOST_PORT, dockerHostPort)
                .set(GMT_MODIFIED, LocalDateTime.now())
                .where(DOCKER_IP.eq(dockerIp))
                .execute()
        }
    }

    fun updateDockerIpLoad(
        dslContext: DSLContext,
        dockerIp: String,
        dockerHostPort: Int,
        used: Int,
        cpuLoad: Int,
        memLoad: Int,
        diskLoad: Int,
        diskIOLoad: Int,
        enable: Boolean
    ) {
        with(TDispatchPipelineDockerIpInfo.T_DISPATCH_PIPELINE_DOCKER_IP_INFO) {
            dslContext.update(this)
                .set(DOCKER_HOST_PORT, dockerHostPort)
                .set(USED_NUM, used)
                .set(CPU_LOAD, cpuLoad)
                .set(MEM_LOAD, memLoad)
                .set(DISK_LOAD, diskLoad)
                .set(DISK_IO_LOAD, diskIOLoad)
                .set(ENABLE, enable)
                .set(GMT_MODIFIED, LocalDateTime.now())
                .where(DOCKER_IP.eq(dockerIp))
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
    ): TDispatchPipelineDockerIpInfoRecord? {
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
                .fetchOne(0, Long::class.java) ?: 0L
        }
    }

    fun getAvailableDockerIpList(
        dslContext: DSLContext,
        grayEnv: Boolean,
        cpuLoad: Int,
        memLoad: Int,
        diskLoad: Int,
        diskIOLoad: Int,
        usedNum: Int,
        clusterName: String,
        specialIpSet: Set<String> = setOf()
    ): Result<TDispatchPipelineDockerIpInfoRecord> {
        with(TDispatchPipelineDockerIpInfo.T_DISPATCH_PIPELINE_DOCKER_IP_INFO) {
            val conditions =
                mutableListOf<Condition>(
                    ENABLE.eq(true),
                    GRAY_ENV.eq(grayEnv),
                    CLUSTER_NAME.eq(clusterName)
                )

            if (specialIpSet.isEmpty() || specialIpSet.toString() == "[]") {
                // 没有配置专机，则过滤开启了专机独享的ip
                conditions.add(SPECIAL_ON.eq(false))
            }

            conditions.add(CPU_LOAD.lessOrEqual(cpuLoad))
            conditions.add(MEM_LOAD.lessOrEqual(memLoad))
            conditions.add(DISK_LOAD.lessOrEqual(diskLoad))
            conditions.add(DISK_IO_LOAD.lessOrEqual(diskIOLoad))
            conditions.add(USED_NUM.lessOrEqual(usedNum))

            if (specialIpSet.isNotEmpty() && specialIpSet.toString() != "[]") {
                conditions.add(DOCKER_IP.`in`(specialIpSet))
            }
            logger.info("getAvailableDockerIpList conditions: $conditions")

            return dslContext.selectFrom(this)
                .where(conditions)
                .orderBy(DISK_LOAD.asc())
                .fetch()
        }
    }

    fun getDockerIpList(
        dslContext: DSLContext,
        enable: Boolean,
        grayEnv: Boolean
    ): Result<TDispatchPipelineDockerIpInfoRecord> {
        with(TDispatchPipelineDockerIpInfo.T_DISPATCH_PIPELINE_DOCKER_IP_INFO) {
            return dslContext.selectFrom(this)
                .where(ENABLE.eq(enable))
                .and(GRAY_ENV.eq(grayEnv))
                .fetch()
        }
    }

    fun getEnableDockerIpCount(
        dslContext: DSLContext,
        grayEnv: Boolean
    ): Long {
        with(TDispatchPipelineDockerIpInfo.T_DISPATCH_PIPELINE_DOCKER_IP_INFO) {
            return dslContext.selectCount()
                .from(this)
                .where(ENABLE.eq(true))
                .and(GRAY_ENV.eq(grayEnv))
                .fetchOne(0, Long::class.java) ?: 0L
        }
    }

    fun delete(
        dslContext: DSLContext,
        dockerIp: String
    ): Int {
        return with(TDispatchPipelineDockerIpInfo.T_DISPATCH_PIPELINE_DOCKER_IP_INFO) {
            dslContext.delete(this)
                .where(DOCKER_IP.eq(dockerIp))
                .execute()
        }
    }

    fun getExpiredIpStatus(dslContext: DSLContext): Result<TDispatchPipelineDockerIpInfoRecord> {
        with(TDispatchPipelineDockerIpInfo.T_DISPATCH_PIPELINE_DOCKER_IP_INFO) {
            return dslContext.selectFrom(this)
                .where(ENABLE.eq(true))
                .and(GMT_MODIFIED.lessOrEqual(timestampSubSecond(60)))
                .fetch()
        }
    }

    fun timestampSubSecond(second: Long): Field<LocalDateTime> {
        return DSL.field("date_sub(NOW(), interval $second second)",
            LocalDateTime::class.java)
    }
}

/*
CREATE TABLE `T_DISPATCH_PIPELINE_DOCKER_IP_INFO` (
    `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `DOCKER_IP` varchar(64) NOT NULL DEFAULT '' COMMENT 'DOCKER IP',
    `DOCKER_HOST_PORT` int(11) NOT NULL DEFAULT 80 COMMENT 'DOCKER PORT',
    `CAPACITY` int(11) NOT NULL DEFAULT 0 COMMENT '节点容器总容量',
    `USED_NUM` int(11) NOT NULL DEFAULT 0 COMMENT '节点容器已使用容量',
    `CPU_LOAD` int(11) NOT NULL DEFAULT 0 COMMENT '节点容器CPU负载',
    `MEM_LOAD` int(11) NOT NULL DEFAULT 0 COMMENT '节点容器MEM负载',
    `DISK_LOAD` int(11) NOT NULL DEFAULT 0 COMMENT '节点容器DISK负载',
    `DISK_IO_LOAD` int(11) NOT NULL DEFAULT 0 COMMENT '节点容器DISK IO负载',
    `ENABLE` bit(1) DEFAULT 0 COMMENT '节点是否可用',
    `SPECIAL_ON` bit(1) DEFAULT 0 COMMENT '节点是否作为专用机',
    `CLUSTER_NAME` VARCHAR(64) DEFAULT '' COMMENT '节点所属集群',
    `GRAY_ENV` bit(1) DEFAULT 0 COMMENT '是否为灰度节点',
    `GMT_CREATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `GMT_MODIFIED` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`ID`),
    UNIQUE KEY `UNI_IP` (`DOCKER_IP`),
    INDEX `idx_1` (`ENABLE`, `GRAY_ENV`, `CPU_LOAD`, `MEM_LOAD`, `DISK_LOAD`, `DISK_IO_LOAD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DOCKER构建机负载表';

ALTER TABLE `T_DISPATCH_PIPELINE_DOCKER_IP_INFO` ADD COLUMN `CLUSTER_NAME` varchar(64) DEFAULT 'OPENSOURCE' COMMENT '节点所属集群';
*/
