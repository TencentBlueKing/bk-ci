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

package com.tencent.devops.process.engine.service.template

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.dao.template.TemplateInstanceBaseDao
import com.tencent.devops.process.engine.dao.template.TemplateInstanceItemDao
import com.tencent.devops.process.pojo.template.TemplateInstanceBaseStatus
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class TemplateInstanceCronService @Autowired constructor(
    private val dslContext: DSLContext,
    private val templateInstanceBaseDao: TemplateInstanceBaseDao,
    private val templateInstanceItemDao: TemplateInstanceItemDao,
    private val templateService: TemplateService,
    private val redisOperation: RedisOperation
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TemplateInstanceCronService::class.java)
        private const val LOCK_KEY = "templateInstanceItemLock"
    }

    @Scheduled(cron = "0 0/1 * * * ?")
    fun templateInstance() {
        val lock = RedisLock(redisOperation, LOCK_KEY, 3000)
        try {
            if (!lock.tryLock()) {
                logger.info("get lock failed, skip")
                return
            }
            val templateInstanceBaseList = templateInstanceBaseDao.getTemplateInstanceBaseList(
                dslContext = dslContext,
                status = TemplateInstanceBaseStatus.INIT.name,
                descFlag = false,
                page = 1,
                pageSize = 10
            )
            templateInstanceBaseList?.forEach { templateInstanceBase ->
                val baseId = templateInstanceBase.id
                // 把模板批量更新记录状态置为”实例化中“
                templateInstanceBaseDao.updateTemplateInstanceBase(
                    dslContext = dslContext,
                    baseId = baseId,
                    status = TemplateInstanceBaseStatus.INSTANCING.name,
                    userId = "system"
                )
                val templateInstanceItemList = templateInstanceItemDao.getTemplateInstanceItemListByBaseId(
                    dslContext = dslContext,
                    baseId = baseId,
                    descFlag = false,
                    page = 1,
                    pageSize = 100
                )
                val projectId = templateInstanceBase.projectId
                val totalItemNum = templateInstanceBase.totalItemNum
                var successItemNum = templateInstanceBase.successItemNum
                var failItemNum = templateInstanceBase.failItemNum
                val successPipelines = ArrayList<String>()
                val failurePipelines = ArrayList<String>()
                templateInstanceItemList?.forEach { templateInstanceItem ->
                    val userId = templateInstanceItem.creator
                    val pipelineName = templateInstanceItem.pipelineName
                    try {

                        successPipelines.add(pipelineName)
                        successItemNum++
                    } catch (t: Throwable) {
                        logger.warn("Fail to update the pipeline $pipelineName of project $projectId by user $userId", t)
                        failurePipelines.add(pipelineName)
                        failItemNum++
                    }
                }
                if (successItemNum + failItemNum == totalItemNum) {
                    // 发送邮件通知
                }
            }
        } catch (t: Throwable) {
            logger.warn("templateInstance failed", t)
        } finally {
            lock.unlock()
        }
    }
}
