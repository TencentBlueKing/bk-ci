package com.tencent.bkrepo.repository.service.repo

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.repository.pojo.repo.RepositoryInfo

interface SoftwareRepositoryService {

    fun listRepoPage(
        projectId: String? = null,
        pageNumber: Int,
        pageSize: Int,
        name: String? = null,
        type: RepositoryType? = null
    ): Page<RepositoryInfo>

    fun listRepo(
        projectId: String? = null,
        name: String? = null,
        type: RepositoryType? = null,
        includeGeneric: Boolean
    ): List<RepositoryInfo>
}
