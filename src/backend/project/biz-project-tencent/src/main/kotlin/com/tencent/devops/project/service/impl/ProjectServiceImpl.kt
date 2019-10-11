package com.tencent.devops.project.service.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.auth.code.AuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.common.web.mq.EXCHANGE_PAASCC_PROJECT_CREATE
import com.tencent.devops.common.web.mq.ROUTE_PAASCC_PROJECT_CREATE
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.jmx.api.ProjectJmxApi
import com.tencent.devops.project.pojo.*
import com.tencent.devops.project.pojo.enums.ProjectChannelCode
import com.tencent.devops.project.pojo.enums.ProjectValidateType
import com.tencent.devops.project.pojo.tof.Response
import com.tencent.devops.project.service.ProjectPermissionService
import com.tencent.devops.project.service.s3.S3Service
import com.tencent.devops.project.service.tof.TOFService
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
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
        private val tofService: TOFService
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getOrCreatePreProject(userId: String, accessToken: String): ProjectVO {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getProjectByGroup(userId: String, bgName: String?, deptName: String?, centerName: String?): List<ProjectVO> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun updateUsableStatus(userId: String, projectId: String, enabled: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getProjectUsers(accessToken: String, userId: String, projectCode: String): Result<List<String>?> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getProjectUserRoles(accessToken: String, userId: String, projectCode: String, serviceCode: AuthServiceCode): List<UserRole> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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

    companion object{
        val logger = LoggerFactory.getLogger(this:: class.java)
    }
}