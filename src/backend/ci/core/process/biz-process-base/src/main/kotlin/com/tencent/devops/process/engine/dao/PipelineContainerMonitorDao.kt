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

package com.tencent.devops.process.engine.dao

import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.model.process.tables.TPipelineContainerMonitor
import com.tencent.devops.model.process.tables.records.TPipelineContainerMonitorRecord
import com.tencent.devops.process.pojo.PipelineContainerMonitor
import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

/**
 * deng
 * 2019-01-01
 */
@Repository
class PipelineContainerMonitorDao @Autowired constructor(private val dslContext: DSLContext) {

    fun list(): Result<TPipelineContainerMonitorRecord> {
        with(TPipelineContainerMonitor.T_PIPELINE_CONTAINER_MONITOR) {
            return dslContext.selectFrom(this)
                .fetch()
        }
    }

    fun delete(osType: VMBaseOS, buildType: BuildType): Int {
        with(TPipelineContainerMonitor.T_PIPELINE_CONTAINER_MONITOR) {
            return dslContext.deleteFrom(this)
                .where(OS_TYPE.eq(osType.name))
                .and(BUILD_TYPE.eq(buildType.name))
                .execute()
        }
    }

    fun update(monitor: PipelineContainerMonitor): Int {
        return dslContext.transactionResult { configuration ->
            val context = DSL.using(configuration)

            with(TPipelineContainerMonitor.T_PIPELINE_CONTAINER_MONITOR) {
                val existRecord = context.selectFrom(this)
                    .where(BUILD_TYPE.eq(monitor.buildType.name))
                    .and(OS_TYPE.eq(monitor.osType.name))
                    .fetchOne()
                if (existRecord != null) {
                    context.update(this)
                        .set(MAX_STARTUP_TIME, monitor.maxStartupTime)
                        .set(MAX_EXECUTE_TIME, monitor.maxExecuteTime)
                        .set(USERS, monitor.users.joinToString(","))
                        .where(ID.eq(existRecord.id))
                        .execute()
                } else {
                    context.insertInto(
                        this,
                        OS_TYPE,
                        BUILD_TYPE,
                        MAX_STARTUP_TIME,
                        MAX_EXECUTE_TIME,
                        USERS
                    )
                        .values(
                            monitor.osType.name,
                            monitor.buildType.name,
                            monitor.maxStartupTime,
                            monitor.maxExecuteTime,
                            monitor.users.joinToString(",")
                        ).execute()
                }
            }
        }
    }
}