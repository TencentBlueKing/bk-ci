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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.BusTypeEnum
import com.tencent.devops.common.api.enums.TaskStatusEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.model.process.tables.records.TPipelineAtomReplaceHistoryRecord
import com.tencent.devops.model.process.tables.records.TPipelineAtomReplaceItemRecord
import com.tencent.devops.process.dao.PipelineAtomReplaceBaseDao
import com.tencent.devops.process.dao.PipelineAtomReplaceHistoryDao
import com.tencent.devops.process.dao.PipelineAtomReplaceItemDao
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.service.template.TemplateFacadeService
import javax.ws.rs.core.Response
import org.jooq.DSLContext
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class PipelineAtomRollBackCronService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineAtomReplaceBaseDao: PipelineAtomReplaceBaseDao,
    private val pipelineAtomReplaceItemDao: PipelineAtomReplaceItemDao,
    private val pipelineAtomReplaceHistoryDao: PipelineAtomReplaceHistoryDao,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val templateFacadeService: TemplateFacadeService,
    private val redisOperation: RedisOperation
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineAtomRollBackCronService::class.java)
        private const val LOCK_KEY = "pipelineAtomRollBack"
        private const val ITEM_PAGE_SIZE = 10
        private const val HISTORY_PAGE_SIZE = 100
    }

    @Value("\${pipeline.atom.replaceSwitch:true}")
    private val switch: Boolean = true

    @Scheduled(cron = "0 0/1 * * * ?")
    fun pipelineAtomRollBack() {
        if (!switch) {
            // 开关关闭，则不替换回滚
            return
        }
        val lock = RedisLock(redisOperation, LOCK_KEY, 3000)
        try {
            if (!lock.tryLock()) {
                logger.info("get lock failed, skip")
                return
            }
            logger.info("begin pipelineAtomRollBack!!")
            // 按批次回滚已经替换的插件
            val atomReplaceBaseRecords = pipelineAtomReplaceBaseDao.getAtomReplaceBaseList(
                dslContext = dslContext,
                descFlag = false,
                statusList = listOf(TaskStatusEnum.PENDING_ROLLBACK.name),
                page = 1,
                pageSize = 1
            )
            atomReplaceBaseRecords?.forEach nextBase@{ atomReplaceBaseRecord ->
                val baseId = atomReplaceBaseRecord.id
                val userId = atomReplaceBaseRecord.modifier
                pipelineAtomReplaceBaseDao.updateAtomReplaceBase(
                    dslContext = dslContext,
                    baseId = baseId,
                    status = TaskStatusEnum.ROLLBACKING.name,
                    userId = userId
                )
                val atomReplaceItemCount = pipelineAtomReplaceItemDao.getAtomReplaceItemCountByBaseId(
                        dslContext = dslContext,
                        baseId = baseId,
                        statusList = listOf(TaskStatusEnum.PENDING_ROLLBACK.name)
                    )
                if (atomReplaceItemCount < 1) {
                    return@nextBase
                }
                // 开始处理该批次要替换的插件
                handleAtomReplaceBase(atomReplaceItemCount, baseId, userId)
            }
        } catch (ignored: Throwable) {
            logger.warn("pipelineAtomRollBack failed", ignored)
        } finally {
            lock.unlock()
        }
    }

    private fun handleAtomReplaceBase(atomReplaceItemCount: Long, baseId: String, userId: String) {
        try {
            val totalPages = PageUtil.calTotalPage(ITEM_PAGE_SIZE, atomReplaceItemCount)
            for (page in 1..totalPages) {
                val atomReplaceItemList = pipelineAtomReplaceItemDao.getAtomReplaceItemListByBaseId(
                    dslContext = dslContext,
                    baseId = baseId,
                    statusList = listOf(TaskStatusEnum.PENDING_ROLLBACK.name),
                    descFlag = false,
                    page = page,
                    pageSize = ITEM_PAGE_SIZE
                )
                atomReplaceItemList?.forEach nextItem@{ atomReplaceItem ->
                    handleAtomReplaceItem(atomReplaceItem, baseId, page)
                }
            }
            pipelineAtomReplaceBaseDao.updateAtomReplaceBase(
                dslContext = dslContext,
                baseId = baseId,
                status = TaskStatusEnum.ROLLBACK_SUCCESS.name,
                userId = userId
            )
            logger.info("pipelineAtomRollBack baseId:$baseId rollback success!!")
        } catch (ignored: Throwable) {
            logger.warn("pipelineAtomRollBack baseId:$baseId rollback fail:", ignored)
            pipelineAtomReplaceBaseDao.updateAtomReplaceBase(
                dslContext = dslContext,
                baseId = baseId,
                status = TaskStatusEnum.ROLLBACK_FAIL.name,
                userId = userId
            )
        }
    }

    private fun handleAtomReplaceItem(
        atomReplaceItem: TPipelineAtomReplaceItemRecord,
        baseId: String,
        page: Int
    ) {
        val itemId = atomReplaceItem.id
        try {
            pipelineAtomReplaceItemDao.updateAtomReplaceItemByItemId(
                dslContext = dslContext,
                itemId = itemId,
                status = TaskStatusEnum.ROLLBACKING.name,
                userId = atomReplaceItem.modifier
            )
            // 依次替换任务中已经替换插件成功的流水线和模板
            do {
                val pipelineReplaceHistoryList = pipelineAtomReplaceHistoryDao.getAtomReplaceHistoryList(
                    dslContext = dslContext,
                    baseId = baseId,
                    itemId = itemId,
                    busType = BusTypeEnum.PIPELINE.name,
                    statusList = listOf(TaskStatusEnum.SUCCESS.name),
                    descFlag = false,
                    page = page,
                    pageSize = HISTORY_PAGE_SIZE
                )
                pipelineReplaceHistoryList?.forEach { pipelineReplaceHistory ->
                    rollBackPipelineReplaceHistory(pipelineReplaceHistory)
                }
                val templateReplaceHistoryList = pipelineAtomReplaceHistoryDao.getAtomReplaceHistoryList(
                    dslContext = dslContext,
                    baseId = baseId,
                    itemId = itemId,
                    busType = BusTypeEnum.TEMPLATE.name,
                    statusList = listOf(TaskStatusEnum.SUCCESS.name),
                    descFlag = false,
                    page = page,
                    pageSize = HISTORY_PAGE_SIZE
                )
                templateReplaceHistoryList?.forEach { templateReplaceHistory ->
                    rollBackTemplateReplaceHistory(templateReplaceHistory)
                }
            } while (getReplaceAtomCondition(pipelineReplaceHistoryList, templateReplaceHistoryList))
            // 将任务记录的状态更新为”回滚成功“
            pipelineAtomReplaceItemDao.updateAtomReplaceItemByItemId(
                dslContext = dslContext,
                itemId = itemId,
                status = TaskStatusEnum.ROLLBACK_SUCCESS.name,
                userId = atomReplaceItem.modifier
            )
            logger.info("pipelineAtomRollBack itemId:$itemId rollback success!!")
        } catch (ignored: Throwable) {
            logger.warn("pipelineAtomRollBack itemId:$itemId rollback fail:", ignored)
            pipelineAtomReplaceItemDao.updateAtomReplaceItemByItemId(
                dslContext = dslContext,
                itemId = itemId,
                status = TaskStatusEnum.ROLLBACK_FAIL.name,
                userId = atomReplaceItem.modifier
            )
        }
    }

    private fun getReplaceAtomCondition(
        pipelineReplaceHistoryList: Result<TPipelineAtomReplaceHistoryRecord>?,
        templateReplaceHistoryList: Result<TPipelineAtomReplaceHistoryRecord>?
    ) = pipelineReplaceHistoryList?.isNotEmpty == true || templateReplaceHistoryList?.isNotEmpty == true

    private fun rollBackPipelineReplaceHistory(pipelineReplaceHistory: TPipelineAtomReplaceHistoryRecord) {
        val historyId = pipelineReplaceHistory.id
        val projectId = pipelineReplaceHistory.projectId
        val pipelineId = pipelineReplaceHistory.busId
        try {
            val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
            if (pipelineInfo == null) {
                val params = arrayOf(pipelineId)
                throw ErrorCodeException(
                    statusCode = Response.Status.INTERNAL_SERVER_ERROR.statusCode,
                    errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                    params = params
                )
            }
            val sourceVersion = pipelineReplaceHistory.sourceVersion
            val sourceModel = pipelineRepositoryService.getModel(
                projectId = projectId,
                pipelineId = pipelineId,
                version = sourceVersion
            )
            if (sourceModel == null) {
                val params = arrayOf("$pipelineId+$sourceVersion")
                throw ErrorCodeException(
                    statusCode = Response.Status.INTERNAL_SERVER_ERROR.statusCode,
                    errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                    params = params
                )
            }
            sourceModel.latestVersion = 0 // latestVersion置为0以便适配修改流水线的校验逻辑
            pipelineRepositoryService.deployPipeline(
                model = sourceModel,
                projectId = projectId,
                signPipelineId = pipelineId,
                userId = pipelineInfo.lastModifyUser,
                channelCode = pipelineInfo.channelCode,
                create = false
            )
            pipelineAtomReplaceHistoryDao.updateAtomReplaceHistory(
                dslContext = dslContext,
                id = historyId,
                status = TaskStatusEnum.ROLLBACK_SUCCESS.name,
                userId = pipelineReplaceHistory.creator
            )
        } catch (ignored: Throwable) {
            logger.warn("rollback pipeline atom fail:", ignored)
            pipelineAtomReplaceHistoryDao.updateAtomReplaceHistory(
                dslContext = dslContext,
                id = historyId,
                status = TaskStatusEnum.ROLLBACK_FAIL.name,
                userId = pipelineReplaceHistory.creator,
                log = getErrorMessage(ignored)
            )
        }
    }

    private fun rollBackTemplateReplaceHistory(templateReplaceHistory: TPipelineAtomReplaceHistoryRecord) {
        val historyId = templateReplaceHistory.id
        val templateId = templateReplaceHistory.busId
        val projectId = templateReplaceHistory.projectId
        val template = templateFacadeService.getTemplate(
            projectId = templateReplaceHistory.projectId,
            userId = templateReplaceHistory.creator,
            templateId = templateId,
            version = templateReplaceHistory.sourceVersion.toLong()
        )
        try {
            templateFacadeService.updateTemplate(
                projectId = projectId,
                userId = template.creator,
                templateId = templateId,
                versionName = template.currentVersion.versionName,
                template = template.template
            )
            pipelineAtomReplaceHistoryDao.updateAtomReplaceHistory(
                dslContext = dslContext,
                id = historyId,
                status = TaskStatusEnum.ROLLBACK_SUCCESS.name,
                userId = templateReplaceHistory.creator
            )
        } catch (ignored: Throwable) {
            logger.warn("rollback template atom fail:", ignored)
            pipelineAtomReplaceHistoryDao.updateAtomReplaceHistory(
                dslContext = dslContext,
                id = historyId,
                status = TaskStatusEnum.ROLLBACK_FAIL.name,
                userId = templateReplaceHistory.creator,
                log = getErrorMessage(ignored)
            )
        }
    }

    private fun getErrorMessage(ignored: Throwable): String? {
        var message = ignored.message
        if (message != null && message.length > 128) {
            message = message.substring(0, 127)
        }
        return message
    }
}
