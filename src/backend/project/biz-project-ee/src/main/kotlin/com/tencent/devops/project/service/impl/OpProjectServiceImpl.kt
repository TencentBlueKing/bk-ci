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
import com.tencent.devops.model.project.tables.records.TProjectRecord
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.dao.ProjectLabelRelDao
import com.tencent.devops.project.pojo.OpGrayProject
import com.tencent.devops.project.pojo.OpProjectUpdateInfoRequest
import com.tencent.devops.project.pojo.ProjectInfoResponse
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.service.OpProjectService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils

@Service
class OpProjectServiceImpl @Autowired constructor(
        private val dslContext: DSLContext,
        private val projectDao: ProjectDao,
        private val projectLabelRelDao: ProjectLabelRelDao,
        private val redisOperation: RedisOperation,
        private val gray: Gray
) : OpProjectService {

    companion object {
        private val logger = LoggerFactory.getLogger(OpProjectServiceImpl::class.java)
    }

    override fun listGrayProject(): Result<OpGrayProject> {
        // 从redis中获取灰度项目列表
        return Result(OpGrayProject(grayProjectSet().toList()))
    }

    fun grayProjectSet() =
            (redisOperation.getSetMembers(gray.getGrayRedisKey()) ?: emptySet()).filter { !it.isBlank() }.toSet()

    override fun setGrayProject(projectCodeList: List<String>, operateFlag: Int): Boolean {
        logger.info("the projectCodeList is: $projectCodeList,operateFlag is:$operateFlag")
        // 使用set集合（去除重复元素）操作提交的项目列表
        for (item in projectCodeList) {
            if (1 == operateFlag) {
                redisOperation.addSetValue(gray.getGrayRedisKey(), item) // 添加项目为灰度项目
            } else if (2 == operateFlag) {
                redisOperation.removeSetMember(gray.getGrayRedisKey(), item) // 取消项目为灰度项目
            }
        }
        val projectCodeSet = grayProjectSet()
        logger.info("the set projectSet is: $projectCodeSet")
        return true
    }

    override fun updateProjectFromOp(userId: String, accessToken: String, projectInfoRequest: OpProjectUpdateInfoRequest): Int {
        logger.info("the projectInfoRequest is: $projectInfoRequest")
        val projectId = projectInfoRequest.projectId
        val dbProjectRecord = projectDao.get(dslContext, projectId)
        if (dbProjectRecord == null) {
            logger.warn("The project $projectId is not exist")
            throw OperationException("项目不存在")
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
                throw OperationException("项目名或英文名重复")
            }
            // 先解除项目与标签的关联关系，然后再从新建立二者之间的关系
            projectLabelRelDao.deleteByProjectId(transactionContext, projectId)
            val labelIdList = projectInfoRequest.labelIdList
            if (!CollectionUtils.isEmpty(labelIdList)) projectLabelRelDao.batchAdd(
                    transactionContext,
                    projectId,
                    labelIdList!!
            )

        }
        return if (!flag) {
            0 // 更新操作
        } else {
            return when {
                2 == projectInfoRequest.approvalStatus -> 1 // 审批通过
                3 == projectInfoRequest.approvalStatus -> 2 // 驳回
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

        val projectCodeSet = if (grayFlag) {
            redisOperation.getSetMembers(gray.getGrayRedisKey())
        } else {
            null
        }

        val projectInfos = projectDao.getProjectList(
                dslContext,
                projectName,
                englishName,
                projectType,
                isSecrecy,
                creator,
                approver,
                approvalStatus,
                offset,
                limit,
                grayFlag,
                projectCodeSet
        )
        val totalCount = projectDao.getProjectCount(
                dslContext,
                projectName,
                englishName,
                projectType,
                isSecrecy,
                creator,
                approver,
                approvalStatus,
                grayFlag,
                projectCodeSet
        )
        val dataList = mutableListOf<ProjectInfoResponse>()
        val grayProjectSet = grayProjectSet()
        for (i in projectInfos.indices) {
            val projectData = projectInfos[i]
            val projectInfo = getProjectInfoResponse(projectData, grayProjectSet)
            dataList.add(projectInfo)
        }
        dataObj["projectList"] = dataList
        dataObj["count"] = totalCount
        return Result(dataObj)
    }

    private fun getProjectInfoResponse(projectData: TProjectRecord, grayProjectSet: Set<String>): ProjectInfoResponse {
        return ProjectInfoResponse(
                projectData.projectId,
                projectData.projectName,
                projectData.englishName,
                projectData.creatorBgName,
                projectData.creatorDeptName,
                projectData.creatorCenterName,
                projectData.bgId,
                projectData.bgName,
                projectData.deptId,
                projectData.deptName,
                projectData.centerId,
                projectData.centerName,
                projectData.projectType,
                projectData.approver,
                projectData.approvalTime?.timestampmilli(),
                projectData.approvalStatus,
                projectData.isSecrecy,
                projectData.creator,
                projectData.createdAt.timestampmilli(),
                projectData.ccAppId,
                projectData.useBk,
                projectData.isOfflined,
                projectData.kind,
                projectData.enabled ?: true,
                grayProjectSet.contains(projectData.englishName),
                projectData.hybridCcAppId,
                projectData.enableExternal
        )
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
                        dslContext,
                        projectName,
                        englishName,
                        projectType,
                        isSecrecy,
                        creator,
                        approver,
                        approvalStatus,
                        grayFlag,
                        grayProjectSet()
                )
        )
    }

}

