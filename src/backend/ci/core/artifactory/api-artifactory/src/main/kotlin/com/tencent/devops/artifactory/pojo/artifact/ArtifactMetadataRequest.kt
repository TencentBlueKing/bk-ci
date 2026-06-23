/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 */

package com.tencent.devops.artifactory.pojo.artifact

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 产出物元数据上报请求
 * 供插件 SDK 调用
 */
@Schema(title = "产出物元数据上报请求")
data class ArtifactMetadataRequest(
    @get:Schema(title = "流水线名称")
    val pipelineName: String? = null,

    @get:Schema(title = "构建号")
    val buildNum: Int? = null,

    @get:Schema(title = "阶段ID")
    val stageId: String? = null,

    @get:Schema(title = "构建容器ID")
    val containerId: String? = null,

    @get:Schema(title = "任务ID")
    val taskId: String? = null,

    @get:Schema(title = "执行次数")
    val executeCount: Int? = null,

    @get:Schema(title = "产出物类型：FILE/IMAGE/REPORT/PACKAGE等")
    val artifactType: String = "IMAGE",

    @get:Schema(title = "产出物名称，如文件名、镜像名")
    val artifactName: String,

    @get:Schema(title = "产出物版本，如镜像Tag、包版本")
    val artifactVersion: String? = null,

    @get:Schema(title = "产出物唯一资源标识，如文件路径、镜像完整地址")
    val artifactUri: String? = null,

    @get:Schema(title = "产出物仓库地址，如制品库地址、镜像Registry")
    val artifactRepoUrl: String? = null,

    @get:Schema(title = "产出物摘要，如sha256、镜像digest")
    val artifactDigest: String? = null,

    @get:Schema(title = "产出物大小，单位字节")
    val artifactSize: Long? = null,

    @get:Schema(title = "代码库地址")
    val codeRepoUrl: String? = null,

    @get:Schema(title = "代码提交ID")
    val commitId: String? = null,

    @get:Schema(title = "扩展元数据，JSON格式")
    val extraInfo: String? = null
)
