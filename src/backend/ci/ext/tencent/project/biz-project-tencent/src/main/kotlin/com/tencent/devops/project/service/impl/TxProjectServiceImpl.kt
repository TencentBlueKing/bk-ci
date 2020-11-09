package com.tencent.devops.project.service.impl

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.bkrepo.common.api.util.JsonUtils.objectMapper
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.BkAuthProperties
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.common.service.gray.RepoGray
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.dispatch.ProjectDispatcher
import com.tencent.devops.project.jmx.api.ProjectJmxApi
import com.tencent.devops.project.pojo.*
import com.tencent.devops.project.pojo.tof.Response
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.project.service.ProjectLocalService
import com.tencent.devops.project.service.ProjectPaasCCService
import com.tencent.devops.project.service.ProjectPermissionService
import com.tencent.devops.project.service.s3.S3Service
import com.tencent.devops.project.service.tof.TOFService
import com.tencent.devops.project.util.ImageUtil
import com.tencent.devops.project.util.ProjectUtils
import okhttp3.Request
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.util.ArrayList

@Service
class TxProjectServiceImpl @Autowired constructor(
        projectPermissionService: ProjectPermissionService,
        private val dslContext: DSLContext,
        private val projectDao: ProjectDao,
        private val s3Service: S3Service,
        private val tofService: TOFService,
        private val bkRepoClient: BkRepoClient,
        private val repoGray: RepoGray,
        private val projectPaasCCService: ProjectPaasCCService,
        private val bkAuthProperties: BkAuthProperties,
        private val bsAuthProjectApi: AuthProjectApi,
        private val bsPipelineAuthServiceCode: BSPipelineAuthServiceCode,
        projectJmxApi: ProjectJmxApi,
        redisOperation: RedisOperation,
        gray: Gray,
        client: Client,
        projectDispatcher: ProjectDispatcher
) : AbsProjectServiceImpl(projectPermissionService, dslContext, projectDao, projectJmxApi, redisOperation, gray, client, projectDispatcher) {

    private var authUrl: String = "${bkAuthProperties.url}/projects"

	override fun getByEnglishName(englishName: String, accessToken: String?): ProjectVO? {
		val projectVO = getInfoByEnglishName(englishName)
		val projectAuthIds = getProjectFromAuth("", accessToken)
		if (!projectAuthIds.contains(projectVO!!.projectId)) {
			logger.warn("The user don't have the permission to get the project $englishName")
            return null
		}
		return projectVO
	}

	override fun getDeptInfo(userId: String): UserDeptDetail {
        return tofService.getUserDeptDetail(userId, "") // 获取用户机构信息
    }

    override fun createExtProjectInfo(userId: String, projectId: String, accessToken: String?, projectCreateInfo: ProjectCreateInfo, isUserProject: Boolean?) {
        // 添加repo项目
        val createSuccess = bkRepoClient.createBkRepoResource(userId, projectCreateInfo.englishName)
        logger.info("create bkrepo project ${projectCreateInfo.englishName} success: $createSuccess")
        if (createSuccess) {
            repoGray.addGrayProject(projectCreateInfo.englishName, redisOperation)
            logger.info("add project ${projectCreateInfo.englishName} to repoGrey")
        }

        if(!accessToken.isNullOrEmpty() && isUserProject!!) {
            // 添加paas项目
            projectPaasCCService.createPaasCCProject(
                    userId = userId,
                    projectId = projectId,
                    accessToken = accessToken!!,
                    projectCreateInfo = projectCreateInfo
            )
        }
    }

    override fun saveLogoAddress(userId: String, projectCode: String, logoFile: File): String {
        return s3Service.saveLogo(logoFile, projectCode)
    }

    override fun deleteAuth(projectId: String, accessToken: String?) {
        logger.warn("Deleting the project $projectId from auth")
        try {
            val url = "$authUrl/$projectId?access_token=$accessToken"
            val request = Request.Builder().url(url).delete().build()
            val responseContent = request(request, "Fail to delete the project $projectId")
            logger.info("Get the delete project $projectId response $responseContent")
            val response: Response<Any?> = objectMapper.readValue(responseContent)
            if (response.code.toInt() != 0) {
                logger.warn("Fail to delete the project $projectId with response $responseContent")
            }
            logger.info("Finish deleting the project $projectId from auth")
        } catch (t: Throwable) {
            logger.warn("Fail to delete the project $projectId from auth", t)
        }
    }

    override fun getProjectFromAuth(userId: String?, accessToken: String?): Set<String> {
        val url = "$authUrl?access_token=$accessToken"
        logger.info("Start to get auth projects - ($url)")
        val request = Request.Builder().url(url).get().build()
        val responseContent = request(request, MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PEM_QUERY_ERROR))
        val result = objectMapper.readValue<Result<ArrayList<AuthProjectForList>>>(responseContent)
        if (result.isNotOk()) {
            logger.warn("Fail to get the project info with response $responseContent")
            throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PEM_QUERY_ERROR))
        }
        if (result.data == null) {
            return emptySet()
        }

        return result.data!!.map {
            it.project_id
        }.toSet()
    }

    override fun updateInfoReplace(projectUpdateInfo: ProjectUpdateInfo) {
        val appName = if (projectUpdateInfo.ccAppId != null && projectUpdateInfo.ccAppId!! > 0) {
            tofService.getCCAppName(projectUpdateInfo.ccAppId!!)
        } else {
            null
        }
        projectUpdateInfo.ccAppName = appName
    }

    private fun request(request: Request, errorMessage: String): String {
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("Fail to request($request) with code ${response.code()} , message ${response.message()} and response $responseContent")
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

    companion object {
        private const val Width = 128
        private const val Height = 128
        private val logger = LoggerFactory.getLogger(TxProjectServiceImpl::class.java)!!
    }
}