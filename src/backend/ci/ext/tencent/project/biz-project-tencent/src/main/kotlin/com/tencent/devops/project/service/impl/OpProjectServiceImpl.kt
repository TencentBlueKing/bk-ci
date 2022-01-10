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

package com.tencent.devops.project.service.impl

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.AuthTokenApi
import com.tencent.devops.common.auth.code.AuthServiceCode
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.common.service.gray.MacOSGray
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.project.SECRECY_PROJECT_REDIS_KEY
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.dao.ProjectLabelRelDao
import com.tencent.devops.project.dao.ProjectLocalDao
import com.tencent.devops.project.dispatch.ProjectDispatcher
import com.tencent.devops.project.pojo.OpProjectUpdateInfoRequest
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectExtSystemTagDTO
import com.tencent.devops.project.pojo.ProjectTagUpdateDTO
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.enums.SystemEnums
import com.tencent.devops.project.pojo.mq.ProjectCreateBroadCastEvent
import com.tencent.devops.project.pojo.mq.ProjectUpdateBroadCastEvent
import com.tencent.devops.project.service.ProjectPaasCCService
import com.tencent.devops.project.service.ProjectTagService
import com.tencent.devops.project.service.tof.TOFService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils

@Service
class OpProjectServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val projectDao: ProjectDao,
    private val projectLocalDao: ProjectLocalDao,
    private val projectLabelRelDao: ProjectLabelRelDao,
    private val redisOperation: RedisOperation,
    gray: Gray,
    private val projectDispatcher: ProjectDispatcher,
    private val paasCCService: ProjectPaasCCService,
    private val bkAuthProjectApi: AuthProjectApi,
    private val bsAuthTokenApi: AuthTokenApi,
    macosGray: MacOSGray,
    private val tofService: TOFService,
    private val bsPipelineAuthServiceCode: AuthServiceCode,
    private val projectTagService: ProjectTagService
) : AbsOpProjectServiceImpl(
    dslContext,
    projectDao,
    projectLabelRelDao,
    redisOperation,
    gray,
    macosGray,
    projectDispatcher
) {

    private final val redisProjectKey = "BK:PROJECT:INFO:"

    @Value("\${tag.prod:#{null}}")
    private val prodTag: String? = null

    @Value("\${tag.gray:#{null}}")
    private val grayTag: String? = null

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
            } catch (e: DuplicateKeyException) {
                logger.warn("Duplicate project $projectInfoRequest", e)
                throw OperationException("项目名或英文名重复")
            }
            // 先解除项目与标签的关联关系，然后再从新建立二者之间的关系
            projectLabelRelDao.deleteByProjectId(transactionContext, projectId)
            val labelIdList = projectInfoRequest.labelIdList
            if (!CollectionUtils.isEmpty(labelIdList)) projectLabelRelDao.batchAdd(
                dslContext = transactionContext,
                projectId = projectId,
                labelIdList = labelIdList!!
            )
            if (!projectInfoRequest.secrecyFlag) {
                redisOperation.removeSetMember(SECRECY_PROJECT_REDIS_KEY, dbProjectRecord.englishName)
            } else {
                redisOperation.addSetValue(SECRECY_PROJECT_REDIS_KEY, dbProjectRecord.englishName)
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
                        description = dbProjectRecord.description,
                        englishName = dbProjectRecord.englishName,
                        ccAppId = projectInfoRequest.ccAppId,
                        ccAppName = projectInfoRequest.cc_app_name,
                        kind = projectInfoRequest.kind,
                        secrecy = projectInfoRequest.secrecyFlag
                    )
                )
            )
        }
        return if (!flag) {
            0 // 更新操作
        } else {
            return when (projectInfoRequest.approvalStatus) {
                2 -> 1 // 审批通过
                3 -> 2 // 驳回
                else -> 0
            }
        }
    }

    override fun synProject(projectCode: String, isRefresh: Boolean?): Result<Boolean> {
        var isSyn = false
        if (redisOperation.get(redisProjectKey + projectCode) != null) {
            return Result(isSyn)
        }

        val projectInfo = projectDao.getByEnglishName(dslContext, projectCode)
        if (projectInfo == null) {
            logger.error("syn project $projectCode is not exist")
            throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PROJECT_NOT_EXIST))
        }
        val accessToken = bsAuthTokenApi.getAccessToken(bsPipelineAuthServiceCode)
        val paasProjectInfo = paasCCService.getPaasCCProjectInfo(projectCode, accessToken)
        if (paasProjectInfo == null) {
            logger.info("synProject projectCode:$projectCode, paasCC is not exist. start Syn")
            if (isRefresh!!) {
                projectDispatcher.dispatch(
                    ProjectCreateBroadCastEvent(
                        userId = projectInfo.creator,
                        projectId = projectInfo.projectId,
                        projectInfo = ProjectCreateInfo(
                            projectName = projectInfo.projectName,
                            englishName = projectInfo.englishName,
                            projectType = projectInfo.projectType,
                            description = projectInfo.description,
                            bgId = projectInfo.bgId,
                            bgName = projectInfo.bgName,
                            deptId = projectInfo.deptId,
                            deptName = projectInfo.deptName,
                            centerId = projectInfo.centerId,
                            centerName = projectInfo.centerName,
                            secrecy = projectInfo.isSecrecy,
                            kind = projectInfo.kind
                        )
                    )
                )
            }
            isSyn = true
        }
        val authProjectInfo = bkAuthProjectApi.getProjectInfo(bsPipelineAuthServiceCode, projectCode)
        if (authProjectInfo == null) {
            logger.info("synProject projectCode:$projectCode, authCenter is not exist. start Syn")
//            if (isRefresh!!) {
//            projectPermissionService.createResources(
//                userId = projectInfo.creator,
//                projectList = listOf(
//                    ResourceRegisterInfo(
//                        projectInfo.englishName,
//                        projectInfo.projectName
//                    )
//                )
//            )
//            }
            logger.info("project syn success, projectCode[$projectCode], creator[${projectInfo.creator}]")
            isSyn = true
        }
        if (!isSyn) {
            redisOperation.set(redisProjectKey + projectCode, projectCode, null, true)
        }

        return Result(isSyn)
    }

    override fun synProjectInit(isRefresh: Boolean?): Result<List<String>> {
        logger.info("synProject time: ${System.currentTimeMillis()}")
        val synProject = mutableListOf<String>()
        val startTime = System.currentTimeMillis()
        var page = 1
        val limit = 100
        var isContinue = true
        val excludeName = mutableListOf<String>()
        excludeName.add("CODE_")
        excludeName.add("git_")

        val failList = mutableListOf<String>()

        while (isContinue) {
            val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page, limit)
            logger.info("synProject page: $page")
            val projectInfos = projectLocalDao.getProjectListExclude(
                dslContext = dslContext,
                offset = sqlLimit.offset,
                limit = sqlLimit.limit,
                englishNamesExclude = excludeName
            )

            val lastPageInfos = projectDao.getProjectList(
                dslContext = dslContext,
                projectName = null,
                englishName = null,
                projectType = null,
                isSecrecy = null,
                creator = null,
                approver = null,
                approvalStatus = null,
                grayFlag = false,
                englishNames = null,
                limit = sqlLimit.limit,
                offset = sqlLimit.offset
            )

            if (lastPageInfos.size < limit) {
                isContinue = false
            }
            if (page <= 1) {
                logger.info("project List: ${projectInfos[1]}")
            }
            for (i in projectInfos.indices) {
                val projectData = projectInfos[i]
                try {
                    val isSyn = synProject(projectData.englishName, isRefresh)
                    if (isSyn.data!!) {
                        logger.info("project ${projectData.englishName} need syn authCenter or Paas")
                        synProject.add(projectData.englishName)
                    }
                } catch (ex: Exception) {
                    logger.warn("syn project fail: errorMsg:[$ex]")
                    failList.add(projectData.englishName)
                }
            }
            page++
        }
        val endTime = System.currentTimeMillis()
        logger.warn("syn fail list: $failList")
        logger.info("syn project time: ${endTime - startTime}, syn project count: ${synProject.size} ")
        return Result(synProject)
    }

    override fun setGrayExt(projectCodeList: List<String>, operateFlag: Int, system: SystemEnums) {
        val routerTag = when (operateFlag) {
            grayLable -> {
                grayTag
            }
            prodLable -> {
                prodTag
            }
            else -> {
                null
            }
        }

        if (routerTag.isNullOrEmpty()) {
            return
        }

        if (system == SystemEnums.CI) {
            val projectTagUpdateDTO = ProjectTagUpdateDTO(
                routerTag = routerTag!!,
                bgId = null,
                deptId = null,
                centerId = null,
                projectCodeList = projectCodeList,
                channel = null
            )
            projectTagService.updateTagByProject(projectTagUpdateDTO)
        } else if (system == SystemEnums.CODECC || system == SystemEnums.REPO) {
            val projectTagUpdateDTO = ProjectExtSystemTagDTO(
                routerTag = routerTag!!,
                projectCodeList = projectCodeList,
                system = system.name
            )
            projectTagService.updateExtSystemRouterTag(projectTagUpdateDTO)
        } else {
            return
        }
    }

    companion object {
        final const val grayLable = 1
        final const val prodLable = 2
    }
}
