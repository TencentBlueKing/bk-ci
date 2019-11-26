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
package com.tencent.devops.openapi.service.v2

import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.client.Client
import com.tencent.devops.openapi.api.external.measure.PipelineBuildResponseData
import com.tencent.devops.openapi.api.external.measure.ServiceMeasureResource
import com.tencent.devops.project.api.service.ServiceUserResource
import com.tencent.devops.quality.api.v2.ServiceQualityInterceptResource
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.StopWatch

/**
 * @Description
 * @Date 2019/9/1
 * @Version 1.0
 */
@Service
class ApigwBuildServiceV2(private val client: Client) {

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwBuildServiceV2::class.java)
    }

    fun getBuildList(
        userId: String,
        bgId: String,
        beginDate: Long?,
        endDate: Long?,
        offset: Int?,
        limit: Int?,
        interfaceName: String? = "ApigwBuildServiceV2"
    ): List<PipelineBuildResponseData>? {
        logger.info("$interfaceName:getBuildList:Input($userId,$bgId,$beginDate,$endDate,$offset,$limit)")
        //权限校验
        val userDeptInfo = client.get(ServiceUserResource::class).getDetailFromCache(userId).data
        if (userDeptInfo == null || userDeptInfo.bgId.trim() != userId.trim()) {
            logger.warn("$interfaceName:PermissionForbidden:userDeptInfo.bgId=${userDeptInfo?.bgId},userId=${userId}")
            throw PermissionForbiddenException(
                message = "$userId doesn't have perssion to access data of bg(bgId=$bgId)"
            )
        }
        val watch = StopWatch()
        watch.start("get buildList from measure")
        val buildList = client.get(ServiceMeasureResource::class).getBuildList(
            beginDate = beginDate ?: 1,
            endDate = endDate ?: DateTime.now().millis,
            bgId = bgId,
            offset = offset ?: 0,
            limit = limit ?: 10
        ).data
        watch.stop()
        buildList?.forEach {
            watch.start("get quality data for build(${it.buildId})")
            it.qualityData = client.get(ServiceQualityInterceptResource::class).listHistory(
                projectId = it.projectId,
                pipelineId = it.pipelineId,
                buildId = it.buildId
            ).data
            watch.stop()
        }
        logger.info("$interfaceName:watch:$watch")
        return buildList
    }
}