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

package com.tencent.devops.environment.pojo.job.agentres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class AgentPortConfig(
    @get:Schema(title = "BT传输")
    @JsonProperty("bt_port")
    val btPort: Int,
    @get:Schema(title = "任务服务端口")
    @JsonProperty("io_port")
    val ioPort: Int,
    @get:Schema(title = "数据上报端口")
    @JsonProperty("data_port")
    val dataPort: Int,
    @get:Schema(title = "")
    @JsonProperty("proc_port")
    val procPort: Int,
    @get:Schema(title = "")
    @JsonProperty("trunk_port")
    val trunkPort: Int,
    @get:Schema(title = "")
    @JsonProperty("bt_port_end")
    val btPortEnd: Int,
    @get:Schema(title = "")
    @JsonProperty("tracker_port")
    val trackerPort: Int,
    @get:Schema(title = "")
    @JsonProperty("bt_port_start")
    val btPortStart: Int,
    @get:Schema(title = "")
    @JsonProperty("db_proxy_port")
    val dbProxyPort: Int,
    @get:Schema(title = "")
    @JsonProperty("file_svr_port")
    val fileSvrPort: Int,
    @get:Schema(title = "")
    @JsonProperty("api_server_port")
    val apiServerPort: Int,
    @get:Schema(title = "")
    @JsonProperty("agent_thrift_port")
    val agentThriftPort: Int,
    @get:Schema(title = "")
    @JsonProperty("btsvr_thrift_port")
    val btsvrThriftPort: Int,
    @get:Schema(title = "")
    @JsonProperty("data_prometheus_port")
    val dataPrometheusPort: Int
)
