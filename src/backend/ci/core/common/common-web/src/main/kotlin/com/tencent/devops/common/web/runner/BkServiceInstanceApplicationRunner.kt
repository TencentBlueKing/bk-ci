package com.tencent.devops.common.web.runner

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.common.service.utils.BkServiceUtil
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.common.service.utils.KubernetesUtils
import com.tencent.devops.common.service.utils.SpringContextUtil
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClient
import org.springframework.context.annotation.DependsOn
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
@DependsOn("rabbitAdmin")
class BkServiceInstanceApplicationRunner constructor(
    private val compositeDiscoveryClient: CompositeDiscoveryClient,
    private val bkTag: BkTag,
    private val redisOperation: RedisOperation,
    private val rabbitAdmin: RabbitAdmin
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
                val cacheKey = BkServiceUtil.getServiceHostKey(serviceName)
                logger.info("initServiceHostInfo serviceName:$serviceName cacheKey:$cacheKey")
                val discoveryTag = bkTag.getFinalTag()
                val namespace = discoveryTag.replace("kubernetes-", "")
                val svrName = KubernetesUtils.getSvrName(serviceName, namespace)
                // 睡眠一会儿以便从注册最新拿到微服务最新的IP列表
                sleep(THREAD_SLEEP_TIMEOUT)
                val serviceHosts = compositeDiscoveryClient.getInstances(svrName).map { it.host }.toTypedArray()
                logger.info(
                    "initServiceHostInfo serviceName:[$serviceName],IP:[${CommonUtils.getInnerIP()}],serviceHosts:${
                        JsonUtil.toJson(
                            serviceHosts
                        )
                    }"
                )
                val environment: Environment = SpringContextUtil.getBean(Environment::class.java)
                val shardingDbFlag = environment.containsProperty("spring.datasource.dataSourceConfigs[0].url")
                logger.info("initServiceHostInfo serviceName:$serviceName shardingDbFlag:$shardingDbFlag")
                if (shardingDbFlag) {
                    // 如果微服务的DB采用了分库分表方案，那么需要清理规则动态队列
                    try {
                        deleteDynamicMqQueue(cacheKey, serviceName, serviceHosts)
                    } catch (ignored: Throwable) {
                        logger.warn(
                            "serviceName:$serviceName delete " +
                                "dynamicMqQueue(${BkServiceUtil.getDynamicMqQueue()}) fail!", ignored
                        )
                    }
                }
                // 清空redis中微服务的主机IP列表
                redisOperation.delete(cacheKey)
                // 把微服务的最新主机IP列表写入redis中
                redisOperation.sadd(cacheKey, *serviceHosts)
            }

            private fun deleteDynamicMqQueue(cacheKey: String, serviceName: String, serviceHosts: Array<String>) {
                // 将微服务在redis的主机IP列表取出
                val historyServiceHosts = redisOperation.getSetMembers(cacheKey)?.toMutableSet()
                logger.info("initServiceHostInfo serviceName:$serviceName historyServiceHosts:$historyServiceHosts")
                if (historyServiceHosts.isNullOrEmpty()) {
                    return
                }
                historyServiceHosts.removeAll(serviceHosts.toSet())
                historyServiceHosts.forEach { historyServiceHost ->
                    val queueName = BkServiceUtil.getDynamicMqQueue()
                    logger.info("serviceName:$serviceName delete dynamicMqQueue($queueName) start!")
                    var queueProperties = rabbitAdmin.getQueueProperties(queueName)
                    if (queueProperties == null) {
                        // 队列属性为空说明删除成功
                        logger.info("serviceName:$serviceName dynamicMqQueue($queueName) does not exist!")
                        return
                    }
                    rabbitAdmin.purgeQueue(queueName)
                    rabbitAdmin.deleteQueue(queueName)
                    queueProperties = rabbitAdmin.getQueueProperties(queueName)
                    if (queueProperties != null) {
                        // 队列属性不为空说明删除未成功，把队列名称写入redis中
                        logger.info("serviceName:$serviceName delete dynamicMqQueue($queueName) fail!")
                    } else {
                        logger.info("serviceName:$serviceName delete dynamicMqQueue($queueName) success!")
                    }
                }
            }
        }.start()
    }
}
