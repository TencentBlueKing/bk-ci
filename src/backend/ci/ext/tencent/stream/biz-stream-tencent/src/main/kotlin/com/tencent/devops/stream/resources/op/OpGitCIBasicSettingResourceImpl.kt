package com.tencent.devops.stream.resources.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.stream.api.op.OpGitCIBasicSettingResource
import com.tencent.devops.stream.pojo.StreamBasicSetting
import com.tencent.devops.stream.service.TXStreamBasicSettingService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpGitCIBasicSettingResourceImpl @Autowired constructor(
    private val txStreamBasicSettingService: TXStreamBasicSettingService
) : OpGitCIBasicSettingResource {

    override fun save(userId: String, gitCIBasicSetting: StreamBasicSetting): Result<Boolean> {
        return Result(txStreamBasicSettingService.saveStreamConf(userId, gitCIBasicSetting))
    }

    override fun fixProjectInfo(): Result<Int> {
        return Result(txStreamBasicSettingService.fixProjectInfo())
    }

    override fun updateBasicSetting(
        gitProjectId: Long,
        enableCommitCheck: Boolean?,
        enableMrComment: Boolean?
    ): Result<Boolean> {
        return Result(
            txStreamBasicSettingService.updateProjectSetting(
                gitProjectId = gitProjectId,
                enableCommitCheck = enableCommitCheck,
                enableMrComment = enableMrComment
            )
        )
    }

    override fun fixProjectNameSpace(): Result<Int> {
        return Result(txStreamBasicSettingService.fixProjectNameSpace())
    }

    override fun updateEnableUserIdByNewUser(
        oldUserId: String,
        newUserId: String,
        limitNumber: Int
    ): Result<Boolean> {
        return Result(
            txStreamBasicSettingService.updateEnableUserIdByNewUser(
                oldUserId = oldUserId,
                newUserId = newUserId,
                limitNumber = limitNumber
            )
        )
    }

    override fun updateGitDomain(
        oldGitDomain: String,
        newGitDomain: String,
        limitNumber: Int
    ): Result<Boolean> {
        return Result(
            txStreamBasicSettingService.updateGitDomain(
                oldGitDomain = oldGitDomain,
                newGitDomain = newGitDomain,
                limitNumber = limitNumber
            )
        )
    }
}
