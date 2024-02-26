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

package com.tencent.devops.misc.pojo.process

import com.tencent.devops.misc.lock.MigrationLock
import io.swagger.v3.oas.annotations.media.Schema
import org.jooq.DSLContext

@Schema(title = "删除迁移数据参数")
data class DeleteMigrationDataParam(
    @get:Schema(title = "jooq上下文", required = true)
    val dslContext: DSLContext,
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "迁移集群名称", required = true)
    val targetClusterName: String,
    @get:Schema(title = "迁移数据源名称", required = true)
    val targetDataSourceName: String,
    @get:Schema(title = "流水线ID", required = false)
    val pipelineId: String? = null,
    @get:Schema(title = "迁移锁", required = false)
    val migrationLock: MigrationLock? = null,
    @get:Schema(title = "广播表删除标识", required = false)
    val broadcastTableDeleteFlag: Boolean? = true,
    @get:Schema(title = "归档流水线标识", required = false)
    val archivePipelineFlag: Boolean? = null
)
