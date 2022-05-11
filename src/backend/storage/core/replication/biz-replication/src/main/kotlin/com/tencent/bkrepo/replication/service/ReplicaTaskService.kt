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

package com.tencent.bkrepo.replication.service

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.replication.pojo.task.ReplicaTaskDetail
import com.tencent.bkrepo.replication.pojo.task.ReplicaTaskInfo
import com.tencent.bkrepo.replication.pojo.task.request.ReplicaTaskCopyRequest
import com.tencent.bkrepo.replication.pojo.task.request.ReplicaTaskCreateRequest
import com.tencent.bkrepo.replication.pojo.task.request.ReplicaTaskUpdateRequest
import com.tencent.bkrepo.replication.pojo.task.request.TaskPageParam

/**
 * 同步任务服务接口
 */
interface ReplicaTaskService {

    /**
     * 根据任务id查询任务信息
     * @param taskId 任务id
     */
    fun getByTaskId(taskId: String): ReplicaTaskInfo?

    /**
     * 根据任务key查询任务信息
     * @param key 任务key
     */
    fun getByTaskKey(key: String): ReplicaTaskInfo

    /**
     * 根据任务key查询任务详情
     * @param key 任务key
     */
    fun getDetailByTaskKey(key: String): ReplicaTaskDetail

    /**
     * 分页查询同步任务
     */
    fun listTasksPage(projectId: String, param: TaskPageParam): Page<ReplicaTaskInfo>

    /**
     * 查询所有待执行的调度任务
     * 待执行定义:
     * 1. 立即执行还未执行
     * 2. 指定时间执行还未执行
     * 3. cron表达式周期执行
     */
    fun listUndoScheduledTasks(): List<ReplicaTaskInfo>

    /**
     * 根据项目id和仓库名称查询相关联的任务
     */
    fun listRealTimeTasks(projectId: String, repoName: String): List<ReplicaTaskDetail>

    /**
     * 创建同步任务
     * 目前只允许创建ReplicaType.SCHEDULED类型的任务
     *
     * @param request 创建请求
     */
    fun create(request: ReplicaTaskCreateRequest): ReplicaTaskInfo

    /**
     * 根据[key]删除同步任务
     * @param key 任务唯一key
     */
    fun deleteByTaskKey(key: String)

    /**
     * 根据[key]切换任务状态
     */
    fun toggleStatus(key: String)

    /**
     * 复制同步任务
     */
    fun copy(request: ReplicaTaskCopyRequest)

    /**
     * 更新同步任务
     */
    fun update(request: ReplicaTaskUpdateRequest)

    /**
     * 手动执行任务
     */
    fun execute(key: String)
}
