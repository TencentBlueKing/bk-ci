/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.dispatch.docker.common

object Constants {
    /**
     * Redis Key
     */
    const val DOCKER_IP_COUNT_KEY_PREFIX = "dispatch_docker_ip_count_"

    /**
     * Docker构建高配资源白名单Key
     */
    const val DOCKER_RESOURCE_WHITE_LIST_KEY_PREFIX = "docker_resource_white_list_"

    /**
     * 无编译环境新方案白名单Key
     */
    const val BUILD_LESS_WHITE_LIST_KEY_PREFIX = "dispatchdocker:buildless_whitelist"

    /**
     * 拉代码优化工蜂项目ID白名单Key
     */
    const val QPC_WHITE_LIST_KEY_PREFIX = "dispatchdocker:qpc_white_list"

    const val DOCKERHOST_STARTUP_URI = "/api/docker/build/start"
    const val DOCKERHOST_END_URI = "/api/docker/build/end"
    const val BUILD_LESS_STARTUP_URI = "/api/service/build/start"
    const val BUILD_LESS_END_URI = "/api/service/build/end"
    const val K8S_BUILD_LESS_STARTUP_URI = "/api/buildless/build/start"
    const val K8S_BUILD_LESS_END_URI = "/api/buildless/build/end"
}
