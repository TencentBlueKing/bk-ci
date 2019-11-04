/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.dispatch.service.bcs

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.auth.api.AuthTokenApi
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.pipeline.enums.DockerVersion
import com.tencent.devops.common.service.Profile
import com.tencent.devops.dispatch.pojo.BCSCreateInstanceResponse
import com.tencent.devops.dispatch.pojo.BCSCreateNamespaceResponse
import com.tencent.devops.dispatch.pojo.BCSDeleteInstanceResponse
import com.tencent.devops.dispatch.pojo.BCSDeleteNamespaceResponse
import com.tencent.devops.dispatch.pojo.DockerTemplate
import com.tencent.devops.log.utils.LogUtils
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BCSService @Autowired constructor(
    private val profile: Profile,
    private val rabbitTemplate: RabbitTemplate,
    private val templateService: PipelineDockerTemplateService,
    private val bkAuthTokenApi: AuthTokenApi,
    private val pipelineAuthServiceCode: PipelineAuthServiceCode,
    private val objectMapper: ObjectMapper
) {

    private val TLINUX_1_2_ID = 1
    private val TLINUX_2_2_ID = 2
    private val CUSTOM_ID = 3

//    private val okHttpClient = HttpUtil.getHttpClient()

    fun startDocker(
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        agentId: String,
        secretKey: String,
        dockerBuildVersion: String?,
        executeCount: Int?
    ): Pair<Long/*namespace_id*/, BCSCreateInstanceResponse> {

        logger.info("Start the docker for the build($buildId) with docker version($dockerBuildVersion)")
        val template = when (dockerBuildVersion) {
            DockerVersion.TLINUX1_2.value -> templateService.getTemplateById(TLINUX_1_2_ID)
            DockerVersion.TLINUX2_2.value -> templateService.getTemplateById(TLINUX_2_2_ID)
            else -> templateService.getTemplateById(CUSTOM_ID)
        }

        LogUtils.addLine(rabbitTemplate, buildId, "Start docker $dockerBuildVersion for the build", "", "", executeCount ?: 1)

        val token = bkAuthTokenApi.getAccessToken(pipelineAuthServiceCode)
        val url = "${getCreateInstanceUrl(template)}?access_token=$token"
        val namespace = createNamespace(pipelineId, buildId, vmSeqId, agentId, template, token)
        // val projectOriginId = getProjectOriginId(projectId)
        val reqBody = composeTemplateRequest(template, agentId, template.bcsProjectId, secretKey, namespace.first, namespace.second, namespace.third,
                pipelineId, vmSeqId, dockerBuildVersion)
        logger.info("Create application, url:$url")
        logger.info("Create application, reqBody:$reqBody")

        val request = Request.Builder()
                .url(url)
                .post(RequestBody.create(
                        MediaType.parse("application/json; charset=utf-8"), reqBody))
                .build()

//        val client = okHttpClient.newBuilder().build()
//        client.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("Fail to start the docker for the build($buildId) of pipeline($pipelineId) because of $responseContent")
                throw RuntimeException("Fail to start docker")
            }
            logger.info("Success to start the docker ($responseContent)")
            return Pair(namespace.first, objectMapper.readValue(responseContent))
        }
    }

    fun shutdownDocker(instanceId: Long, namespaceId: Long): Boolean {
        logger.info("Start to shutdown the docker $instanceId")
        val template = templateService.getTemplate()
        val token = bkAuthTokenApi.getAccessToken(pipelineAuthServiceCode)
        var success = false
        try {
            val url = "${getDeleteInstanceUrl(template)}?access_token=$token"
            val reqBody = composeDeleteInstanceBody(instanceId)
            logger.info("Delete bcs instance url: $url")
            logger.info("Delete bcs instance reqBody: $reqBody")
            val request = Request.Builder()
                    .url(url)
                    .delete(RequestBody.create(
                            MediaType.parse("application/json; charset=utf-8"), reqBody))
                    .build()
//            val client = okHttpClient.newBuilder().build()
//            client.newCall(request).execute().use { response ->
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body()!!.string()
                if (!response.isSuccessful) {
                    logger.warn("Fail to delete the docker instance($instanceId) because of $responseContent")
                    // throw RuntimeException("Fail to delete docker")
                    return@use
                }
                logger.info("Success to delete the docker ($responseContent)")
                val deleteResponse: BCSDeleteInstanceResponse = objectMapper.readValue(responseContent)
                success = (deleteResponse.code == 0)
            }
        } catch (e: Throwable) {
            logger.warn("Fail to stop the docker")
        }

        return deleteNamespace(template, namespaceId, token) && success
    }

    private fun createNamespace(pipelineId: String, buildId: String, vmSeqId: String, agentId: String, template: DockerTemplate, token: String): Triple<Long, String, String> {
        val url = "${getCreateNamespaceUrl(template)}?access_token=$token"
        logger.info("Create namespace request url: $url")
        val reqbody = composeCreateNamespaceRequest(pipelineId, buildId, vmSeqId, agentId, template)
        logger.info("Create namespace request body: $reqbody")
        val request = Request.Builder()
                .url(url)
                .post(RequestBody.create(
                        MediaType.parse("application/json; charset=utf-8"), reqbody))
                .build()

//        val client = okHttpClient.newBuilder().build()
//        client.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("Fail to create namespace of pipeline($pipelineId) build($buildId) vmSeq($vmSeqId) because of code(${response.code()}) and response $responseContent")
                throw RuntimeException("Fail to create namespace")
            }
            logger.info("Success to create namespace ($responseContent)")
            val namespaceResponse: BCSCreateNamespaceResponse = objectMapper.readValue(responseContent)
            return Triple(namespaceResponse.data.id, namespaceResponse.data.name, namespaceResponse.data.cluster_id)
        }
    }

    private fun deleteNamespace(template: DockerTemplate, namespaceId: Long, token: String): Boolean {
        logger.info("Start to delete the namespace($namespaceId) of template($template)")
        val url = composeUrl(getDeleteNamespaceUrl(template, namespaceId), token)
        logger.info("Delete namespace url:$url")
        val request = Request.Builder()
                .url(url)
                .post(RequestBody.create(null, ByteArray(0)))
                .build()

//        val client = okHttpClient.newBuilder().build()

//        client.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("Fail to delete namespace($namespaceId) because of code(${response.code()}) and response $responseContent")
                throw RuntimeException("Fail to delete namespace")
            }
            logger.info("Success to delete namespace ($responseContent)")
            val namespaceResponse: BCSDeleteNamespaceResponse = objectMapper.readValue(responseContent)
            return namespaceResponse.code == 0
        }
    }

    private fun composeUrl(url: String, token: String) =
            "$url?access_token=$token"
    /**
     * {
     *   "inst_id_list": [2319]
     * }
     */
    private fun composeDeleteInstanceBody(instanceId: Long) =
            JSONObject()
                    .put("inst_id_list",
                JSONArray().put(instanceId))
                    .toString()

    /**
     * {
     *   "name": "test-by-api",
     *   "cluster_id": "BCS-K8S-15018"
     * }
     */
    private fun composeCreateNamespaceRequest(pipelineId: String, buildId: String, vmSeqId: String, agentId: String, template: DockerTemplate) =
            JSONObject()
                    .put("name", getNamespace(agentId, vmSeqId))
                    .put("cluster_id", template.clusterId)
                    .toString()

    private fun getNamespace(agentId: String, vmSeqId: String) =
            "$agentId-$vmSeqId"

    private fun composeTemplateRequest(
        template: DockerTemplate,
        agentId: String,
        projectId: String,
        secretKey: String,
        namespaceId: Long,
        namespaceName: String,
        clusterId: String,
        pipelineId: String,
        vmSeqId: String,
        dockerBuildVersion: String?
    ): String {
        val json = JSONObject()

        /**
         * {
         *     "cluster_ns_info": {
         *         "BCS-MESOS-10011": {
         *             "bellke-api-1": {
         *                 "test1": "1",
         *                 "test2": "1"
         *             }
         *         }
         *     },
         *     "version_id": 256,
         *     "show_version_id": 111,
         *     "show_version_name": "v1",
         *     "instance_entity": {
         *         "Application": [{
         *             "id": 134,
         *             "name": "bellke-application-1"
         *         }]
         *     },
         *     "is_start": true
         * }
         */

        // 如果是tlinux1.2或者2.2则直接使用模板1，2即可，模板中已经将镜像固定，如果是自定义的镜像，则需要以变量形式传入创建模板的请求中
        if (dockerBuildVersion == DockerVersion.TLINUX1_2.value || dockerBuildVersion == DockerVersion.TLINUX2_2.value) {
            json.put("cluster_ns_info", JSONObject().put(clusterId, JSONObject().put(namespaceName,
                    JSONObject().put("projectId", projectId).put("agentId", agentId).put("secretKey", secretKey).put("pipelineId", pipelineId).put("vmSeqId", vmSeqId))))
                    .put("version_id", template.versionId)
                    .put("show_version_id", template.showVersionId)
                    .put("show_version_name", template.showVersionName)
                    .put("instance_entity", JSONObject().put("Deployment",
                            JSONArray()
                                    .put(JSONObject().put("id", template.deploymentId)
                                            .put("name", template.deploymentName))))
        } else {
            val imageNameTag = dockerBuildVersion!!.split(":")
            json.put("cluster_ns_info", JSONObject().put(clusterId, JSONObject().put(namespaceName,
                    JSONObject().put("projectId", projectId).put("agentId", agentId).put("secretKey", secretKey).put("pipelineId", pipelineId).put("vmSeqId", vmSeqId)
                            .put("dockerImageName", imageNameTag[0]).put("dockerImageTag", imageNameTag[1]))))
                    .put("version_id", template.versionId)
                    .put("show_version_id", template.showVersionId)
                    .put("show_version_name", template.showVersionName)
                    .put("instance_entity", JSONObject().put("Deployment",
                            JSONArray()
                                    .put(JSONObject().put("id", template.deploymentId)
                                            .put("name", template.deploymentName))))
        }

        return json.toString()
    }

    private fun getProjectOriginId(projectCode: String): String {
        val token = bkAuthTokenApi.getAccessToken(pipelineAuthServiceCode)
        val url = "${getProjectUrl()}$projectCode/?access_token=$token"
        logger.info("Get project info, request url: $url")
        val request = Request.Builder()
                .url(url)
                .get()
                .build()
        OkhttpUtils.doHttp(request).use { response ->
            val data = response.body()!!.string()
            logger.info("Get project info, response: $data")
            if (!response.isSuccessful) {
                throw RuntimeException(data)
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(data)
            val code = responseData["code"] as Int
            if (0 == code) {
                val dataMap = responseData["data"] as Map<String, Any>
                return dataMap["project_id"] as String
            } else {
                throw RuntimeException(data)
            }
        }
    }

    private fun getCreateInstanceUrl(template: DockerTemplate) =
            "${getInstancePrefix(template)}/"

    private fun getDeleteInstanceUrl(template: DockerTemplate) =
            "${getInstancePrefix(template)}/batch_delete"

    private fun getCreateNamespaceUrl(template: DockerTemplate) =
            "${getNamespacePrefix(template)}/namespaces"

    private fun getDeleteNamespaceUrl(template: DockerTemplate, namespaceId: Long) =
            "${getNamespacePrefix(template)}/namespace/$namespaceId"

    private fun getNamespacePrefix(template: DockerTemplate) =
            getUrlPrefix(template) + "/ns/cc_app_ids/${template.ccAppId}/projects/${template.bcsProjectId}"

    private fun getInstancePrefix(template: DockerTemplate) =
            getUrlPrefix(template) + "/apps/cc_app_ids/${template.ccAppId}/projects/${template.bcsProjectId}/instances"

    private fun getUrlPrefix(template: DockerTemplate): String {
        val env = when {
            profile.isDev() -> "test"
            profile.isTest() -> "test"
            else -> "prod"
        }

        return "http://api.apigw-biz.o.oa.com/api/paas-cd/$env"
    }

    private fun getProjectUrl(): String {
        val env = when {
            profile.isDev() -> "test"
            profile.isTest() -> "test"
            else -> "prod"
        }
        return "http://api.apigw-biz.o.oa.com/api/paas-cc/$env/projects/"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BCSService::class.java)
    }
}

