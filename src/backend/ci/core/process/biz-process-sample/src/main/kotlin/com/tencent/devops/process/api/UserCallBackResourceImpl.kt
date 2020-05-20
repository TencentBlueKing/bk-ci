package com.tencent.devops.process.api

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.pipeline.event.CallBackEvent
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.user.UserCallBackResource
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.service.ProjectPipelineCallBackService
import com.tencent.devops.process.pojo.ProjectPipelineCallBack
import com.tencent.devops.process.pojo.pipeline.enums.CallBackNetWorkRegionType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import java.net.URLEncoder
import kotlin.text.Regex

@RestResource
class UserCallBackResourceImpl @Autowired constructor(
    val projectPipelineCallBackService: ProjectPipelineCallBackService,
    val authProjectApi: AuthProjectApi,
    private val pipelineAuthServiceCode: PipelineAuthServiceCode
) : UserCallBackResource {
    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }

    @Value("\${devopsGateway.idcProxy:}")
    private var gatewayIDCProxy: String = ""

    override fun create(
        userId: String,
        projectId: String,
        url: String,
        region: CallBackNetWorkRegionType,
        event: CallBackEvent,
        secretToken: String?
    ): Result<Boolean> {
        // 验证用户是否为管理员
        if (!authProjectApi.isProjectUser(userId, pipelineAuthServiceCode, projectId, BkAuthGroup.MANAGER)) {
            logger.info("create Project callback createUser is not project manager,createUser[$userId] projectId[$projectId]")
            throw ErrorCodeException(errorCode = ProcessMessageCode.USER_NEED_PROJECT_X_PERMISSION, params = arrayOf(userId, projectId))
        }
        // 验证url的合法性
        val regex = Regex("(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]", RegexOption.IGNORE_CASE)
        val regexResult = url.matches(regex)
        if (!regexResult) {
            throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_CALLBACK_URL_INVALID)
        }
        val encodeUrl = URLEncoder.encode(url, "UTF-8")
        val callBackUrl = when (region) {
            CallBackNetWorkRegionType.IDC -> url
            CallBackNetWorkRegionType.OSS -> {
                if (gatewayIDCProxy.isNotBlank()) {
                    "$gatewayIDCProxy/proxy-oss?url=$encodeUrl"
                } else {
                    url
                }
            }
            CallBackNetWorkRegionType.DEVNET -> {
                if (gatewayIDCProxy.isNotBlank()) {
                    "$gatewayIDCProxy/proxy-devnet?url=$encodeUrl"
                } else {
                    url
                }
            }
        }
        val projectPipelineCallBack = ProjectPipelineCallBack(
            projectId = projectId,
            callBackUrl = callBackUrl,
            events = event.name,
            secretToken = secretToken
        )
        projectPipelineCallBackService.createCallBack(userId, projectPipelineCallBack)
        return Result(true)
    }
}