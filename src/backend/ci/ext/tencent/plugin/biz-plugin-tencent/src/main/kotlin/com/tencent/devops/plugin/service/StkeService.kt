package com.tencent.devops.plugin.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.plugin.pojo.stke.ConfigMapData
import com.tencent.devops.plugin.pojo.stke.ConfigMapUpdateParam
import com.tencent.devops.plugin.pojo.stke.StkeType
import com.tencent.devops.plugin.pojo.stke.StkeUpdateParam
import com.tencent.devops.plugin.utils.StkeHttpClientUtils
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class StkeService @Autowired constructor() {

    @Value("\${stke.cert_pem:#{null}}")
    private val cert_pem: String? = null

    @Value("\${stke.cert_key_pem:#{null}}")
    private val cert_key_pem: String? = null

    private val stkeUrlPrefix = "https://tke.kubernetes.oa.com/v2/forward/stke/apis"

    fun update(
        stkeType: StkeType,
        clusterName: String,
        namespace: String,
        appsName: String,
        updateParam: StkeUpdateParam
    ): String? {
        val client = StkeHttpClientUtils.getHttpClient(certPem = cert_pem, certKeyPem = cert_key_pem)

        val request = when (stkeType) {
            StkeType.DEPLOYMENT, StkeType.STATEFUL_SET -> {
                val url = "$stkeUrlPrefix/apps/v1/namespaces/$namespace/${stkeType.type}/$appsName"
                val requestBody = RequestBody.create(null, JsonUtil.toJson(updateParam))
                Request.Builder()
                    .url(url)
                    .addHeader("X-TKE-ClusterName", clusterName)
                    .addHeader(
                        "Content-Type",
                        if (stkeType == StkeType.STATEFUL_SET_PLUS) "application/merge-patch+json" else "application/strategic-merge-patch+json"
                    )
                    .patch(requestBody)
                    .build()
            }
            StkeType.STATEFUL_SET_PLUS -> {
                val url =
                    "http://kubernetes.oa.com/apis/platform.stke/v1alpha1/namespaces/$namespace/${stkeType.type}/$appsName"
                val requestBody = RequestBody.create(null, JsonUtil.toJson(updateParam))
                Request.Builder()
                    .url(url)
                    .addHeader("X-TKE-ClusterName", clusterName)
                    .addHeader("Content-Type", "json")
                    .put(requestBody)
                    .build()
            }
        }

        try {
            client.newCall(request).execute().use { response ->
                return response.body()!!.string()
            }
        } catch (e: Exception) {
            logger.error("Stke plugin request failed $e")
            return null
        }
    }

    fun getPodsStatus(
        stkeType: StkeType,
        clusterName: String,
        namespace: String,
        appsName: String
    ): String? {
        val client = StkeHttpClientUtils.getHttpClient(certPem = cert_pem, certKeyPem = cert_key_pem)

        val podsInfoUrl = when (stkeType) {
            StkeType.STATEFUL_SET -> "$stkeUrlPrefix/apps/v1/namespaces/$namespace/${stkeType.type}/$appsName/pods"
            StkeType.DEPLOYMENT -> "$stkeUrlPrefix/apps/v1beta2/namespaces/$namespace/${stkeType.type}/$appsName/pods"
            StkeType.STATEFUL_SET_PLUS -> "$stkeUrlPrefix/platform.stke/v1alpha1/namespaces/$namespace/${stkeType.type}/$appsName/pods"
        }

        val podsRequest = Request.Builder()
            .url(podsInfoUrl)
            .addHeader("X-TKE-ClusterName", clusterName)
            .get()
            .build()

        try {
            client.newCall(podsRequest).execute().use { response ->
                return response.body()!!.string()
            }
        } catch (e: Exception) {
            logger.error("Stke plugin request failed $e")
            return null
        }
    }

    fun getWorkload(
        stkeType: StkeType,
        clusterName: String,
        appsName: String,
        namespace: String
    ): String? {
        val client = StkeHttpClientUtils.getHttpClient(certPem = cert_pem, certKeyPem = cert_key_pem)

        val workloadUrl = when (stkeType) {
            StkeType.STATEFUL_SET -> "$stkeUrlPrefix/apps/v1/namespaces/$namespace/${stkeType.type}/$appsName"
            StkeType.DEPLOYMENT -> "$stkeUrlPrefix/apps/v1/namespaces/$namespace/${stkeType.type}/$appsName"
            StkeType.STATEFUL_SET_PLUS -> "$stkeUrlPrefix/platform.stke/v1alpha1/namespaces/$namespace/${stkeType.type}/$appsName"
        }

        val workRequest = Request.Builder()
            .url(workloadUrl)
            .addHeader("X-TKE-ClusterName", clusterName)
            .get()
            .build()

        try {
            client.newCall(workRequest).execute().use { response ->
                return response.body()!!.string()
            }
        } catch (e: Exception) {
            logger.error("Stke plugin request failed $e")
            return null
        }
    }

    fun getManagers(
        projectId: String
    ): String? {
        val client = StkeHttpClientUtils.getHttpClient(certPem = cert_pem, certKeyPem = cert_key_pem)

        val url = "$stkeUrlPrefix/platform.tke/v1/projects/$projectId"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        try {
            client.newCall(request).execute().use { response ->
                return response.body()!!.string()
            }
        } catch (e: Exception) {
            logger.error("Stke plugin request failed $e")
            return null
        }
    }

    fun updateConfigMap(
        clusterName: String,
        namespace: String,
        configMapName: String,
        configMapData: List<ConfigMapData>
    ): String? {
        val client = StkeHttpClientUtils.getHttpClient(certPem = cert_pem, certKeyPem = cert_key_pem)

        val url = "https://tke.kubernetes.oa.com/v2/forward/stke/api/v1/namespaces/$namespace/configmaps/$configMapName"

        val updateParam = configMapData.map {
            ConfigMapUpdateParam(
                op = "replace",
                path = "/data/${it.key}",
                value = it.value)
        }
        val requestBody = RequestBody.create(null, JsonUtil.toJson(updateParam))
        val request = Request.Builder()
                    .url(url)
                    .addHeader("X-TKE-ClusterName", clusterName)
                    .addHeader("Content-Type", "application/json-patch+json")
                    .patch(requestBody)
                    .build()
        try {
            client.newCall(request).execute().use { response ->
                return response.body()!!.string()
            }
        } catch (e: Exception) {
            logger.error("Stke plugin request failed $e")
            return null
        }
    }

    fun updateWorkLoad(
        stkeType: StkeType,
        clusterName: String,
        namespace: String,
        workLoadName: String,
        operator: String,
        buildId: String
    ): String? {
        val client = StkeHttpClientUtils.getHttpClient(certPem = cert_pem, certKeyPem = cert_key_pem)

        val url = when (stkeType) {
            StkeType.STATEFUL_SET -> "$stkeUrlPrefix/apps/v1/namespaces/$namespace/${stkeType.type}/$workLoadName"
            StkeType.DEPLOYMENT -> "$stkeUrlPrefix/apps/v1/namespaces/$namespace/${stkeType.type}/$workLoadName"
            StkeType.STATEFUL_SET_PLUS -> "$stkeUrlPrefix/platform.stke/v1alpha1/namespaces/$namespace/${stkeType.type}/$workLoadName"
        }

        val updateParam = listOf(
            ConfigMapUpdateParam(
                op = "replace",
                path = "/spec/template/metadata/annotations/devops.oa.com-update_configmap-operator-buildId",
                value = "$operator-$buildId"
            )
        )
        val requestBody = RequestBody.create(null, JsonUtil.toJson(updateParam))
        val request = Request.Builder()
                    .url(url)
                    .addHeader("X-TKE-ClusterName", clusterName)
                    .addHeader("Content-Type", "application/json-patch+json")
                    .patch(requestBody)
                    .build()
        try {
            client.newCall(request).execute().use { response ->
                return response.body()!!.string()
            }
        } catch (e: Exception) {
            logger.error("Stke plugin request failed $e")
            return null
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StkeService::class.java)
    }
}