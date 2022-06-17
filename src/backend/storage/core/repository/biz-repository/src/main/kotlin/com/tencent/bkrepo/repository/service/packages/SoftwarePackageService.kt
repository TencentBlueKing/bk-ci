package com.tencent.bkrepo.repository.service.packages

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.repository.pojo.software.ProjectPackageOverview

interface SoftwarePackageService {
    /**
     * 包搜索总览
     */
    fun packageOverview(
        repoType: RepositoryType,
        projectId: String?,
        repoName: String?,
        packageName: String?
    ): List<ProjectPackageOverview>

    /**
     * 软件源根据[queryModel]搜索包
     */
    fun searchPackage(queryModel: QueryModel): Page<MutableMap<*, *>>
}
