/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.scanner.service

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.query.model.PageLimit
import com.tencent.bkrepo.scanner.pojo.ScanTask
import com.tencent.bkrepo.scanner.pojo.ScanTriggerType
import com.tencent.bkrepo.scanner.pojo.SubScanTask
import com.tencent.bkrepo.scanner.pojo.request.ArtifactVulnerabilityRequest
import com.tencent.bkrepo.scanner.pojo.request.BatchScanRequest
import com.tencent.bkrepo.scanner.pojo.request.FileScanResultDetailRequest
import com.tencent.bkrepo.scanner.pojo.request.FileScanResultOverviewRequest
import com.tencent.bkrepo.scanner.pojo.request.ReportResultRequest
import com.tencent.bkrepo.scanner.pojo.request.ScanRequest
import com.tencent.bkrepo.scanner.pojo.request.ScanTaskQuery
import com.tencent.bkrepo.scanner.pojo.request.SingleScanRequest
import com.tencent.bkrepo.scanner.pojo.response.ArtifactVulnerabilityInfo
import com.tencent.bkrepo.scanner.pojo.response.FileScanResultDetail
import com.tencent.bkrepo.scanner.pojo.response.FileScanResultOverview

/**
 * 扫描服务
 */
interface ScanService {
    /**
     * 创建扫描任务，启动扫描
     *
     * @param scanRequest 扫描参数，指定使用的扫描器和需要扫描的文件
     * @param triggerType 触发类型
     */
    fun scan(scanRequest: ScanRequest, triggerType: ScanTriggerType): ScanTask

    /**
     * 扫描单个文件
     *
     * @param request 扫描请求
     */
    fun singleScan(request: SingleScanRequest): ScanTask

    /**
     * 批量扫描
     *
     * @param request 批量扫描请求
     */
    fun batchScan(request: BatchScanRequest): ScanTask

    /**
     * 停止子任务
     *
     * @param projectId 项目id
     * @param subtaskId 使用特定扫描方案的制品最新一次扫描记录id
     *
     * @return true 停止成功，false 停止失败
     */
    fun stopByPlanArtifactLatestSubtaskId(projectId: String, subtaskId: String): Boolean

    /**
     * 停止子任务
     *
     * @param projectId 项目id
     * @param subtaskId 子任务id
     *
     * @return true 停止成功，false 停止失败
     */
    fun stopSubtask(projectId: String, subtaskId: String): Boolean

    /**
     * 获取扫描任务
     *
     * @param taskId 任务id
     */
    fun task(taskId: String): ScanTask

    /**
     * 分页获取扫描任务
     */
    fun tasks(scanTaskQuery: ScanTaskQuery, pageLimit: PageLimit): Page<ScanTask>

    /**
     * 扫描结果上报
     *
     * @param reportResultRequest 扫描结果上报请求
     */
    fun reportResult(reportResultRequest: ReportResultRequest)

    /**
     * 获取扫描结果预览
     *
     * @param request 扫描预览请求参数
     *
     * @return 扫描结果预览数据
     */
    fun resultOverview(request: FileScanResultOverviewRequest): List<FileScanResultOverview>

    /**
     * 获取文件扫描报告详情
     *
     * @param request 获取文件扫描报告请求参数
     *
     * @return 文件扫描报告详情
     */
    fun resultDetail(request: FileScanResultDetailRequest): FileScanResultDetail

    /**
     * 获取文件扫描报告详情
     */
    fun resultDetail(request: ArtifactVulnerabilityRequest): Page<ArtifactVulnerabilityInfo>

    /**
     * 更新子扫描任务状态
     *
     * @param subScanTaskId 子任务id
     * @param subScanTaskStatus 要更新成哪个状态
     *
     * @return 是否更新成功
     */
    fun updateSubScanTaskStatus(subScanTaskId: String, subScanTaskStatus: String): Boolean

    /**
     * 拉取子任务
     *
     * @return 没有可执行的任务时返回null，否则返回一个待执行的任务
     */
    fun pull(): SubScanTask?
}
