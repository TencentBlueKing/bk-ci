package com.tencent.bk.codecc.defect.component

import com.tencent.bk.codecc.defect.pojo.AggregateDispatchFileName
import com.tencent.devops.common.script.CommandLineUtils
import org.apache.commons.exec.DefaultExecuteResultHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File

@Component
class DefectClusterComponent {

    companion object {
        private val logger = LoggerFactory.getLogger(DefectClusterComponent::class.java)
    }

    fun executeCluster(aggregateDispatchFileName: AggregateDispatchFileName): Boolean {

        return try {
            logger.info("start to execute cluster! input file: ${aggregateDispatchFileName.inputFileName}, output file ${aggregateDispatchFileName.outputFileName}")
            val result = asyncExecuteUnixCommand(
                "./pp-cluster --input ${aggregateDispatchFileName.inputFileName} --output ${aggregateDispatchFileName.outputFileName} --pretty",
                File("/opt"), null
            )
            logger.info("execute cluster finish! result : $result")
            val outputFile = File(aggregateDispatchFileName.outputFileName)
            //排除读延时因素
            var i = 0
            while (!outputFile.exists() && i < 3){
                logger.info("waiting for generating output file")
                Thread.sleep(2000L)
                i++
            }
            return outputFile.exists()
        } catch (t: Throwable) {
            logger.error("execute cluster fail! error : ${t.message}", t)
            false
        }
    }

    /**
     * 异步执行可执行文件
     */
    private fun asyncExecuteUnixCommand(
        command: String,
        sourceDir: File?,
        execResultHandler: DefaultExecuteResultHandler?
    ): String {
        try {
            return CommandLineUtils.execute(command, sourceDir, true, "", execResultHandler)
        } catch (t: Throwable) {
            logger.info("Fail to run the command because of error(${t.message})")
            throw t
        }
    }
}