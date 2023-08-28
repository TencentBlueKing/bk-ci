package com.tencent.devops.common.service.utils

import com.tencent.devops.common.api.annotation.ServiceInterface
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.CrudEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.service.Profile
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.core.env.Environment
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

object BkServiceUtil {

    private val interfaces = ConcurrentHashMap<KClass<*>, String>()

    fun getServiceHostKey(serviceName: String? = null): String {
        val profileName = getProfileName()
        val finalServiceName = if (serviceName.isNullOrBlank()) {
            serviceName
        } else {
            findServiceName()
        }
        return "ENV:$profileName:SERVICE:$finalServiceName:HOSTS"
    }

    fun getServiceRoutingRuleActionFinishKey(
        routingName: String,
        actionType: CrudEnum
    ): String {
        val profileName = getProfileName()
        return "ENV:$profileName:SHARDING_ROUTING_RULE_UPDATE_FINISH:$routingName:${actionType.name}"
    }

    private fun getProfileName(): String {
        val profile = SpringContextUtil.getBean(Profile::class.java)
        return profile.getActiveProfiles().joinToString().trim()
    }

    fun findServiceName(clz: KClass<*>? = null): String {
        val environment: Environment = SpringContextUtil.getBean(Environment::class.java)
        val assemblyServiceName: String? = environment.getProperty("spring.cloud.consul.discovery.service-name")
        // 单体结构，不分微服务的方式
        if (!assemblyServiceName.isNullOrBlank()) {
            return assemblyServiceName
        }
        val serviceName = if (clz != null) {
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
            environment.getProperty("spring.application.name") ?: KubernetesUtils.getMsName()
        }
        val serviceSuffix: String? = environment.getProperty("service-suffix")
        return if (serviceSuffix.isNullOrBlank() || KubernetesUtils.inContainer()) {
            serviceName
        } else {
            "$serviceName$serviceSuffix"
        }
    }
}
