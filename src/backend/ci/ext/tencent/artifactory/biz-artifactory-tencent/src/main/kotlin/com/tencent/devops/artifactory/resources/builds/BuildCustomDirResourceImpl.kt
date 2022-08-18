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

package com.tencent.devops.artifactory.resources.builds

import com.tencent.devops.artifactory.api.builds.BuildCustomDirResource
import com.tencent.devops.artifactory.pojo.CombinationPath
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.PathList
import com.tencent.devops.artifactory.pojo.PathPair
import com.tencent.devops.artifactory.service.bkrepo.BkRepoBuildCustomDirService
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildCustomDirResourceImpl @Autowired constructor(
    private val bkRepoBuildCustomDirService: BkRepoBuildCustomDirService,
    private val client: Client
) : BuildCustomDirResource {
    override fun list(pipelineId: String, projectId: String, path: String): List<FileInfo> {
        if (path.contains(".")) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.ERROR_INVALID_PARAM_,
                defaultMessage = "please confirm the param is directory...",
                params = arrayOf(path)
            )
        }
        val userId = getLastModifyUser(projectId, pipelineId)
        return bkRepoBuildCustomDirService.list(userId, projectId, path)
    }

    override fun mkdir(pipelineId: String, projectId: String, path: String): Result<Boolean> {
        logger.info("mkdir, projectId: $projectId")
        val userId = getLastModifyUser(projectId, pipelineId)
        bkRepoBuildCustomDirService.mkdir(userId, projectId, path)
        return Result(true)
    }

    override fun rename(pipelineId: String, projectId: String, pathPair: PathPair): Result<Boolean> {
        val userId = getLastModifyUser(projectId, pipelineId)
        bkRepoBuildCustomDirService.rename(userId, projectId, pathPair.srcPath, pathPair.destPath)
        return Result(true)
    }

    override fun copy(pipelineId: String, projectId: String, combinationPath: CombinationPath): Result<Boolean> {
        val userId = getLastModifyUser(projectId, pipelineId)
        bkRepoBuildCustomDirService.copy(userId, projectId, combinationPath)
        return Result(true)
    }

    override fun move(pipelineId: String, projectId: String, combinationPath: CombinationPath): Result<Boolean> {
        val userId = getLastModifyUser(projectId, pipelineId)
        bkRepoBuildCustomDirService.move(userId, projectId, combinationPath)
        return Result(true)
    }

    override fun delete(pipelineId: String, projectId: String, pathList: PathList): Result<Boolean> {
        val userId = getLastModifyUser(projectId, pipelineId)
        bkRepoBuildCustomDirService.delete(userId, projectId, pathList)
        return Result(true)
    }

    private fun getLastModifyUser(projectId: String, pipelineId: String): String {
        return client.get(ServicePipelineResource::class)
            .getPipelineInfo(projectId, pipelineId, null).data!!.lastModifyUser
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BuildCustomDirResourceImpl::class.java)
    }
}
