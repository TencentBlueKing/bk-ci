/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

package com.tencent.devops.environment.resources

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.UserBcsClusterResource
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.pojo.BcsCluster
import com.tencent.devops.environment.pojo.BcsImageInfo
import com.tencent.devops.environment.pojo.BcsVmModel
import com.tencent.devops.environment.pojo.BcsVmParam
import com.tencent.devops.environment.pojo.ProjectInfo
import com.tencent.devops.environment.service.BcsClusterService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserBcsClusterResourceImpl @Autowired constructor(private val bcsClusterService: BcsClusterService) :
    UserBcsClusterResource {

    override fun addBcsVmNodes(userId: String, projectId: String, bcsVmParam: BcsVmParam): Result<Boolean> {
        if (bcsVmParam.validity > 14) {
            throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_ENV_EXPIRED_DAYS,
                defaultMessage = "有效期不能超过14天",
                params = arrayOf("14")
            )
        }

        bcsClusterService.addBcsVmNodes(userId, projectId, bcsVmParam)
        return Result(true)
    }

    override fun getProjectInfo(userId: String, projectId: String): Result<ProjectInfo> {
        return Result(bcsClusterService.getProjectInfo(userId, projectId))
    }

    override fun getClusterList(): Result<List<BcsCluster>> {
        return Result(bcsClusterService.listBcsCluster())
    }

    override fun getImageList(userId: String, projectId: String): Result<List<BcsImageInfo>> {
        return Result(bcsClusterService.listBcsImageList())
    }

    override fun getVmModelList(userId: String, projectId: String): Result<List<BcsVmModel>> {
        return Result(bcsClusterService.listBcsVmModel())
    }
}