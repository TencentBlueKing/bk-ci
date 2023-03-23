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
package com.tencent.devops.lambda.service.project

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.kafka.KafkaClient
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.lambda.LambdaMessageCode.STARTUP_CONFIGURATION_MISSING
import com.tencent.devops.lambda.config.LambdaKafkaTopicConfig
import com.tencent.devops.lambda.dao.project.LambdaProjectDao
import com.tencent.devops.lambda.pojo.project.DataPlatProjectInfo
import com.tencent.devops.project.pojo.mq.ProjectCreateBroadCastEvent
import com.tencent.devops.project.pojo.mq.ProjectUpdateBroadCastEvent
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ForkJoinPool

@Service
class LambdaProjectService @Autowired constructor(
    private val kafkaClient: KafkaClient,
    private val dslContext: DSLContext,
    private val lambdaProjectDao: LambdaProjectDao,
    private val lambdaKafkaTopicConfig: LambdaKafkaTopicConfig
) {

    fun onReceiveProjectCreate(event: ProjectCreateBroadCastEvent) {
        try {
            logger.info("onReceiveProjectCreate projectId: ${event.projectId}")
            val projectInfo = DataPlatProjectInfo(
                projectName = event.projectInfo.projectName,
                projectType = event.projectInfo.projectType,
                bgId = event.projectInfo.bgId,
                bgName = event.projectInfo.bgName,
                centerId = event.projectInfo.centerId,
                centerName = event.projectInfo.centerName,
                deptId = event.projectInfo.deptId,
                deptName = event.projectInfo.deptName,
                description = event.projectInfo.description,
                englishName = event.projectInfo.englishName,
                ccAppId = 0,
                ccAppName = "",
                kind = event.projectInfo.kind,
                secrecy = event.projectInfo.secrecy,
                washTime = LocalDateTime.now().format(dateTimeFormatter)
            )
            val projectInfoTopic = checkParamBlank(lambdaKafkaTopicConfig.projectInfoTopic, "projectInfoTopic")
            kafkaClient.send(projectInfoTopic, JsonUtil.toJson(projectInfo))
//            kafkaClient.send(KafkaTopic.LANDUN_PROJECT_INFO_TOPIC, JsonUtil.toJson(projectInfo))
        } catch (e: Exception) {
            logger.warn("onReceiveProjectCreate failed, projectId: ${event.projectId}", e)
        }
    }

    fun onReceiveProjectUpdate(event: ProjectUpdateBroadCastEvent) {
        try {
            logger.info("onReceiveProjectUpdate projectId: ${event.projectId}")
            val projectInfo = DataPlatProjectInfo(
                projectName = event.projectInfo.projectName,
                projectType = event.projectInfo.projectType,
                bgId = event.projectInfo.bgId,
                bgName = event.projectInfo.bgName,
                centerId = event.projectInfo.centerId,
                centerName = event.projectInfo.centerName,
                deptId = event.projectInfo.deptId,
                deptName = event.projectInfo.deptName,
                description = event.projectInfo.description,
                englishName = event.projectInfo.englishName,
                ccAppId = event.projectInfo.ccAppId,
                ccAppName = event.projectInfo.ccAppName,
                kind = event.projectInfo.kind,
                secrecy = event.projectInfo.secrecy,
                washTime = LocalDateTime.now().format(dateTimeFormatter)
            )
            val projectInfoTopic = checkParamBlank(lambdaKafkaTopicConfig.projectInfoTopic, "projectInfoTopic")
            kafkaClient.send(projectInfoTopic, JsonUtil.toJson(projectInfo))
//            kafkaClient.send(KafkaTopic.LANDUN_PROJECT_INFO_TOPIC, JsonUtil.toJson(projectInfo))
        } catch (e: Exception) {
            logger.warn("onReceiveProjectUpdate failed, projectId: ${event.projectId}", e)
        }
    }

    fun syncProjectInfo(minId: Long, maxId: Long): Boolean {
        logger.info("====================>> syncProjectInfo startId: $minId, endId: $maxId")
        val projectList = lambdaProjectDao.getProjectList(dslContext, minId, maxId)
        val forkJoinPool = ForkJoinPool(10)
        forkJoinPool.submit {
            projectList.parallelStream().forEach {
                try {
                    val projectInfo = DataPlatProjectInfo(
                        projectName = it.projectName,
                        projectType = it.projectType,
                        bgId = it.bgId,
                        bgName = it.bgName,
                        centerId = it.centerId,
                        centerName = it.centerName,
                        deptId = it.deptId,
                        deptName = it.deptName,
                        description = it.description,
                        englishName = it.englishName,
                        ccAppId = it.ccAppId,
                        ccAppName = it.ccAppName,
                        kind = it.kind,
                        secrecy = it.isSecrecy,
                        washTime = LocalDateTime.now().format(dateTimeFormatter)
                    )
                    val projectInfoTopic = checkParamBlank(lambdaKafkaTopicConfig.projectInfoTopic, "projectInfoTopic")
                    kafkaClient.send(projectInfoTopic, JsonUtil.toJson(projectInfo))
//                    kafkaClient.send(KafkaTopic.LANDUN_PROJECT_INFO_TOPIC, JsonUtil.toJson(projectInfo))
                    logger.info("Success send project: ${it.projectId} to kafka.")
                } catch (e: Exception) {
                    logger.warn("Failed send project: ${it.projectId} to kafka.", e)
                }
            }
        }

        return true
    }

    private fun checkParamBlank(param: String?, message: String): String {
        if (param.isNullOrBlank()) {
            throw ParamBlankException(
                MessageUtil.getMessageByLocale(
                messageCode = STARTUP_CONFIGURATION_MISSING,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()),
                params = arrayOf(message)
            ))
        }
        return param
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LambdaProjectService::class.java)
        private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}
