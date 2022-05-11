/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.replication.replica.schedule

import com.tencent.bkrepo.replication.replica.base.ReplicaContext
import com.tencent.bkrepo.replication.manager.LocalDataManager
import com.tencent.bkrepo.replication.replica.base.AbstractReplicaService
import com.tencent.bkrepo.replication.service.ReplicaRecordService
import org.springframework.stereotype.Component

/**
 * 调度类任务同步器
 */
@Component
class ScheduledReplicaService(
    replicaRecordService: ReplicaRecordService,
    localDataManager: LocalDataManager
) : AbstractReplicaService(replicaRecordService, localDataManager) {

    override fun replica(context: ReplicaContext) {
        with(context) {
            // 检查版本
            replicator.checkVersion(this)
            // 同步项目
            replicator.replicaProject(this)
            // 同步仓库
            replicator.replicaRepo(this)
            // 按仓库同步
            if (includeAllData(this)) {
                replicaByRepo(this)
                return
            }
            // 按包同步
            taskObject.packageConstraints.orEmpty().forEach {
                replicaByPackageConstraint(this, it)
            }
            // 按路径同步
            taskObject.pathConstraints.orEmpty().forEach {
                replicaByPathConstraint(this, it)
            }
        }
    }

    /**
     * 是否包含所有仓库数据
     */
    private fun includeAllData(context: ReplicaContext): Boolean {
        return context.taskObject.packageConstraints.isNullOrEmpty() &&
            context.taskObject.pathConstraints.isNullOrEmpty()
    }
}
