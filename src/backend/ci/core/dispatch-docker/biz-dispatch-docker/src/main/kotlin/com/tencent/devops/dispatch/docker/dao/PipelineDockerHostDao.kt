/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.dispatch.docker.dao

import com.tencent.devops.dispatch.docker.pojo.enums.DockerHostType
import com.tencent.devops.model.dispatch.tables.TDispatchPipelineDockerHost
import com.tencent.devops.model.dispatch.tables.records.TDispatchPipelineDockerHostRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineDockerHostDao {

    fun insertHost(
        dslContext: DSLContext,
        projectId: String,
        hostIp: String,
        remark: String?
    ): Int {
        with(TDispatchPipelineDockerHost.T_DISPATCH_PIPELINE_DOCKER_HOST) {
            val now = LocalDateTime.now()
            return dslContext.insertInto(
                this,
                PROJECT_CODE,
                HOST_IP,
                REMARK,
                CREATED_TIME,
                UPDATED_TIME
            )
                .values(
                    projectId,
                    hostIp,
                    remark ?: "",
                    now,
                    now
                ).execute()
        }
    }

    fun getHost(
        dslContext: DSLContext,
        projectId: String,
        type: DockerHostType = DockerHostType.BUILD
    ): TDispatchPipelineDockerHostRecord? {
        with(TDispatchPipelineDockerHost.T_DISPATCH_PIPELINE_DOCKER_HOST) {
            return dslContext.selectFrom(this)
                .where(PROJECT_CODE.eq(projectId))
                .and(TYPE.eq(type.ordinal))
                .fetchAny()
        }
    }

    fun getHostIps(
        dslContext: DSLContext,
        projectId: String,
        type: DockerHostType = DockerHostType.BUILD
    ): List<String> {
        with(TDispatchPipelineDockerHost.T_DISPATCH_PIPELINE_DOCKER_HOST) {
            val result = dslContext.select(HOST_IP).from(this)
                .where(PROJECT_CODE.eq(projectId))
                .and(TYPE.eq(type.ordinal))
                .fetchOne(HOST_IP)

            return if (result != null && result.isNotEmpty()) {
                result.split(",")
            } else {
                emptyList()
            }
        }
    }
}

/*

DROP TABLE IF EXISTS `T_DISPATCH_PIPELINE_DOCKER_HOST`;
CREATE TABLE `T_DISPATCH_PIPELINE_DOCKER_HOST` (
  `PROJECT_CODE` varchar(128) NOT NULL,
  `HOST_IP` varchar(128) NOT NULL,
  `REMARK` varchar(1024) DEFAULT NULL,
  `CREATED_TIME` datetime NOT NULL,
  `UPDATED_TIME` datetime NOT NULL,
  `TYPE` int(11) NOT NULL DEFAULT '0' COMMENT '母机类型, 0表示是构建环境的构建机, 1表示是无编译环境的机器',
  `ROUTE_KEY` varchar(45) DEFAULT NULL COMMENT '消息队列的路由KEY',
  PRIMARY KEY (`PROJECT_CODE`,`HOST_IP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8
 */
