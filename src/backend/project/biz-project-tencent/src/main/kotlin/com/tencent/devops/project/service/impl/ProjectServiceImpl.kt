package com.tencent.devops.project.service.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.auth.api.BSAuthProjectApi
import com.tencent.devops.common.auth.api.BkAuthServiceCode
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.code.AuthServiceCode
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.common.web.mq.*
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.jmx.api.ProjectJmxApi
import com.tencent.devops.project.pojo.*
import com.tencent.devops.project.pojo.enums.ProjectChannelCode
import com.tencent.devops.project.pojo.enums.ProjectTypeEnum
import com.tencent.devops.project.pojo.enums.ProjectValidateType
import com.tencent.devops.project.pojo.tof.Response
import com.tencent.devops.project.service.ProjectPermissionService
import com.tencent.devops.project.service.s3.S3Service
import com.tencent.devops.project.service.tof.TOFService
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.MessageProperties
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.util.*
import javax.imageio.ImageIO

@Service
class ProjectServiceImpl @Autowired constructor(
        private val projectPermissionService: ProjectPermissionService,
        private val dslContext: DSLContext,
        private val projectDao: ProjectDao,
        private val projectJmxApi: ProjectJmxApi,
        private val redisOperation: RedisOperation,
        private val gray: Gray,
        private val client: Client,
        private val rabbitTemplate: RabbitTemplate,
        private val s3Service: S3Service,
        private val objectMapper: ObjectMapper,
        private val tofService: TOFService,
        private val bkAuthProjectApi: BSAuthProjectApi,
        private val bsPipelineAuthServiceCode: BSPipelineAuthServiceCode,
        private val jmxApi: ProjectJmxApi
): AbsProjectServiceImpl(projectPermissionService, dslContext, projectDao, projectJmxApi, redisOperation, gray, client){

    @Value("\${auth.url}")
    private lateinit var authUrl: String

    @Value("\${paas_cc.url}")
    private lateinit var ccUrl: String

    override fun create(userId: String, accessToken: String, projectCreateInfo: ProjectCreateInfo) {
        validate(ProjectValidateType.project_name, projectCreateInfo.projectName)
        validate(ProjectValidateType.english_name, projectCreateInfo.englishName)

        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            // 随机生成图片
            val logoFile = drawImage(projectCreateInfo.englishName.substring(0, 1).toUpperCase())
            try {
                // 发送服务器
                val logoAddress = s3Service.saveLogo(logoFile, projectCreateInfo.englishName)

                // 创建AUTH项目
                val authUrl = "$authUrl?access_token=$accessToken"
                val param: MutableMap<String, String> = mutableMapOf("project_code" to projectCreateInfo.englishName)
                val mediaType = MediaType.parse("application/json; charset=utf-8")
                val json = objectMapper.writeValueAsString(param)
                val requestBody = RequestBody.create(mediaType, json)
                val request = Request.Builder().url(authUrl).post(requestBody).build()
                val responseContent = request(request, "调用权限中心创建项目失败")
                val result = objectMapper.readValue<Result<AuthProjectForCreateResult>>(responseContent)
                if (result.isNotOk()) {
                    logger.warn("Fail to create the project of response $responseContent")
                    throw OperationException("调用权限中心创建项目失败: ${result.message}")
                }
                val authProjectForCreateResult = result.data
                val projectId = if (authProjectForCreateResult != null) {
                    if (authProjectForCreateResult.project_id.isBlank()) {
                        throw OperationException("权限中心创建的项目ID无效")
                    }
                    authProjectForCreateResult.project_id
                } else {
                    logger.warn("Fail to get the project id from response $responseContent")
                    throw OperationException("权限中心创建的项目ID无效")
                }
                val userDeptDetail = tofService.getUserDeptDetail(userId, "") // 获取用户机构信息
                try {
                    projectDao.create(
                            dslContext = dslContext,
                            userId = userId,
                            logoAddress = logoAddress,
                            projectCreateInfo = projectCreateInfo,
                            userDeptDetail = userDeptDetail,
                            projectId = projectId,
                            channelCode = ProjectChannelCode.BS
                    )
                } catch (e: DuplicateKeyException) {
                    logger.warn("Duplicate project $projectCreateInfo", e)
                    throw OperationException("项目名或者英文名重复")
                } catch (t: Throwable) {
                    logger.warn("Fail to create the project ($projectCreateInfo)", t)
                    deleteProjectFromAuth(projectId, accessToken)
                    throw t
                }

                rabbitTemplate.convertAndSend(
                        EXCHANGE_PAASCC_PROJECT_CREATE,
                        ROUTE_PAASCC_PROJECT_CREATE, PaasCCCreateProject(
                        userId = userId,
                        accessToken = accessToken,
                        projectId = projectId,
                        retryCount = 0,
                        projectCreateInfo = projectCreateInfo
                )
                )
                success = true
            } finally {
                if (logoFile.exists()) {
                    logoFile.delete()
                }
            }
        } finally {
//            jmxApi.execute(PROJECT_CREATE, System.currentTimeMillis() - startEpoch, success)
        }
    }

    override fun getProjectEnNamesByOrganization(userId: String, bgId: Long?, deptName: String?, centerName: String?, interfaceName: String?): List<String> {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val list = projectDao.listByOrganization(
                    dslContext = dslContext,
                    bgId = bgId,
                    deptName = deptName,
                    centerName = centerName
            )?.filter { it.enabled == null || it.enabled }?.map { it.englishName }?.toList() ?: emptyList()
            success = true
            return list
        } finally {
            jmxApi.execute("getProjectEnNamesByOrganization", System.currentTimeMillis() - startEpoch, success)
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list project EnNames,userName:$userId")
        }
    }

    override fun getOrCreatePreProject(userId: String, accessToken: String): ProjectVO {
        val projectCode = "_$userId"
        var userProjectRecord = projectDao.getByEnglishName(dslContext, projectCode)
        if (userProjectRecord != null) {
            return packagingBean(userProjectRecord, setOf())
        }

        val projectCreateInfo = ProjectCreateInfo(
                projectName = projectCode,
                englishName = projectCode,
                projectType = ProjectTypeEnum.SUPPORT_PRODUCT.index,
                description = "prebuild project for $userId",
                bgId = 0L,
                bgName = "",
                deptId = 0L,
                deptName = "",
                centerId = 0L,
                centerName = "",
                secrecy = false,
                kind = 0
        )

        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            // 随机生成图片
            val logoFile = drawImage(projectCreateInfo.englishName.substring(0, 1).toUpperCase())
            try {
                // 发送服务器
                val logoAddress = s3Service.saveLogo(logoFile, projectCreateInfo.englishName)

                var projectId = getProjectIdInAuth(projectCode, accessToken)

                if (null == projectId) {
                    // 创建AUTH项目
                    val authUrl = "$authUrl?access_token=$accessToken"
                    val param: MutableMap<String, String> =
                            mutableMapOf("project_code" to projectCreateInfo.englishName)
                    val mediaType = MediaType.parse("application/json; charset=utf-8")
                    val json = objectMapper.writeValueAsString(param)
                    val requestBody = RequestBody.create(mediaType, json)
                    val request = Request.Builder().url(authUrl).post(requestBody).build()
                    val responseContent = request(request, "调用权限中心创建项目失败")
                    val result = objectMapper.readValue<Result<AuthProjectForCreateResult>>(responseContent)
                    if (result.isNotOk()) {
                        logger.warn("Fail to create the project of response $responseContent")
                        throw OperationException("调用权限中心创建项目失败: ${result.message}")
                    }
                    val authProjectForCreateResult = result.data
                    projectId = if (authProjectForCreateResult != null) {
                        if (authProjectForCreateResult.project_id.isBlank()) {
                            throw OperationException("权限中心创建的项目ID无效")
                        }
                        authProjectForCreateResult.project_id
                    } else {
                        logger.warn("Fail to get the project id from response $responseContent")
                        throw OperationException("权限中心创建的项目ID无效")
                    }
                }
                val userDeptDetail = tofService.getUserDeptDetail(userId, "") // 获取用户机构信息
                try {
                    projectDao.create(
                            dslContext = dslContext,
                            userId = userId,
                            logoAddress = logoAddress,
                            projectCreateInfo = projectCreateInfo,
                            userDeptDetail = userDeptDetail,
                            projectId = projectId,
                            channelCode = ProjectChannelCode.BS
                    )
                } catch (e: DuplicateKeyException) {
                    logger.warn("Duplicate project $projectCreateInfo", e)
                    throw OperationException("项目名或者英文名重复")
                } catch (t: Throwable) {
                    logger.warn("Fail to create the project ($projectCreateInfo)", t)
                    deleteProjectFromAuth(projectId, accessToken)
                    throw t
                }

                rabbitTemplate.convertAndSend(
                        EXCHANGE_PAASCC_PROJECT_CREATE,
                        ROUTE_PAASCC_PROJECT_CREATE, PaasCCCreateProject(
                        userId = userId,
                        accessToken = accessToken,
                        projectId = projectId,
                        retryCount = 0,
                        projectCreateInfo = projectCreateInfo
                )
                )
                success = true
            } finally {
                if (logoFile.exists()) {
                    logoFile.delete()
                }
            }
        } finally {
            jmxApi.execute(PROJECT_CREATE, System.currentTimeMillis() - startEpoch, success)
        }

        userProjectRecord = projectDao.getByEnglishName(dslContext, projectCode)
        return packagingBean(userProjectRecord!!, setOf())
    }

    override fun getProjectByGroup(userId: String, bgName: String?, deptName: String?, centerName: String?): List<ProjectVO> {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val grayProjectSet = grayProjectSet()
            val list = ArrayList<ProjectVO>()
            projectDao.listByGroup(dslContext, bgName, deptName, centerName).filter { it.enabled == null || it.enabled }
                    .map {
                        list.add(packagingBean(it, grayProjectSet))
                    }
            success = true
            return list
        } finally {
            jmxApi.execute(PROJECT_LIST, System.currentTimeMillis() - startEpoch, success)
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list projects,userName:$userId")
        }
    }

    override fun updateUsableStatus(userId: String, projectId: String, enabled: Boolean) {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            logger.info("[$userId|$projectId|$enabled] Start to update project usable status")
            if (bkAuthProjectApi.getProjectUsers(bsPipelineAuthServiceCode, projectId, BkAuthGroup.MANAGER).contains(
                            userId
                    )
            ) {
                val updateCnt = projectDao.updateUsableStatus(dslContext, userId, projectId, enabled)
                if (updateCnt != 1) {
                    logger.warn("更新数据库出错，变更行数为:$updateCnt")
                }
            } else {
                throw OperationException("没有该项目的操作权限")
            }
            logger.info("[$userId|[$projectId] Project usable status is changed to $enabled")
            success = true
        } finally {
            jmxApi.execute(PROJECT_UPDATE, System.currentTimeMillis() - startEpoch, success)
        }
    }

    override fun getProjectUsers(accessToken: String, userId: String, projectCode: String): Result<List<String>?> {
        logger.info("getProjectUsers accessToken is :$accessToken,userId is :$userId,projectCode is :$projectCode")
        // 检查用户是否有查询项目下用户列表的权限
        val validateResult = verifyUserProjectPermission(accessToken, projectCode, userId)
        logger.info("getProjectUsers validateResult is :$validateResult")
        val validateFlag = validateResult.data
        if (null == validateFlag || !validateFlag) {
            val messageResult = MessageCodeUtil.generateResponseDataObject<String>(CommonMessageCode.PERMISSION_DENIED)
            return Result(messageResult.status, messageResult.message, null)
        }
        val projectUserList = bkAuthProjectApi.getProjectUsers(bsPipelineAuthServiceCode, projectCode)
        logger.info("getProjectUsers projectUserList is :$projectUserList")
        return Result(projectUserList)
    }

    override fun getProjectUserRoles(accessToken: String, userId: String, projectCode: String, serviceCode: AuthServiceCode): List<UserRole> {
        val groupAndUsersList = bkAuthProjectApi.getProjectGroupAndUserList(serviceCode, projectCode)
        return groupAndUsersList.filter { it.userIdList.contains(userId) }
                .map { UserRole(it.displayName, it.roleId, it.roleName, it.type) }
    }

    override fun getByEnglishName(accessToken: String, englishName: String): ProjectVO {
        val projectVO = getByEnglishName(englishName) ?: throw OperationException("项目不存在")
        val projectAuthIds = getAuthProjectIds(accessToken)
        if (!projectAuthIds.contains(projectVO.projectId)) {
            logger.warn("The user don't have the permission to get the project $englishName")
            throw OperationException("项目不存在")
        }
        return projectVO
    }

    override fun update(userId: String, accessToken: String, projectId: String, projectUpdateInfo: ProjectUpdateInfo) {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            try {
                val appName = if (projectUpdateInfo.ccAppId != null && projectUpdateInfo.ccAppId!! > 0) {
                    tofService.getCCAppName(projectUpdateInfo.ccAppId!!)
                } else {
                    null
                }
                projectUpdateInfo.ccAppName = appName
                projectDao.update(dslContext, userId, projectId, projectUpdateInfo)
            } catch (e: DuplicateKeyException) {
                logger.warn("Duplicate project $projectUpdateInfo", e)
                throw OperationException("项目名或英文名重复")
            }
            rabbitTemplate.convertAndSend(
                    EXCHANGE_PAASCC_PROJECT_UPDATE,
                    ROUTE_PAASCC_PROJECT_UPDATE, PaasCCUpdateProject(
                    userId = userId,
                    accessToken = accessToken,
                    projectId = projectId,
                    retryCount = 0,
                    projectUpdateInfo = projectUpdateInfo
            )
            )
            success = true
        } finally {
            jmxApi.execute(PROJECT_UPDATE, System.currentTimeMillis() - startEpoch, success)
        }
    }

    override fun updateLogo(userId: String, accessToken: String, projectId: String, inputStream: InputStream, disposition: FormDataContentDisposition): Result<Boolean> {
        logger.info("Update the logo of project $projectId")
        val project = projectDao.get(dslContext, projectId)
        if (project != null) {
            var logoFile: File? = null
            try {
                logoFile = convertFile(inputStream)
                val logoAddress = s3Service.saveLogo(logoFile, project.englishName)
                projectDao.updateLogoAddress(dslContext, userId, projectId, logoAddress)
                rabbitTemplate.convertAndSend(
                        EXCHANGE_PAASCC_PROJECT_UPDATE_LOGO,
                        ROUTE_PAASCC_PROJECT_UPDATE_LOGO, PaasCCUpdateProjectLogo(
                        userId = userId,
                        accessToken = accessToken,
                        projectId = projectId,
                        retryCount = 0,
                        projectUpdateLogoInfo = ProjectUpdateLogoInfo(logoAddress, userId)
                )
                )
            } catch (e: Exception) {
                logger.warn("fail update projectLogo", e)
                throw OperationException("更新项目logo失败")
            } finally {
                logoFile?.delete()
            }
        } else {
            logger.warn("$project is null or $project is empty")
            throw OperationException("查询不到有效的项目")
        }
        return Result(true)
    }

    override fun list(accessToken: String, includeDisable: Boolean?): List<ProjectVO> {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val projectIdList = getAuthProjectIds(accessToken).toSet()
            val list = ArrayList<ProjectVO>()

            val grayProjectSet = grayProjectSet()

            projectDao.list(dslContext, projectIdList).filter {
                includeDisable == true || it.enabled == null || it.enabled
            }.map {
                list.add(packagingBean(it, grayProjectSet))
            }
            success = true
            return list
        } finally {
            jmxApi.execute(PROJECT_LIST, System.currentTimeMillis() - startEpoch, success)
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list projects")
        }
    }

    override fun list(projectCodes: Set<String>): List<ProjectVO> {
        return super.list(projectCodes)
    }

    private fun drawImage(logoStr: String): File {
        val logoBackgroundColor = arrayOf("#FF5656", "#FFB400", "#30D878", "#3C96FF")
        val max = logoBackgroundColor.size - 1
        val min = 0
        val random = Random()
        val backgroundIndex = random.nextInt(max) % (max - min + 1) + min
        val width = 128
        val height = 128
        // 创建BufferedImage对象
        val bi = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        // 获取Graphics2D
        val g2d = bi.createGraphics()
        // 设置透明度
        g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 1.0f)

        when (backgroundIndex) {
            0 -> {
                g2d.background = Color.RED
            }
            1 -> {
                g2d.background = Color.YELLOW
            }
            2 -> {
                g2d.background = Color.GREEN
            }
            3 -> {
                g2d.background = Color.BLUE
            }
        }
        g2d.clearRect(0, 0, width, height)
        g2d.color = Color.WHITE
        g2d.stroke = BasicStroke(1.0f)
        val font = Font("宋体", Font.PLAIN, 64)
        g2d.font = font
        val fontMetrics = g2d.fontMetrics
        val heightAscent = fontMetrics.ascent

        val context = g2d.fontRenderContext
        val stringBounds = font.getStringBounds(logoStr, context)
        val fontWidth = stringBounds.width.toFloat()

        g2d.drawString(
                logoStr,
                (width / 2 - fontWidth / 2),
                (height / 2 + heightAscent / 2).toFloat()
        )
        // 透明度设置 结束
        g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER)
        // 释放对象
        g2d.dispose()
        // 保存文件
        val logo = Files.createTempFile("default_", ".png").toFile()
        ImageIO.write(bi, "png", logo)
        return logo
    }

    fun verifyUserProjectPermission(accessToken: String, projectCode: String, userId: String): Result<Boolean> {
        val url = "$authUrl/$projectCode/users/$userId/verfiy?access_token=$accessToken"
        logger.info("the verifyUserProjectPermission url is:$url")
        val body = RequestBody.create(MediaType.parse(MessageProperties.CONTENT_TYPE_JSON), "{}")
        val request = Request.Builder().url(url).post(body).build()
        val responseContent = request(request, "verifyUserProjectPermission error")
        val result = objectMapper.readValue<Result<Any?>>(responseContent)
        logger.info("the verifyUserProjectPermission result is:$result")
        if (result.isOk()) {
            return Result(true)
        }
        return Result(false)
    }

    private fun getAuthProjectIds(accessToken: String): List<String/*projectId*/> {
        val url = "$authUrl?access_token=$accessToken"
        val request = Request.Builder().url(url).get().build()
        val responseContent = request(request, "从权限中心获取用户的项目信息失败")
        val result = objectMapper.readValue<Result<ArrayList<AuthProjectForList>>>(responseContent)
        if (result.isNotOk()) {
            logger.warn("Fail to get the project info with response $responseContent")
            throw OperationException("从权限中心获取用户的项目信息失败")
        }
        if (result.data == null) {
            return emptyList()
        }

        return result.data!!.map {
            it.project_id
        }.toList()
    }

    private fun convertFile(inputStream: InputStream): File {
        val logo = Files.createTempFile("default_", ".png").toFile()

        logo.outputStream().use {
            inputStream.copyTo(it)
        }

        return logo
    }

    private fun request(request: Request, errorMessage: String): String {
//        val httpClient = okHttpClient.newBuilder().build()
        OkhttpUtils.doHttp(request).use { response ->
            //        httpClient.newCall(request).execute().use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("Fail to request($request) with code ${response.code()} , message ${response.message()} and response $responseContent")
                throw OperationException(errorMessage)
            }
            return responseContent
        }
    }

    private fun deleteProjectFromAuth(projectId: String, accessToken: String, retry: Boolean = true) {
        logger.warn("Deleting the project $projectId from auth")
        try {
            val url = "$authUrl/$projectId?access_token=$accessToken"
            val request = Request.Builder().url(url).delete().build()
            val responseContent = request(request, "Fail to delete the project $projectId")
            logger.info("Get the delete project $projectId response $responseContent")
            val response: Response<Any?> = objectMapper.readValue(responseContent)
            if (response.code.toInt() != 0) {
                logger.warn("Fail to delete the project $projectId with response $responseContent")
                deleteProjectFromAuth(projectId, accessToken, false)
            }
            logger.info("Finish deleting the project $projectId from auth")
        } catch (t: Throwable) {
            logger.warn("Fail to delete the project $projectId from auth", t)
            if (retry) {
                deleteProjectFromAuth(projectId, accessToken, false)
            }
        }
    }

    fun getProjectIdInAuth(projectCode: String, accessToken: String): String? {
        try {
            val url = "$authUrl/projects/$projectCode?access_token=$accessToken"
            logger.info("Get request url: $url")
            OkhttpUtils.doGet(url).use { resp ->
                val responseStr = resp.body()!!.string()
                logger.info("responseBody: $responseStr")
                val response: Map<String, Any> = jacksonObjectMapper().readValue(responseStr)
                return if (response["code"] as Int == 0) {
                    response["project_id"] as String
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            logger.error("Get project info error", e)
            throw RuntimeException("Get project info error: ${e.message}")
        }
    }

    companion object{
        val logger = LoggerFactory.getLogger(this:: class.java)
        const val PROJECT_LIST = "project_list"
        const val PROJECT_CREATE = "project_create"
        const val PROJECT_UPDATE = "project_update"
    }
}