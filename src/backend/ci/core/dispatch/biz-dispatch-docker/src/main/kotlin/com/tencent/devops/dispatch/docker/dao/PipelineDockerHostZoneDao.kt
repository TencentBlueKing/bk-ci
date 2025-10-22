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

import com.tencent.devops.common.api.pojo.Zone
import com.tencent.devops.dispatch.docker.pojo.enums.DockerHostType
import com.tencent.devops.common.service.utils.ByteUtils
import com.tencent.devops.model.dispatch.tables.TDispatchPipelineDockerHostZone
import com.tencent.devops.model.dispatch.tables.records.TDispatchPipelineDockerHostZoneRecord
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository@Suppress("ALL")
class PipelineDockerHostZoneDao {

    fun insertHostZone(
        dslContext: DSLContext,
        hostIp: String,
        zone: String,
        remark: String?,
        type: DockerHostType = DockerHostType.BUILD,
        routeKey: String? = null
    ): Int {
        with(TDispatchPipelineDockerHostZone.T_DISPATCH_PIPELINE_DOCKER_HOST_ZONE) {
            val now = LocalDateTime.now()
            return dslContext.insertInto(
                this,
                HOST_IP,
                ZONE,
                TYPE,
                ROUTE_KEY,
                REMARK,
                CREATED_TIME,
                UPDATED_TIME
            )
                .values(
                    hostIp,
                    zone,
                    type.ordinal,
                    routeKey,
                    remark ?: "",
                    now,
                    now
                ).execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        hostIp: String
    ) {
        with(TDispatchPipelineDockerHostZone.T_DISPATCH_PIPELINE_DOCKER_HOST_ZONE) {
            dslContext.deleteFrom(this)
                    .where(HOST_IP.eq(hostIp))
                    .execute()
        }
    }

    fun count(dslContext: DSLContext) = dslContext.selectCount()
            .from(TDispatchPipelineDockerHostZone.T_DISPATCH_PIPELINE_DOCKER_HOST_ZONE)
            .fetchOne(0, Int::class.java)!!

    fun getList(
        dslContext: DSLContext,
        page: Int,
        pageSize: Int
    ): Result<TDispatchPipelineDockerHostZoneRecord>? {
        with(TDispatchPipelineDockerHostZone.T_DISPATCH_PIPELINE_DOCKER_HOST_ZONE) {
            return dslContext.selectFrom(this)
                    .orderBy(CREATED_TIME.desc())
                    .limit(pageSize).offset((page - 1) * pageSize)
                    .fetch()
        }
    }

    fun enable(
        dslContext: DSLContext,
        hostIp: String,
        enable: Boolean
    ) {
        with(TDispatchPipelineDockerHostZone.T_DISPATCH_PIPELINE_DOCKER_HOST_ZONE) {
            dslContext.update(this)
                    .set(ENABLE, ByteUtils.bool2Byte(enable))
                    .where(HOST_IP.eq(hostIp))
                    .execute()
        }
    }

    fun getHostZone(dslContext: DSLContext, hostIp: String): TDispatchPipelineDockerHostZoneRecord? {
        with(TDispatchPipelineDockerHostZone.T_DISPATCH_PIPELINE_DOCKER_HOST_ZONE) {
            return dslContext.selectFrom(this)
                .where(HOST_IP.eq(hostIp))
                .fetchAny()
        }
    }

    fun getOneHostByRandom(dslContext: DSLContext): TDispatchPipelineDockerHostZoneRecord {
        with(TDispatchPipelineDockerHostZone.T_DISPATCH_PIPELINE_DOCKER_HOST_ZONE) {
            return dslContext.selectFrom(this)
                    .orderBy(DSL.rand())
                    .fetchAny()!!
        }
    }

    fun getOneHostZoneByZone(dslContext: DSLContext, zone: Zone): TDispatchPipelineDockerHostZoneRecord? {
        with(TDispatchPipelineDockerHostZone.T_DISPATCH_PIPELINE_DOCKER_HOST_ZONE) {
            return dslContext.selectFrom(this)
                .where(ZONE.eq(zone.name))
                .fetchAny()
        }
    }
}

/*

DROP TABLE IF EXISTS `T_DISPATCH_PIPELINE_DOCKER_HOST_ZONE`;
CREATE TABLE `T_DISPATCH_PIPELINE_DOCKER_HOST_ZONE` (
  `HOST_IP` varchar(128) NOT NULL,
  `ZONE` varchar(128) NOT NULL,
  `ENABLE` TINYINT(1) NULL DEFAULT 1,
  `REMARK` varchar(1024) NULL,
  `CREATED_TIME` datetime NOT NULL,
  `UPDATED_TIME` datetime NOT NULL,
  `TYPE` int(11) NOT NULL DEFAULT '0' COMMENT '母机类型, 0表示是构建环境的构建机, 1表示是无编译环境的机器',
  `ROUTE_KEY` varchar(45) DEFAULT NULL COMMENT '消息队列的路由KEY',
  PRIMARY KEY (`HOST_IP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

 */
