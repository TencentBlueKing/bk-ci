package com.tencent.devops.plugin.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.plugin.pojo.stke.StkePodsStatusResp
import com.tencent.devops.plugin.pojo.stke.StkeType
import com.tencent.devops.plugin.pojo.stke.StkeUpdateParam
import com.tencent.devops.plugin.utils.StkeHttpClientUtils
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class StkeService @Autowired constructor() {

    companion object {
        private val logger = LoggerFactory.getLogger(StkeService::class.java)
    }

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

        if (cert_key_pem == null || cert_pem == null) {
            logger.error("Stkeplugin cert_pem/cert_key_pem can not find")
            return ""
        }

        val client = StkeHttpClientUtils.getHttpClient(certPem = cert_pem, certKeyPem = cert_key_pem)

        val type = when (stkeType) {
            StkeType.DEPLOYMENT -> {
                "deployments"
            }
            StkeType.STATEFUL_SET -> {
                "statefulsets"
            }
            StkeType.STATEFUL_SET_PLUS -> {
                "statefulsetpluses"
            }
        }

        val url = "https://tke.kubernetes.oa.com/v2/forward/stke/apis/apps/v1/namespaces/$namespace/$type/$appsName"
        val requestBody = RequestBody.create(null, JsonUtil.toJson(updateParam))
        val request = Request.Builder()
            .url(url)
            .addHeader("X-TKE-ClusterName", clusterName)
            .addHeader("Content-Type", "application/strategic-merge-patch+json")
            .patch(requestBody)
            .build()

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
        if (cert_key_pem == null || cert_pem == null) {
            logger.error("cert_pem/cert_key_pem can not find")
            return ""
        }

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

        if (cert_key_pem == null || cert_pem == null) {
            logger.error("cert_pem/cert_key_pem can not find")
            return ""
        }

        val client = StkeHttpClientUtils.getHttpClient(certPem = cert_pem, certKeyPem = cert_key_pem)

        val workloadUrl = when (stkeType) {
            StkeType.STATEFUL_SET -> "https://tke.kubernetes.oa.com/v2/forward/stke/apis/apps/v1/namespaces/$namespace/statefulsets/$appsName"
            StkeType.DEPLOYMENT -> "https://tke.kubernetes.oa.com/v2/forward/stke/apis/apps/v1beta2/namespaces/$namespace/deployments/$appsName"
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
}