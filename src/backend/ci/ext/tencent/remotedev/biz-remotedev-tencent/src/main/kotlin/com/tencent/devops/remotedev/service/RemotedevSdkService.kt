package com.tencent.devops.remotedev.service

import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditAttribute
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.auth.api.oauth2.Oauth2ServiceEndpointResource
import com.tencent.devops.auth.pojo.Oauth2PassWordRequest
import com.tencent.devops.auth.pojo.dto.ClientDetailsDTO
import com.tencent.devops.auth.pojo.enum.Oauth2GrantType
import com.tencent.devops.auth.pojo.vo.Oauth2AccessTokenVo
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.audit.TencentActionAuditContent
import com.tencent.devops.common.auth.api.TencentActionId
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.kafka.KafkaClient
import com.tencent.devops.remotedev.config.BkConfig
import com.tencent.devops.remotedev.dao.WorkspaceAppOauth2MaterialsDao
import com.tencent.devops.remotedev.pojo.DesktopTokenSign
import com.tencent.devops.remotedev.pojo.project.WeSecProjectWorkspace
import com.tencent.devops.remotedev.pojo.sdk.SdkReportDataType
import com.tencent.devops.remotedev.utils.RsaUtil
import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.ws.rs.core.Response
import org.apache.commons.codec.digest.DigestUtils
import org.jooq.impl.DefaultDSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * 存放云桌面 sdk 相关逻辑的地方
 */
