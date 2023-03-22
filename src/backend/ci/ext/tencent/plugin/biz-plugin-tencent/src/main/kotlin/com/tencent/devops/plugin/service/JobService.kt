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

package com.tencent.devops.plugin.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.environment.api.ServiceEnvironmentResource
import com.tencent.devops.environment.api.ServiceNodeResource
import com.tencent.devops.environment.pojo.EnvWithPermission
import com.tencent.devops.environment.pojo.NodeBaseInfo
import com.tencent.devops.environment.pojo.NodeWithPermission
import com.tencent.devops.plugin.constant.PluginCode.BK_BUILDID_NOT_FOUND
import com.tencent.devops.plugin.constant.PluginCode.BK_PIPELINEID_NOT_FOUND
import com.tencent.devops.process.api.builds.BuildHistoryBuildResource
import com.tencent.devops.process.api.service.ServiceOperationResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * @Description 为插件【作业平台-执行脚本】提供build接口的实现
 * @Author Jsonwan
 * @Date 2019/8/19
 * @Version 1.0
 */
@Service
class JobService @Autowired constructor(
    val client: Client
) {
    companion object {
        private val logger = LoggerFactory.getLogger(JobService::class.java)
    }
    // 根据buildId查出构建启动人
    private fun getUserId(buildId: String): String? {
        logger.info("getUserId(buildId=$buildId)=")
        val historyBuildResult = client.get(BuildHistoryBuildResource::class).getSingleHistoryByBuildId(buildId)
        logger.info("userId=${historyBuildResult.data?.userId}|====end==getUserId====(buildId=$buildId)")
        return historyBuildResult.data?.userId
    }

    fun listUsableServerEnvs(projectId: String, buildId: String): Result<List<EnvWithPermission>> {
        logger.info("listUsableServerEnvs(projectId=$projectId,buildId=$buildId)=")
        val userId = getUserId(buildId) ?: return Result(500, MessageUtil.getMessageByLocale(
            messageCode = BK_BUILDID_NOT_FOUND,
            language = I18nUtil.getLanguage(),
            params = arrayOf(buildId)
        ))
        // 以启动人的身份调用service接口获取信息
        val result = client.get(ServiceEnvironmentResource::class).listUsableServerEnvs(userId, projectId)
        logger.info("listUsableServerEnvs==Return===\n${jacksonObjectMapper().writeValueAsString(result)}")
        return result
    }

    fun listUsableServerNodes(projectId: String, buildId: String): Result<List<NodeWithPermission>> {
        logger.info("listUsableServerNodes(projectId=$projectId,buildId=$buildId)=")
        val userId = getUserId(buildId) ?: return Result(500, MessageUtil.getMessageByLocale(
            messageCode = BK_BUILDID_NOT_FOUND,
            language = I18nUtil.getLanguage(),
            params = arrayOf(buildId)
        ))
        // 以启动人的身份调用service接口获取信息
        val result = client.get(ServiceNodeResource::class).listUsableServerNodes(userId, projectId)
        logger.info("listUsableServerNodes==Return===\n${jacksonObjectMapper().writeValueAsString(result)}")
        return result
    }

    fun listRawByEnvHashIds(projectId: String, buildId: String, envHashIds: List<String>): Result<List<EnvWithPermission>> {
        logger.info("listRawByEnvHashIds(projectId=$projectId,buildId=$buildId,envHashIds=${envHashIds.reduce{s1,s2 -> "[$s1,$s2]"}})=")
        val userId = getUserId(buildId) ?: return Result(500, MessageUtil.getMessageByLocale(
            messageCode = BK_BUILDID_NOT_FOUND,
            language = I18nUtil.getLanguage(),
            params = arrayOf(buildId)
        ))
        val result = client.get(ServiceEnvironmentResource::class).listRawByEnvHashIds(userId, projectId, envHashIds)
        logger.info("listRawByEnvHashIds==Return===\n${jacksonObjectMapper().writeValueAsString(result)}")
        return result
    }

    fun listRawByEnvNames(projectId: String, buildId: String, envNames: List<String>): Result<List<EnvWithPermission>> {
        logger.info("listRawByEnvNames(projectId=$projectId,buildId=$buildId,envNames=${envNames.reduce{s1,s2 -> "[$s1,$s2]"}})=")
        val userId = getUserId(buildId) ?: return Result(500, MessageUtil.getMessageByLocale(
            messageCode = BK_BUILDID_NOT_FOUND,
            language = I18nUtil.getLanguage(),
            params = arrayOf(buildId)
        ))
        val result = client.get(ServiceEnvironmentResource::class).listRawByEnvNames(userId, projectId, envNames)
        logger.info("listRawByEnvNames==Return===\n${jacksonObjectMapper().writeValueAsString(result)}")
        return result
    }

    fun listRawNodesByHashIds(projectId: String, buildId: String, nodeHashIds: List<String>): Result<List<NodeBaseInfo>> {
        logger.info("listRawNodesByHashIds(projectId=$projectId,buildId=$buildId,nodeHashIds=${nodeHashIds.reduce{s1,s2 -> "[$s1,$s2]"}})=")
        val userId = getUserId(buildId) ?: return Result(500, MessageUtil.getMessageByLocale(
            messageCode = BK_BUILDID_NOT_FOUND,
            language = I18nUtil.getLanguage(),
            params = arrayOf(buildId)
        ))
        val result = client.get(ServiceNodeResource::class).listRawByHashIds(userId, projectId, nodeHashIds)
        logger.info("listRawNodesByHashIds==Return===\n${jacksonObjectMapper().writeValueAsString(result)}")
        return result
    }

    fun listUsableServerEnvsByLastUpdateUser(projectId: String, pipelineId: String): Result<List<EnvWithPermission>> {
        logger.info("listUsableServerEnvsByLastUpdateUser(projectId=$projectId, pipelineId=$pipelineId")
        val userId = getLastUpdateUserId(projectId, pipelineId)
            ?: return Result(500, MessageUtil.getMessageByLocale(
                messageCode = BK_PIPELINEID_NOT_FOUND,
                language = I18nUtil.getLanguage(),
                params = arrayOf(pipelineId)
            ))
        // 以流水线最后修改人的身份调用service接口获取信息
        val result = client.get(ServiceEnvironmentResource::class).listUsableServerEnvs(userId, projectId)
        logger.info("listUsableServerEnvs==Return===\n${jacksonObjectMapper().writeValueAsString(result)}")
        return result
    }

    fun listUsableServerNodesByLastUpdateUser(projectId: String, pipelineId: String): Result<List<NodeWithPermission>> {
        logger.info("listUsableServerNodesByLastUpdateUser(projectId=$projectId, pipelineId=$pipelineId")
        val userId = getLastUpdateUserId(projectId, pipelineId)
            ?: return Result(500, MessageUtil.getMessageByLocale(
                messageCode = BK_PIPELINEID_NOT_FOUND,
                language = I18nUtil.getLanguage(),
                params = arrayOf(pipelineId)
            ))
        // 以流水线最后修改人的身份调用service接口获取信息
        val result = client.get(ServiceNodeResource::class).listUsableServerNodes(userId, projectId)
        logger.info("listUsableServerNodes==Return===\n${jacksonObjectMapper().writeValueAsString(result)}")
        return result
    }

    // 根据pipelineId查出最后修改人
    private fun getLastUpdateUserId(projectId: String, pipelineId: String): String? {
        logger.info("getLastUpdateUserId(pipelineId=$pipelineId)=")
        val updateUser = client.get(ServiceOperationResource::class).getUpdateUser(projectId, pipelineId)
        logger.info("userId=${updateUser.data}|====end==getUserId====(pipelineId=$pipelineId)")
        return updateUser.data
    }
}
