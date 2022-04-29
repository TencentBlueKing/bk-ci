package com.tencent.devops.dispatch.bcs.common

import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.process.engine.common.VMUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class LogsPrinter @Autowired constructor(
    private val buildLogPrinter: BuildLogPrinter
) {

    companion object {
        private val logger = LoggerFactory.getLogger(LogsPrinter::class.java)
    }

    fun printLogs(dispatchMessage: DispatchMessage, message: String) {
        with(dispatchMessage) {
            try {
                log(
                    buildId = buildId,
                    builderHashId = containerHashId,
                    vmSeqId = vmSeqId,
                    message = message,
                    executeCount = executeCount
                )
            } catch (e: Throwable) {
                // 日志有问题就不打日志了，不能影响正常流程
                logger.error("", e)
            }
        }
    }

    fun log(buildId: String, builderHashId: String?, vmSeqId: String, message: String, executeCount: Int?) {
        buildLogPrinter.addLine(
            buildId = buildId,
            message = message,
            tag = VMUtils.genStartVMTaskId(vmSeqId),
            jobId = builderHashId,
            executeCount = executeCount ?: 1
        )
    }

    fun logRed(buildId: String, builderHashId: String?, vmSeqId: String, message: String, executeCount: Int?) {
        buildLogPrinter.addRedLine(
            buildId = buildId,
            message = message,
            tag = VMUtils.genStartVMTaskId(vmSeqId),
            jobId = builderHashId,
            executeCount = executeCount ?: 1
        )
    }
}
