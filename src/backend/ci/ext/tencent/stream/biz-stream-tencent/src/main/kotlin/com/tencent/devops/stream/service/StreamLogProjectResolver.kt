/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved. BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 */

package com.tencent.devops.stream.service

/**
 * 判断当前浏览的 Stream 项目是否有权查看该构建日志。
 *
 * 允许：流水线所属仓、事件仓、Fork/远程源仓。
 */
object StreamLogProjectResolver {

    fun canViewFromGitProject(
        viewingGitProjectId: Long,
        ownerGitProjectId: Long,
        eventGitProjectId: Long?,
        sourceGitProjectId: Long?
    ): Boolean {
        return viewingGitProjectId == ownerGitProjectId ||
            viewingGitProjectId == eventGitProjectId ||
            viewingGitProjectId == sourceGitProjectId
    }
}
