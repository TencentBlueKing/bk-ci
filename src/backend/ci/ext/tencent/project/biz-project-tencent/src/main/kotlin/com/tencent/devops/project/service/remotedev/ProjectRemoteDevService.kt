package com.tencent.devops.project.service.remotedev

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.auth.api.service.ServiceMonitorSpaceResource
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.dao.RemoteDevDao
import com.tencent.devops.project.pojo.ProjectProperties
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.apache.commons.codec.digest.HmacAlgorithms
import org.apache.commons.codec.digest.HmacUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * 将 project 中 remotedev 相关业务逻辑放到这里，避免污染
 */
@Service
class ProjectRemoteDevService @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val client: Client,
    private val dslContext: DSLContext,
    private val projectDao: ProjectDao,
    private val remoteDevDao: RemoteDevDao
) {

    @Value("\${remoteDev.appCode:}")
    val appCode = ""

    @Value("\${remoteDev.appToken:}")
    val appSecret = ""

    @Value("\${remoteDev.bkMonitorUrl:}")
    val bkMonitorUrl = ""

    @Value("\${remoteDev.bkrepoDevxUrl:}")
    val bkrepoDevxUrl = ""

    @Value("\${remoteDev.bkrepoDevxSha256Key:}")
    val bkrepoDevxSha256Key = ""

    @Value("\${remoteDev.enableRemotedevNotifyCode:}")
    val enableRemotedevNotifyCode = ""

    // 开启 remotedev 相关逻辑
    fun enableRemoteDev(
        userId: String,
        projectCode: String,
        enableRepoData: EnableBkRepoData
    ) {
        // 迁移监控权限, 从 auth 获取 bizid
        if (migrateMonitorResource(listOf(projectCode))) {
            val bizId = try {
                val data = client.get(ServiceMonitorSpaceResource::class)
                    .getMonitorSpaceBizId(userId, projectCode).data
                if (data.isNullOrBlank()) {
                    logger.warn("enableRemoteDev getMonitorSpaceBizId null or blank")
                    null
                } else {
                    data.toLong()
                }
            } catch (e: Exception) {
                logger.error("enableRemoteDev getMonitorSpaceBizId error", e)
                null
            }

            // 创建监控表盘
            if (bizId != null) {
                quickImportDashboard(bizId)
            }
        }

        // 启动bkrepo相关配置
        enableBkRepo(enableRepoData)
    }

    private fun quickImportDashboard(
        bizId: Long
    ) {
        val url = "$bkMonitorUrl/quick_import_dashboard/"
        val headerStr = objectMapper.writeValueAsString(
            mapOf("bk_app_code" to appCode, "bk_app_secret" to appSecret)
        ).replace("\\s".toRegex(), "")
        val requestBody = objectMapper.writeValueAsString(QuickImportDashboardReq(bkBizId = bizId))
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("X-Bkapi-Authorization", headerStr)
            .build()

        try {
            OkhttpUtils.doHttp(request).use {
                val responseStr = it.body!!.string()
                if (!it.isSuccessful) {
                    logger.warn("quickImportDashboard request failed, uri:($url)|response: ($requestBody)")
                    return
                }
                val resp = objectMapper.readValue<BkMonitorResp>(responseStr)
                if (resp.code != 200L || !resp.result) {
                    logger.warn("quickImportDashboard request failed, url:($url)|response:($responseStr)")
                    return
                }
                return
            }
        } catch (e: Exception) {
            logger.warn("quickImportDashboard request failed", e)
        }
    }

    private fun migrateMonitorResource(projectList: List<String>): Boolean {
        return try {
            logger.debug("migrateMonitorResource projects {}", projectList)
            client.get(ServiceMonitorSpaceResource::class).migrateMonitorResource(projectList).data ?: false
        } catch (e: Exception) {
            logger.warn("migrateMonitorResource projects {} error", projectList, e)
            false
        }
    }

    // 启动bkrepo相关配置
    private fun enableBkRepo(
        data: EnableBkRepoData
    ) {
        val body = objectMapper.writeValueAsString(data)
        val url = "$bkrepoDevxUrl/repository/api/webhook/receiver/bkci"
        val requestBody = body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .headers(
                mapOf(
                    "X-DEVOPS-EVENT" to "DEVX_ENABLED",
                    "X-DEVOPS-SIGNATURE-256" to HmacUtils(
                        HmacAlgorithms.HMAC_SHA_256,
                        bkrepoDevxSha256Key
                    ).hmacHex(body)
                ).toHeaders()
            )
            .post(requestBody)
            .build()
        logger.debug("enableBkRepo|{}|{}", request.headers, body)
        try {
            OkhttpUtils.doHttp(request).use {
                val responseStr = it.body!!.string()
                if (!it.isSuccessful) {
                    logger.warn("enableBkRepo request failed, uri:($url)|response: ($responseStr)")
                    return
                }
            }
        } catch (e: Exception) {
            logger.error("enableBkRepo request api[${request.url.toUrl()}] error: ${e.localizedMessage}")
        }
    }

    fun updateRemoteDevInfo(
        userId: String,
        projectCode: String,
        addcloudDesktopNum: Int?,
        enableRemotedev: Boolean?
    ): Boolean {
        if (addcloudDesktopNum == null && enableRemotedev == null) {
            return true
        }
        val record = projectDao.getByEnglishName(dslContext, projectCode) ?: return false
        if (record.properties == null && enableRemotedev == null) {
            return false
        }
        var prop = if (record.properties != null) {
            JsonUtil.to(record.properties, ProjectProperties::class.java)
        } else {
            ProjectProperties()
        }
        if (addcloudDesktopNum != null) {
            prop = prop.copy(cloudDesktopNum = prop.cloudDesktopNum + addcloudDesktopNum)
        }
        if (enableRemotedev != null) {
            prop = prop.copy(remotedev = enableRemotedev)
            val dbProperties = if (record.properties != null) {
                JsonUtil.to(record.properties, ProjectProperties::class.java)
            } else {
                ProjectProperties()
            }
            // 更新云研发项目时相关操作
            val enableRemoteDev = dbProperties.remotedev != true && enableRemotedev == true
            if (enableRemoteDev) {
                enableRemoteDev(
                    userId = userId,
                    projectCode = record.englishName,
                    enableRepoData = EnableBkRepoData(
                        projectName = record.projectName,
                        projectCode = record.englishName,
                        bgId = record.bgId.toString(),
                        bgName = record.bgName,
                        centerId = record.centerId.toString(),
                        centerName = record.centerName,
                        deptId = record.deptId.toString(),
                        deptName = record.deptName,
                        englishName = record.englishName,
                        productId = record.productId
                    )
                )
                // 新开启的云研发项目给所有管理员发通知
                val manager = dbProperties.remotedevManager
                    ?.split(";")?.filter { it.isNotBlank() }?.toSet()
                    ?: emptySet()

                sendEnableRemoteDevNotify(
                    sendNotifyUser = manager,
                    projectCode = record.englishName,
                    projectName = record.projectName,
                    cloudDesktopNum = prop.cloudDesktopNum
                )
            }
        }

        return projectDao.updatePropertiesByCode(dslContext, projectCode, prop) > 0
    }

    fun sendEnableRemoteDevNotify(
        sendNotifyUser: Set<String>,
        projectCode: String,
        projectName: String,
        cloudDesktopNum: Int
    ) {
        if (sendNotifyUser.isEmpty()) {
            return
        }
        client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(
            SendNotifyMessageTemplateRequest(
                templateCode = enableRemotedevNotifyCode,
                receivers = sendNotifyUser.toMutableSet(),
                notifyType = mutableSetOf(NotifyType.EMAIL.name),
                titleParams = mapOf(
                    "projectName" to projectName
                ),
                bodyParams = mapOf(
                    "projectCode" to projectCode,
                    "projectName" to projectName,
                    "cloudDesktopNum" to cloudDesktopNum.toString()
                )
            )
        )
    }

    fun fetchRemoteDevProject(projectId: String?): Map<String, String> {
        val projects = remoteDevDao.fetchProjectEnableRemoteDev(
            dslContext = dslContext,
            englishName = projectId
        )

        return projects.associate { it.englishName to it.projectName }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectRemoteDevService::class.java)
    }
}

data class QuickImportDashboardReq(
    @JsonProperty("bk_biz_id")
    val bkBizId: Long,
    @JsonProperty("dash_name")
    val dashName: String = "bkci/BKCI-云桌面"
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BkMonitorResp(
    val result: Boolean,
    val code: Long,
    val message: String
)

data class EnableBkRepoData(
    val projectName: String,
    val projectCode: String,
    val bgId: String?,
    val bgName: String?,
    val centerId: String?,
    val centerName: String?,
    val deptId: String?,
    val deptName: String?,
    val englishName: String,
    val productId: Int?
)
