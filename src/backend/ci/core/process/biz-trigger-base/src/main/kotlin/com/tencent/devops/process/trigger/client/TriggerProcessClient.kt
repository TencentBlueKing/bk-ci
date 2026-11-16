package com.tencent.devops.process.trigger.client

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.consul.ConsulConstants
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.BkTag
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

/**
 * 触发链路调用 process 接口
 *
 * webhook、定时等触发事件只投递到一个集群，项目可能在其他集群，需按项目 tag 路由到项目所在集群的 process。
 * 同进程部署时 client.get 命中本地 bean，tag 不生效，所以项目不在本集群时走网关，由网关按 X-GATEWAY-TAG 转发。
 */
@Component
class TriggerProcessClient(
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val bkTag: BkTag
) {

    fun <T : Any, R> call(projectId: String, clz: KClass<T>, action: (T) -> R): R {
        val projectTag = redisOperation.hget(ConsulConstants.PROJECT_TAG_REDIS_KEY, projectId)
        val target = if (projectTag.isNullOrBlank() || isLocalCluster(projectTag)) {
            client.get(clz)
        } else {
            client.getGateway(clz)
        }
        return bkTag.invokeByTag(projectTag) { action(target) }
    }

    private fun isLocalCluster(projectTag: String): Boolean {
        // 容器化集群的本地 tag 带 kubernetes- 前缀，项目 routerTag 不带
        return bkTag.getLocalTag().removePrefix(KUBERNETES_TAG_PREFIX) == projectTag
    }

    companion object {
        private const val KUBERNETES_TAG_PREFIX = "kubernetes-"
    }
}
