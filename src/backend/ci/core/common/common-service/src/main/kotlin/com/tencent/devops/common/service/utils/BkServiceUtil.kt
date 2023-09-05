package com.tencent.devops.common.service.utils

import com.tencent.devops.common.api.annotation.ServiceInterface
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.CrudEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.service.Profile
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.core.env.Environment
import java.text.MessageFormat
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

object BkServiceUtil {

    private val interfaces = ConcurrentHashMap<KClass<*>, String>()

    fun getServiceHostKey(serviceName: String? = null): String {
        val profileName = getProfileName()
        val finalServiceName = if (!serviceName.isNullOrBlank()) {
            serviceName
        } else {
            findServiceName()
        }
        return "ENV:$profileName:SERVICE:$finalServiceName:HOSTS"
    }

    fun getServiceRoutingRuleActionFinishKey(
        serviceName: String,
        routingName: String,
        actionType: CrudEnum
    ): String {
        val profileName = getProfileName()
        val actionName = actionType.name
        return "ENV:$profileName:SERVICE:$serviceName:SHARDING_ROUTING_RULE_UPDATE_FINISH:$routingName:$actionName"
    }

    private fun getProfileName(): String {
        val profile = SpringContextUtil.getBean(Profile::class.java)
        return profile.getActiveProfiles().joinToString().trim()
    }

    fun findServiceName(clz: KClass<*>? = null, serviceName: String? = null): String {
        val environment: Environment = SpringContextUtil.getBean(Environment::class.java)
        val assemblyServiceName: String? = environment.getProperty("spring.cloud.consul.discovery.service-name")
        // 单体结构，不分微服务的方式
        if (!assemblyServiceName.isNullOrBlank()) {
            return assemblyServiceName
        }
        val serviceSuffix: String? = environment.getProperty("service-suffix")
        val tmpServiceName = if (!serviceName.isNullOrBlank()) {
            serviceName
        } else if (clz != null) {
            interfaces.getOrPut(clz) {
                val serviceInterface = AnnotationUtils.findAnnotation(clz.java, ServiceInterface::class.java)
                if (serviceInterface != null && serviceInterface.value.isNotBlank()) {
                    serviceInterface.value
                } else {
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
        } else {
            var applicationName = environment.getProperty("spring.application.name")
            if (!serviceSuffix.isNullOrBlank()) {
                applicationName = applicationName?.removeSuffix(serviceSuffix)
            }
            applicationName ?: KubernetesUtils.getMsName()
        }
        return if (serviceSuffix.isNullOrBlank() || KubernetesUtils.inContainer()) {
            tmpServiceName
        } else {
            "$tmpServiceName$serviceSuffix"
        }
    }

    fun getDynamicMqQueue(serviceName: String? = null, ip: String? = null): String {
        val queueTemplate = "q.sharding.routing.rule.exchange.{0}_{1}_queue"
        // 用微服务名和服务器IP替换占位符
        val params = arrayOf(serviceName ?: findServiceName(), ip ?: CommonUtils.getInnerIP())
        return MessageFormat(queueTemplate).format(params)
    }
}
