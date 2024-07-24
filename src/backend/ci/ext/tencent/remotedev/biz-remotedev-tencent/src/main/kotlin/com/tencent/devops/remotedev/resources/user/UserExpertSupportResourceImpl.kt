package com.tencent.devops.remotedev.resources.user

import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.user.UserExpertSupportResource
import com.tencent.devops.remotedev.pojo.expert.CreateSupportData
import com.tencent.devops.remotedev.pojo.expert.ExpandDiskTaskDetail
import com.tencent.devops.remotedev.pojo.expert.ExpandDiskValidateResp
import com.tencent.devops.remotedev.pojo.expert.ExpertSupportConfigType
import com.tencent.devops.remotedev.service.expert.ExpertSupportService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserExpertSupportResourceImpl @Autowired constructor(
    private val expertSupportService: ExpertSupportService
) : UserExpertSupportResource {
    override fun addExpertSup(userId: String, data: CreateSupportData): Result<Boolean> {
        expertSupportService.createSupport(userId, data)
        return Result(true)
    }

    override fun fetchExpertSup(userId: String): Result<List<String>> {
        return Result(expertSupportService.fetchSupportConfig(ExpertSupportConfigType.ERROR).map { it.content })
    }

    @AuditEntry(actionId = ActionId.CGS_EXPAND_DISK)
    override fun expandDisk(userId: String, workspaceName: String, size: String): Result<ExpandDiskValidateResp?> {
        val data = expertSupportService.expandDisk(workspaceName, userId, size) ?: return Result(null)
        return Result(
            ExpandDiskValidateResp(
                valid = data.valid,
                message = data.message
            )
        )
    }

    override fun expandDiskDetail(userId: String, workspaceName: String): Result<ExpandDiskTaskDetail?> {
        return Result(expertSupportService.expandDiskDetail(workspaceName))
    }
}
