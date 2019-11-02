package com.tencent.devops.store.resources.image.user

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.image.pojo.DockerRepo
import com.tencent.devops.store.api.image.user.UserImageRepoResource
import com.tencent.devops.store.service.image.ImageRepoService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserImageRepoResourceImpl @Autowired constructor(
    private val imageRepoService: ImageRepoService
) : UserImageRepoResource {

    override fun getBkRelImageInfo(userId: String, imageRepoName: String, imageId: String?): Result<DockerRepo?> {
        return imageRepoService.getBkRelImageInfo(userId, imageRepoName, imageId)
    }
}