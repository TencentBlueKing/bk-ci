package com.tencent.devops.prebuild.resources

import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.CiYamlUtils
import com.tencent.devops.common.ci.yaml.CIBuildYaml
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentStaticInfo
import com.tencent.devops.log.model.pojo.QueryLogs
import com.tencent.devops.plugin.codecc.pojo.CodeccCallback
import com.tencent.devops.prebuild.api.UserPreBuildResource
import com.tencent.devops.prebuild.pojo.PreProject
import com.tencent.devops.prebuild.pojo.StartUpReq
import com.tencent.devops.prebuild.pojo.UserProject
import com.tencent.devops.prebuild.pojo.HistoryResponse
import com.tencent.devops.prebuild.service.PreBuildService
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserPreBuildResourceImpl @Autowired constructor(
    private val preBuildService: PreBuildService
) : UserPreBuildResource {
    override fun getUserProject(userId: String, accessToken: String): Result<UserProject> {
        return Result(preBuildService.getOrCreateUserProject(userId, accessToken))
    }

    override fun getOrCreateAgent(userId: String, os: OS, ip: String, hostName: String): Result<ThirdPartyAgentStaticInfo> {
        return Result(preBuildService.getOrCreatePreAgent(userId, os, ip, hostName))
    }

    override fun listPreProject(userId: String): Result<List<PreProject>> {
        return Result(preBuildService.listPreProject(userId))
    }

    override fun preProjectNameExist(userId: String, preProjectId: String): Result<Boolean> {
        return Result(preBuildService.projectNameExist(userId, preProjectId))
    }

    override fun manualStartup(userId: String, preProjectId: String, startUpReq: StartUpReq): Result<BuildId> {
        val yaml = try {
            val yamlStr = CiYamlUtils.formatYaml(startUpReq.yaml)
            val yamlObject = YamlUtil.getObjectMapper().readValue(yamlStr, CIBuildYaml::class.java)
            CiYamlUtils.normalizePrebuildYaml(yamlObject)
        } catch (e: Throwable) {
            logger.error("Invalid yml, error message: ", e)
            return Result(1, "Invalid yml")
        }
        val agentInfo = preBuildService.getAgent(userId, startUpReq.os, startUpReq.ip, startUpReq.hostname)
        if (null == agentInfo) {
            logger.error("Agent not install")
            return Result(2, "Agent not install")
        }

        return Result(preBuildService.startBuild(userId, preProjectId, startUpReq.workspace, startUpReq.yaml, yaml, agentInfo))
    }

    override fun manualShutdown(userId: String, accessToken: String, preProjectId: String, buildId: String): Result<Boolean> {
        return Result(preBuildService.shutDown(userId, accessToken, preProjectId, buildId))
    }

    override fun getBuildDetail(userId: String, preProjectId: String, buildId: String): Result<ModelDetail> {
        return preBuildService.getBuildDetail(userId, preProjectId, buildId)
    }

    override fun getBuildLogs(userId: String, preProjectId: String, buildId: String): Result<QueryLogs> {
        return Result(preBuildService.getInitLogs(userId, preProjectId, buildId))
    }

    override fun getAfterLogs(userId: String, preProjectId: String, buildId: String, start: Long): Result<QueryLogs> {
        return Result(preBuildService.getAfterLogs(userId, preProjectId, buildId, start))
    }

    override fun getReport(userId: String, buildId: String): Result<CodeccCallback?> {
        return preBuildService.getCodeccReport(userId, buildId)
    }

    override fun getHistory(userId: String, preProjectId: String, page: Int?, pageSize: Int?): Result<List<HistoryResponse>> {
        return Result(preBuildService.getHistory(userId, preProjectId, page, pageSize))
    }

    override fun getBuildLink(userId: String, preProjectId: String, buildId: String): Result<String> {
        return Result(preBuildService.getBuildLink(userId, preProjectId, buildId))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserPreBuildResourceImpl::class.java)
    }
}
