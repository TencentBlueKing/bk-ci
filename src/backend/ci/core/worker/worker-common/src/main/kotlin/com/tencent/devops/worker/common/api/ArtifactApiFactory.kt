package com.tencent.devops.worker.common.api

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.worker.common.api.archive.ArchiveSDKApi
import com.tencent.devops.worker.common.api.report.ReportSDKApi
import com.tencent.devops.worker.common.exception.ApiNotExistException
import org.reflections.Reflections
import org.slf4j.LoggerFactory
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

object ArtifactApiFactory {

    private val logger = LoggerFactory.getLogger(ArtifactApiFactory::class.java)
    private val apiMap = ConcurrentHashMap<String, Any>()

    init {
        val reflections = Reflections("com.tencent.devops.worker.common.api")
        val archiveApiClass = reflections.getSubTypesOf(ArchiveSDKApi::class.java)
        val reportApiClass = reflections.getSubTypesOf(ReportSDKApi::class.java)
        archiveApiClass.filter { !Modifier.isAbstract(it.modifiers) }.forEach {
            val obj = it.newInstance()
            val realm = it.getMethod("getRealm").invoke(obj).toString()
            apiMap["$realm-${ArchiveSDKApi::class.simpleName}"] = obj
        }
        reportApiClass.filter { !Modifier.isAbstract(it.modifiers) }.forEach {
            val obj = it.newInstance()
            val realm = it.getMethod("getRealm").invoke(obj).toString()
            apiMap["$realm-${ReportSDKApi::class.simpleName}"] = obj
        }
        logger.info("artifact api candidate map: $apiMap")
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> create(apiInterfaceClass: KClass<T>): T {
        val realm = Realm().getRealm()
        val obj = apiMap["$realm-${apiInterfaceClass.simpleName}"]
            ?: throw ApiNotExistException("api interface $apiInterfaceClass have no $realm implement class")
        return obj as T
    }

    private class Realm : AbstractBuildResourceApi() {
        fun getRealm(): String {
            val path = "/ms/artifactory/api/build/artifactories/conf/realm"
            val request = buildGet(path)
            val responseContent = request(request, "get artifactory realm error")
            return objectMapper.readValue<Result<String>>(responseContent).data!!
        }
    }
}
