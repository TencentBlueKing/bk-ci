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

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.bkrepo.common.api.util.JsonUtils.objectMapper
import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.auth.service.ManagerService
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.BSAuthTokenApi
import com.tencent.devops.common.auth.api.BkAuthProperties
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.auth.code.ProjectAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.client.consul.ConsulContent
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.dispatch.ProjectDispatcher
import com.tencent.devops.project.jmx.api.ProjectJmxApi
import com.tencent.devops.project.pojo.AuthProjectForList
import com.tencent.devops.project.pojo.ProjectCreateExtInfo
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectCreateUserInfo
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.mq.ProjectCreateBroadCastEvent
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.project.service.ProjectDataSourceAssignService
import com.tencent.devops.project.service.ProjectExtPermissionService
import com.tencent.devops.project.service.ProjectPaasCCService
import com.tencent.devops.project.service.ProjectPermissionService
import com.tencent.devops.project.service.iam.ProjectIamV0Service
import com.tencent.devops.project.service.s3.S3Service
import com.tencent.devops.project.service.tof.TOFService
import com.tencent.devops.project.util.ImageUtil
import com.tencent.devops.project.util.ProjectUtils
import okhttp3.Request
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File

@Suppress("ALL")
@Service
class TxProjectServiceImpl @Autowired constructor(
    projectPermissionService: ProjectPermissionService,
    private val dslContext: DSLContext,
    private val projectDao: ProjectDao,
    private val s3Service: S3Service,
    private val tofService: TOFService,
    private val bkRepoClient: BkRepoClient,
    private val projectPaasCCService: ProjectPaasCCService,
    private val bkAuthProperties: BkAuthProperties,
    private val bsAuthProjectApi: AuthProjectApi,
    private val bsPipelineAuthServiceCode: BSPipelineAuthServiceCode,
    projectJmxApi: ProjectJmxApi,
    redisOperation: RedisOperation,
    gray: Gray,
    client: Client,
    private val projectDispatcher: ProjectDispatcher,
    private val authPermissionApi: AuthPermissionApi,
    private val projectAuthServiceCode: ProjectAuthServiceCode,
    private val projectDataSourceAssignService: ProjectDataSourceAssignService,
    private val managerService: ManagerService,
    private val projectIamV0Service: ProjectIamV0Service,
    private val tokenService: ClientTokenService,
    private val bsAuthTokenApi: BSAuthTokenApi,
    private val projectExtPermissionService: ProjectExtPermissionService
) : AbsProjectServiceImpl(
    projectPermissionService = projectPermissionService,
    dslContext = dslContext,
    projectDao = projectDao,
    projectJmxApi = projectJmxApi,
    redisOperation = redisOperation,
    gray = gray,
    client = client,
    projectDispatcher = projectDispatcher,
    authPermissionApi = authPermissionApi,
    projectAuthServiceCode = projectAuthServiceCode,
    projectDataSourceAssignService = projectDataSourceAssignService
) {

    @Value("\${iam.v0.url:#{null}}")
    private var v0IamUrl: String = ""

    @Value("\${tag.v3:#{null}}")
    private var v3Tag: String = ""

    override fun getByEnglishName(userId: String, englishName: String, accessToken: String?): ProjectVO? {
        val projectVO = getInfoByEnglishName(englishName)
        if (projectVO == null) {
            logger.warn("The projectCode $englishName is not exist")
            return null
        }
        // 判断用户是否为管理员，若为管理员则不调用iam
        val isManager = managerService.isManagerPermission(
            userId = userId,
            projectId = englishName,
            resourceType = AuthResourceType.PROJECT,
            authPermission = AuthPermission.VIEW
        )

        if (isManager) {
            logger.info("getByEnglishName $userId is $englishName manager")
            return projectVO
        }

        val englishNames = getProjectFromAuth(userId, accessToken)
        if (englishNames.isEmpty()) {
            return null
        }
        if (!englishNames.contains(projectVO!!.englishName)) {
            logger.warn("The user don't have the permission to get the project $englishName")
            return null
        }
        return projectVO
    }

    override fun list(userId: String, accessToken: String?, enabled: Boolean?): List<ProjectVO> {
        val startEpoch = System.currentTimeMillis()
        try {

            val englishNames = getProjectFromAuth(userId, accessToken).toSet()
            if (englishNames.isEmpty()) {
                return emptyList()
            }
            logger.info("项目列表：$englishNames")
            val list = ArrayList<ProjectVO>()
            projectDao.listByCodes(dslContext, englishNames).map {
                list.add(ProjectUtils.packagingBean(it, grayProjectSet()))
            }
            return list
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list projects")
        }
    }

    override fun getDeptInfo(userId: String): UserDeptDetail {
        try {
            return tofService.getUserDeptDetail(userId, "")
        } catch (e: OperationException) {
            // stream场景下会传公共账号,tof不存在公共账号
            logger.warn("getDeptInfo: $e")
            return UserDeptDetail(
                bgId = "0",
                bgName = "",
                centerId = "0",
                centerName = "",
                deptId = "0",
                deptName = "",
                groupId = "0",
                groupName = ""
            )
        }
    }

    override fun createExtProjectInfo(
        userId: String,
        projectId: String,
        accessToken: String?,
        projectCreateInfo: ProjectCreateInfo,
        projectCreateExtInfo: ProjectCreateExtInfo
    ) {
        // 添加repo项目
        val createSuccess = bkRepoClient.createBkRepoResource(userId, projectCreateInfo.englishName)
        logger.info("create bkrepo project ${projectCreateInfo.englishName} success: $createSuccess")

        if (projectCreateExtInfo.needAuth!!) {
            val newAccessToken = if (accessToken.isNullOrBlank()) {
                bsAuthTokenApi.getAccessToken(bsPipelineAuthServiceCode)
            } else accessToken
            // 添加paas项目
            projectPaasCCService.createPaasCCProject(
                userId = userId,
                projectId = projectId,
                accessToken = newAccessToken!!,
                projectCreateInfo = projectCreateInfo
            )
        }

        // 工蜂CI项目不会添加paas项目，但也需要广播
        projectDispatcher.dispatch(
            ProjectCreateBroadCastEvent(
                userId = userId,
                projectId = projectId,
                projectInfo = projectCreateInfo
            )
        )
    }

    override fun saveLogoAddress(userId: String, projectCode: String, logoFile: File): String {
        return s3Service.saveLogo(logoFile, projectCode)
    }

    override fun deleteAuth(projectId: String, accessToken: String?) {
//        logger.warn("Deleting the project $projectId from auth")
//        try {
//            val url = "$authUrl/$projectId?access_token=$accessToken"
//            val request = Request.Builder().url(url).delete().build()
//            val responseContent = request(request, "Fail to delete the project $projectId")
//            logger.info("Get the delete project $projectId response $responseContent")
//            val response: Response<Any?> = objectMapper.readValue(responseContent)
//            if (response.code.toInt() != 0) {
//                logger.warn("Fail to delete the project $projectId with response $responseContent")
//            }
//            logger.info("Finish deleting the project $projectId from auth")
//        } catch (t: Throwable) {
//            logger.warn("Fail to delete the project $projectId from auth", t)
//        }
        projectPermissionService.deleteResource(projectId)
    }

    // 此处为兼容V0,V3并存的情况, 拉群用户有权限的项目列表需取V0+V3的并集, 无论啥集群, 都需取两个iam环境下的数据, 完全迁移完后可直接指向V3
    override fun getProjectFromAuth(userId: String?, accessToken: String?): List<String> {
        // 全部迁移完后,直接用此实现
//        val projectEnglishNames = projectPermissionService.getUserProjects(userId!!)
        val iamV0List = getV0UserProject(userId, accessToken)
        logger.info("$userId V0 project: $iamV0List")

        // 请求V3的项目,流量必须指向到v3,需指定项目头
        val iamV3List = getV3UserProject(userId!!)
        logger.info("$userId V3 project: $iamV3List")
        val projectList = mutableSetOf<String>()
        projectList.addAll(iamV0List)
        if (!iamV3List.isNullOrEmpty()) {
            projectList.addAll(iamV3List)
        }
        return projectList.toList()
    }

    override fun updateInfoReplace(projectUpdateInfo: ProjectUpdateInfo) {
        return
    }

    private fun request(request: Request, errorMessage: String): String {
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("Fail to request($request) with code ${response.code()}, " +
                    "message ${response.message()} and response $responseContent")
                throw OperationException(errorMessage)
            }
            return responseContent
        }
    }

    override fun drawFile(projectCode: String): File {
        // 随机生成首字母图片
        val firstChar = projectCode.substring(0, 1).toUpperCase()
        return ImageUtil.drawImage(firstChar)
    }

    override fun validatePermission(projectCode: String, userId: String, permission: AuthPermission): Boolean {
        val group = if (permission == AuthPermission.MANAGE) {
            BkAuthGroup.MANAGER
        } else {
            null
        }
        return bsAuthProjectApi.isProjectUser(userId, bsPipelineAuthServiceCode, projectCode, group)
    }

    override fun modifyProjectAuthResource(projectCode: String, projectName: String) {
        return
    }

    fun getInfoByEnglishName(englishName: String): ProjectVO? {
        val record = projectDao.getByEnglishName(dslContext, englishName) ?: return null
        return ProjectUtils.packagingBean(record, grayProjectSet())
    }

    override fun hasCreatePermission(userId: String): Boolean {
        return true
    }

    override fun organizationMarkUp(projectCreateInfo: ProjectCreateInfo, userDeptDetail: UserDeptDetail): ProjectCreateInfo {
        val bgId = if (projectCreateInfo.bgId == 0L) userDeptDetail.bgId.toLong() else projectCreateInfo.bgId
        val deptId = if (projectCreateInfo.deptId == 0L) userDeptDetail.deptId.toLong() else projectCreateInfo.deptId
        val centerId = if (projectCreateInfo.centerId == 0L) {
            userDeptDetail.centerId.toLong()
        } else projectCreateInfo.centerId
        val bgName = if (projectCreateInfo.bgName.isNullOrEmpty()) userDeptDetail.bgName else projectCreateInfo.bgName
        val deptName = if (projectCreateInfo.deptName.isNullOrEmpty()) userDeptDetail.deptName else projectCreateInfo.deptName
        val centerName = if (projectCreateInfo.centerName.isNullOrEmpty()) userDeptDetail.centerName else projectCreateInfo.centerName

        return ProjectCreateInfo(
            projectName = projectCreateInfo.projectName,
            projectType = projectCreateInfo.projectType,
            secrecy = projectCreateInfo.secrecy,
            description = projectCreateInfo.description,
            kind = projectCreateInfo.kind,
            bgId = bgId,
            bgName = bgName,
            centerId = centerId,
            centerName = centerName,
            deptId = deptId,
            deptName = deptName,
            englishName = projectCreateInfo.englishName
        )
    }

    private fun getV0UserProject(userId: String?, accessToken: String?): List<String> {
        val token = if (accessToken.isNullOrEmpty()) {
            bsAuthTokenApi.getAccessToken(bsPipelineAuthServiceCode)
        } else {
            accessToken
        }
        val url = "$v0IamUrl/projects?access_token=$token&user_id=$userId"
        logger.info("Start to get auth projects - ($url)")
        val request = Request.Builder().url(url).get().build()
        val responseContent = request(request, MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PEM_QUERY_ERROR))
        val result = objectMapper.readValue<Result<ArrayList<AuthProjectForList>>>(responseContent)
        if (result.isNotOk()) {
            logger.warn("Fail to get the project info with response $responseContent")
            throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PEM_QUERY_ERROR))
        }
        if (result.data == null) {
            return emptyList()
        }

        return result.data!!.map {
            it.project_code
        }
    }

    private fun getV3UserProject(userId: String): List<String>? {
        if (v3Tag.isNullOrEmpty()) {
            return emptyList()
        }
        logger.info("getV3userProject tag: $v3Tag")
        try {
            return ConsulContent.invokeByTag(v3Tag) {
                try {
                    // 请求V3的项目,流量必须指向到v3,需指定项目头
                    client.get(ServiceProjectAuthResource::class).getUserProjects(
                        userId = userId,
                        token = tokenService.getSystemToken(null)!!
                    ).data
                } catch (e: Exception) {
                    logger.warn("ServiceGitForAppResource is error", e)
                    return@invokeByTag null
                }
            }
        } catch (e: Exception) {
            // 为防止V0,V3发布存在时间差,导致项目列表拉取异常
            logger.warn("getV3Project fail $userId $e")
            return emptyList()
        }
    }

    override fun createProjectUser(projectId: String, createInfo: ProjectCreateUserInfo): Boolean {
        projectExtPermissionService.createUser2Project(
            createUser = createInfo.createUserId,
            projectCode = projectId,
            roleName = createInfo.roleName,
            roleId = createInfo.roleId,
            userIds = createInfo.userIds!!,
            checkManager = true
        )
        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TxProjectServiceImpl::class.java)!!
    }
}
