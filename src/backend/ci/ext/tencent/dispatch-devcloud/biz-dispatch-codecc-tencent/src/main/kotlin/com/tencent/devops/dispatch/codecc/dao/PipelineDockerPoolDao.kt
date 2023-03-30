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

package com.tencent.devops.dispatch.codecc.dao

import com.tencent.devops.model.dispatch.codecc.tables.TDispatchPipelineDockerPool
import com.tencent.devops.model.dispatch.codecc.tables.records.TDispatchPipelineDockerPoolRecord
import com.tencent.devops.dispatch.codecc.pojo.PipelineTaskStatus
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineDockerPoolDao @Autowired constructor() {
    fun create(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeq: String,
        poolNo: Int,
        status: Int
    ) {
        with(TDispatchPipelineDockerPool.T_DISPATCH_PIPELINE_DOCKER_POOL) {
            dslContext.insertInto(
                this,
                PIPELINE_ID,
                VM_SEQ,
                POOL_NO,
                STATUS,
                GMT_CREATE,
                GMT_MODIFIED
            ).values(
                pipelineId,
                vmSeq,
                poolNo,
                status,
                LocalDateTime.now(),
                LocalDateTime.now()
            ).execute()
        }
    }

    fun getPoolNoStatus(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeq: String,
        poolNo: Int
    ): TDispatchPipelineDockerPoolRecord? {
        with(TDispatchPipelineDockerPool.T_DISPATCH_PIPELINE_DOCKER_POOL) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ.eq(vmSeq))
                .and(POOL_NO.eq(poolNo))
                .fetchOne()
        }
    }

    fun updatePoolStatus(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeq: String,
        poolNo: Int,
        status: Int
    ): Boolean {
        with(TDispatchPipelineDockerPool.T_DISPATCH_PIPELINE_DOCKER_POOL) {
            return dslContext.update(this)
                .set(STATUS, status)
                .set(GMT_MODIFIED, LocalDateTime.now())
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ.eq(vmSeq))
                .and(POOL_NO.eq(poolNo))
                .execute() == 1
        }
    }

    fun getTimeOutPool(dslContext: DSLContext): Result<TDispatchPipelineDockerPoolRecord> {
        with(TDispatchPipelineDockerPool.T_DISPATCH_PIPELINE_DOCKER_POOL) {
            return dslContext.selectFrom(this)
                .where(STATUS.eq(2))
                .and(GMT_MODIFIED.lessOrEqual(timestampSubDay(2)))
                .fetch()
        }
    }

    fun updateTimeOutPool(dslContext: DSLContext): Boolean {
        with(TDispatchPipelineDockerPool.T_DISPATCH_PIPELINE_DOCKER_POOL) {
            return dslContext.update(this)
                .set(STATUS, PipelineTaskStatus.DONE.status)
                .set(GMT_MODIFIED, LocalDateTime.now())
                .where(STATUS.eq(2))
                .and(GMT_MODIFIED.lessOrEqual(timestampSubDay(2)))
                .execute() == 1
        }
    }

    fun timestampSubDay(day: Long): Field<LocalDateTime> {
        return DSL.field("date_sub(NOW(), interval $day day)",
            LocalDateTime::class.java)
    }
}

/*
CREATE TABLE `T_DISPATCH_PIPELINE_DOCKER_POOL` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `PIPELINE_ID` varchar(64) NOT NULL DEFAULT '' COMMENT '流水线ID',
  `VM_SEQ` varchar(64) NOT NULL DEFAULT '' COMMENT '构建机序号',
  `POOL_NO` int(11) NOT NULL DEFAULT 0 COMMENT '构建池序号',
  `STATUS` int(11) NOT NULL DEFAULT 0 COMMENT '构建池状态',
  `GMT_CREATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `GMT_MODIFIED` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UNI_BUILD_SEQ` (`PIPELINE_ID`,`VM_SEQ`, `POOL_NO`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DOCKER并发构建池状态表';*/
