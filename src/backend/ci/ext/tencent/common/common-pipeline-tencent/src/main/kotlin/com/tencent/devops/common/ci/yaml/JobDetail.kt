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

package com.tencent.devops.common.ci.yaml

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.tencent.devops.common.ci.image.Pool
import com.tencent.devops.common.ci.task.AbstractTask

/**
 * WARN: 请谨慎修改这个类 , 不要随意添加或者删除变量 , 否则可能导致依赖yaml的功能(gitci,prebuild等)异常
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class JobDetail(
    val name: String?,
    val displayName: String?,
    val type: String?,
    val pool: Pool?,
    val steps: List<AbstractTask>,
    val condition: String?,
    val resourceType: ResourceType?
)

/**
 * @Tip 后面的修改以下面的格式为准 , 如果不确定 , 需拉上相关开发和产品讨论
 *
 * stages:
 *   - stage:
 *       - job:
 *           resourceType: REMOTE | LOCAL
 *           pool:
 *             type: DockerOnVm | DockerOnDevCloud | DockerOnPcg | Windows | Macos | SelfHosted
 *             container: mirrors.tencent.com/tlinux2.2:latest
 *             credential:
 *               credentialId: xxx
 *               user: xxx
 *               password: xxx
 *             visualStudioVersion: 2019 | 2020
 *             agentId: xxx
 *             agentName: xxx
 *             workspace: xxx
 *             env:
 *               jdk: 1.8.0_161
 */