@Service
class RemotedevSdkService @Autowired constructor(
    private val kafkaClient: KafkaClient,
    private val bkConfig: BkConfig,
    private val workspaceService: WorkspaceService,
    private val permissionService: PermissionService,
    private val workspaceAppOauth2MaterialsDao: WorkspaceAppOauth2MaterialsDao,
    private val dslContext: DefaultDSLContext,
    private val client: Client
) {
    @Value("\${sdk.kafka.topics.codeccQualityReport:#{null}}")
    val codeccQualityReport: String? = null

    @Value("\${sdk.kafka.topics.codeccCodeMonitor:#{null}}")
    val codeccCodeMonitor: String? = null

    fun getAppToken(desktopIP: String, sign: DesktopTokenSign): String {
        val ws = workspaceService.getWorkspaceList4WeSec(
            ip = desktopIP
        ).firstOrNull() ?: throwTokenFail(desktopIP, "unknown ip", "not find $desktopIP")
        check(ws, sign, desktopIP)
        val dToken = permissionService.init1Password(
            ws.owner ?: throwTokenFail(desktopIP, "unknown owner", "${ws.workspaceName} not has owner"),
            ws.workspaceName,
            ws.projectId,
            600
        )
        val rsaPublicKey = kotlin.runCatching { RsaUtil.generatePublicKey(Base64.getDecoder().decode(sign.publicKey)) }
            .onFailure { throwTokenFail(desktopIP, "wrong publicKey", sign.publicKey) }.getOrThrow()
        return RsaUtil.rsaEncrypt(dToken, rsaPublicKey)
    }

    fun getAccessToken(desktopIP: String, sign: DesktopTokenSign): Oauth2AccessTokenVo {
        val ws = workspaceService.getWorkspaceList4WeSec(
            ip = desktopIP
        ).firstOrNull() ?: throwTokenFail(desktopIP, "unknown ip", "not find $desktopIP")
        check(ws, sign, desktopIP)
        val userId = ws.owner ?: throwTokenFail(desktopIP, "unknown owner", "${ws.workspaceName} not has owner")
        val clientDetail = workspaceAppOauth2MaterialsDao.fetchAny(
            dslContext = dslContext,
            appId = sign.appId,
            workspaceName = ws.workspaceName
        )?.let { material ->
            ClientDetailsDTO(
                clientId = material.clientId,
                clientSecret = material.clientSecret,
                clientName = loadClientName(sign.appId, ws.workspaceName),
                scope = "",
                icon = "",
                authorizedGrantTypes = listOf(Oauth2GrantType.PASS_WORD),
                webServerRedirectUri = "",
                accessTokenValidity = TimeUnit.DAYS.toSeconds(180),
                refreshTokenValidity = TimeUnit.DAYS.toSeconds(180),
                createUser = ws.creator
            )
        } ?: run {
            createMaterial(sign, ws)
        }
        return kotlin.runCatching {
            client.get(Oauth2ServiceEndpointResource::class).getAccessToken(
                clientDetail.clientId,
                clientDetail.clientSecret,
                Oauth2PassWordRequest(
                    Oauth2GrantType.PASS_WORD,
                    userName = userId,
                    passWord = userId
                )
            ).data!!
        }.onFailure {
            throwTokenFail(sign.appId, "create get access token failed", "${it.message}")
        }.getOrThrow()
    }

    fun getAppIdOauthClientDetail(desktopIP: String, appId: String): ClientDetailsDTO? {
        val ws = workspaceService.getWorkspaceList4WeSec(
            ip = desktopIP
        ).firstOrNull() ?: throwTokenFail(desktopIP, "unknown ip", "not find $desktopIP")
        return workspaceAppOauth2MaterialsDao.fetchAny(
            dslContext = dslContext,
            appId = appId,
            workspaceName = ws.workspaceName
        )?.let { material ->
            ClientDetailsDTO(
                clientId = material.clientId,
                clientSecret = material.clientSecret,
                clientName = loadClientName(appId, ws.workspaceName),
                scope = "",
                icon = "",
                authorizedGrantTypes = listOf(Oauth2GrantType.PASS_WORD),
                webServerRedirectUri = "",
                accessTokenValidity = TimeUnit.DAYS.toSeconds(180),
                refreshTokenValidity = TimeUnit.DAYS.toSeconds(180),
                createUser = ws.creator
            )
        }
    }

    fun reportData(type: Int, base64Data: String) {
        val dataType = SdkReportDataType.fromValue(type) ?: run {
            logger.warn("report data unknow type $type")
            return
        }
        val data = String(Base64.getDecoder().decode(base64Data), Charsets.UTF_8)
        sendData(dataType, data)
    }

    private fun createMaterial(
        sign: DesktopTokenSign,
        ws: WeSecProjectWorkspace
    ): ClientDetailsDTO {
        val clientId = UUID.randomUUID().toString().take(32)
        val clientSecret = UUID.randomUUID().toString().take(64)
        workspaceAppOauth2MaterialsDao.create(
            dslContext = dslContext,
            appId = sign.appId,
            workspaceName = ws.workspaceName,
            clientId = clientId,
            clientSecret = clientSecret
        )
        val dto = ClientDetailsDTO(
            clientId = clientId,
            clientSecret = clientSecret,
            clientName = loadClientName(sign.appId, ws.workspaceName),
            scope = "",
            icon = "",
            authorizedGrantTypes = listOf(Oauth2GrantType.PASS_WORD),
            webServerRedirectUri = "",
            accessTokenValidity = TimeUnit.DAYS.toSeconds(180),
            refreshTokenValidity = TimeUnit.DAYS.toSeconds(180),
            createUser = ws.creator
        )
        kotlin.runCatching {
            client.get(Oauth2ServiceEndpointResource::class).createClientDetails(dto)
        }.onFailure {
            throwTokenFail(sign.appId, "create oauth client failed", "$dto|${it.message}")
        }
        return dto
    }

    private fun loadClientName(
        appId: String,
        workspaceName: String
    ) = "remotedev-$appId-$workspaceName"

    @ActionAuditRecord(
        actionId = TencentActionId.CGS_TOKEN_GENERATE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CGS
        ),
        attributes = [AuditAttribute(name = TencentActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#ws?.projectId")],
        scopeId = "#ws?.projectId",
        content = TencentActionAuditContent.CGS_TOKEN_GENERATE_CONTENT
    )
    fun check(
        ws: WeSecProjectWorkspace,
        sign: DesktopTokenSign,
        desktopIP: String
    ) {
        // 审计
        ActionAuditContext.current().addInstanceInfo(
            ws.workspaceName,
            desktopIP,
            null,
            sign
        )
        // 校验指纹
        val realFingerprint = DigestUtils.md5Hex("${ws.macAddress}${bkConfig.desktopSdkToken}").uppercase()
        if (realFingerprint != sign.fingerprint) {
            throwTokenFail(desktopIP, "wrong fingerprint", "$realFingerprint != ${sign.fingerprint}")
        }
        // 校验签名
        // <md5(mac_addr+token)>,<appid>,<原始文件名>,<文件版本>,<修改日期>,<产品名称>,<产品版本>,<exe文件的sha1>,<当前10位时间戳>,<public key>
        val unsigned = "${sign.fingerprint}," +
            "${sign.appId}," +
            "${sign.fileName}," +
            "${sign.fileVersion}," +
            "${sign.fileUpdateTime}," +
            "${sign.productName}," +
            "${sign.productVersion}," +
            "${sign.sha1}," +
            "${sign.timestamp}," +
            sign.publicKey
        val realSigned = ShaUtils.hmacSha1(bkConfig.desktopSdkToken.toByteArray(), unsigned.toByteArray()).uppercase()
        if (realSigned != sign.sign) {
            throwTokenFail(desktopIP, "wrong sign", "$realSigned != ${sign.sign}")
        }
    }

    private fun throwTokenFail(desktopIP: String, failMessage: String, failDetailMessage: String): Nothing {
        logger.warn("$desktopIP get token fail:$failMessage.<$failDetailMessage>")
        throw CustomException(Response.Status.FORBIDDEN, failMessage)
    }

    private fun sendData(type: SdkReportDataType, data: String) {
        val topic = when (type) {
            SdkReportDataType.CODECC_QUALITY_REPORT -> codeccQualityReport
            SdkReportDataType.CODECC_CODE_MONITOR -> codeccCodeMonitor
        }
        if (topic.isNullOrBlank()) {
            logger.error("$topic's topic is null")
            return
        }
        kafkaClient.send(topic, data)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RemotedevSdkService::class.java)
    }
}
