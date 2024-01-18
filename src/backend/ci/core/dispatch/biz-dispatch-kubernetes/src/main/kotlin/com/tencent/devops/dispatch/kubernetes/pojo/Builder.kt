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

package com.tencent.devops.dispatch.kubernetes.pojo

/**
 * 创建/启动构建机参数
 * @param name 构建机名称
 * @param image 镜像（镜像名:版本)
 * @param registry docker凭据
 * @param resource kubernetes 需要的资源
 * @param env 构建机环境变量
 * @param command 构建机启动命令
 * @param nfs Nfs配置
 * @param specialBuilder 特殊构建集群配置
 * @param privateBuilder 私有构建集群配置
 */
data class Builder(
    val name: String,
    val image: String,
    val registry: KubernetesDockerRegistry?,
    val resource: KubernetesResource,
    val env: Map<String, String>?,
    val command: List<String>?,
    val nfs: List<NfsConfig>?,
    val specialBuilder: SpecialBuilderConfig?,
    val privateBuilder: SpecialBuilderConfig?
)

/**
 * 一些特殊构建机调度的配置
 */
data class SpecialBuilderConfig(
    val name: String
)
