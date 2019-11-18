package com.tencent.devops.artifactory.service

import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.common.auth.api.AuthPermission

interface PipelineDirService {
    fun list(userId: String, projectId: String, path: String): List<FileInfo>
    fun list(userId: String, projectId: String, argPath: String, authPermission: AuthPermission): List<FileInfo>
    fun show(userId: String, projectId: String, argPath: String): FileDetail
}