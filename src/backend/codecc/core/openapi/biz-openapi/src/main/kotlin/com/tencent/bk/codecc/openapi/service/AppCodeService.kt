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
package com.tencent.bk.codecc.openapi.service

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.bk.codecc.openapi.op.model.OrgDetailEntity
import com.tencent.bk.codecc.openapi.dao.AppCodeOrgDao
import com.tencent.bk.codecc.openapi.dao.AppCodeProjectDao
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource
import com.tencent.devops.common.client.Client
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * @Description
 * @Date 2020/3/17
 * @Version 1.0
 */
@Service
class AppCodeService(
    private val client: Client,
    private val appCodeOrgDao: AppCodeOrgDao,
    private val appCodeProjectDao: AppCodeProjectDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(AppCodeService::class.java)
    }

    private val appCodeProjectCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build<String, Map<String, String>>(
            object : CacheLoader<String, Map<String, String>>() {
                override fun load(appCode: String): Map<String, String> {
                    return try {
                        val projectMap = getAppCodeProject(appCode)
                        logger.info("appCode[$appCode] openapi projectMap:$projectMap.")
                        projectMap
                    } catch (t: Throwable) {
                        logger.info("appCode[$appCode] failed to get projectMap.")
                        mutableMapOf()
                    }
                }
            }
        )

    private val appCodeGroupCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build<String/*appCode*/, List<OrgDetailEntity>/*Map<projectId,AppCodeGroup>*/>(
            object : CacheLoader<String, List<OrgDetailEntity>>() {
                override fun load(appCode: String): List<OrgDetailEntity> {
                    return try {
                        val orgDetailList = getAppCodeGroup(appCode)
                        logger.info("appCode[$appCode] openapi appCodeGroup:$orgDetailList.")
                        orgDetailList
                    } catch (t: Throwable) {
                        logger.info("appCode[$appCode] failed to get appCodeGroup.")
                        listOf()
                    }
                }
            }
        )

    private fun getAppCodeProject(appCode: String): Map<String, String> {
        val appCodeProjectEntity = appCodeProjectDao.findFirstByAppCode(appCode)
        val result = mutableMapOf<String, String>()
        return when {
            null == appCodeProjectEntity -> mapOf()
            appCodeProjectEntity.projectIds.isNullOrEmpty() -> mapOf()
            else -> {
                appCodeProjectEntity.projectIds.forEach {
                    result[it] = it
                }
                result
            }
        }
    }

    private fun getAppCodeGroup(appCode: String): List<OrgDetailEntity> {
        val appCodeOrgEntity = appCodeOrgDao.findFirstByAppCode(appCode)
        return when {
            null == appCodeOrgEntity -> listOf()
            appCodeOrgEntity.orgList.isNullOrEmpty() -> listOf()
            else -> {
                appCodeOrgEntity.orgList
            }
        }
    }

    fun validAppCode(appCode: String, projectId: String, taskId: String): Boolean {
        val taskDetailVO =
            client.get(ServiceTaskRestResource::class.java).getTaskInfoWithoutToolsByTaskId(taskId.toLong()).data
        if (taskDetailVO == null || taskDetailVO.projectId != projectId) {
            logger.info("task id[$taskId] has different project id[${taskDetailVO?.projectId ?: ""}] with the input[$projectId]")
            return false
        }
        val appCodeProject = appCodeProjectCache.get(appCode)
        logger.info("appCode[$appCode] projectId[$projectId] openapi appCodeProjectCache:$appCodeProject.")
        if (appCodeProject.isNotEmpty()) {
            if (appCodeProject.containsKey(projectId)) {
                logger.info("appCode[$appCode] projectId[$projectId] openapi appCodeProjectCache matched.")
                return true
            }
        }
        logger.info("appCode[$appCode] projectId[$projectId] openapi appCodeProjectCache no matched.")
        val orgDetailList = appCodeGroupCache.get(appCode)
        logger.info("appCode[$appCode] projectId[$projectId] openapi appCodeGroupCache:$orgDetailList.")
        if (!orgDetailList.isNullOrEmpty()) {
            logger.info("appCode[$appCode] projectId[$taskId] openapi appCodeGroupCache taskDetailVO:$taskDetailVO.")
            orgDetailList.forEach {
                if (it.centerId != null && taskDetailVO.centerId != 0 && it.centerId == taskDetailVO.centerId) {
                    logger.info("appCode[$appCode] projectId[$projectId] openapi appCodeGroupCache centerId matched.")
                    return true
                }
                if (it.deptId != null && taskDetailVO.deptId != 0 && it.deptId == taskDetailVO.deptId) {
                    logger.info("appCode[$appCode] projectId[$projectId] openapi appCodeGroupCache deptId matched.")
                    return true
                }
                if (it.bgId != null && taskDetailVO.bgId != 0 && it.bgId == taskDetailVO.bgId) {
                    logger.info("appCode[$appCode] projectId[$projectId] openapi appCodeGroupCache bgId matched.")
                    return true
                }
                if (it.centerId == null && it.deptId == null && it.bgId == null) {
                    logger.info("appCode[$appCode] projectId[$projectId] openapi appCodeGroupCache all group matched.")
                    return true
                }
            }
        }
        logger.info("appCode[$appCode] projectId[$projectId] taskId[$taskId] openapi appCodeGroupCache no matched.")
        return false
    }
}
