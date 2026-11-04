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
 * 同进程部署时 client.get 命中本地 bean，tag 不生效，只有 trigger 独立部署后才能跨集群路由。
 */
@Component
class TriggerProcessClient(
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val bkTag: BkTag
) {

    fun <T : Any, R> call(projectId: String, clz: KClass<T>, action: (T) -> R): R {
        val projectTag = redisOperation.hget(ConsulConstants.PROJECT_TAG_REDIS_KEY, projectId)
        return bkTag.invokeByTag(projectTag) { action(client.get(clz)) }
    }
}
