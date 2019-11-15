package com.tencent.devops.artifactory.service

interface CustomDirGsService {
    fun getDownloadUrl(projectId: String, fileName: String, userId: String): String
}