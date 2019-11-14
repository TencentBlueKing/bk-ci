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

package com.tencent.devops.prebuild.resources

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.log.model.pojo.QueryLogs
import com.tencent.devops.plugin.pojo.codecc.CodeccCallback
import com.tencent.devops.prebuild.api.UserPreBuildResource
import com.tencent.devops.prebuild.pojo.UserProject
import com.tencent.devops.prebuild.pojo.Prebuild
import com.tencent.devops.prebuild.pojo.PreProjectReq
import com.tencent.devops.prebuild.pojo.InitPreProjectTask
import com.tencent.devops.prebuild.pojo.HistoryResponse
import com.tencent.devops.prebuild.service.PreBuildService
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import com.tencent.devops.prebuild.pojo.PreProject
import com.tencent.devops.prebuild.pojo.UserNode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserPreBuildResourceImpl @Autowired constructor(
    private val preBuildService: PreBuildService
) : UserPreBuildResource {

    override fun getUserProject(userId: String, accessToken: String): Result<UserProject> {
        return Result(preBuildService.getOrCreateUserProject(userId, accessToken))
    }

    override fun createUserNode(userId: String): Result<UserNode> {
        return Result(preBuildService.createUserNode(userId))
    }

    override fun listNode(userId: String): Result<List<UserNode>> {
        return Result(preBuildService.listUserNodes(userId))
    }

    override fun executeCmdInNode(userId: String, command: String): Result<Pair<Int, String>> {
        return Result(preBuildService.executeCmdInUserNode(userId, command))
    }

    override fun listPreProject(userId: String): Result<List<PreProject>> {
        return Result(preBuildService.listPreProject(userId))
    }

    override fun preProjectNameExist(userId: String, preProjectId: String): Result<Boolean> {
        return Result(preBuildService.projectNameExist(userId, preProjectId))
    }

    override fun manualStartup(userId: String, accessToken: String, preProjectId: String, yaml: String): Result<BuildId> {
        logger.info("buildYaml: $yaml")
        val preBuild = try {
            YamlUtil.getObjectMapper().readValue(yaml, Prebuild::class.java)
        } catch (e: Throwable) {
            logger.error("Exception:", e)
            throw OperationException("操作失败, YAML不合法")
        }

        return Result(preBuildService.manualStartup(userId, accessToken, preProjectId, yaml, preBuild))
    }

    override fun manualShutdown(userId: String, accessToken: String, preProjectId: String, buildId: String): Result<Boolean> {
        return Result(preBuildService.shutDown(userId, accessToken, preProjectId, buildId))
    }

    override fun getBuildDetail(userId: String, preProjectId: String, buildId: String): Result<ModelDetail> {
        return preBuildService.getBuildDetail(userId, preProjectId, buildId)
    }

    override fun getBuildLogs(userId: String, preProjectId: String, buildId: String): Result<QueryLogs> {
        return Result(preBuildService.getInitLogs(userId, preProjectId, buildId))
    }

    override fun getAfterLogs(userId: String, preProjectId: String, buildId: String, start: Long): Result<QueryLogs> {
        return Result(preBuildService.getAfterLogs(userId, preProjectId, buildId, start))
    }

    override fun getReport(userId: String, buildId: String): Result<CodeccCallback?> {
        return preBuildService.getCodeccReport(userId, buildId)
    }

    override fun init(userId: String, accessToken: String, req: PreProjectReq): Result<InitPreProjectTask> {
        return Result(preBuildService.initPreProject(userId, accessToken, req))
    }

    override fun queryInitTaskStatus(userId: String, taskId: String): Result<InitPreProjectTask> {
        return Result(preBuildService.queryInitTask(userId, taskId))
    }

    override fun getHistory(userId: String, preProjectId: String, page: Int?, pageSize: Int?): Result<List<HistoryResponse>> {
        return Result(preBuildService.getHistory(userId, preProjectId, page, pageSize))
    }

    override fun getBuildLink(userId: String, preProjectId: String, buildId: String): Result<String> {
        return Result(preBuildService.getBuildLink(userId, preProjectId, buildId))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserPreBuildResourceImpl::class.java)
    }
}
