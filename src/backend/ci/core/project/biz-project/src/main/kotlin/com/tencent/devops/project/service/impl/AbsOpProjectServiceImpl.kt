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

package com.tencent.devops.project.service.impl

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.common.service.gray.MacOSGray
import com.tencent.devops.common.service.gray.RepoGray
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.model.project.tables.records.TProjectRecord
import com.tencent.devops.project.ProjectInfoResponse
import com.tencent.devops.project.ProjectInfoResponseMacOSGray
import com.tencent.devops.project.ProjectInfoResponseRepoGray
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.dao.ProjectLabelRelDao
import com.tencent.devops.project.dispatch.ProjectDispatcher
import com.tencent.devops.project.pojo.OpGrayProject
import com.tencent.devops.project.pojo.OpProjectUpdateInfoRequest
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.mq.ProjectUpdateBroadCastEvent
import com.tencent.devops.project.service.OpProjectService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.util.CollectionUtils

abstract class AbsOpProjectServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val projectDao: ProjectDao,
    private val projectLabelRelDao: ProjectLabelRelDao,
    private val redisOperation: RedisOperation,
    private val gray: Gray,
    private val repoGray: RepoGray,
    private val macosGray: MacOSGray,
    private val projectDispatcher: ProjectDispatcher
) : OpProjectService {

    override fun listGrayProject(): Result<OpGrayProject> {
        // 从redis中获取灰度项目列表
        return Result(OpGrayProject(grayProjectSet().toList()))
    }

    override fun setGrayProject(projectCodeList: List<String>, operateFlag: Int): Boolean {
        logger.info("Set gray projec:the projectCodeList is: $projectCodeList,operateFlag is:$operateFlag")
        // 使用set集合（去除重复元素）操作提交的项目列表
        for (item in projectCodeList) {
            if (1 == operateFlag) {
                gray.addGrayProject(item, redisOperation) // 添加项目为灰度项目
//                redisOperation.addSetValue(gray.getGrayRedisKey(), item) // 添加项目为灰度项目
            } else if (2 == operateFlag) {
                gray.removeGrayProject(item, redisOperation) // 取消项目为灰度项目
//                redisOperation.removeSetMember(gray.getGrayRedisKey(), item) // 取消项目为灰度项目
            }
        }
        val projectCodeSet = grayProjectSet()
        logger.info("the set projectSet is: $projectCodeSet")
        return true
    }

    override fun setRepoGrayProject(projectCodeList: List<String>, operateFlag: Int): Boolean {
        logger.info("Set bkrepo gray project:the projectCodeList is: $projectCodeList,operateFlag is:$operateFlag")
        for (item in projectCodeList) {
            if (1 == operateFlag) {
                repoGray.addGrayProject(item, redisOperation)
//                redisOperation.addSetValue(repoGray.getRepoGrayRedisKey(), item)
            } else if (2 == operateFlag) {
                repoGray.removeGrayProject(item, redisOperation)
//                redisOperation.removeSetMember(repoGray.getRepoGrayRedisKey(), item)
            }
        }
        return true
    }

    override fun setRepoNotGrayProject(projectCodeList: List<String>, operateFlag: Int): Boolean {
        logger.info("setRepoNotGrayProject, projectCodeList: $projectCodeList, operateFlag: $operateFlag")
        for (item in projectCodeList) {
            if (1 == operateFlag) {
                repoGray.addNotGrayProject(item, redisOperation)
            } else if (2 == operateFlag) {
                repoGray.removeNotGrayProject(item, redisOperation)
            }
        }
        return true
    }

    override fun setMacOSGrayProject(projectCodeList: List<String>, operateFlag: Int): Boolean {
        logger.info("Set macos gray project:the projectCodeList is: $projectCodeList,operateFlag is:$operateFlag")
        for (item in projectCodeList) {
            if (1 == operateFlag) {
                macosGray.addGrayProject(item, redisOperation)
//                redisOperation.addSetValue(macosGray.getRepoGrayRedisKey(), item)
            } else if (2 == operateFlag) {
                macosGray.removeGrayProject(item, redisOperation)
//                redisOperation.removeSetMember(macosGray.getRepoGrayRedisKey(), item)
            }
        }
        val projectCodeSet = grayProjectSet()
        logger.info("the set projectSet is: $projectCodeSet")
        return true
    }

    override fun updateProjectFromOp(
        userId: String,
        accessToken: String,
        projectInfoRequest: OpProjectUpdateInfoRequest
    ): Int {
        logger.info("the projectInfoRequest is: $projectInfoRequest")
        val projectId = projectInfoRequest.projectId
        val dbProjectRecord = projectDao.get(dslContext, projectId)
        if (dbProjectRecord == null) {
            logger.warn("The project $projectId is not exist")
            throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PROJECT_NOT_EXIST))
        }
        // 判断项目是不是审核的情况
        var flag = false
        if (1 == dbProjectRecord.approvalStatus && (2 == projectInfoRequest.approvalStatus || 3 == projectInfoRequest.approvalStatus)) {
            flag = true
            projectInfoRequest.approver = projectInfoRequest.approver
            projectInfoRequest.approvalTime = System.currentTimeMillis()
        } else {
            projectInfoRequest.approver = dbProjectRecord.approver
            projectInfoRequest.approvalTime = dbProjectRecord.approvalTime?.timestampmilli()
        }
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)

            try {
                projectDao.updateProjectFromOp(transactionContext, projectInfoRequest)
            } catch (ignored: DuplicateKeyException) {
                logger.warn("Duplicate project $projectInfoRequest", ignored)
                throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PROJECT_NAME_EXIST))
            }
            // 先解除项目与标签的关联关系，然后再从新建立二者之间的关系
            projectLabelRelDao.deleteByProjectId(transactionContext, projectId)
            val labelIdList = projectInfoRequest.labelIdList
            if (!CollectionUtils.isEmpty(labelIdList)) {
                projectLabelRelDao.batchAdd(
                    dslContext = transactionContext,
                    projectId = projectId,
                    labelIdList = labelIdList!!
                )
            }
            projectDispatcher.dispatch(
                ProjectUpdateBroadCastEvent(
                    userId = userId,
                    projectId = projectId,
                    projectInfo = ProjectUpdateInfo(
                        projectName = projectInfoRequest.projectName,
                        projectType = projectInfoRequest.projectType,
                        bgId = projectInfoRequest.bgId,
                        bgName = projectInfoRequest.bgName,
                        centerId = projectInfoRequest.centerId,
                        centerName = projectInfoRequest.centerName,
                        deptId = projectInfoRequest.deptId,
                        deptName = projectInfoRequest.deptName,
                        description = dbProjectRecord.description ?: "",
                        englishName = dbProjectRecord.englishName,
                        ccAppId = projectInfoRequest.ccAppId,
                        ccAppName = projectInfoRequest.cc_app_name,
                        kind = projectInfoRequest.kind
                    )
                )
            )
        }
        return if (!flag) {
            0 // 更新操作
        } else {
            when (projectInfoRequest.approvalStatus) {
                2 -> 1 // 审批通过
                3 -> 2 // 驳回
                else -> 0
            }
        }
    }

    override fun getProjectList(
        projectName: String?,
        englishName: String?,
        projectType: Int?,
        isSecrecy: Boolean?,
        creator: String?,
        approver: String?,
        approvalStatus: Int?,
        offset: Int,
        limit: Int,
        grayFlag: Boolean
    ): Result<Map<String, Any?>?> {
        val dataObj = mutableMapOf<String, Any?>()

        val grayProjectSet = grayProjectSet()

        val projectInfos = projectDao.getProjectList(
            dslContext = dslContext,
            projectName = projectName,
            englishName = englishName,
            projectType = projectType,
            isSecrecy = isSecrecy,
            creator = creator,
            approver = approver,
            approvalStatus = approvalStatus,
            offset = offset,
            limit = limit,
            grayFlag = grayFlag,
            englishNames = grayProjectSet
        )
        val totalCount = projectDao.getProjectCount(
            dslContext = dslContext,
            projectName = projectName,
            englishName = englishName,
            projectType = projectType,
            isSecrecy = isSecrecy,
            creator = creator,
            approver = approver,
            approvalStatus = approvalStatus,
            grayFlag = grayFlag,
            englishNames = grayProjectSet
        )
        val dataList = mutableListOf<ProjectInfoResponse>()

        for (i in projectInfos.indices) {
            val projectData = projectInfos[i]
            val projectInfo = getProjectInfoResponse(projectData, grayProjectSet)
            dataList.add(projectInfo)
        }
        dataObj["projectList"] = dataList
        dataObj["count"] = totalCount
        return Result(dataObj)
    }

    override fun getProjectList(
        projectName: String?,
        englishName: String?,
        projectType: Int?,
        isSecrecy: Boolean?,
        creator: String?,
        approver: String?,
        approvalStatus: Int?,
        offset: Int,
        limit: Int,
        grayFlag: Boolean,
        repoGrayFlag: Boolean
    ): Result<Map<String, Any?>?> {
        val dataObj = mutableMapOf<String, Any?>()

        val grayProjectSet = grayProjectSet()

        val repoGrayProjectSet = repoGrayProjectSet()

        val projectInfos = projectDao.getProjectList(
            dslContext = dslContext,
            projectName = projectName,
            englishName = englishName,
            projectType = projectType,
            isSecrecy = isSecrecy,
            creator = creator,
            approver = approver,
            approvalStatus = approvalStatus,
            offset = offset,
            limit = limit,
            grayFlag = grayFlag,
            repoGrayFlag = repoGrayFlag,
            grayNames = grayProjectSet,
            repoGrayNames = repoGrayProjectSet
        )
        val totalCount = projectDao.getProjectCount(
            dslContext = dslContext,
            projectName = projectName,
            englishName = englishName,
            projectType = projectType,
            isSecrecy = isSecrecy,
            creator = creator,
            approver = approver,
            approvalStatus = approvalStatus,
            grayFlag = grayFlag,
            repoGrayFlag = repoGrayFlag,
            grayNames = grayProjectSet,
            repoGrayNames = repoGrayProjectSet
        )
        val dataList = mutableListOf<ProjectInfoResponseRepoGray>()

        for (i in projectInfos.indices) {
            val projectData = projectInfos[i]
            val projectInfo = getProjectInfoResponseRepoGray(
                projectData = projectData,
                grayProjectSet = grayProjectSet,
                repoProjectSet = repoGrayProjectSet
            )
            dataList.add(projectInfo)
        }
        dataObj["projectList"] = dataList
        dataObj["count"] = totalCount
        return Result(dataObj)
    }

    override fun getProjectList(
        projectName: String?,
        englishName: String?,
        projectType: Int?,
        isSecrecy: Boolean?,
        creator: String?,
        approver: String?,
        approvalStatus: Int?,
        offset: Int,
        limit: Int,
        grayFlag: Boolean,
        repoGrayFlag: Boolean,
        macosGrayFlag: Boolean
    ): Result<Map<String, Any?>?> {
        val dataObj = mutableMapOf<String, Any?>()

        val grayProjectSet = grayProjectSet()
        val repoGrayProjectSet = repoGrayProjectSet()
        val macosGrayProjectSet = macosGray.grayProjectSet(redisOperation)

        val projectInfos = projectDao.getProjectList(
            dslContext = dslContext,
            projectName = projectName,
            englishName = englishName,
            projectType = projectType,
            isSecrecy = isSecrecy,
            creator = creator,
            approver = approver,
            approvalStatus = approvalStatus,
            offset = offset,
            limit = limit,
            grayFlag = grayFlag,
            repoGrayFlag = repoGrayFlag,
            macosGrayFlag = macosGrayFlag,
            grayNames = grayProjectSet,
            repoGrayNames = repoGrayProjectSet,
            macosGrayNames = macosGrayProjectSet
        )
        val totalCount = projectDao.getProjectCount(
            dslContext = dslContext,
            projectName = projectName,
            englishName = englishName,
            projectType = projectType,
            isSecrecy = isSecrecy,
            creator = creator,
            approver = approver,
            approvalStatus = approvalStatus,
            grayFlag = grayFlag,
            repoGrayFlag = repoGrayFlag,
            macosGrayFlag = macosGrayFlag,
            grayNames = grayProjectSet,
            repoGrayNames = repoGrayProjectSet,
            macosGrayNames = macosGrayProjectSet
        )
        val dataList = mutableListOf<ProjectInfoResponseMacOSGray>()

        for (i in projectInfos.indices) {
            val projectData = projectInfos[i]
            val projectInfo =
                getProjectInfoResponseMacOSGray(
                    projectData = projectData,
                    grayProjectSet = grayProjectSet,
                    repoProjectSet = repoGrayProjectSet,
                    macosProjectSet = macosGrayProjectSet
                )
            dataList.add(projectInfo)
        }
        dataObj["projectList"] = dataList
        dataObj["count"] = totalCount
        return Result(dataObj)
    }

    override fun getProjectCount(
        projectName: String?,
        englishName: String?,
        projectType: Int?,
        isSecrecy: Boolean?,
        creator: String?,
        approver: String?,
        approvalStatus: Int?,
        grayFlag: Boolean
    ): Result<Int> {
        return Result(
            data = projectDao.getProjectCount(
                dslContext = dslContext,
                projectName = projectName,
                englishName = englishName,
                projectType = projectType,
                isSecrecy = isSecrecy,
                creator = creator,
                approver = approver,
                approvalStatus = approvalStatus,
                grayFlag = grayFlag,
                englishNames = grayProjectSet()
            )
        )
    }

    private fun grayProjectSet() = gray.grayProjectSet(redisOperation)

    private fun repoGrayProjectSet() = repoGray.grayProjectSet(redisOperation)

    private fun getProjectInfoResponse(projectData: TProjectRecord, grayProjectSet: Set<String>): ProjectInfoResponse {
        return ProjectInfoResponse(
            projectId = projectData.projectId,
            projectName = projectData.projectName,
            projectEnglishName = projectData.englishName,
            creatorBgName = projectData.creatorBgName,
            creatorDeptName = projectData.creatorDeptName,
            creatorCenterName = projectData.creatorCenterName,
            bgId = projectData.bgId,
            bgName = projectData.bgName,
            deptId = projectData.deptId,
            deptName = projectData.deptName,
            centerId = projectData.centerId,
            centerName = projectData.centerName,
            projectType = projectData.projectType,
            approver = projectData.approver,
            approvalTime = projectData.approvalTime?.timestampmilli(),
            approvalStatus = projectData.approvalStatus,
            secrecyFlag = projectData.isSecrecy,
            creator = projectData.creator,
            createdAtTime = projectData.createdAt.timestampmilli(),
            ccAppId = projectData.ccAppId,
            useBk = projectData.useBk,
            offlinedFlag = projectData.isOfflined,
            kind = projectData.kind,
            enabled = projectData.enabled ?: true,
            grayFlag = grayProjectSet.contains(projectData.englishName),
            hybridCCAppId = projectData.hybridCcAppId,
            enableExternal = projectData.enableExternal,
            enableIdc = projectData.enableIdc,
            pipelineLimit = projectData.pipelineLimit
        )
    }

    private fun getProjectInfoResponseRepoGray(
        projectData: TProjectRecord,
        grayProjectSet: Set<String>,
        repoProjectSet: Set<String>
    ): ProjectInfoResponseRepoGray {
        return ProjectInfoResponseRepoGray(
            projectId = projectData.projectId,
            projectName = projectData.projectName,
            projectEnglishName = projectData.englishName,
            creatorBgName = projectData.creatorBgName,
            creatorDeptName = projectData.creatorDeptName,
            creatorCenterName = projectData.creatorCenterName,
            bgId = projectData.bgId,
            bgName = projectData.bgName,
            deptId = projectData.deptId,
            deptName = projectData.deptName,
            centerId = projectData.centerId,
            centerName = projectData.centerName,
            projectType = projectData.projectType,
            approver = projectData.approver,
            approvalTime = projectData.approvalTime?.timestampmilli(),
            approvalStatus = projectData.approvalStatus,
            secrecyFlag = projectData.isSecrecy,
            creator = projectData.creator,
            createdAtTime = projectData.createdAt.timestampmilli(),
            ccAppId = projectData.ccAppId,
            useBk = projectData.useBk,
            offlinedFlag = projectData.isOfflined,
            kind = projectData.kind,
            enabled = projectData.enabled ?: true,
            grayFlag = grayProjectSet.contains(projectData.englishName),
            repoGrayFlag = repoProjectSet.contains(projectData.englishName),
            hybridCCAppId = projectData.hybridCcAppId,
            enableExternal = projectData.enableExternal,
            enableIdc = projectData.enableIdc,
            pipelineLimit = projectData.pipelineLimit
        )
    }

    private fun getProjectInfoResponseMacOSGray(
        projectData: TProjectRecord,
        grayProjectSet: Set<String>,
        repoProjectSet: Set<String>,
        macosProjectSet: Set<String>
    ): ProjectInfoResponseMacOSGray {
        return ProjectInfoResponseMacOSGray(
            projectId = projectData.projectId,
            projectName = projectData.projectName,
            projectEnglishName = projectData.englishName,
            creatorBgName = projectData.creatorBgName,
            creatorDeptName = projectData.creatorDeptName,
            creatorCenterName = projectData.creatorCenterName,
            bgId = projectData.bgId,
            bgName = projectData.bgName,
            deptId = projectData.deptId,
            deptName = projectData.deptName,
            centerId = projectData.centerId,
            centerName = projectData.centerName,
            projectType = projectData.projectType,
            approver = projectData.approver,
            approvalTime = projectData.approvalTime?.timestampmilli(),
            approvalStatus = projectData.approvalStatus,
            secrecyFlag = projectData.isSecrecy,
            creator = projectData.creator,
            createdAtTime = projectData.createdAt.timestampmilli(),
            ccAppId = projectData.ccAppId,
            useBk = projectData.useBk,
            offlinedFlag = projectData.isOfflined,
            kind = projectData.kind,
            enabled = projectData.enabled ?: true,
            grayFlag = grayProjectSet.contains(projectData.englishName),
            repoGrayFlag = repoProjectSet.contains(projectData.englishName),
            macosGrayFlag = macosProjectSet.contains(projectData.englishName),
            hybridCCAppId = projectData.hybridCcAppId,
            enableExternal = projectData.enableExternal,
            enableIdc = projectData.enableIdc,
            pipelineLimit = projectData.pipelineLimit
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)!!
    }
}