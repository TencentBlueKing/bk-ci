/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.quality.resources.v2

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.quality.api.v2.UserQualityControlPointResource
import com.tencent.devops.quality.api.v2.pojo.QualityControlPoint
import com.tencent.devops.quality.api.v2.pojo.response.ControlPointStageGroup
import com.tencent.devops.quality.service.v2.QualityControlPointService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserQualityControlPointResourceImpl @Autowired constructor(
    val controlPointService: QualityControlPointService
) : UserQualityControlPointResource {
    override fun listElementType(userId: String, projectId: String): Result<List<String>> {
        return Result(controlPointService.userList(userId, projectId).filter { it.enable }.map { it.type })
    }

    override fun get(userId: String, projectId: String, elementType: String): Result<QualityControlPoint> {
        return Result(data = controlPointService.userGetByType(projectId, elementType)
            ?: throw OperationException("control point ($elementType) not found"))
    }

    override fun listProjectElementType(userId: String, projectId: String): Result<List<String>> {
        return Result(controlPointService.userList(userId, projectId).filter { it.enable }.map { it.type })
    }

    override fun getProjectControlPoint(
        userId: String,
        projectId: String,
        elementType: String
    ): Result<QualityControlPoint> {
        return Result(data = controlPointService.userGetByType(projectId, elementType)
            ?: throw OperationException("control point ($elementType) not found"))
    }

    override fun list(userId: String, projectId: String): Result<List<ControlPointStageGroup>> {
        return Result(controlPointService.userList(userId = userId, projectId = projectId)
            .filter { it.enable }.groupBy { it.stage }.map { ControlPointStageGroup(it.key, it.value) })
    }
}
