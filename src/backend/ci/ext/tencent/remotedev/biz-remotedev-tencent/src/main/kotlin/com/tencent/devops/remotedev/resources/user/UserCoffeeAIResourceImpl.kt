package com.tencent.devops.remotedev.resources.user

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.user.UserCoffeeAIResource
import com.tencent.devops.remotedev.pojo.WorkspaceAiInfo
import com.tencent.devops.remotedev.service.CoffeeAIService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserCoffeeAIResourceImpl @Autowired constructor(
    private val coffeeAIService: CoffeeAIService
) : UserCoffeeAIResource {

    companion object {
        private val logger = LoggerFactory.getLogger(UserCoffeeAIResourceImpl::class.java)
    }

    override fun getWebSocketToken(userId: String): Result<String> {
        return try {
            logger.info("用户请求WebSocket令牌：userId={}", userId)
            val token = coffeeAIService.generateUserToken(userId)
            Result(token)
        } catch (e: Exception) {
            logger.error("生成用户WebSocket令牌失败：userId={}", userId, e)
            Result(status = 1, message = "生成令牌失败: ${e.message}")
        }
    }

    override fun getAiWorkspaceList(userId: String): Result<List<WorkspaceAiInfo>> {
        return try {
            logger.info("用户查询AI云桌面列表：userId={}", userId)
            Result(coffeeAIService.getAiWorkspaceList(userId))
        } catch (e: Exception) {
            logger.error("查询AI云桌面列表失败：userId={}", userId, e)
            Result(status = 1, message = "查询失败: ${e.message}")
        }
    }
}
