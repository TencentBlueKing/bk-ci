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
package com.tencent.bk.codecc.apiquery.service

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.bk.codecc.apiquery.op.dao.AppCodeAuthDao
import com.tencent.bk.codecc.apiquery.op.model.AppCodeAdminEntity
import com.tencent.bk.codecc.apiquery.op.model.OrgDetailEntity
import com.tencent.bk.codecc.apiquery.task.TaskQueryReq
import com.tencent.bk.codecc.apiquery.task.dao.TaskDao
import com.tencent.bk.codecc.apiquery.task.model.TaskInfoModel
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
    private val appCodeAuthDao: AppCodeAuthDao,
    private val taskDao: TaskDao
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

    private val appCodeToolCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build<String, Map<String, String>>/*Map<projectId,AppCodeGroup>*/(
            object : CacheLoader<String, Map<String, String>>() {
                override fun load(appCode: String): Map<String, String> {
                    return try {
                        val toolMap = getAppCodeTool(appCode)
                        logger.info("appCode[$appCode] openapi toolMap:$toolMap.")
                        toolMap
                    } catch (t: Throwable) {
                        logger.info("appCode[$appCode] failed to get projectMap.")
                        mutableMapOf()
                    }
                }
            }
        )

    private val appCodeAdminCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build<String/*appCode*/, Boolean/*Map<projectId,AppCodeGroup>*/>(
            object : CacheLoader<String, Boolean>() {
                override fun load(appCode: String): Boolean {
                    return try {
                        val appCodeAdminList = getAppCodeAdmin()
                        if (!appCodeAdminList.isNullOrEmpty()) {
                            appCodeAdminList[0].appCode.contains(appCode)
                        } else {
                            false
                        }
                    } catch (t: Throwable) {
                        logger.info("appCode[$appCode] failed to get appCodeAdmin.")
                        false
                    }
                }
            }
        )

    private fun getAppCodeProject(appCode: String): Map<String, String> {
        val appCodeProjectEntity = appCodeAuthDao.findProjectInfoByAppCode(appCode)
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
        val appCodeOrgEntity = appCodeAuthDao.findOrgInfoByAppCode(appCode)
        return when {
            null == appCodeOrgEntity -> listOf()
            appCodeOrgEntity.orgList.isNullOrEmpty() -> listOf()
            else -> {
                appCodeOrgEntity.orgList
            }
        }
    }

    private fun getAppCodeTool(appCode: String): Map<String, String> {
        val appCodeToolEntity = appCodeAuthDao.findToolInfoByAppCode(appCode)
        val result = mutableMapOf<String, String>()
        return when {
            null == appCodeToolEntity -> mapOf()
            appCodeToolEntity.toolNameList.isNullOrEmpty() -> mapOf()
            else -> {
                appCodeToolEntity.toolNameList.forEach {
                    result[it] = it
                }
                result
            }
        }
    }

    private fun getAppCodeAdmin(): List<AppCodeAdminEntity>? {
        return appCodeAuthDao.findAdminInfoByAppCode()
    }

    fun validAppCode(
        appCode: String,
        projectId: String?,
        taskIds: List<Long>?,
        toolName: String?,
        taskQueryReq: TaskQueryReq?
    ): Boolean {
        if (appCodeAdminCache.get(appCode)) {
            logger.info("app code admin [$appCode]")
            return true
        }
        // 1. 当projectid和taskIds都不为空时，校验之间的对应关系
        var taskInfoList: List<TaskInfoModel> = mutableListOf()
        if (!taskIds.isNullOrEmpty()) {
            // 当taskIds不为空时，进行赋值
            taskInfoList = taskDao.findTaskInfoListByTaskIds(taskIds)
            if (!projectId.isNullOrBlank()) {
                logger.info("appCode[$appCode] projectId[$projectId] task ids [$taskIds] validate mapping.")
                if (taskInfoList.isNullOrEmpty() || taskInfoList.any { it.projectId != projectId }) {
                    logger.info("task id list[$taskIds] has different project id with the input[$projectId]")
                    return false
                }
            }
        }

        val appCodeProject = appCodeProjectCache.get(appCode)
        // 2. 当projectid不为空时，单独校验projectid
        if (!projectId.isNullOrBlank()) {
            logger.info("appCode[$appCode] projectId[$projectId] openapi appCodeProjectCache:$appCodeProject.")
            if (appCodeProject.isNotEmpty()) {
                if (appCodeProject.containsKey(projectId)) {
                    logger.info("appCode[$appCode] projectId[$projectId] openapi appCodeProjectCache matched.")
                    return true
                }
            }
            logger.info("appCode[$appCode] projectId[$projectId] openapi appCodeProjectCache no matched.")
        }

        // 3. 当taskIds不为空时，先校验taskIds的project权限，再校验组织架构权限
        val orgDetailList = appCodeGroupCache.get(appCode)
        if (!taskInfoList.isNullOrEmpty()) {
            logger.info("appCode[$appCode] taskId[$taskIds] openapi appCodeProjectCache:$appCodeProject.")
            if (taskInfoList.all { appCodeProject.containsKey(it.projectId) }) {
                logger.info("appCode[$appCode] taskIds[$taskIds] openapi appCodeProjectCache matched.")
                return true
            }
            logger.info("appCode[$appCode] taskIds[$taskIds] openapi appCodeProjectCache no matched.")

            logger.info("appCode[$appCode] projectId[$projectId] openapi appCodeGroupCache:$orgDetailList.")
            if (!orgDetailList.isNullOrEmpty()) {
                logger.info("appCode[$appCode] projectId[$taskIds] openapi appCodeGroupCache taskDetailVO:$taskInfoList.")
                orgDetailList.forEach {
                    if (it.centerId != null && taskInfoList.all { taskInfoModel -> taskInfoModel.centerId != 0 && it.centerId == taskInfoModel.centerId }) {
                        logger.info("appCode[$appCode] projectId[$projectId] openapi appCodeGroupCache centerId matched.")
                        return true
                    }
                    if (it.deptId != null && taskInfoList.all { taskInfoModel -> taskInfoModel.deptId != 0 && it.deptId == taskInfoModel.deptId }) {
                        logger.info("appCode[$appCode] projectId[$projectId] openapi appCodeGroupCache deptId matched.")
                        return true
                    }
                    if (it.bgId != null && taskInfoList.all { taskInfoModel -> taskInfoModel.bgId != 0 && it.bgId == taskInfoModel.bgId }) {
                        logger.info("appCode[$appCode] projectId[$projectId] openapi appCodeGroupCache bgId matched.")
                        return true
                    }
                    if (it.centerId == null && it.deptId == null && it.bgId == null) {
                        logger.info("appCode[$appCode] projectId[$projectId] openapi appCodeGroupCache all group matched.")
                        return true
                    }
                }
            }
        }

        // 4. 当toolName参数不为空时，校验工具名参数权限
        val appCodeTool = appCodeToolCache.get(appCode)
        if (!toolName.isNullOrBlank()) {
            logger.info("appCode[$appCode] tool[$toolName] openapi appCodeToolCache:$appCodeTool.")
            if (appCodeTool.isNotEmpty()) {
                if (appCodeTool.containsKey(toolName)) {
                    logger.info("appCode[$appCode] tool[$toolName] openapi appCodeProjectCache matched.")
                    return true
                }
            }
            logger.info("appCode[$appCode] tool[$toolName] openapi appCodeProjectCache no matched.")
        }

        // 5.当taskQueryReq不为空时，校验传入组织架构权限
        if (taskQueryReq != null) {
            if (taskQueryReq.deptId != null) {
                logger.info("appCode[$appCode] deptId[${taskQueryReq.deptId}] openapi appCodeGroupCache:$orgDetailList.")
                orgDetailList.forEach {
                    if (it.deptId != null && taskQueryReq.deptId == it.deptId) {
                        logger.info("appCode[$appCode] deptId[${taskQueryReq.deptId}] openapi appCodeGroupCache deptId matched.")
                        return true
                    }
                }
            }
            if (taskQueryReq.bgId != null) {
                logger.info("appCode[$appCode] bgId[${taskQueryReq.bgId}] openapi appCodeGroupCache:$orgDetailList.")
                orgDetailList.forEach {
                    if (it.bgId != null && taskQueryReq.bgId == it.bgId) {
                        logger.info("appCode[$appCode] bgId[${taskQueryReq.bgId}] openapi appCodeGroupCache bgId matched.")
                        return true
                    }
                }
            }
        }

        logger.info("appCode[$appCode] projectId[$projectId] taskId[$taskIds] openapi appCodeGroupCache no matched.")
        return false
    }
}
