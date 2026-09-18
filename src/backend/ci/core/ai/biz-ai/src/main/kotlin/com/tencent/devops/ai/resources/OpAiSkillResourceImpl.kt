package com.tencent.devops.ai.resources

import com.tencent.devops.ai.api.op.OpAiSkillResource
import com.tencent.devops.ai.pojo.AiSkillCreate
import com.tencent.devops.ai.pojo.AiSkillInfo
import com.tencent.devops.ai.pojo.AiSkillUpdate
import com.tencent.devops.ai.service.AiSkillService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpAiSkillResourceImpl @Autowired constructor(
    private val skillService: AiSkillService
) : OpAiSkillResource {
    override fun list(): Result<List<AiSkillInfo>> = Result(skillService.listAllForOp())
    override fun create(request: AiSkillCreate): Result<AiSkillInfo> = Result(skillService.createSystem(request))
    override fun update(skillId: String, request: AiSkillUpdate): Result<Boolean> =
        Result(skillService.updateForOp(skillId, request))
    override fun delete(skillId: String): Result<Boolean> = Result(skillService.deleteForOp(skillId))
}
