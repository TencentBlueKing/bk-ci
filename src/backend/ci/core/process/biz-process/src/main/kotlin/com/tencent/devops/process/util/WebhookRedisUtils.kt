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

package com.tencent.devops.process.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitlabWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeSVNWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerElement
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class WebhookRedisUtils @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper
) {

    fun getProjectName(projectName: String): String {
        // 如果项目名是三层的，比如ied/ied_kihan_rep/server_proj，那对应的rep_name 是 ied_kihan_rep
        val repoSplit = projectName.split("/")
        if (repoSplit.size != 3) {
            return projectName
        }
        return repoSplit[1].trim()
    }

    fun addWebhook2Redis(
        pipelineId: String,
        name: String,
        type: ScmType,
        getExistWebhookPipelineByType: (type: ScmType) -> HashMap<String, Set<String>>
    ) {
        val projectName = getProjectName(name)
        val lock = RedisLock(redisOperation, getWebhookRedisLockKey(type), 30)
        try {
            lock.lock()
            val key = getWebhookRedisProjectKey(type)
            val values = redisOperation.hget(key, projectName)
            val webHookProjects: Set<String> = if (values == null) {
                logger.info("The $type web hook project is not exist, try refresh")
                getExistWebhookPipelineByType(type)[projectName]
            } else {
                try {
                    val tmp: Set<String> = objectMapper.readValue(values)
                    tmp
                } catch (e: Exception) {
                    logger.warn("Fail to convert the webhook project -> pipelines to obj($values)", e)
                    getExistWebhookPipelineByType(type)[projectName]
                }
            } ?: emptySet()

            val results = webHookProjects.plus(pipelineId)

            logger.info("Adding the webhooks($results) of projectName($projectName) and type($type)")
            redisOperation.hset(key, projectName, objectMapper.writeValueAsString(results))
        } finally {
            lock.unlock()
        }
    }

    fun getWebhookPipelines(
        name: String,
        type: String,
        getExistWebhookPipelineByType: (type: ScmType) -> HashMap<String, Set<String>>
    ): Set<String> {
        val projectName = getProjectName(name)
        val repoType = getWebhookScmType(type)

        val key = getWebhookRedisProjectKey(repoType)
        var values = redisOperation.hget(key, projectName)
        return if (values == null) {
            val lock = RedisLock(redisOperation, getWebhookRedisLockKey(repoType), 30)
            try {
                lock.lock()
                values = redisOperation.hget(key, projectName)
                if (values == null) {
                    logger.info("The $type web hook project is not exist, try refresh")
                    getAndUpdate(repoType, key, projectName, getExistWebhookPipelineByType)
                } else {
                    try {
                        val tmp: Set<String> = objectMapper.readValue(values)
                        tmp
                    } catch (e: Exception) {
                        logger.warn("Fail to convert the webhook project -> pipelines to obj($values)", e)
                        getAndUpdate(repoType, key, projectName, getExistWebhookPipelineByType)
                    }
                }
            } finally {
                lock.unlock()
            }
        } else {
            try {
                val tmp: Set<String> = objectMapper.readValue(values)
                tmp
            } catch (e: Exception) {
                logger.warn("Fail to convert the webhook project -> pipelines to obj($values)", e)
                getAndUpdate(repoType, key, projectName, getExistWebhookPipelineByType)
            }
        } ?: emptySet()
    }

    private fun getAndUpdate(
        type: ScmType,
        key: String,
        projectName: String,
        getExistWebhookPipelineByType: (type: ScmType) -> HashMap<String, Set<String>>
    ): Set<String>? {
        val exist = getExistWebhookPipelineByType(type)
        logger.info("Setting the pipelines($exist) of type($type)")
        exist.forEach { project, pipelines ->
            redisOperation.hset(key, project, objectMapper.writeValueAsString(pipelines))
        }
        return exist[projectName]
    }

    private fun getWebhookScmType(type: String) =
            when (type) {
                CodeGitWebHookTriggerElement.classType -> {
                    ScmType.CODE_GIT
                }
                CodeSVNWebHookTriggerElement.classType -> {
                    ScmType.CODE_SVN
                }
                CodeGitlabWebHookTriggerElement.classType -> {
                    ScmType.CODE_GITLAB
                }
                CodeGithubWebHookTriggerElement.classType -> {
                    ScmType.GITHUB
                }
                CodeTGitWebHookTriggerElement.classType -> {
                    ScmType.CODE_TGIT
                }
                else -> {
                    throw RuntimeException("Unknown web hook type($type)")
                }
            }

    companion object {
        private val logger = LoggerFactory.getLogger(WebhookRedisUtils::class.java)
    }

    private fun getWebhookRedisLockKey(type: ScmType) =
            "webhook_${type.name}_redis_lock"

    private fun getWebhookRedisProjectKey(type: ScmType) =
            "process_webhook_${type.name}_projects"
}
