/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.opdata.registry.consul

import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.opdata.message.OpDataMessageCode.ServiceInstanceNotFound
import com.tencent.bkrepo.opdata.pojo.registry.InstanceInfo
import com.tencent.bkrepo.opdata.pojo.registry.InstanceStatus
import com.tencent.bkrepo.opdata.pojo.registry.ServiceInfo
import com.tencent.bkrepo.opdata.registry.RegistryClient
import com.tencent.bkrepo.opdata.registry.consul.exception.ConsulApiException
import com.tencent.bkrepo.opdata.registry.consul.pojo.ConsulInstanceCheck
import com.tencent.bkrepo.opdata.registry.consul.pojo.ConsulInstanceCheck.Companion.STATUS_PASSING
import com.tencent.bkrepo.opdata.registry.consul.pojo.ConsulInstanceHealth
import com.tencent.bkrepo.opdata.registry.consul.pojo.ConsulInstanceId
import com.tencent.bkrepo.opdata.util.parseResAndThrowExceptionOnRequestFailed
import com.tencent.bkrepo.opdata.util.requestBuilder
import com.tencent.bkrepo.opdata.util.throwExceptionOnRequestFailed
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.internal.Util.EMPTY_REQUEST
import org.springframework.cloud.consul.ConsulProperties

