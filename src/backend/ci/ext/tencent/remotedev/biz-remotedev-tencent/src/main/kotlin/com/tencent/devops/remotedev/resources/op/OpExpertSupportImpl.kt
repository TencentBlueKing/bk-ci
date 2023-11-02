package com.tencent.devops.remotedev.resources.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.op.OpExpertSupport
import com.tencent.devops.remotedev.pojo.expertSupport.CreateExpertSupportConfigData
import com.tencent.devops.remotedev.pojo.expertSupport.ExpertSupportConfigType
import com.tencent.devops.remotedev.pojo.expertSupport.FetchExpertSupResp
import com.tencent.devops.remotedev.service.expertSupport.ExpertSupportService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpExpertSupportImpl @Autowired constructor(
    private val expertSupportService: ExpertSupportService
) : OpExpertSupport {
    override fun fetchSupportConfig(userId: String, type: ExpertSupportConfigType): Result<List<FetchExpertSupResp>> {
        return Result(expertSupportService.fetchSupportConfig(type))
    }

    override fun addSupConfig(userId: String, data: CreateExpertSupportConfigData): Result<Boolean> {
        expertSupportService.addSupportConfig(data)
        return Result(true)
    }

    override fun deleteConfig(userId: String, id: Long) {
        expertSupportService.deleteSupportConfig(id)
    }
}