/*

fun main(argv: Array<String>) {
    val okHttpClient = okhttp3.OkHttpClient.Builder()
            .connectTimeout(5L, TimeUnit.SECONDS)
            .readTimeout(60L, TimeUnit.SECONDS)
            .writeTimeout(60L, TimeUnit.SECONDS)
            .build()

    val token = "HKamOC6mA1fe7ot286QxjAOYSj110I"
    val url = "http://api.apigw-biz.o.oa.com/api/paas-cd/test/apps/cc_app_ids/100205/projects/b3b58d228f244c13b83bef3af882155c/instances?access_token=$token"
    val body = JSONObject()
            .put("namespaces", "112")
            .put("version_id", 5621)
            .put("show_version_id", 554)
            .put("show_version_name", "version1")
            .put("instance_entity", JSONObject().put("Deployment",
                    JSONArray()
                            .put(JSONObject().put("id", 890)
                                    .put("name", "rdeng-template-2"))))
    val request = Request.Builder()
            .url(url)
            .post(RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"), body.toString()))
            .build()

    val client = okHttpClient.newBuilder().build()
    client.newCall(request).execute().use { response ->
        val responseContent = response.body()!!.string()
        if (!response.isSuccessful) {
            throw RuntimeException("Fail to start docker")
        }
        val instanceResponse: BCSCreateInstanceResponse = JsonUtil.getObjectMapper().readValue(responseContent)
        println(instanceResponse)
    }
}
        */