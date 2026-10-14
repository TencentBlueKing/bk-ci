/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved. BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 */

package com.tencent.devops.stream.service

/**
 * 查询 log 时使用的真实归属，不能使用前端路由上的 projectId。
 */
data class StreamLogQueryTarget(
    val projectId: String,
    val pipelineId: String
)
