package com.tencent.devops.remotedev.resources.user

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.user.UserExpertSupportResource
import com.tencent.devops.remotedev.pojo.expertSupport.CreateSupportData
import com.tencent.devops.remotedev.service.expertSupport.ExpertSupportService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserExpertSupportResourceImpl @Autowired constructor(
    private val expertSupportService: ExpertSupportService
) : UserExpertSupportResource {
    override fun addExpertSup(userId: String, data: CreateSupportData): Result<Boolean> {
        expertSupportService.createSupport(data)
        return Result(true)
    }

//    override fun fetchExpertSup(
//        userId: String,
//        projectId: String,
//        hostIp: String,
//        status: ExpertSupportStatus?
//    ): Result<List<FetchSupportResp>> {
//        return Result(
//            expertSupportService.fetchSupport(
//                projectId = projectId,
//                hostIp = hostIp,
//                status = status ?: ExpertSupportStatus.CREATE
//            )
//        )
//    }
}