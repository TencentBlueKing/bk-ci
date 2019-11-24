package com.tencent.devops.prebuild.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.prebuild.pojo.ide.IdeDirInfo
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentStaticInfo
import com.tencent.devops.prebuild.api.WebIDEResource
import com.tencent.devops.prebuild.pojo.IDEAgentReq
import com.tencent.devops.prebuild.pojo.IDEInfo
import com.tencent.devops.prebuild.service.WebIDEService
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.project.pojo.ProjectVO
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class WebIDEResourceImpl @Autowired constructor(private val webIDEService: WebIDEService) : WebIDEResource {
    override fun updateLastOpenDir(userId: String, ip: String, path: String): Result<Boolean> {
        return Result(webIDEService.updateLastOpenDir(userId, ip, path))
    }

    override fun lastOpenDir(userId: String, ip: String): Result<IdeDirInfo> {
        return Result(webIDEService.lastOpenDir(userId, ip))
    }

    override fun heartBeat(userId: String, ip: String): Result<Boolean> {
        return Result(webIDEService.heartBeat(userId, ip))
    }

    override fun getUserProject(userId: String, accessToken: String): Result<ProjectVO?> {
        return Result(webIDEService.getUserProject(userId, accessToken))
    }

    override fun getAgentInstallLink(userId: String, projectId: String, zoneName: String, operationSystem: String, initIp: String): Result<ThirdPartyAgentStaticInfo> {
        return Result(webIDEService.getAgentInstallLink(userId, projectId, operationSystem, zoneName, initIp))
    }

    override fun getUserIDEList(userId: String, projectId: String): Result<List<IDEInfo>> {
        return Result(webIDEService.getUserIDEInfo(userId, projectId))
    }

    override fun setupAgent(userId: String, req: IDEAgentReq): Result<BuildId> {
        return Result(webIDEService.setupAgent(userId, req.projectId, req.ip))
    }
}