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

package com.tencent.bkrepo.opdata.registry.consul.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Consul中的微服务实例检查结果
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ConsulInstanceCheck(
    /**
     * 实例所在节点名
     */
    @JsonProperty("Node")
    val node: String,
    @JsonProperty("CheckID")
    val checkId: String,
    /**
     * 检查id格式为service:${serviceId}
     */
    @JsonProperty("Status")
    val status: String,
    /**
     * 格式为${serviceName}-${servicePort}
     */
    @JsonProperty("ServiceID")
    val serviceId: String,
    @JsonProperty("ServiceName")
    val serviceName: String,
    @JsonProperty("ServiceTags")
    val serviceTags: List<String>
) {
    companion object {
        const val STATUS_PASSING = "passing"
        const val STATUS_WARNING = "warning"
        const val STATUS_FAILING = "failing"
    }
}
