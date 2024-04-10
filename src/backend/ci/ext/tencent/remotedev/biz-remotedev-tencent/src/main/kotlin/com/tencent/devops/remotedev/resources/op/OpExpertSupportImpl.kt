package com.tencent.devops.remotedev.resources.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.op.OpExpertSupport
import com.tencent.devops.remotedev.pojo.expert.CreateExpertSupportConfigData
import com.tencent.devops.remotedev.pojo.expert.ExpertSupportConfigType
import com.tencent.devops.remotedev.pojo.expert.FetchExpertSupResp
import com.tencent.devops.remotedev.pojo.expert.UpdateSupportData
import com.tencent.devops.remotedev.service.expert.ExpertSupportService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpExpertSupportImpl @Autowired constructor(
    private val expertSupportService: ExpertSupportService
) : OpExpertSupport {
    override fun updateExpertSup(userId: String, data: UpdateSupportData): Result<Boolean> {
        expertSupportService.updateSupportStatus(data)
        return Result(true)
    }

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
