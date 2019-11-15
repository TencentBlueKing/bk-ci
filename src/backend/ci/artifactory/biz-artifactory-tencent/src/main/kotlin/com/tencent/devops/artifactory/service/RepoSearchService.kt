package com.tencent.devops.artifactory.service

import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.artifactory.pojo.SearchProps

interface RepoSearchService {
    fun searchFileAndProperty(
        userId: String,
        projectId: String,
        searchProps: SearchProps
    ): Pair<Long, List<FileInfo>>

    fun serviceSearchFileByRegex(
        projectId: String,
        pipelineId: String,
        buildId: String,
        regexPath: String,
        customized: Boolean
    ): Pair<Long, List<FileInfo>>

    fun serviceSearchFileAndProperty(
        projectId: String,
        searchProps: List<Property>,
        customized: Boolean? = null
    ): Pair<Long, List<FileInfo>>

    fun serviceSearchFileAndPropertyByOr(
        projectId: String,
        searchProps: List<Property>,
        customized: Boolean? = null
    ): Pair<Long, List<FileInfo>>

    fun getJforgInfoByteewTime(
        page: Int,
        pageSize: Int,
        startTime: Long,
        endTime: Long
    ): List<FileInfo>
}