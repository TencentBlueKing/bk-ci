package com.tencent.devops.common.web.runner

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.common.service.utils.KubernetesUtils
import com.tencent.devops.common.service.utils.BkServiceUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClient
import org.springframework.stereotype.Component

@Component
class BkServiceInstanceApplicationRunner constructor(
    private val compositeDiscoveryClient: CompositeDiscoveryClient,
    private val bkTag: BkTag,
    private val redisOperation: RedisOperation
) : ApplicationRunner {

    companion object {
        private val logger = LoggerFactory.getLogger(BkServiceInstanceApplicationRunner::class.java)
        private const val THREAD_SLEEP_TIMEOUT = 32000L
    }

    @Suppress("SpreadOperator")
    override fun run(args: ApplicationArguments) {
        object : Thread() {
            override fun run() {
                val serviceName = BkServiceUtil.findServiceName()
                logger.info("initServiceHostInfo serviceName:$serviceName begin")
                val discoveryTag = bkTag.getFinalTag()
                val namespace = discoveryTag.replace("kubernetes-", "")
                val svrName = KubernetesUtils.getSvrName(serviceName, namespace)
                // 睡眠一会儿以便从注册最新拿到微服务最新的IP列表
                sleep(THREAD_SLEEP_TIMEOUT)
                val serviceHosts = compositeDiscoveryClient.getInstances(svrName).map { it.host }.toTypedArray()
                logger.info("initServiceHostInfo serviceName[$serviceName] serviceHosts:$serviceHosts")
                // 把微服务的主机IP列表写入redis中
                redisOperation.sadd(BkServiceUtil.getServiceHostKey(serviceName), *serviceHosts)
            }
        }.start()
    }
}
