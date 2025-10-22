/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.dispatch.docker.dao

import com.tencent.devops.dispatch.docker.pojo.DockerIpInfoVO
import com.tencent.devops.dispatch.docker.pojo.enums.DockerHostClusterType
import com.tencent.devops.model.dispatch.tables.TDispatchPipelineDockerIpInfo
import com.tencent.devops.model.dispatch.tables.records.TDispatchPipelineDockerIpInfoRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository@Suppress("ALL")
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
                    .set(GRAY_ENV, grayEnv)
                    .set(SPECIAL_ON, specialOn)
                    .set(CLUSTER_NAME, clusterName)
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
                    GRAY_ENV,
                    SPECIAL_ON,
                    CLUSTER_NAME,
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
                    grayEnv,
                    specialOn,
                    clusterName,
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

    fun updateBuildLessLoad(
        dslContext: DSLContext,
        dockerIp: String,
        dockerIpInfoVO: DockerIpInfoVO
    ) {
        with(TDispatchPipelineDockerIpInfo.T_DISPATCH_PIPELINE_DOCKER_IP_INFO) {
            val preRecord = dslContext.selectFrom(this)
                .where(DOCKER_IP.eq(dockerIp))
                .and(DOCKER_HOST_PORT.eq(dockerIpInfoVO.dockerHostPort))
                .fetchAny()
            if (preRecord != null) {
                dslContext.update(this)
                    .set(USED_NUM, dockerIpInfoVO.usedNum)
                    .set(CPU_LOAD, dockerIpInfoVO.averageCpuLoad)
                    .set(MEM_LOAD, dockerIpInfoVO.averageMemLoad)
                    .set(DISK_LOAD, dockerIpInfoVO.averageDiskLoad)
                    .set(DISK_IO_LOAD, dockerIpInfoVO.averageDiskIOLoad)
                    .set(ENABLE, dockerIpInfoVO.enable)
                    .set(GRAY_ENV, dockerIpInfoVO.grayEnv)
                    .set(CLUSTER_NAME, dockerIpInfoVO.clusterType!!.name)
                    .set(GMT_MODIFIED, LocalDateTime.now())
                    .where(DOCKER_IP.eq(dockerIp))
                    .and(DOCKER_HOST_PORT.eq(dockerIpInfoVO.dockerHostPort))
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
                    GRAY_ENV,
                    SPECIAL_ON,
                    CLUSTER_NAME,
                    GMT_CREATE,
                    GMT_MODIFIED
                ).values(
                    dockerIp,
                    dockerIpInfoVO.dockerHostPort,
                    dockerIpInfoVO.capacity,
                    dockerIpInfoVO.usedNum,
                    dockerIpInfoVO.averageCpuLoad,
                    dockerIpInfoVO.averageMemLoad,
                    dockerIpInfoVO.averageDiskLoad,
                    dockerIpInfoVO.averageDiskIOLoad,
                    dockerIpInfoVO.enable,
                    dockerIpInfoVO.grayEnv,
                    dockerIpInfoVO.specialOn,
                    dockerIpInfoVO.clusterType?.name,
                    LocalDateTime.now(),
                    LocalDateTime.now()
                ).execute()
            }
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
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun getAvailableDockerIpList(
        dslContext: DSLContext,
        clusterName: DockerHostClusterType,
        cpuLoad: Int,
        memLoad: Int,
        diskLoad: Int,
        diskIOLoad: Int,
        usedNum: Int,
        specialIpSet: Set<String> = setOf()
    ): Result<TDispatchPipelineDockerIpInfoRecord> {
        with(TDispatchPipelineDockerIpInfo.T_DISPATCH_PIPELINE_DOCKER_IP_INFO) {
            val conditions =
                mutableListOf<Condition>(
                    ENABLE.eq(true),
                    CLUSTER_NAME.eq(clusterName.name)
                )

            if (clusterName == DockerHostClusterType.COMMON &&
                (specialIpSet.isEmpty() || specialIpSet.toString() == "[]")) {
                // 没有配置专机，则过滤开启了专机独享的ip，并且只有公共集群才考虑专机问题
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
        clusterName: DockerHostClusterType? = null
    ): Result<TDispatchPipelineDockerIpInfoRecord> {
        with(TDispatchPipelineDockerIpInfo.T_DISPATCH_PIPELINE_DOCKER_IP_INFO) {
            val conditions =
                mutableListOf<Condition>(
                    ENABLE.eq(enable)
                )

            if (clusterName != null) {
                conditions.add(CLUSTER_NAME.eq(clusterName.name))
            }

            return dslContext.selectFrom(this)
                .where(conditions)
                .fetch()
        }
    }

    fun getEnableDockerIpCount(
        dslContext: DSLContext,
        clusterName: DockerHostClusterType
    ): Long {
        with(TDispatchPipelineDockerIpInfo.T_DISPATCH_PIPELINE_DOCKER_IP_INFO) {
            return dslContext.selectCount()
                .from(this)
                .where(ENABLE.eq(true))
                .and(CLUSTER_NAME.eq(clusterName.name))
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun getAllDockerIpCount(
        dslContext: DSLContext,
        clusterName: DockerHostClusterType
    ): Long? {
        with(TDispatchPipelineDockerIpInfo.T_DISPATCH_PIPELINE_DOCKER_IP_INFO) {
            return dslContext.selectCount()
                .from(this)
                .where(CLUSTER_NAME.eq(clusterName.name))
                .fetchOne(0, Long::class.java)
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
    `GRAY_ENV` bit(1) DEFAULT 0 COMMENT '是否为灰度节点',
    `GMT_CREATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `GMT_MODIFIED` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`ID`),
    UNIQUE KEY `UNI_IP` (`DOCKER_IP`),
    INDEX `idx_1` (`ENABLE`, `GRAY_ENV`, `CPU_LOAD`, `MEM_LOAD`, `DISK_LOAD`, `DISK_IO_LOAD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DOCKER构建机负载表';*/
