package com.tencent.devops.stream.resources.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.stream.api.op.OpGitCIBasicSettingResource
import com.tencent.devops.stream.pojo.v2.GitCIBasicSetting
import com.tencent.devops.stream.v2.service.StreamBasicSettingService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpGitCIBasicSettingResourceImpl @Autowired constructor(
    private val streamBasicSettingService: StreamBasicSettingService
) : OpGitCIBasicSettingResource {

    override fun save(userId: String, gitCIBasicSetting: GitCIBasicSetting): Result<Boolean> {
        return Result(streamBasicSettingService.saveGitCIConf(userId, gitCIBasicSetting))
    }

    override fun fixProjectInfo(): Result<Int> {
        return Result(streamBasicSettingService.fixProjectInfo())
    }

    override fun fixProjectNameSpace(): Result<Int> {
        return Result(streamBasicSettingService.fixProjectNameSpace())
    }
}
