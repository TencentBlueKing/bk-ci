package com.tencent.devops.artifactory.resources

import com.tencent.devops.artifactory.api.user.UserCustomRepoResource
import com.tencent.devops.artifactory.pojo.DirNode
import com.tencent.devops.artifactory.service.CustomRepoService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource

@RestResource
class UserCustomRepoResourceImpl(
    private val customRepoService: CustomRepoService
) : UserCustomRepoResource {
    override fun dirTree(userId: String, projectId: String, path: String?, name: String?): Result<DirNode> {
        return Result(customRepoService.dirTree(userId, projectId, path, name))
    }
}