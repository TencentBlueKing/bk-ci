package com.tencent.devops.metrics.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.annotation.BkApiPermission
import com.tencent.devops.common.web.constant.BkApiHandleType
import com.tencent.devops.metrics.api.UserMetricsResource
import com.tencent.devops.metrics.service.MetricsQueryService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserMetricsResourceImpl @Autowired constructor(
    private val metricsQueryService: MetricsQueryService
) : UserMetricsResource {
    
    companion object {
        private val logger = LoggerFactory.getLogger(UserMetricsResourceImpl::class.java)
    }
    
    @BkApiPermission([BkApiHandleType.PROJECT_MEMBER_CHECK])
    override fun queryMetrics(
        projectId: String,
        userId: String,
        request: Map<String, Any>
    ): Result<Map<String, Any>> {
        logger.info("Query metrics for project: $projectId, user: $userId, request: $request")
        
        return try {
            val result = metricsQueryService.queryMetrics(projectId, request)
            Result(result)
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid request parameters for project: $projectId", e)
            Result(
                status = 1,
                message = e.message ?: "请求参数不合法",
                data = null
            )
        } catch (e: Exception) {
            logger.error("Query metrics failed for project: $projectId", e)
            Result(
                status = 1,
                message = e.message ?: "查询指标数据失败",
                data = null
            )
        }
    }
}
