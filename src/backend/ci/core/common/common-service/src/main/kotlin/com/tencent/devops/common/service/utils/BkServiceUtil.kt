package com.tencent.devops.common.service.utils

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.annotation.ServiceInterface
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.NUM_ONE
import com.tencent.devops.common.api.constant.NUM_SEVEN
import com.tencent.devops.common.api.enums.CrudEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.service.Profile
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.core.env.Environment
import java.text.MessageFormat
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

/**
 * 微服务工具类
 *
 * @since: 2023-09-12
 * @version: $Revision$ $Date$ $LastChangedBy$
 *
 */
object BkServiceUtil {

    private val interfaces = ConcurrentHashMap<KClass<*>, String>()

    private val environmentCache = Caffeine.newBuilder()
        .maximumSize(NUM_ONE.toLong())
        .expireAfterWrite(NUM_SEVEN.toLong(), TimeUnit.DAYS)
        .build<String, Environment>()

    /**
     * 获取微服务主机IP集合在缓存中的key
     * @param serviceName 微服务名称
     * @return 微服务主机IP集合在缓存中的key
     */
    fun getServiceHostKey(serviceName: String? = null): String {
        val profileName = getProfileName()
        val finalServiceName = if (!serviceName.isNullOrBlank()) {
            serviceName
        } else {
            findServiceName()
        }
        return "ENV:$profileName:SERVICE:$finalServiceName:HOSTS"
    }

    /**
     * 获取微服务主机完成处理路由规则在缓存中的key
     * @param serviceName 微服务名称
     * @param routingName 规则名称
     * @param actionType 操作类型
     * @return 微服务主机完成处理路由规则在缓存中的key
     */
    fun getServiceRoutingRuleActionFinishKey(
        serviceName: String,
        routingName: String,
        actionType: CrudEnum
    ): String {
        val profileName = getProfileName()
        val actionName = actionType.name
        return "ENV:$profileName:SERVICE:$serviceName:SHARDING_ROUTING_RULE_UPDATE_FINISH:$routingName:$actionName"
    }

    /**
     * 获取微服务的profile名称
     * @return 微服务的profile名称
     */
    private fun getProfileName(): String {
        val profile = SpringContextUtil.getBean(Profile::class.java)
        return profile.getActiveProfiles().joinToString().trim()
    }

    /**
     * 获取微服务名称
     * @param clz class对象
     * @param serviceName 微服务名称
     * @return 微服务名称
     */
    fun findServiceName(clz: KClass<*>? = null, serviceName: String? = null): String {
        val environment = getEnvironment()
        val assemblyServiceName: String? = environment.getProperty("spring.cloud.consul.discovery.service-name")
        // 单体结构，不分微服务的方式
        if (!assemblyServiceName.isNullOrBlank()) {
            return assemblyServiceName
        }
        // 获取微服务名称后缀
        val serviceSuffix: String? = environment.getProperty("service-suffix")
        val tmpServiceName = if (!serviceName.isNullOrBlank()) {
            // 如果方法传了微服务名称则以方法传的为准
            serviceName
        } else if (clz != null) {
            // 如果方法传了class对象则通过解析class对象获取微服务名称
            interfaces.getOrPut(clz) {
                parseServiceNameFromClz(clz)
            }
        } else {
            // 如果方法没有传class对象和微服务名称，则微服务名称通过yaml配置的应用名称获取
            var applicationName = getApplicationName()
            if (!serviceSuffix.isNullOrBlank()) {
                applicationName = applicationName?.removeSuffix(serviceSuffix)
            }
            // 如果yaml没有配置应用名称，则通过环境变量获取微服务名称
            applicationName ?: KubernetesUtils.getMsName()
        }
        return if (serviceSuffix.isNullOrBlank() || KubernetesUtils.inContainer()) {
            tmpServiceName
        } else {
            "$tmpServiceName$serviceSuffix"
        }
    }

    private fun getEnvironment(): Environment {
        // 从缓存中获取environment对象
        val environmentCacheKey = Environment::class.java.canonicalName
        var environment: Environment? = environmentCache.getIfPresent(environmentCacheKey)
        if (environment == null) {
            // 缓存中未获取到environment对象，则去spring上下文中获取
            environment = SpringContextUtil.getBean(Environment::class.java)
            // 把environment对象放入本地缓存中
            environmentCache.put(environmentCacheKey, environment)
        }
        return environment
    }

    fun getApplicationName() = getEnvironment().getProperty("spring.application.name")

    /**
     * 从class对象解析出微服务名称
     * @param clz class对象
     * @return 微服务名称
     */
    private fun parseServiceNameFromClz(clz: KClass<*>): String {
        // 获取ServiceInterface注解
        val serviceInterface = AnnotationUtils.findAnnotation(clz.java, ServiceInterface::class.java)
        return if (serviceInterface != null && serviceInterface.value.isNotBlank()) {
            // 如果ServiceInterface注解存在，则微服务名称以注解的值为准
            serviceInterface.value
        } else {
            // 如果ServiceInterface注解不存在，则微服务名称从包路径解析获取
            val packageName = clz.qualifiedName.toString()
            val regex = Regex("""com.tencent.devops.([a-z]+).api.([a-zA-Z]+)""")
            val matches = regex.find(packageName)
                ?: throw ErrorCodeException(
                    errorCode = CommonMessageCode.SERVICE_COULD_NOT_BE_ANALYZED,
                    params = arrayOf(packageName)
                )
            matches.groupValues[1]
        }
    }

    /**
     * 获取动态队列名称
     * @param serviceName 微服务名称
     * @param ip 主机IP
     * @return 动态队列名称
     */
    fun getDynamicMqQueue(serviceName: String? = null, ip: String? = null): String {
        val queueTemplate = "q.sharding.routing.rule.exchange.{0}_{1}_queue"
        // 用微服务名和服务器IP替换占位符
        val params = arrayOf(serviceName ?: findServiceName(), ip ?: CommonUtils.getInnerIP())
        return MessageFormat(queueTemplate).format(params)
    }
}