class ConsulRegistryClient constructor(
    private val httpClient: OkHttpClient,
    private val consulProperties: ConsulProperties
) : RegistryClient {

    override fun services(): List<ServiceInfo> {
        val url = urlBuilder().addPathSegments(CONSUL_LIST_SERVICES_PATH).build()
        val req = url.requestBuilder().build()
        val res = httpClient.newCall(req).execute()
        return res.use {
            parseResAndThrowExceptionOnRequestFailed(res) { res ->
                res.body()!!.string().readJsonString<Map<String, List<String>>>().map {
                    ServiceInfo(it.key, emptyList())
                }
            }
        }
    }

    override fun instances(serviceName: String): List<InstanceInfo> {
        return listConsulInstanceHealth(serviceName).map { convertToInstanceInfo(it) }
    }

    override fun deregister(serviceName: String, instanceId: String): InstanceInfo {
        // 注销服务实例
        val urlBuilder = HttpUrl.Builder().addPathSegments(CONSUL_DEREGISTER_PATH)
        return changeInstanceStatus(urlBuilder, serviceName, instanceId, InstanceStatus.DEREGISTER)
    }

    override fun instanceInfo(serviceName: String, instanceId: String): InstanceInfo {
        val consulInstanceHealth = consulInstanceHealth(serviceName, instanceId)
        return convertToInstanceInfo(consulInstanceHealth)
    }

    override fun maintenance(serviceName: String, instanceId: String, enable: Boolean): InstanceInfo {
        // 获取服务所在节点
        val urlBuilder = HttpUrl.Builder()
            .addPathSegments(CONSUL_MAINTENANCE_PATH)
            .addQueryParameter(CONSUL_QUERY_PARAM_ENABLE, enable.toString())
        val targetStatus = if (enable) {
            InstanceStatus.DEREGISTER
        } else {
            InstanceStatus.RUNNING
        }
        return changeInstanceStatus(urlBuilder, serviceName, instanceId, targetStatus)
    }

    private fun changeInstanceStatus(
        urlBuilder: HttpUrl.Builder,
        serviceName: String,
        instanceId: String,
        targetStatus: InstanceStatus
    ): InstanceInfo {
        val consulInstanceId = ConsulInstanceId.create(instanceId)

        // 获取服务所在节点
        val consulInstanceHealth = consulInstanceHealth(serviceName, instanceId)
        val url = urlBuilder
            .scheme(consulProperties.scheme ?: CONSUL_DEFAULT_SCHEME)
            .host(consulInstanceHealth.consulNode.address)
            .port(consulProperties.port)
            .addPathSegment(consulInstanceId.serviceId)
            .build()
        val req = url.requestBuilder().put(EMPTY_REQUEST).build()
        val res = httpClient.newCall(req).execute()
        res.use { throwExceptionOnRequestFailed(it) }

        return convertToInstanceInfo(consulInstanceHealth).copy(status = targetStatus)
    }

    private fun consulInstanceHealth(serviceName: String, instanceId: String): ConsulInstanceHealth {
        val consulInstances = listConsulInstanceHealth(serviceName, instanceId)
        if (consulInstances.size > 1) {
            throw ConsulApiException("more than 1 instance found, serviceName: $serviceName, instanceId: $instanceId")
        }
        if (consulInstances.isEmpty()) {
            throw NotFoundException(ServiceInstanceNotFound, serviceName, instanceId)
        }
        return consulInstances[0]
    }

    private fun listConsulInstanceHealth(
        serviceName: String,
        instanceId: String? = null
    ): List<ConsulInstanceHealth> {
        val consulInstanceId = instanceId?.let { ConsulInstanceId.create(it) }

        // 创建请求
        val urlBuilder = urlBuilder().addPathSegments(CONSUL_LIST_SERVICE_HEALTH_PATH).addPathSegment(serviceName)
        val url = consulInstanceId?.let {
            val filterExpression = buildFilterExpression(it.serviceId, it.nodeName)
            urlBuilder.addQueryParameter(CONSUL_QUERY_PARAM_FILTER, filterExpression).build()
        } ?: urlBuilder.build()
        val req = url.requestBuilder().build()
        val res = httpClient.newCall(req).execute()

        // 解析请求结果
        res.use {
            val consulInstances = parseResAndThrowExceptionOnRequestFailed<List<ConsulInstanceHealth>>(res) { res ->
                res.body()!!.string().readJsonString()
            }
            if (consulInstanceId == null) {
                return consulInstances
            }
            // 低版本consul不支持filter表达式过滤，请求到结果后手动过滤
            return consulInstances.filter {
                it.consulInstance.id == consulInstanceId.serviceId &&
                    it.consulNode.nodeName == consulInstanceId.nodeName
            }
        }
    }

    private fun buildFilterExpression(serviceId: String, nodeName: String): String {
        return "$CONSUL_FILTER_SELECTOR_SERVICE_ID == \"$serviceId\" and" +
            " $CONSUL_FILTER_SELECTOR_NODE_NAME == \"$nodeName\""
    }

    private fun convertToInstanceInfo(consulInstanceHealth: ConsulInstanceHealth): InstanceInfo {
        val consulInstance = consulInstanceHealth.consulInstance
        val consulNode = consulInstanceHealth.consulNode
        // 过滤非服务实例的健康检查信息
        val consulInstanceStatusList = consulInstanceHealth.consulInstanceChecks.filter { it.serviceName.isNotEmpty() }

        val consulInstanceId =
            ConsulInstanceId.create(consulNode.datacenter, consulNode.nodeName, consulInstance.id)
        return InstanceInfo(
            id = consulInstanceId.instanceIdStr(),
            serviceName = consulInstance.service,
            host = consulInstance.address,
            port = consulInstance.port,
            status = convertToInstanceStatus(consulInstanceStatusList)
        )
    }

    /**
     * 服务的全部检查通过才算正常运行
     *
     * @param instanceStatus 服务checks列表，列表为空时返回为RUNNING状态
     */
    private fun convertToInstanceStatus(instanceStatus: List<ConsulInstanceCheck>): InstanceStatus {
        instanceStatus.forEach {
            if (it.checkId == maintenanceModeCheckId(it.serviceId)) {
                return InstanceStatus.DEREGISTER
            }
            if (it.status != STATUS_PASSING) {
                return InstanceStatus.OFFLINE
            }
        }
        return InstanceStatus.RUNNING
    }

    private fun maintenanceModeCheckId(serviceId: String): String {
        return "_service_maintenance:$serviceId"
    }

    private fun urlBuilder() = HttpUrl.Builder()
        .scheme(consulProperties.scheme ?: CONSUL_DEFAULT_SCHEME)
        .host(consulProperties.host)
        .port(consulProperties.port)

    companion object {
        private const val CONSUL_DEFAULT_SCHEME = "http"

        private const val CONSUL_QUERY_PARAM_ENABLE = "enable"
        private const val CONSUL_QUERY_PARAM_FILTER = "filter"
        private const val CONSUL_FILTER_SELECTOR_SERVICE_ID = "Service.ID"
        private const val CONSUL_FILTER_SELECTOR_NODE_NAME = "Node.Node"

        private const val CONSUL_LIST_SERVICES_PATH = "v1/catalog/services"
        private const val CONSUL_LIST_SERVICE_HEALTH_PATH = "v1/health/service"
        private const val CONSUL_DEREGISTER_PATH = "v1/agent/service/deregister"
        private const val CONSUL_MAINTENANCE_PATH = "v1/agent/service/maintenance"
    }
}
