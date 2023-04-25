package com.tencent.devops.prebuild.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.prebuild.dao.PrebuildProjectDao
import com.tencent.devops.prebuild.pojo.StartUpReq
import com.tencent.devops.prebuild.v2.component.PipelineLayout
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.store.api.atom.ServiceMarketAtomResource
import com.tencent.devops.store.pojo.atom.InstallAtomReq
import org.jooq.DSLContext
import org.slf4j.LoggerFactory

open class CommonPreBuildService constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val prebuildProjectDao: PrebuildProjectDao
) {
    protected val channelCode = ChannelCode.BS

    companion object {
        private val logger = LoggerFactory.getLogger(CommonPreBuildService::class.java)
    }

    private fun getPipelineByName(userId: String, preProjectId: String): Pipeline? {
        try {
            val pipelineList = client.get(ServicePipelineResource::class)
                    .list(userId, getUserProjectId(userId), 1, 1000).data!!.records

            pipelineList.forEach {
                if (it.pipelineName == preProjectId) {
                    return it
                }
            }
        } catch (e: Throwable) {
            logger.error("List pipeline failed, exception:", e)
        }

        return null
    }

    protected fun getUserProjectId(userId: String): String {
        return "_$userId"
    }

    protected fun installMarketAtom(userId: String, atomCode: String) {
        val projectCodes = ArrayList<String>().apply {
            add(getUserProjectId(userId))
        }

        try {
            client.get(ServiceMarketAtomResource::class).installAtom(
                userId,
                channelCode,
                InstallAtomReq(projectCodes, atomCode)
            )
        } catch (e: Throwable) {
            logger.error("install atom($atomCode) failed, exception:", e)
            // 可能之前安装过，继续执行不退出
        }
    }

    protected fun getWhitePath(
        startUpReq: StartUpReq,
        isRunOnDocker: Boolean = false
    ): List<String> {
        val whitePath = mutableListOf<String>()
        // idea右键codecc扫描
        if (!(startUpReq.extraParam!!.codeccScanPath.isNullOrBlank())) {
            whitePath.add(startUpReq.extraParam!!.codeccScanPath!!)
        }
        // push/commit前扫描的文件路径
        if (startUpReq.extraParam!!.incrementFileList != null &&
                startUpReq.extraParam!!.incrementFileList!!.isNotEmpty()
        ) {
            whitePath.addAll(startUpReq.extraParam!!.incrementFileList!!)
        }

        // 使用容器路径替换本地路径
        if (isRunOnDocker) {
            whitePath.forEachIndexed { index, path ->
                val filePath = path.removePrefix(startUpReq.workspace)
                // 路径开头不匹配则不替换
                if (filePath != path) {
                    // 兼容workspace可能带'/'的情况
                    if (startUpReq.workspace.last() == '/') {
                        whitePath[index] = "/data/landun/workspace/$filePath"
                    } else {
                        whitePath[index] = "/data/landun/workspace$filePath"
                    }
                }
            }
        }

        return whitePath
    }

    protected fun getPipelineId(userId: String, preProjectId: String): String? {
        return getPipelineByName(userId, preProjectId)?.pipelineId
    }

    protected fun createEmptyPipeline(
        userId: String,
        pipelineName: String
    ): String {
        val projectId = getUserProjectId(userId)
        val emptyModel = PipelineLayout.Builder()
                .pipelineName(pipelineName)
                .description("From PreCI YAML 2.0")
                .creator(userId)
                .labels(emptyList())
                .stagesEmpty()
                .build()

        val pipelineId =
            client.get(ServicePipelineResource::class).create(userId, projectId, emptyModel, channelCode).data!!.id

        prebuildProjectDao.createOrUpdate(
            dslContext = dslContext,
            prebuildProjectId = pipelineName,
            projectId = projectId,
            owner = userId,
            yaml = "",
            pipelineId = pipelineId,
            workspace = "",
            ideVersion = "",
            pluginVersion = ""
        )

        return pipelineId
    }

    protected fun updatePipeline(
        userId: String,
        preProjectId: String,
        startUpReq: StartUpReq,
        model: Model,
        pipelineId: String
    ) {
        val projectId = getUserProjectId(userId)
        client.get(ServicePipelineResource::class).edit(userId, projectId, pipelineId, model, channelCode)
        prebuildProjectDao.createOrUpdate(
            dslContext = dslContext,
            prebuildProjectId = preProjectId,
            projectId = projectId,
            owner = userId,
            yaml = startUpReq.yaml.trim(),
            pipelineId = pipelineId,
            workspace = startUpReq.workspace,
            ideVersion = startUpReq.extraParam?.ideVersion,
            pluginVersion = startUpReq.extraParam?.pluginVersion
        )

        logger.info("preci projectId: $projectId, pipelineId: $pipelineId")
    }

    protected fun createOrUpdatePipeline(
        userId: String,
        preProjectId: String,
        startUpReq: StartUpReq,
        model: Model
    ): String {
        val projectId = getUserProjectId(userId)
        val pipeline = getPipelineByName(userId, preProjectId)
        val pipelineId = if (null == pipeline) {
            client.get(ServicePipelineResource::class)
                    .create(userId, projectId, model, channelCode).data!!.id
        } else {
            client.get(ServicePipelineResource::class)
                    .edit(userId, projectId, pipeline.pipelineId, model, channelCode)
            pipeline.pipelineId
        }

        prebuildProjectDao.createOrUpdate(
            dslContext = dslContext,
            prebuildProjectId = preProjectId,
            projectId = projectId,
            owner = userId,
            yaml = startUpReq.yaml.trim(),
            pipelineId = pipelineId,
            workspace = startUpReq.workspace,
            ideVersion = startUpReq.extraParam?.ideVersion,
            pluginVersion = startUpReq.extraParam?.pluginVersion
        )

        logger.info("preci projectId: $projectId, pipelineId: $pipelineId")

        return pipelineId
    }
}
