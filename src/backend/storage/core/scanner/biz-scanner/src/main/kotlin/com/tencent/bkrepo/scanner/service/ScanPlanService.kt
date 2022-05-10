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
import com.tencent.bkrepo.scanner.pojo.ScanPlan
import com.tencent.bkrepo.scanner.pojo.request.ArtifactPlanRelationRequest
import com.tencent.bkrepo.scanner.pojo.request.PlanArtifactRequest
import com.tencent.bkrepo.scanner.pojo.request.UpdateScanPlanRequest
import com.tencent.bkrepo.scanner.pojo.response.ArtifactPlanRelation
import com.tencent.bkrepo.scanner.pojo.response.ArtifactScanResultOverview
import com.tencent.bkrepo.scanner.pojo.response.PlanArtifactInfo
import com.tencent.bkrepo.scanner.pojo.response.ScanPlanInfo

/**
 * 扫描方案服务
 */
interface ScanPlanService {

    /**
     * 创建扫描方案
     */
    fun create(request: ScanPlan): ScanPlan

    /**
     * 获取扫描方案列表
     *
     * @param projectId 扫描方案所属项目
     * @param type 扫描方案类型
     *
     * @return 扫描方案列表
     */
    fun list(projectId: String, type: String? = null): List<ScanPlan>

    /**
     * 分页获取扫描方案列表
     *
     * @param projectId 扫描方案所属项目
     * @param type 扫描方案类型
     * @param planNameContains 扫描方案名包含的内容
     *
     * @return 扫描方案列表
     */
    fun page(
        projectId: String,
        type: String?,
        planNameContains: String?,
        pageLimit: PageLimit
    ): Page<ScanPlanInfo>

    /**
     * 获取扫描方案
     *
     * @param projectId 扫描方案所属项目id
     * @param id 扫描方案id
     *
     * @return 扫描方案
     */
    fun find(projectId: String, id: String): ScanPlan?

    /**
     * 删除扫描方案
     *
     * @param projectId 扫描方案所属项目id
     * @param id 扫描方案id
     */
    fun delete(projectId: String, id: String)

    /**
     * 更新扫描方案
     *
     * @param request 更新扫描方案请求
     *
     * @return 更新后的扫描方案
     */
    fun update(request: UpdateScanPlanRequest): ScanPlan

    /**
     * 获取扫描方案最新一次扫描详情
     *
     * @param projectId 扫描方案所属项目id
     * @param id 扫描方案id
     *
     * @return 扫描方案最新一次扫描详情
     */
    fun scanPlanInfo(projectId: String, id: String): ScanPlanInfo?

    /**
     * 分页获取使用指定扫描方案扫描过的制品
     *
     * @param request 获取扫描方案关联的制品请求，包含扫描方案信息和制品筛选条件
     *
     * @return 扫描方案扫描的制品信息
     */
    fun planArtifactPage(request: PlanArtifactRequest): Page<PlanArtifactInfo>

    /**
     * 获取制品扫描结果预览
     *
     * @param projectId 制品所属项目
     * @param subScanTaskId 子扫描任务id
     *
     * @return 制品扫描结果预览信息
     */
    fun planArtifact(projectId: String, subScanTaskId: String): ArtifactScanResultOverview

    /**
     * 获取制品关联的扫描方案列表
     *
     * @param request 获取制品关联的扫描方案请求，包含制品信息
     *
     * @return 制品关联的扫描方案信息
     */
    fun artifactPlanList(request: ArtifactPlanRelationRequest): List<ArtifactPlanRelation>

    /**
     * 获取制品扫描状态
     *
     * @param request 制品信息
     *
     * @return 制品扫描状态
     */
    fun artifactPlanStatus(request: ArtifactPlanRelationRequest): String?
}
