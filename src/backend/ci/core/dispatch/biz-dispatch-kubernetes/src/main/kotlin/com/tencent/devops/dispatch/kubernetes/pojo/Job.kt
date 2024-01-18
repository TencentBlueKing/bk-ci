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
 * 创建job需要的类型
 * @param name job名称
 * @param image 镜像（镜像名:版本)
 * @param registry 镜像仓库信息
 * @param resource kubernetes 需要的资源
 * @param env 构建机环境变量
 * @param command 构建机启动命令
 * @param podNameSelector 节点选择调度配置
 * @param activeDeadlineSeconds 当前Job最长存活时间
 * @param nfs Nfs相关配置
 */
data class Job(
    val name: String,
    val image: String,
    val registry: KubernetesDockerRegistry?,
    val resource: KubernetesResource,
    val env: Map<String, String>?,
    val command: List<String>?,
    val podNameSelector: PodNameSelector?,
    val activeDeadlineSeconds: Int? = 4800,
    val nfs: List<NfsConfig>? = null
)

/**
 * 节点名称选择相关配置
 * @param selector podName
 * @param usePodData 是否和当前节点共享数据盘
 */
data class PodNameSelector(
    val selector: String,
    val usePodData: Boolean?
)

/**
 * 构建并推送镜像Job相关配置，因为本质是个Job所以放在一起
 */
data class BuildAndPushImage(
    val name: String,
    val resource: KubernetesResource,
    val podNameSelector: PodNameSelector,
    val activeDeadlineSeconds: Int? = 4800,
    val info: BuildAndPushImageInfo
)

/**
 * 需要被构建并推送的镜像 相关信息
 * @param dockerFilePath dockerfile路径
 * @param contextPath 存放需要构建镜像的代码的上下文
 * @param destinations 推送镜像完整目标包含仓库地址和tag，例如 xxxx/xxx-hub:v1
 * @param buildArgs 构建参数
 * @param registries 推送镜像需要的凭据
 */
data class BuildAndPushImageInfo(
    val dockerFilePath: String,
    val contextPath: String,
    val destinations: List<String>,
    val buildArgs: Map<String, String>,
    val registries: List<KubernetesDockerRegistry>
)
