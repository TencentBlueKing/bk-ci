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

package com.tencent.devops.process.engine.service.template

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildNo
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.Profile
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.model.process.tables.records.TTemplateRecord
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.engine.dao.template.TemplateInstanceBaseDao
import com.tencent.devops.process.engine.dao.template.TemplateInstanceItemDao
import com.tencent.devops.process.pojo.template.TemplateInstanceBaseStatus
import com.tencent.devops.process.pojo.template.TemplateInstanceUpdate
import com.tencent.devops.process.service.template.TemplateFacadeService
import com.tencent.devops.process.util.TempNotifyTemplateUtils
import com.tencent.devops.project.api.service.ServiceProjectTagResource
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.text.MessageFormat

@Suppress("ALL")
@Service
@RefreshScope
class TemplateInstanceCronService @Autowired constructor(
    private val dslContext: DSLContext,
    private val templateDao: TemplateDao,
    private val templateInstanceBaseDao: TemplateInstanceBaseDao,
    private val templateInstanceItemDao: TemplateInstanceItemDao,
    private val templateService: TemplateFacadeService,
    private val redisOperation: RedisOperation,
    private val client: Client
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TemplateInstanceCronService::class.java)
        private const val LOCK_KEY = "templateInstanceItemLock"
        private const val PAGE_SIZE = 100
    }

    @Value("\${template.instanceListUrl}")
    private val instanceListUrl: String = ""

    @Value("\${template.maxErrorReasonLength:200}")
    private val maxErrorReasonLength: Int = 200

    @Scheduled(cron = "0 0/1 * * * ?")
    fun templateInstance() {
        val profile = SpringContextUtil.getBean(Profile::class.java)
        val activeProfiles = profile.getActiveProfiles()
        val key = if (activeProfiles.size > 1) {
            val sb = StringBuilder()
            activeProfiles.forEach { activeProfile ->
                sb.append("$activeProfile:")
            }
            sb.append(LOCK_KEY).toString()
        } else {
            LOCK_KEY
        }
        val lock = RedisLock(redisOperation, key, 3000)
        try {
            if (!lock.tryLock()) {
                logger.info("get lock[$key] failed, skip")
                return
            }
            val statusList = listOf(TemplateInstanceBaseStatus.INIT.name, TemplateInstanceBaseStatus.INSTANCING.name)
            val templateInstanceBaseList = templateInstanceBaseDao.getTemplateInstanceBaseList(
                dslContext = dslContext,
                statusList = statusList,
                descFlag = false,
                page = 1,
                pageSize = 10
            )
            templateInstanceBaseList?.forEach { templateInstanceBase ->
                val baseId = templateInstanceBase.id
                val projectId = templateInstanceBase.projectId
                val templateId = templateInstanceBase.templateId
                val projectRouterTagCheck =
                    client.get(ServiceProjectTagResource::class).checkProjectRouter(projectId).data
                if (!projectRouterTagCheck!!) {
                    logger.info("project $projectId router tag is not this cluster")
                    return@forEach
                }

                // 把模板批量更新记录状态置为”实例化中“
                templateInstanceBaseDao.updateTemplateInstanceBase(
                    dslContext = dslContext,
                    projectId = projectId,
                    baseId = baseId,
                    status = TemplateInstanceBaseStatus.INSTANCING.name,
                    userId = "system"
                )
                val successPipelines = ArrayList<String>()
                val failurePipelines = ArrayList<String>()
                val templateInstanceItemCount = templateInstanceItemDao.getTemplateInstanceItemCountByBaseId(
                    dslContext = dslContext,
                    projectId = projectId,
                    baseId = baseId
                )
                if (templateInstanceItemCount < 1) {
                    templateInstanceBaseDao.deleteByBaseId(dslContext, projectId, baseId)
                    return@forEach
                }
                val templateVersion = templateInstanceBase.templateVersion.toLong()
                val template: TTemplateRecord?
                try {
                    template = templateDao.getTemplate(dslContext = dslContext, version = templateVersion)
                } catch (e: ErrorCodeException) {
                    if (e.errorCode == ProcessMessageCode.ERROR_TEMPLATE_NOT_EXISTS) {
                        // 模板版本记录如果已经被删，则无需执行更新任务并把任务记录删除
                        logger.warn("the version[$templateVersion] of template[$templateId] is not exist,skip the task")
                        deleteTemplateInstanceTaskRecord(projectId, baseId)
                        return@forEach
                    } else {
                        throw e
                    }
                }
                val totalPages = PageUtil.calTotalPage(PAGE_SIZE, templateInstanceItemCount)
                // 分页切片处理当前批次的待处理任务
                for (page in 1..totalPages) {
                    val templateInstanceItemList = templateInstanceItemDao.getTemplateInstanceItemListByBaseId(
                        dslContext = dslContext,
                        projectId = projectId,
                        baseId = baseId,
                        descFlag = false,
                        page = page,
                        pageSize = PAGE_SIZE
                    )
                    templateInstanceItemList?.forEach { templateInstanceItem ->
                        val userId = templateInstanceItem.creator
                        val pipelineName = templateInstanceItem.pipelineName
                        val paramStr = templateInstanceItem.param
                        val param = if (paramStr != null) {
                            JsonUtil.to(paramStr, object : TypeReference<List<BuildFormProperty>?>() {})
                        } else {
                            null
                        }
                        try {
                            templateService.updateTemplateInstanceInfo(
                                userId = userId,
                                useTemplateSettings = templateInstanceBase.useTemplateSettingsFlag,
                                projectId = projectId,
                                templateId = templateId,
                                templateVersion = template.version,
                                versionName = template.versionName,
                                templateContent = template.template,
                                templateInstanceUpdate = TemplateInstanceUpdate(
                                    pipelineId = templateInstanceItem.pipelineId,
                                    pipelineName = templateInstanceItem.pipelineName,
                                    buildNo = JsonUtil.toOrNull(templateInstanceItem.buildNoInfo, BuildNo::class.java),
                                    param = param
                                )
                            )
                            successPipelines.add(pipelineName)
                        } catch (ignored: Throwable) {
                            logger.warn("Fail to update the pipeline|$pipelineName|$projectId|$userId|$ignored")
                            val message =
                                if (!ignored.message.isNullOrBlank() && ignored.message!!.length > maxErrorReasonLength)
                                    ignored.message!!.substring(0, maxErrorReasonLength) + "......"
                                else ignored.message
                            failurePipelines.add("【$pipelineName】reason: $message")
                        }
                    }
                }
                // 删除模板更新任务记录
                deleteTemplateInstanceTaskRecord(projectId, baseId)
                // 发送执行任务结果通知
                TempNotifyTemplateUtils.sendUpdateTemplateInstanceNotify(
                    client = client,
                    projectId = projectId,
                    receivers = mutableSetOf(templateInstanceBase.creator),
                    instanceListUrl = MessageFormat(instanceListUrl).format(arrayOf(projectId, template.id)),
                    successPipelines = successPipelines,
                    failurePipelines = failurePipelines
                )
            }
        } catch (ignored: Throwable) {
            logger.error("BKSystemErrorMonitor|templateInstance|error=${ignored.message}", ignored)
        } finally {
            lock.unlock()
        }
    }

    private fun deleteTemplateInstanceTaskRecord(projectId: String, baseId: String) {
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            templateInstanceItemDao.deleteByBaseId(context, projectId, baseId)
            templateInstanceBaseDao.deleteByBaseId(context, projectId, baseId)
        }
    }
}
