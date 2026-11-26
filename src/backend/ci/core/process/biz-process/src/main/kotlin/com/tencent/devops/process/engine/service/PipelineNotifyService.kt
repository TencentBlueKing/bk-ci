package com.tencent.devops.process.engine.service

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.pipeline.enums.BuildEndType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.BuildEndInfo
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.BK_CANCEL_ACTION_MANUAL
import com.tencent.devops.process.constant.ProcessMessageCode.BK_CANCEL_ACTION_TIMEOUT
import com.tencent.devops.process.constant.ProcessMessageCode.BK_CANCEL_REASON_MANUAL
import com.tencent.devops.process.constant.ProcessMessageCode.BK_CANCEL_REASON_PARENT_PIPELINE
import com.tencent.devops.process.constant.ProcessMessageCode.BK_CANCEL_REASON_TIMEOUT
import com.tencent.devops.process.engine.service.record.PipelineBuildRecordService
import com.tencent.devops.process.notify.command.BuildNotifyContext
import com.tencent.devops.process.notify.command.NotifyCmd
import com.tencent.devops.process.notify.command.NotifyCmdChain
import com.tencent.devops.process.notify.command.impl.NotifyPipelineCmd
import com.tencent.devops.process.notify.command.impl.NotifySendCmd
import com.tencent.devops.process.notify.command.impl.NotifyUrlBuildCmd
import com.tencent.devops.process.pojo.pipeline.ModelRecord
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.PipelineContextService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineNotifyService @Autowired constructor(
    val buildVariableService: BuildVariableService,
    val pipelineRuntimeService: PipelineRuntimeService,
    val pipelineContextService: PipelineContextService,
    val pipelineRepositoryService: PipelineRepositoryService,
    val pipelineBuildRecordService: PipelineBuildRecordService
) {

    private val commandCache: LoadingCache<Class<out NotifyCmd>, NotifyCmd> = CacheBuilder.newBuilder()
        .maximumSize(CACHE_SIZE).build(
            object : CacheLoader<Class<out NotifyCmd>, NotifyCmd>() {
                override fun load(clazz: Class<out NotifyCmd>): NotifyCmd {
                    return SpringContextUtil.getBean(clazz)
                }
            }
        )

    fun onPipelineShutdown(
        pipelineId: String,
        buildId: String,
        projectId: String,
        buildStatus: BuildStatus
    ) {
        logger.info("onPipelineShutdown new $pipelineId|$buildId|$buildStatus")

        val vars = pipelineContextService.getAllBuildContext(
            buildVariableService.getAllVariable(projectId, pipelineId, buildId)
        ).toMutableMap()

        // #8161 调试中生效调试版本的通知配置
        // #12697 分支版本执行时生效该分支版本的通知配置,统一按本次构建实际运行的版本读取
        val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, pipelineId, buildId) ?: return
        val setting = pipelineRepositoryService.getSettingByPipelineVersion(
            projectId, pipelineId, buildInfo.version
        ) ?: return

        // #11699 取消通知注入取消人/取消原因，供订阅 content 中 ${{ci.operator}}/${{ci.fail_reason}} 解析
        // 与通知分支选择共用 NotifySendCmd.isCancelNotify 判据
        if (NotifySendCmd.isCancelNotify(setting, buildStatus)) {
            injectCancelNotifyVariables(vars, projectId, pipelineId, buildId, buildStatus)
        }

        val context = BuildNotifyContext(
            buildId = buildId,
            projectId = projectId,
            pipelineId = pipelineId,
            variables = vars,
            notifyValue = mutableMapOf(),
            buildStatus = buildStatus,
            cmdFlowSeq = 0,
            pipelineSetting = setting,
            watcher = Watcher("buildNotify")
        )

        val commandList = mutableListOf(
            commandCache.get(NotifyUrlBuildCmd::class.java), // 构建发送url相关信息
            commandCache.get(NotifyPipelineCmd::class.java), // 构建流水线相关相关信息
            commandCache.get(NotifySendCmd::class.java) // 发送消息
        )

        NotifyCmdChain(commandList).doCommand(context)
    }

    /**
     * 取消通知场景向构建变量注入取消人/取消原因。
     * 取值以构建终态详情（BuildEndInfo）为准：用户取消取实际操作人，系统超时取消取 system，
     * 父流水线级联取父流水线的取消人。非超时场景终态详情缺失时回退记录取消人与插件错误信息。
     *
     * @param vars 出参：构建变量表，写入 ci.operator（取消人）与 ci.fail_reason（取消原因）
     * @param buildStatus 构建终态，记录不可用时用于设置超时取消的默认值
     */
    private fun injectCancelNotifyVariables(
        vars: MutableMap<String, String>,
        projectId: String,
        pipelineId: String,
        buildId: String,
        buildStatus: BuildStatus
    ) {
        val isTimeout = buildStatus.isTimeout()
        // 超时取消使用 system 和超时原因，其他取消在记录不可用时保持空值
        vars["ci.operator"] = if (isTimeout) SYSTEM_OPERATOR else ""
        vars["ci.fail_reason"] = ""
        try {
            val language = I18nUtil.getDefaultLocaleLanguage()
            if (isTimeout) {
                vars["ci.fail_reason"] = MessageUtil.getMessageByLocale(BK_CANCEL_REASON_TIMEOUT, language)
            }
            val record = pipelineBuildRecordService.getBuildRecord(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                executeCount = null
            ) ?: return
            val endInfo = record.buildEndInfo
            if (endInfo == null) {
                if (!isTimeout) {
                    // 系统取消时 cancelUser 列是占位值，按需求统一展示为 system
                    vars["ci.operator"] = BuildEndInfo.confirmedOperator(record.cancelUserId) ?: SYSTEM_OPERATOR
                    // 终态详情缺失时取插件错误信息，取不到再展示手动取消
                    vars["ci.fail_reason"] = record.errorReason().takeIf { it.isNotBlank() }
                        ?: MessageUtil.getMessageByLocale(BK_CANCEL_REASON_MANUAL, language)
                }
                return
            }
            when (endInfo.endType) {
                BuildEndType.CANCEL_USER -> {
                    vars["ci.operator"] = endInfo.operator ?: BuildEndInfo.confirmedOperator(record.cancelUserId)
                        ?: SYSTEM_OPERATOR
                    vars["ci.fail_reason"] = MessageUtil.getMessageByLocale(BK_CANCEL_REASON_MANUAL, language)
                }
                BuildEndType.CANCEL_PARENT_PIPELINE -> {
                    // 父流水线的取消人随终态详情落库，父取消动作由其取消原因码还原
                    val parent = endInfo.parentPipelineInfo
                    val parentOperator = parent?.operator ?: SYSTEM_OPERATOR
                    vars["ci.operator"] = parentOperator
                    vars["ci.fail_reason"] = MessageUtil.getMessageByLocale(
                        messageCode = BK_CANCEL_REASON_PARENT_PIPELINE,
                        language = language,
                        params = arrayOf(parentOperator, parentActionOf(language, parent?.cancelReasonCode))
                    )
                }
                else -> {
                    // 超时取消（排队超时、Job/心跳超时）统一展示超时原因；其余系统取消沿用终态详情原因
                    vars["ci.operator"] = SYSTEM_OPERATOR
                    vars["ci.fail_reason"] = if (isTimeout || isTimeoutEnd(endInfo)) {
                        MessageUtil.getMessageByLocale(BK_CANCEL_REASON_TIMEOUT, language)
                    } else {
                        endInfo.reason?.takeIf { it.isNotBlank() } ?: record.errorReason()
                    }
                }
            }
        } catch (ignored: Throwable) {
            logger.warn("NOTIFY|$buildId|inject cancel notify variables failed", ignored)
        }
    }

    private fun isTimeoutEnd(endInfo: BuildEndInfo): Boolean {
        if (endInfo.endType == BuildEndType.TIMEOUT_QUEUE) return true
        val reasonCode = endInfo.reasonCode ?: return false
        return reasonCode in TIMEOUT_REASON_CODES
    }

    /**
     * 父流水线取消动作文案：超时类原因码取「超时」，其余取「手动/API」。
     */
    private fun parentActionOf(language: String, parentCancelReasonCode: String?): String {
        val isTimeout = parentCancelReasonCode != null && parentCancelReasonCode in TIMEOUT_REASON_CODES
        return MessageUtil.getMessageByLocale(
            messageCode = if (isTimeout) BK_CANCEL_ACTION_TIMEOUT else BK_CANCEL_ACTION_MANUAL,
            language = language
        )
    }

    private fun ModelRecord.errorReason(): String {
        return errorInfoList?.joinToString(",") { it.errorMsg } ?: ""
    }

    companion object {
        val logger = LoggerFactory.getLogger(PipelineNotifyService::class.java)
        private const val CACHE_SIZE = 500L
        private const val SYSTEM_OPERATOR = "system"
        private val TIMEOUT_REASON_CODES = setOf(
            ProcessMessageCode.BK_BUILD_CANCEL_SYSTEM_HEARTBEAT_TIMEOUT,
            ProcessMessageCode.BK_BUILD_CANCEL_SYSTEM_JOB_EXEC_TIMEOUT,
            ProcessMessageCode.BK_BUILD_CANCEL_SYSTEM_JOB_QUEUE_TIMEOUT
        )
    }
}
