package com.tencent.devops.plugin.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.plugin.pojo.stke.StkeType
import com.tencent.devops.plugin.pojo.stke.StkeUpdateParam
import com.tencent.devops.plugin.utils.StkeHttpClientUtils
import okhttp3.Request
import okhttp3.RequestBody
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class StkeService @Autowired constructor() {

    @Value("\${stke.cert_pem:#{null}}")
    private val cert_pem: String? = null

    @Value("\${stke.cert_key_pem:#{null}}")
    private val cert_key_pem: String? = null

    fun update(
        stkeType: StkeType,
        clusterName: String,
        namespace: String,
        appsName: String,
        updateParam: StkeUpdateParam
    ): String {

        val client = StkeHttpClientUtils.getHttpClient(certPem = cert_pem, certKeyPem = cert_key_pem)

        val request = when (stkeType) {
            StkeType.DEPLOYMENT, StkeType.STATEFUL_SET -> {
                val type = if (StkeType.STATEFUL_SET == stkeType) "statefulsets" else "deployments"
                val url =
                    "https://tke.kubernetes.oa.com/v2/forward/stke/apis/apps/v1/namespaces/$namespace/$type/$appsName"
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
                    "http://kubernetes.oa.com/apis/platform.stke/v1alpha1/namespaces/$namespace/statefulsetpluses/$appsName"
                val requestBody = RequestBody.create(null, JsonUtil.toJson(updateParam))
                Request.Builder()
                    .url(url)
                    .addHeader("X-TKE-ClusterName", clusterName)
                    .addHeader("Content-Type", "json")
                    .put(requestBody)
                    .build()
            }
        }

        client.newCall(request).execute().use { response ->
            return response.body()!!.string()
        }
    }

    fun getPodsStatus(
        stkeType: StkeType,
        clusterName: String,
        namespace: String,
        appsName: String
    ): String {

        val client = StkeHttpClientUtils.getHttpClient(certPem = cert_pem, certKeyPem = cert_key_pem)

        val podsInfoUrl = when (stkeType) {
            StkeType.STATEFUL_SET -> "https://tke.kubernetes.oa.com/v2/forward/stke/apis/apps/v1/namespaces/$namespace/statefulsets/$appsName/pods"
            StkeType.DEPLOYMENT -> "https://tke.kubernetes.oa.com/v2/forward/stke/apis/apps/v1beta2/namespaces/$namespace/deployments/$appsName/pods"
            StkeType.STATEFUL_SET_PLUS -> "https://tke.kubernetes.oa.com/v2/forward/stke/apis/platform.stke/v1alpha1/namespaces/$namespace/statefulsetpluses/$appsName/pods"
        }

        val podsRequest = Request.Builder()
            .url(podsInfoUrl)
            .addHeader("X-TKE-ClusterName", clusterName)
            .get()
            .build()

        client.newCall(podsRequest).execute().use { response ->
            return response.body()!!.string()
        }
    }

    fun getWorkload(
        stkeType: StkeType,
        clusterName: String,
        appsName: String,
        namespace: String
    ): String {

        val client = StkeHttpClientUtils.getHttpClient(certPem = cert_pem, certKeyPem = cert_key_pem)

        var workloadUrl = when (stkeType) {
            StkeType.STATEFUL_SET -> "https://tke.kubernetes.oa.com/v2/forward/stke/apis/apps/v1/namespaces/$namespace/statefulsets/$appsName"
            StkeType.DEPLOYMENT -> "https://tke.kubernetes.oa.com/v2/forward/stke/apis/apps/v1/namespaces/$namespace/deployments/$appsName"
            StkeType.STATEFUL_SET_PLUS -> "https://tke.kubernetes.oa.com/v2/forward/stke/apis/platform.stke/v1alpha1/namespaces/$namespace/statefulsetpluses/$appsName"
        }

        val workRequest = Request.Builder()
            .url(workloadUrl)
            .addHeader("X-TKE-ClusterName", clusterName)
            .get()
            .build()

        client.newCall(workRequest).execute().use { response ->
            return response.body()!!.string()
        }
    }

    fun getManagers(
        projectId: String
    ): String {

        val client = StkeHttpClientUtils.getHttpClient(certPem = cert_pem, certKeyPem = cert_key_pem)

        val url = "https://tke.kubernetes.oa.com/v2/forward/stke/apis/platform.tke/v1/projects/$projectId"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            return response.body()!!.string()
        }
    }

    fun getNamespaces(
        projectId: String
    ): String {

        val client = StkeHttpClientUtils.getHttpClient(certPem = cert_pem, certKeyPem = cert_key_pem)

        val url =
            "http://kubernetes.oa.com/apis/platform.tke/v1/namespacesetsfieldSelector=spec.projectName=$projectId"
        val request = Request.Builder()
            .url(url)
            .post(RequestBody.create(null, ""))
            .addHeader("X-TKE-ClusterName", "cls-elrc7cfq")
            .build()

        client.newCall(request).execute().use { response ->
            return response.body()!!.string()
        }
    }
}