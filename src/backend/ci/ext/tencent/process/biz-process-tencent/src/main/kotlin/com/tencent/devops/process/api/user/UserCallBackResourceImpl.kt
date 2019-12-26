package com.tencent.devops.process.api.user

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.BSAuthProjectApi
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.pipeline.event.CallBackEvent
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.engine.service.ProjectPipelineCallBackService
import com.tencent.devops.process.pojo.ProjectPipelineCallBack
import com.tencent.devops.process.pojo.pipeline.enums.CallBackNetWorkRegionType
import com.tencent.devops.project.constant.ProjectMessageCode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import java.net.URLEncoder
import kotlin.text.Regex

@RestResource
class UserCallBackResourceImpl @Autowired constructor(
    val projectPipelineCallBackService: ProjectPipelineCallBackService,
    val bkAuthProjectApi: BSAuthProjectApi,
    private val bsPipelineAuthServiceCode: BSPipelineAuthServiceCode
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
        if (!bkAuthProjectApi.isProjectUser(userId, bsPipelineAuthServiceCode, projectId, BkAuthGroup.MANAGER)) {
            logger.info("create Project callback createUser is not project manager,createUser[$userId] projectId[$projectId]")
            throw OperationException((MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.NOT_MANAGER)))
        }
        // 验证url的合法性
        val regex = Regex("(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]", RegexOption.IGNORE_CASE)
        val regexResult = url.matches(regex)
        if (!regexResult) {
            throw InvalidParamException("URL param is invalid.")
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