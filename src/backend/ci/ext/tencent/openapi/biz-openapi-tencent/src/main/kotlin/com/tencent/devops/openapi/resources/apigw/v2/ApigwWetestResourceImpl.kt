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

package com.tencent.devops.openapi.resources.apigw.v2

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.archive.pojo.ArtifactorySearchParam
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v2.ApigwWetestResource
import com.tencent.devops.wetest.api.ServiceWetestTaskResource
import com.tencent.devops.wetest.pojo.wetest.WeTestAtomHistory
import com.tencent.devops.wetest.pojo.wetest.WeTestAtomRecord
import com.tencent.devops.wetest.pojo.wetest.WeTestFunctionTaskResponse
import com.tencent.devops.wetest.pojo.wetest.WeTestModelCloud
import com.tencent.devops.wetest.pojo.wetest.WeTestTaskInstRecord
import com.tencent.devops.wetest.pojo.wetest.WetestAutoTestRequest
import com.tencent.devops.wetest.pojo.wetest.WetestEmailGroup
import com.tencent.devops.wetest.pojo.wetest.WetestInstStatus
import com.tencent.devops.wetest.pojo.wetest.WetestTask
import com.tencent.devops.wetest.pojo.wetest.WetestTaskInst
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwWetestResourceImpl @Autowired constructor(
    private val client: Client
) : ApigwWetestResource {
    override fun uploadRes(
        appCode: String?,
        apigwType: String?,
        accessId: String,
        accessToken: String,
        type: String,
        fileParams: ArtifactorySearchParam
    ): Result<Map<String, Any>> {
        return client.get(ServiceWetestTaskResource::class).uploadRes(accessId, accessToken, type, fileParams)
    }

    override fun uploadResByMd5(
        appCode: String?,
        apigwType: String?,
        accessId: String,
        accessToken: String,
        type: String,
        fileParams: ArtifactorySearchParam
    ): Result<Map<String, Any>> {
        return client.get(ServiceWetestTaskResource::class).uploadResByMd5(accessId, accessToken, type, fileParams)
    }

    override fun autoTest(
        appCode: String?,
        apigwType: String?,
        accessId: String,
        accessToken: String,
        request: WetestAutoTestRequest
    ): Result<Map<String, Any>> {
        return client.get(ServiceWetestTaskResource::class).autoTest(accessId, accessToken, request)
    }

    override fun queryTestStatus(
        appCode: String?,
        apigwType: String?,
        accessId: String,
        accessToken: String,
        testId: String
    ): Result<Map<String, Any>> {
        return client.get(ServiceWetestTaskResource::class).queryTestStatus(accessId, accessToken, testId)
    }

    override fun getTask(
        appCode: String?,
        apigwType: String?,
        taskId: String,
        projectId: String
    ): Result<WetestTask?> {
        return client.get(ServiceWetestTaskResource::class).getTask(taskId, projectId)
    }

    override fun getModelListCloudWetest(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        cloudIds: String,
        online: String?,
        devicetype: String?,
        manu: String?,
        version: String?,
        resolution: String?,
        mem_show: String?
    ): Result<List<WeTestModelCloud>> {
        return client.get(ServiceWetestTaskResource::class).getModelListCloudWetest(
            userId,
            projectId,
            cloudIds,
            online,
            devicetype,
            manu,
            version,
            resolution,
            mem_show
        )
    }

    override fun saveTaskInst(
        appCode: String?,
        apigwType: String?,
        wetestTaskInst: WetestTaskInst
    ): Result<String> {
        return client.get(ServiceWetestTaskResource::class).saveTaskInst(wetestTaskInst)
    }

    override fun updateTaskInstStatus(
        appCode: String?,
        apigwType: String?,
        testId: String,
        status: WetestInstStatus,
        passRate: String?
    ): Result<String> {
        return client.get(ServiceWetestTaskResource::class).updateTaskInstStatus(testId, status, passRate)
    }

    override fun getReportInfo(appCode: String?, apigwType: String?, testId: String): Result<String> {
        return client.get(ServiceWetestTaskResource::class).getReportInfo(testId)
    }

    override fun getFunctionalTaskList(
        appCode: String?,
        apigwType: String?,
        userId: String
    ): Result<WeTestFunctionTaskResponse?> {
        return client.get(ServiceWetestTaskResource::class).getFunctionalTaskList(userId)
    }

    override fun checkAuthPermission(
        appCode: String?,
        apigwType: String?,
        userId: String,
        project: String
    ): Result<Boolean> {
        return client.get(ServiceWetestTaskResource::class).checkAuthPermission(userId, project)
    }

    override fun getPipelineName(
        appCode: String?,
        apigwType: String?,
        pipelineIds: String,
        project: String
    ): Result<Map<String, String>> {
        return client.get(ServiceWetestTaskResource::class).getPipelineName(pipelineIds, project)
    }

    override fun saveAtomHistroy(
        appCode: String?,
        apigwType: String?,
        wetestAtomHistory: WeTestAtomHistory
    ): Result<Map<String, Int>> {
        return client.get(ServiceWetestTaskResource::class).saveAtomHistroy(wetestAtomHistory)
    }

    override fun updateAtomBeginUpload(
        appCode: String?,
        apigwType: String?,
        Id: Int,
        projectId: String,
        result: String
    ): Result<String> {
        return client.get(ServiceWetestTaskResource::class).updateAtomBeginUpload(Id, projectId, result)
    }

    override fun updateAtomBeginTest(
        appCode: String?,
        apigwType: String?,
        Id: Int,
        projectId: String,
        testId: String?,
        result: String
    ): Result<String> {
        return client.get(ServiceWetestTaskResource::class).updateAtomBeginTest(Id, projectId, testId, result)
    }

    override fun updateAtomResult(
        appCode: String?,
        apigwType: String?,
        Id: Int,
        projectId: String,
        result: String
    ): Result<String> {
        return client.get(ServiceWetestTaskResource::class).updateAtomResult(Id, projectId, result)
    }

    override fun taskInstBydate(
        appCode: String?,
        apigwType: String?,
        startDate: String,
        endDate: String
    ): Result<List<WeTestTaskInstRecord>> {
        return client.get(ServiceWetestTaskResource::class).taskInstBydate(startDate, endDate)
    }

    override fun taskBydate(
        appCode: String?,
        apigwType: String?,
        startDate: String,
        endDate: String
    ): Result<List<WetestTask>> {
        return client.get(ServiceWetestTaskResource::class).taskBydate(startDate, endDate)
    }

    override fun emailGroupBydate(
        appCode: String?,
        apigwType: String?,
        startDate: String,
        endDate: String
    ): Result<List<WetestEmailGroup>> {
        return client.get(ServiceWetestTaskResource::class).emailGroupBydate(startDate, endDate)
    }

    override fun atomBydate(
        appCode: String?,
        apigwType: String?,
        startDate: String,
        endDate: String
    ): Result<List<WeTestAtomRecord>> {
        return client.get(ServiceWetestTaskResource::class).atomBydate(startDate, endDate)
    }
}
