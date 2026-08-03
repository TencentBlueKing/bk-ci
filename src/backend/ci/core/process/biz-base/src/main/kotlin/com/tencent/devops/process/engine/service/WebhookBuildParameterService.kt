package com.tencent.devops.process.engine.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_ADD_FILE_PREFIX
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_PREFIX
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_DELETE_FILE_PREFIX
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_MODIFY_FILE_PREFIX
import com.tencent.devops.process.engine.dao.WebhookBuildParameterDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WebhookBuildParameterService @Autowired constructor(
    private val dslContext: DSLContext,
    private val webhookBuildParameterDao: WebhookBuildParameterDao
) {

    fun save(
        projectId: String,
        pipelineId: String,
        buildId: String,
        buildParameters: List<BuildParameters>
    ) {
        var parameters = buildParameters
        var json = JsonUtil.toJson(parameters, false)
        if (json.toByteArray(Charsets.UTF_8).size > WEBHOOK_BUILD_PARAMETER_LENGTH_MAX) {
            // 过滤按提交/文件累加的超大批量变量，保留其余有效参数，避免整体丢弃导致重试时变量缺失
            val filtered = parameters.filter { param ->
                OVERSIZED_PARAMETER_PREFIXES.none { param.key.startsWith(it) }
            }
            val droppedKeys = parameters.map { it.key } - filtered.map { it.key }.toSet()
            logger.warn(
                "webhook build parameter too long, length: ${json.length}, " +
                    "buildId: $buildId|drop oversized params: $droppedKeys"
            )
            parameters = filtered
            json = JsonUtil.toJson(parameters, false)
        }
        // 极端情况下过滤后仍超长，跳过保存避免 DB 写入失败
        if (json.toByteArray(Charsets.UTF_8).size > WEBHOOK_BUILD_PARAMETER_LENGTH_MAX) {
            logger.warn(
                "webhook build parameter still too long after filtering, " +
                    "length: ${json.length}, buildId: $buildId|skip save"
            )
            return
        }
        webhookBuildParameterDao.save(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            buildParameters = json
        )
    }

    fun getBuildParameters(projectId: String, buildId: String): List<BuildParameters>? {
        val record = webhookBuildParameterDao.get(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId
        )
        return record?.buildParameters?.let { JsonUtil.to(it, object : TypeReference<List<BuildParameters>>() {}) }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WebhookBuildParameterService::class.java)
        const val WEBHOOK_BUILD_PARAMETER_LENGTH_MAX = 65535

        // 按提交/文件数量累加的批量变量前缀，超长时优先过滤
        private val OVERSIZED_PARAMETER_PREFIXES = listOf(
            BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_PREFIX, // 含 COMMIT_MSG_/TIMESTAMP_/AUTHOR_ 子前缀
            BK_REPO_GIT_WEBHOOK_PUSH_ADD_FILE_PREFIX,
            BK_REPO_GIT_WEBHOOK_PUSH_MODIFY_FILE_PREFIX,
            BK_REPO_GIT_WEBHOOK_PUSH_DELETE_FILE_PREFIX
        )
    }
}
