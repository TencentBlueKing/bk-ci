package com.tencent.devops.process.engine.service

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.extend.ModelCheckPlugin
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.process.constant.ProcessMessageCode.ILLEGAL_PIPELINE_MODEL_JSON
import com.tencent.devops.process.constant.ProcessMessageCode.USER_NEED_PIPELINE_X_PERMISSION
import com.tencent.devops.process.engine.cfg.PipelineIdGenerator
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.pipeline.PipelineSubscriptionType
import com.tencent.devops.process.pojo.setting.PipelineModelAndSetting
import com.tencent.devops.process.pojo.setting.PipelineRunLockType
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.pojo.setting.Subscription
import com.tencent.devops.process.service.PipelineSettingService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.net.URLEncoder
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput

@Service
class PipelineInfoService @Autowired constructor(
    val pipelineService: PipelineService,
    val pipelineIdGenerator: PipelineIdGenerator,
    val pipelineRepositoryService: PipelineRepositoryService,
    val pipelineSettingService: PipelineSettingService,
    private val modelCheckPlugin: ModelCheckPlugin,
    val pipelinePermissionService: PipelinePermissionService
) {

    fun exportPipeline(userId: String, projectId: String, pipelineId: String): Response {
        pipelinePermissionService.validPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.EDIT,
                message = "用户($userId)无权限在工程($projectId)下导出流水线"
        )
        val settingInfo = getSettingInfo(projectId, pipelineId, userId)
        val model = pipelineRepositoryService.getModel(pipelineId)
        if (settingInfo == null || model == null) {
            throw OperationException(MessageCodeUtil.getCodeLanMessage(ILLEGAL_PIPELINE_MODEL_JSON))
        }
        val modelAndSetting = PipelineModelAndSetting(
                model = model,
                setting = settingInfo!!
        )
        logger.info("exportPipeline |$pipelineId | $projectId| ${JsonUtil.toJson(modelAndSetting)}")
        return exportModelToFile(modelAndSetting, settingInfo.pipelineName)
    }

    fun uploadPipeline(userId: String, projectId: String, pipelineModelAndSetting: PipelineModelAndSetting): String? {
        val permissionCheck = pipelinePermissionService.checkPipelinePermission(
                userId = userId,
                projectId = projectId,
                permission = AuthPermission.CREATE
        )
        if (!permissionCheck) {
            logger.warn("$userId|$projectId uploadPipeline permission check fail")
            throw PermissionForbiddenException(MessageCodeUtil.getCodeMessage(USER_NEED_PIPELINE_X_PERMISSION, arrayOf(AuthPermission.CREATE.value)))
        }
        val model = pipelineModelAndSetting.model
        modelCheckPlugin.clearUpModel(model)

        val newPipelineId = pipelineService.createPipeline(
                userId = userId,
                projectId = projectId,
                model = model,
                channelCode = ChannelCode.BS
        )
        val oldSetting = pipelineModelAndSetting.setting
        val newSetting = PipelineSetting(
                projectId = projectId,
                pipelineId = newPipelineId,
                pipelineName = oldSetting.pipelineName,
                desc = oldSetting.desc,
                successSubscription = oldSetting.successSubscription,
                failSubscription = oldSetting.failSubscription,
                maxPipelineResNum = oldSetting.maxPipelineResNum,
                maxQueueSize = oldSetting.maxQueueSize,
                hasPermission = oldSetting.hasPermission,
                labels = oldSetting.labels,
                runLockType = oldSetting.runLockType,
                waitQueueTimeMinute = oldSetting.waitQueueTimeMinute
        )
        // setting pipeline需替换成新流水线的
        pipelineSettingService.saveSetting(userId, newSetting, false)

        return newPipelineId
    }

    private fun getSettingInfo(projectId: String, pipelineId: String, userId: String): PipelineSetting? {
        val settingInfo = pipelineSettingService.getSetting(pipelineId) ?: return null

        val hasPermission = pipelinePermissionService.isProjectUser(userId = userId, projectId = projectId, group = BkAuthGroup.MANAGER)

        val successType = settingInfo.successType.split(",").filter { i -> i.isNotBlank() }
                .map { type -> PipelineSubscriptionType.valueOf(type) }.toSet()
        val failType = settingInfo.failType.split(",").filter { i -> i.isNotBlank() }
                .map { type -> PipelineSubscriptionType.valueOf(type) }.toSet()

        return PipelineSetting(
                projectId = settingInfo.projectId,
                pipelineId = settingInfo.pipelineId,
                pipelineName = settingInfo.name,
                desc = settingInfo.desc,
                runLockType = PipelineRunLockType.valueOf(settingInfo.runLockType),
                successSubscription = Subscription(
                        types = successType,
                        groups = settingInfo.successGroup.split(",").toSet(),
                        users = settingInfo.successReceiver,
                        wechatGroupFlag = settingInfo.successWechatGroupFlag,
                        wechatGroup = settingInfo.successWechatGroup,
                        wechatGroupMarkdownFlag = settingInfo.successWechatGroupMarkdownFlag,
                        detailFlag = settingInfo.successDetailFlag,
                        content = settingInfo.successContent ?: ""
                ),
                failSubscription = Subscription(
                        types = failType,
                        groups = settingInfo.failGroup.split(",").toSet(),
                        users = settingInfo.failReceiver,
                        wechatGroupFlag = settingInfo.failWechatGroupFlag,
                        wechatGroup = settingInfo.failWechatGroup,
                        wechatGroupMarkdownFlag = settingInfo.failWechatGroupMarkdownFlag,
                        detailFlag = settingInfo.failDetailFlag,
                        content = settingInfo.failContent ?: ""
                ),
                labels = emptyList(),
                waitQueueTimeMinute = DateTimeUtil.secondToMinute(settingInfo.waitQueueTimeSecond),
                maxQueueSize = settingInfo.maxQueueSize,
                hasPermission = hasPermission
        )
    }

    private fun exportModelToFile(modelAndSetting: PipelineModelAndSetting, pipelineName: String): Response {
        // 流式下载
        val fileStream = StreamingOutput { output ->
                val sb = StringBuilder()
                sb.append(JsonUtil.toJson(modelAndSetting))
                output.write(sb.toString().toByteArray())
                output.flush()
        }
        val fileName = URLEncoder.encode("$pipelineName.json", "UTF-8")
        return Response
                .ok(fileStream, MediaType.APPLICATION_OCTET_STREAM_TYPE)
                .header("content-disposition", "attachment; filename = $fileName")
                .header("Cache-Control", "no-cache")
                .build()
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}