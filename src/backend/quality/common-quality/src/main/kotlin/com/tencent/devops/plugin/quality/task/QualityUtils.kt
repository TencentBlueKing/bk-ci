package com.tencent.devops.plugin.quality.task

import com.tencent.devops.common.client.Client
import com.tencent.devops.quality.api.v2.ServiceQualityRuleResource
import com.tencent.devops.quality.api.v2.pojo.request.BuildCheckParams
import com.tencent.devops.quality.pojo.RuleCheckResult
import org.slf4j.LoggerFactory

object QualityUtils {

    private val logger = LoggerFactory.getLogger(QualityUtils::class.java)

    fun getAuditUserList(client: Client, projectId: String, pipelineId: String, buildId: String, taskId: String): Set<String> {
        return try {
            client.get(ServiceQualityRuleResource::class).getAuditUserList(
                    projectId,
                    pipelineId,
                    buildId,
                    taskId
            ).data ?: setOf()
        } catch (e: Exception) {
            logger.error("quality get audit user list fail: ${e.message}", e)
            return setOf()
        }
    }

    fun check(client: Client, buildCheckParams: BuildCheckParams): RuleCheckResult {
        return try {
            client.get(ServiceQualityRuleResource::class).check(
                    buildCheckParams
            ).data!!
        } catch (e: Exception) {
            logger.error("quality get audit user list fail: ${e.message}", e)
            return RuleCheckResult(
                    true,
                    true,
                    15 * 6000,
                    listOf()
            )
        }
    }
}