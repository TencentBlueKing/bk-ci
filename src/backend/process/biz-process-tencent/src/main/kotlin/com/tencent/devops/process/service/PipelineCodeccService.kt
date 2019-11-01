package com.tencent.devops.process.service

import com.tencent.devops.common.pipeline.pojo.coverity.CoverityResult
import com.tencent.devops.common.pipeline.pojo.coverity.ProjectLanguage
import com.tencent.devops.common.pipeline.pojo.element.atom.LinuxPaasCodeCCScriptElement
import com.tencent.devops.common.pipeline.utils.CoverityUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.constant.WebsocketCode
import com.tencent.devops.common.websocket.dispatch.WebSocketDispatcher
import com.tencent.devops.common.websocket.enum.NotityLevel
import com.tencent.devops.common.websocket.pojo.BuildPageInfo
import com.tencent.devops.common.websocket.pojo.NotifyPost
import com.tencent.devops.common.websocket.pojo.WebSocketType
import com.tencent.devops.process.engine.pojo.PipelineModelTask
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.websocket.EditPageBuild
import com.tencent.devops.process.websocket.HistoryPageBuild
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineCodeccService @Autowired constructor(
    private val pipelineTaskService: PipelineTaskService,
    private val websocketPushDispatcher: WebSocketDispatcher,
    private val redisOperation: RedisOperation
) {
    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    fun updateCodeccTask(userId: String, setting: PipelineSetting) {
        val element = getCodeccElement(setting.projectId, setting.pipelineId) ?: return
        updateTask(element, setting.pipelineName, userId)
    }

    fun updateCodeccTask(userId: String, projectId: String, pipelineId: String, pipelineName: String) {
        val element = getCodeccElement(projectId, pipelineId) ?: return
        logger.info("[$pipelineId]-updateCodeccTask,element:{$element}")
        updateTask(element, pipelineName, userId)
    }

    fun getCodeccElement(projectId: String, pipelineId: String): PipelineModelTask? {
        val elementList = pipelineTaskService.list(projectId, setOf(pipelineId))[pipelineId] ?: listOf()
        return elementList.firstOrNull { it.classType == LinuxPaasCodeCCScriptElement.classType }
    }

    fun createTask(
        projectId: String,
        pipelineId: String,
        userId: String,
        pipelineName: String,
        element: LinuxPaasCodeCCScriptElement?,
        variables: Map<String, Any>?
    ): CoverityResult {
        try {
            // Create a new one
            val task = CoverityUtils.createTask(
                projectId,
                pipelineId,
                pipelineName,
                userId,
                element!!.languages,
                element.compilePlat ?: "LINUX",
                element.tools ?: listOf("COVERITY"),
                element.pyVersion ?: "",
                element.eslintRc ?: "",
                element.scanType ?: "1",
                element.phpcsStandard ?: "",
                element.goPath ?: "",
                element.ccnThreshold,
                null,
                genToolSet(variables ?: mutableMapOf())
            )
            return task
        } catch (e: Exception) {
            val post = NotifyPost(
                module = "process",
                message = e.message!!,
                level = NotityLevel.HIGH_LEVEL.getLevel(),
                dealUrl = EditPageBuild().buildPage(
                    BuildPageInfo(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = null,
                        atomId = null
                    )
                ),
                code = WebsocketCode.CODECC_ADD_ERROR,
                webSocketType = WebSocketType.changWebType(WebSocketType.CODECC),
                page = ""
            )
//
//            websocketPushDispatcher.dispatch(
//                pipelineErrorNotifyEvent(pipelineId, userId, projectId, post, WebSocketType.CODECC)
//            )
            logger.warn("[$pipelineId]调用codecc add返回异常。webSocket推送异常信息[$post]")
            throw e
        }
    }

    private fun updateTask(element: PipelineModelTask, pipelineName: String, userId: String) {
        try {
            logger.info("[${element.pipelineId}],update task start]")
            val taskParams = element.taskParams
            val taskId = taskParams["codeCCTaskId"] ?: return
            val language = solveJsonArr(taskParams["languages"]?.toString()).map { ProjectLanguage.valueOf(it) }
            val tools = solveJsonArr(taskParams["tools"]?.toString())
            val needCodeContent = taskParams["needCodeContent"]?.toString()
            CoverityUtils.updateTask(
                pipelineName,
                userId,
                taskId.toString(),
                language,
                taskParams["compilePlat"]?.toString() ?: "LINUX",
                if (tools.isNotEmpty()) tools else listOf("COVERITY"),
                taskParams["pyVersion"]?.toString() ?: "",
                taskParams["eslintRc"]?.toString() ?: "",
                taskParams["scanType"]?.toString() ?: "1",
                taskParams["phpcsStandard"]?.toString() ?: "",
                taskParams["goPath"]?.toString() ?: "",
                taskParams["ccnThreshold"] as? Int,
                needCodeContent,
                genToolSet(taskParams)
            )
            logger.info("[${element.pipelineId}],update task end]")
        } catch (e: Exception) {
            logger.error("update codecc task fail: ${e.message}", e)
//            val post = NotifyPost(
//                module = "process",
//                message = e.message!!,
//                level = NotityLevel.HIGH_LEVEL.getLevel(),
//                dealUrl = EditPageBuild().buildPage(
//                    BuildPageInfo(
//                        projectId = element.projectId,
//                        pipelineId = element.pipelineId,
//                        buildId = null,
//                        atomId = null
//                    )
//                ),
//                code = WebsocketCode.CODECC_UPDATE_ERROR,
//                webSocketType = WebSocketType.changWebType(WebSocketType.CODECC)
//            )
//            websocketPushDispatcher.dispatch(
//                pipelineErrorNotifyEvent(element.pipelineId, userId, element.projectId, post, WebSocketType.CODECC)
//            )
//            logger.warn("[${element.pipelineId}]调用codecc update返回异常。webSocket推送异常信息[$post]")
        }
    }

//    private fun pipelineErrorNotifyEvent(
//        pipelineId: String,
//        userId: String,
//        projectId: String,
//        notifypost: NotifyPost,
//        websocketType: WebSocketType,
//        buildId: String? = null
//    ): IWebsocketPush {
//        return NotifyWebsocketPush(
//            buildId = buildId,
//            projectId = projectId,
//            pipelineId = pipelineId,
//            userId = userId,
//            pushType = websocketType,
//            pathClass = HistoryPageBuild(),
//            page = notifypost.dealUrl,
//            redisOperation = redisOperation,
//            notifyPost = notifypost
//        )
//    }

    private fun genToolSet(taskParams: Map<String, Any>): Map<String, String> {
        val map = mutableMapOf<String, String>()

        val coverityToolSetId = taskParams["coverityToolSetId"] as? String
        val klocworkToolSetId = taskParams["klocworkToolSetId"] as? String
        val cpplintToolSetId = taskParams["cpplintToolSetId"] as? String
        val eslintToolSetId = taskParams["eslintToolSetId"] as? String
        val pylintToolSetId = taskParams["pylintToolSetId"] as? String
        val gometalinterToolSetId = taskParams["gometalinterToolSetId"] as? String
        val checkStyleToolSetId = taskParams["checkStyleToolSetId"] as? String
        val styleCopToolSetId = taskParams["styleCopToolSetId"] as? String
        val detektToolSetId = taskParams["detektToolSetId"] as? String
        val phpcsToolSetId = taskParams["phpcsToolSetId"] as? String
        val sensitiveToolSetId = taskParams["sensitiveToolSetId"] as? String
        val occheckToolSetId = taskParams["occheckToolSetId"] as? String

        if (!coverityToolSetId.isNullOrBlank()) map["coverityToolSetId"] = coverityToolSetId!!
        if (!klocworkToolSetId.isNullOrBlank()) map["klocworkToolSetId"] = klocworkToolSetId!!
        if (!cpplintToolSetId.isNullOrBlank()) map["cpplintToolSetId"] = cpplintToolSetId!!
        if (!eslintToolSetId.isNullOrBlank()) map["eslintToolSetId"] = eslintToolSetId!!
        if (!pylintToolSetId.isNullOrBlank()) map["pylintToolSetId"] = pylintToolSetId!!
        if (!gometalinterToolSetId.isNullOrBlank()) map["gometalinterToolSetId"] = gometalinterToolSetId!!
        if (!checkStyleToolSetId.isNullOrBlank()) map["checkStyleToolSetId"] = checkStyleToolSetId!!
        if (!styleCopToolSetId.isNullOrBlank()) map["styleCopToolSetId"] = styleCopToolSetId!!
        if (!detektToolSetId.isNullOrBlank()) map["detektToolSetId"] = detektToolSetId!!
        if (!phpcsToolSetId.isNullOrBlank()) map["phpcsToolSetId"] = phpcsToolSetId!!
        if (!sensitiveToolSetId.isNullOrBlank()) map["sensitiveToolSetId"] = sensitiveToolSetId!!
        if (!occheckToolSetId.isNullOrBlank()) map["occheckToolSetId"] = occheckToolSetId!!

        return map
    }

    private fun solveJsonArr(toolString: String?): List<String> {
        if (toolString.isNullOrBlank()) return listOf()
        return toolString!!.trim().removePrefix("[").removeSuffix("]").split(",").map { it.trim() }
    }
}
