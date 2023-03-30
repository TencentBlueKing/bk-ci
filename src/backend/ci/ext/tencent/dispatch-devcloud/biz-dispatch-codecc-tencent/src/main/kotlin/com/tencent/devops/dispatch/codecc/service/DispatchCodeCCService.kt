package com.tencent.devops.dispatch.codecc.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.dispatch.codecc.pojo.codecc.DockerHostBuildInfo
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DispatchCodeCCService @Autowired constructor(
    private val rabbitTemplate: RabbitTemplate,
    private val buildLogPrinter: BuildLogPrinter
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DispatchCodeCCService::class.java)
    }

    /**
     * 获取任务构建信息
     */
    fun fetchBuildInfo(hostTag: String): Result<DockerHostBuildInfo>? {
        return Result(1, "no task in queue")
    }

    /**
     * 回滚构建信息
     */
    fun rollbackBuild(buildId: String, vmSeqId: Int, shutdown: Boolean?): Result<Boolean>? {
        return Result(0, "success", true)
    }

    /**
     * 完成构建信息
     */
    fun endBuild(hostTag: String): Result<DockerHostBuildInfo>? {
        return Result(1, "no task to end")
    }

    /**
     * 上报容器id
     */
    fun reportContainerId(
        buildId: String,
        vmSeqId: String,
        containerId: String,
        hostTag: String?
    ): Result<Boolean> {
        return Result(true)
    }

    fun log(buildId: String, red: Boolean, message: String, tag: String? = "", jobId: String? = "") {
        logger.info("write log from docker host, buildId: $buildId, msg: $message, tag: $tag, jobId= $jobId")
        if (red) {
            buildLogPrinter.addRedLine(buildId, message, tag ?: "", jobId ?: "", 1)
        } else {
            buildLogPrinter.addLine(buildId, message, tag ?: "", jobId ?: "", 1)
        }
    }
}
