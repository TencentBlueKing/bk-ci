package com.tencent.devops.stream.resources.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.stream.api.op.OpGitCIBasicSettingResource
import com.tencent.devops.stream.pojo.StreamBasicSetting
import com.tencent.devops.stream.v2.service.TXStreamBasicSettingService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpGitCIBasicSettingResourceImpl @Autowired constructor(
    private val TXStreamBasicSettingService: TXStreamBasicSettingService
) : OpGitCIBasicSettingResource {

    override fun save(userId: String, gitCIBasicSetting: StreamBasicSetting): Result<Boolean> {
        return Result(TXStreamBasicSettingService.saveStreamConf(userId, gitCIBasicSetting))
    }

    override fun fixProjectInfo(): Result<Int> {
        return Result(TXStreamBasicSettingService.fixProjectInfo())
    }

    override fun updateBasicSetting(
        gitProjectId: Long,
        enableCommitCheck: Boolean?,
        enableMrComment: Boolean?
    ): Result<Boolean> {
        return Result(
            TXStreamBasicSettingService.updateProjectSetting(
                gitProjectId = gitProjectId,
                enableCommitCheck = enableCommitCheck,
                enableMrComment = enableMrComment
            )
        )
    }

    override fun fixProjectNameSpace(): Result<Int> {
        return Result(TXStreamBasicSettingService.fixProjectNameSpace())
    }
}
