package com.tencent.devops.prebuild.v2.service

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.ci.task.CodeCCScanInContainerTask
import com.tencent.devops.common.ci.task.ServiceJobDevCloudInput
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.matrix.DispatchInfo
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.process.yaml.modelCreate.inner.ModelCreateEvent
import com.tencent.devops.process.yaml.modelCreate.inner.PreCIInfo
import com.tencent.devops.process.yaml.modelCreate.inner.TXInnerModelCreator
import com.tencent.devops.process.yaml.modelCreate.pojo.PreCIDispatchInfo
import com.tencent.devops.process.yaml.v2.models.Resources
import com.tencent.devops.process.yaml.v2.models.job.Job
import com.tencent.devops.process.yaml.v2.models.job.JobRunsOnType
import com.tencent.devops.process.yaml.v2.models.step.Step
import com.tencent.devops.store.api.atom.ServiceMarketAtomResource
import com.tencent.devops.store.pojo.atom.InstallAtomReq
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import javax.ws.rs.core.Response

@Primary
@Component
class PreCITXInnerModelCreatorImpl(private val preCIInfo: PreCIInfo) : TXInnerModelCreator {
    companion object {
        private val logger = LoggerFactory.getLogger(PreCITXInnerModelCreatorImpl::class.java)
        private const val REMOTE_SYNC_CODE_PLUGIN_ATOM_CODE = "syncCodeToRemote"
        private const val LOCAL_SYNC_CODE_PLUGIN_ATOM_CODE = "syncLocalCode"
    }

    private val channelCode = ChannelCode.BS

    override val marketRunTask: Boolean
        get() = false

    override val runPlugInAtomCode: String?
        get() = null

    override val runPlugInVersion: String?
        get() = null

    override val defaultImage: String
        get() = "http://mirrors.tencent.com/ci/tlinux3_ci:1.5.0"

    override fun makeCheckoutElement(
        step: Step,
        event: ModelCreateEvent,
        additionalOptions: ElementAdditionalOptions
    ): MarketBuildAtomElement {
        throw CustomException(Response.Status.BAD_REQUEST, "not support")
    }

    override fun getServiceJobDevCloudInput(
        image: String,
        imageName: String,
        imageTag: String,
        params: String
    ): ServiceJobDevCloudInput? {
        throw CustomException(Response.Status.BAD_REQUEST, "not support")
    }

    override fun getDispatchInfo(
        name: String,
        job: Job,
        projectCode: String,
        defaultImage: String,
        resources: Resources?
    ): DispatchInfo {
        return PreCIDispatchInfo(
            name = "dispatchInfo_${job.id}",
            job = job,
            projectCode = projectCode,
            defaultImage = defaultImage,
            resources = resources
        )
    }

    override fun preInstallMarketAtom(client: Client, event: ModelCreateEvent) {
    }

    override fun makeMarketBuildAtomElement(
        job: Job,
        step: Step,
        event: ModelCreateEvent,
        additionalOptions: ElementAdditionalOptions
    ): MarketBuildAtomElement? {
        val data = mutableMapOf<String, Any>()
        val atomCode = step.uses!!.split('@')[0]

        // 若是"代码同步"插件标识
        if (atomCode.equals(LOCAL_SYNC_CODE_PLUGIN_ATOM_CODE, ignoreCase = true)) {
            // 若是本地构建机，则无需进行"代码同步"插件
            if (job.runsOn.poolName == JobRunsOnType.LOCAL.type) {
                return null
            }

            // 安装"代码同步"插件
            installMarketAtom(REMOTE_SYNC_CODE_PLUGIN_ATOM_CODE)
            val input = step.with?.toMutableMap() ?: mutableMapOf()
            input["agentId"] = input["agentId"] ?: preCIInfo.agentId
            input["workspace"] = input["workspace"] ?: preCIInfo.workspace
            input["useDelete"] = input["useDelete"] ?: true
            input["syncGitRepository"] = input["syncGitRepository"] ?: false
            data["input"] = input

            return MarketBuildAtomElement(
                name = step.name ?: "同步本地代码",
                id = null,
                atomCode = "syncAgentCode",
                version = "3.*",
                data = data,
                additionalOptions = additionalOptions
            )
        } else {
            data["input"] = step.with ?: Any()
            setWhitePath(atomCode, data, job)

            return MarketBuildAtomElement(
                name = step.name ?: step.uses!!.split('@')[0],
                id = step.id,
                atomCode = step.uses!!.split('@')[0],
                version = step.uses!!.split('@')[1],
                data = data,
                additionalOptions = additionalOptions
            )
        }
    }

    /**
     * 设置白名单
     */
    private fun setWhitePath(atomCode: String, data: MutableMap<String, Any>, job: Job) {
        if (atomCode == CodeCCScanInContainerTask.atomCode && preCIInfo?.extraParam != null
        ) {
            val input = (data["input"] as Map<*, *>).toMutableMap()
            val isRunOnDocker =
                JobRunsOnType.DEV_CLOUD.type == job.runsOn.poolName || JobRunsOnType.DOCKER.type == job.runsOn.poolName
            input["path"] = getWhitePathList(isRunOnDocker)
        }
    }

    /**
     * 获取白名单列表
     */
    private fun getWhitePathList(isRunOnDocker: Boolean = false): List<String> {
        val whitePathList = mutableListOf<String>()
        val info = preCIInfo

        // idea右键扫描
        if (!(info.extraParam!!.codeccScanPath.isNullOrBlank())) {
            whitePathList.add(info.extraParam!!.codeccScanPath!!)
        }

        // push/commit前扫描的文件路径
        if (info.extraParam!!.incrementFileList != null &&
                info.extraParam!!.incrementFileList!!.isNotEmpty()
        ) {
            whitePathList.addAll(info.extraParam!!.incrementFileList!!)
        }

        // 若不是容器中执行的，则无法进行本地路径替换
        if (!isRunOnDocker) {
            return whitePathList
        }

        // 容器文件路径处理
        whitePathList.forEachIndexed { index, path ->
            val filePath = path.removePrefix(info.workspace)
            // 路径开头不匹配则不替换
            if (filePath != path) {
                // 兼容workspace可能带'/'的情况
                if (info.workspace.last() == '/') {
                    whitePathList[index] = "/data/landun/workspace/$filePath"
                } else {
                    whitePathList[index] = "/data/landun/workspace$filePath"
                }
            }
        }

        return whitePathList
    }

    /**
     * 安装插件
     */
    private fun installMarketAtom(atomCode: String) {
        val projectCodes = ArrayList<String>().apply { add(preCIInfo.projectId) }

        try {
            val request = InstallAtomReq(projectCodes, atomCode)
            val client = SpringContextUtil.getBean(Client::class.java)
            client.get(ServiceMarketAtomResource::class).installAtom(preCIInfo.userId, channelCode, request)
        } catch (e: Throwable) {
            // 可能之前安装过，继续执行不中断
            logger.error("install atom($atomCode) failed, exception:", e)
        }
    }
}