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

import com.tencent.devops.model.dispatch.tables.TDispatchPipelineDockerTaskSimple
import com.tencent.devops.model.dispatch.tables.records.TDispatchPipelineDockerTaskSimpleRecord
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository@Suppress("ALL")
class PipelineDockerTaskSimpleDao @Autowired constructor() {
    fun createOrUpdate(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeq: String,
        dockerIp: String,
        dockerResourceOptionsId: Int
    ) {
        with(TDispatchPipelineDockerTaskSimple.T_DISPATCH_PIPELINE_DOCKER_TASK_SIMPLE) {
            dslContext.insertInto(
                this,
                PIPELINE_ID,
                VM_SEQ,
                DOCKER_IP,
                DOCKER_RESOURCE_OPTION,
                GMT_CREATE,
                GMT_MODIFIED
            ).values(
                pipelineId,
                vmSeq,
                dockerIp,
                dockerResourceOptionsId,
                LocalDateTime.now(),
                LocalDateTime.now()
            ).onDuplicateKeyUpdate()
                .set(DOCKER_IP, dockerIp)
                .set(DOCKER_RESOURCE_OPTION, dockerResourceOptionsId)
                .set(GMT_MODIFIED, LocalDateTime.now())
                .execute()
        }
    }

    fun updateDockerIp(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeq: String,
        dockerIp: String
    ) {
        with(TDispatchPipelineDockerTaskSimple.T_DISPATCH_PIPELINE_DOCKER_TASK_SIMPLE) {
            dslContext.update(this)
                .set(DOCKER_IP, dockerIp)
                .set(GMT_MODIFIED, LocalDateTime.now())
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ.eq(vmSeq))
                .execute()
        }
    }

    fun deleteByDockerIp(
        dslContext: DSLContext,
        dockerIp: String
    ) {
        with(TDispatchPipelineDockerTaskSimple.T_DISPATCH_PIPELINE_DOCKER_TASK_SIMPLE) {
            dslContext.deleteFrom(this).where(DOCKER_IP.eq(dockerIp)).execute()
        }
    }

    fun deleteByPipelineIdAndVmSeqId(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeqId: String
    ) {
        with(TDispatchPipelineDockerTaskSimple.T_DISPATCH_PIPELINE_DOCKER_TASK_SIMPLE) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ.eq(vmSeqId))
                .execute()
        }
    }

    fun getByPipelineIdAndVMSeq(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeq: String
    ): TDispatchPipelineDockerTaskSimpleRecord? {
        with(TDispatchPipelineDockerTaskSimple.T_DISPATCH_PIPELINE_DOCKER_TASK_SIMPLE) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ.eq(vmSeq))
                .fetchOne()
        }
    }
}

/*
CREATE TABLE `T_DISPATCH_PIPELINE_DOCKER_TASK_SIMPLE` (
    `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `PIPELINE_ID` varchar(64) NOT NULL DEFAULT '' COMMENT '流水线ID',
    `VM_SEQ` varchar(64) NOT NULL DEFAULT '' COMMENT '构建机序号',
    `DOCKER_IP` varchar(64) NOT NULL DEFAULT '' COMMENT '构建容器IP',
    `GMT_CREATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `GMT_MODIFIED` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`ID`),
    UNIQUE KEY `UNI_BUILD_SEQ` (`PIPELINE_ID`,`VM_SEQ`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DOCKER构建任务表';*/
