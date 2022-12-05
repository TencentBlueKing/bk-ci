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
import com.tencent.bkrepo.replication.pojo.record.ExecutionResult
import com.tencent.bkrepo.replication.pojo.record.ExecutionStatus
import com.tencent.bkrepo.replication.pojo.record.ReplicaProgress
import com.tencent.bkrepo.replication.pojo.record.ReplicaRecordDetail
import com.tencent.bkrepo.replication.pojo.record.ReplicaRecordDetailListOption
import com.tencent.bkrepo.replication.pojo.record.ReplicaRecordInfo
import com.tencent.bkrepo.replication.pojo.record.ReplicaRecordListOption
import com.tencent.bkrepo.replication.pojo.record.ReplicaTaskRecordInfo
import com.tencent.bkrepo.replication.pojo.record.request.RecordDetailInitialRequest

/**
 * 同步任务执行记录服务接口
 */
interface ReplicaRecordService {

    /**
     * 开启新的执行记录，会执行以下动作
     * 1. 创建新的执行记录并初始化状态
     * 2. 修改上次运行时间为当前时间，并计算下次运行时间
     * 3. 修改上次运行状态为执行中
     * 4. 修改当前任务状态为执行中
     */
    fun startNewRecord(key: String): ReplicaRecordInfo

    /**
     * 初始化一条同步记录
     * @param taskKey 任务key
     */
    fun initialRecord(taskKey: String): ReplicaRecordInfo

    /**
     * 完成同步记录
     * @param recordId 记录id
     * @param status 执行状态
     * @param errorReason 错误原因，当status为失败情况下才设置
     */
    fun completeRecord(recordId: String, status: ExecutionStatus, errorReason: String? = null)

    /**
     * 初始化一条同步详情
     * @param request 同步详情初始化请求
     */
    fun initialRecordDetail(request: RecordDetailInitialRequest): ReplicaRecordDetail

    /**
     * 更新同步详情进度
     * @param detailId 同步详情id
     * @param progress 同步进度
     */
    fun updateRecordDetailProgress(detailId: String, progress: ReplicaProgress)

    /**
     * 完成同步详情
     * @param detailId 记录id
     * @param result 执行结果
     */
    fun completeRecordDetail(detailId: String, result: ExecutionResult)

    /**
     * 根据任务[key]查询执行记录
     * 返回结果按照开始时间倒排，最后执行的在最前
     * @param key 任务key
     */
    fun listRecordsByTaskKey(key: String): List<ReplicaRecordInfo>

    /**
     * 根据任务[key]分页查询执行记录
     *
     * @param key 任务key
     * @param option 条件
     */
    fun listRecordsPage(key: String, option: ReplicaRecordListOption): Page<ReplicaRecordInfo>

    /**
     * 根据[recordId]查询执行详情列表
     * 返回结果按照开始时间倒排，最后执行的在最前
     * @param recordId 执行记录id
     */
    fun listDetailsByRecordId(recordId: String): List<ReplicaRecordDetail>

    /**
     * 根据[recordId]查询执行记录信息和任务信息
     *
     * @param recordId 执行记录id
     */
    fun getRecordAndTaskInfoByRecordId(recordId: String): ReplicaTaskRecordInfo

    /**
     * 根据[id]查询执行记录
     *
     * @param id 记录id
     */
    fun getRecordById(id: String): ReplicaRecordInfo?

    /**
     * 根据[id]查询执行记录详情
     *
     * @param id 记录详情id
     */
    fun getRecordDetailById(id: String): ReplicaRecordDetail?

    /**
     * 根据任务[key]删除执行记录
     * @param key 任务key
     */
    fun deleteByTaskKey(key: String)

    /**
     * 分页查询执行日志详情列表
     *
     * @param recordId 日志id
     * @param option 列表选项
     */
    fun listRecordDetailPage(recordId: String, option: ReplicaRecordDetailListOption): Page<ReplicaRecordDetail>

    /**
     * 创建或查询日志记录
     * 用于实时计划，只有一条日志记录
     * @param key 任务key
     */
    fun findOrCreateLatestRecord(key: String): ReplicaRecordInfo
}
