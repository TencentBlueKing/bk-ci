package com.tencent.bk.codecc.defect.component.abstract

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.bk.codecc.defect.component.ScmJsonComponent
import com.tencent.bk.codecc.defect.model.BuildEntity
import com.tencent.bk.codecc.defect.pojo.AggregateDefectGroupModel
import com.tencent.bk.codecc.defect.pojo.AggregateDefectInputModel
import com.tencent.bk.codecc.defect.pojo.AggregateDefectOutputModel
import com.tencent.bk.codecc.defect.pojo.AggregateDispatchFileName
import com.tencent.bk.codecc.defect.pojo.FileMD5TotalModel
import com.tencent.bk.codecc.task.vo.TaskDetailVO
import com.tencent.codecc.common.db.CommonEntity
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.api.codecc.util.JsonUtil
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.common.script.CommandLineUtils
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.mq.EXCHANGE_CLUSTER_ALLOCATION_OPENSOURCE
import com.tencent.devops.common.web.mq.ROUTE_CLUSTER_ALLOCATION_OPENSOURCE
import org.apache.commons.exec.DefaultExecuteResultHandler
import org.apache.commons.exec.ExecuteException
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.AsyncRabbitTemplate
import java.io.File

abstract class AbstractDefectTracingClass<T : CommonEntity>(
    private val scmJsonComponent: ScmJsonComponent
) {

    companion object {
        private val logger = LoggerFactory.getLogger(AbstractDefectTracingClass::class.java)
    }

    /**
     * 抽象告警跟踪方法
     */
    abstract fun defectTracing(
        taskDetailVO: TaskDetailVO,
        toolName: String,
        buildEntity: BuildEntity?,
        originalFileList: List<T>,
        currentFileList: List<T>
    ): List<T>

    /**
     * 处理告警聚类
     */
    protected fun aggregateDefectByHash(
        taskDetailVO: TaskDetailVO,
        toolName: String,
        buildId: String,
        defectHashList: List<AggregateDefectInputModel>
    ): List<AggregateDefectOutputModel> {
        // 输出的分类对象
        val outputDefectList = mutableListOf<AggregateDefectOutputModel>()

        if (ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value() == taskDetailVO.createFrom && toolName != ComConstants.Tool.CCN.name) {
            val resultHandlerMap = executeClusterActionForOpenSource(
                taskDetailVO.nameEn,
                toolName,
                buildId,
                defectHashList
            )
            resultHandlerMap.forEach { (t, u) ->
                try {
                    logger.info("for each return value")
                    if (u.get()) {
                        outputDefectList.addAll(getFileContent(t) ?: mutableListOf())
                        logger.info("return value is true")
                    } else {
                        logger.info("return value is false")
                    }
                } catch (e: Exception) {
                    logger.info("async aggregate defects fail! output file : $t")
                }
            }
        } else {
            val resultHandlerMap = executeClusterActionForProd(
                taskDetailVO.nameEn,
                toolName,
                buildId,
                defectHashList
            )
            resultHandlerMap.forEach { (t, u) ->
                // 等待直到有结果
                u.waitFor()
                if (null == u.exception) {
                    outputDefectList.addAll(getFileContent(t) ?: mutableListOf())
                }
            }
        }

        return outputDefectList
    }

    /**
     * 执行聚类动作
     */
    private fun executeClusterActionForOpenSource(
        streamName: String,
        toolName: String,
        buildId: String,
        defectHashList: List<AggregateDefectInputModel>
    ): MutableMap<String, AsyncRabbitTemplate.RabbitConverterFuture<Boolean>> {
        // 将告警按文件路径聚类，最多不能超过50000个，既保证效率，又保证不会多出太多线程
        val aggregateGroupList = groupByFilePath(defectHashList)

        // 运行的future清单
        val resultHandlerMap = mutableMapOf<String, AsyncRabbitTemplate.RabbitConverterFuture<Boolean>>()

        logger.info("aggregate group list size : ${aggregateGroupList.size}")
        // 按照分组进行聚类计算
        defectHashList.groupBy {
            aggregateGroupList.indexOfFirst { aggregateDefectGroupModel ->
                aggregateDefectGroupModel.filePathList.contains((if (it.relPath.isNullOrBlank()) it.filePath else it.relPath))
            }
        }.forEach { (t, u) ->

            val inputFileName = "${streamName}_${toolName}_${buildId}_${t}_aggregate_input_data.json"
            val inputFilePath = scmJsonComponent.index(inputFileName, ScmJsonComponent.AGGREGATE)
            logger.info("aggregate inputFileName : $inputFilePath")
            val inputFile = File(inputFilePath)

            val outputFileName = "${streamName}_${toolName}_${buildId}_${t}_aggregate_output_data.json"
            val outputFilePath = scmJsonComponent.index(outputFileName, ScmJsonComponent.AGGREGATE)
            logger.info("aggregate outputFileName : $outputFilePath")
            val outputFile = File(outputFilePath)

//             val resultHandler = DefaultExecuteResultHandler()
//             resultHandlerMap[outputFileName] = resultHandler
            try {
                // 写入输入数据
                if (!inputFile.exists()) {
                    inputFile.parentFile.mkdirs()
                    inputFile.createNewFile()
                }
                if (outputFile.exists()) {
                    outputFile.delete()
                }
                inputFile.writeText(JsonUtil.getObjectMapper().writeValueAsString(u))
                scmJsonComponent.upload(inputFilePath, inputFileName, ScmJsonComponent.AGGREGATE)
                try {
                    val aggregateFileName = AggregateDispatchFileName(
                        inputFileName = inputFileName,
                        inputFilePath = inputFilePath,
                        outputFileName = outputFileName,
                        outputFilePath = outputFilePath
                    )
                    val asyncRabbitTemplate =
                        SpringContextUtil.getBean(AsyncRabbitTemplate::class.java, "opensourceAsyncRabbitTamplte")
                    val asyncMsgFuture = asyncRabbitTemplate.convertSendAndReceive<Boolean>(
                        EXCHANGE_CLUSTER_ALLOCATION_OPENSOURCE, ROUTE_CLUSTER_ALLOCATION_OPENSOURCE,
                        aggregateFileName
                    )
                    resultHandlerMap[outputFileName] = asyncMsgFuture

                    /*asyncExecuteUnixCommand(
                        "./pp-cluster --input $inputFileName --output $outputFileName --pretty",
                        File("/opt"), resultHandler
                    )*/
                } catch (e: Exception) {
                    // 如果抛异常，则不加入map中
                    logger.error("execute unix command fail! stream name : $streamName, tool name: $toolName")
//                     resultHandler.onProcessFailed(ExecuteException("execute failed!", 102, e))
                }
            } catch (e: Exception) {
                logger.error("cluster fail! number : $t", e)
//                 resultHandler.onProcessFailed(ExecuteException("execute failed!", 102, e))
            }
        }
        return resultHandlerMap
    }

    /**
     * 执行聚类动作
     */
    private fun executeClusterActionForProd(
        streamName: String,
        toolName: String,
        buildId: String,
        defectHashList: List<AggregateDefectInputModel>
    ): MutableMap<String, DefaultExecuteResultHandler> {
        // 将告警按文件路径聚类，最多不能超过50000个，既保证效率，又保证不会多出太多线程
        val aggregateGroupList = groupByFilePath(defectHashList)

        // 运行的future清单
        val resultHandlerMap = mutableMapOf<String, DefaultExecuteResultHandler>()

        logger.info("aggregate group list size : ${aggregateGroupList.size}")
        // 按照分组进行聚类计算
        defectHashList.groupBy {
            aggregateGroupList.indexOfFirst { aggregateDefectGroupModel ->
                aggregateDefectGroupModel.filePathList.contains((if (it.relPath.isNullOrBlank()) it.filePath else it.relPath))
            }
        }.forEach { (t, u) ->

            val inputFileName = "${streamName}_${toolName}_${buildId}_${t}_aggregate_input_data.json"
            val inputFilePath = scmJsonComponent.index(inputFileName, ScmJsonComponent.AGGREGATE)
            logger.info("aggregate inputFileName : $inputFilePath")
            val inputFile = File(inputFilePath)

            var outputFileName = "${streamName}_${toolName}_${buildId}_${t}_aggregate_output_data.json"
            outputFileName = scmJsonComponent.index(outputFileName, ScmJsonComponent.AGGREGATE)
            logger.info("aggregate outputFileName : $outputFileName")
            val outputFile = File(outputFileName)

            val resultHandler = DefaultExecuteResultHandler()
            resultHandlerMap[outputFileName] = resultHandler
            try {
                // 写入输入数据
                if (!inputFile.exists()) {
                    inputFile.parentFile.mkdirs()
                    inputFile.createNewFile()
                }
                if (outputFile.exists()) {
                    outputFile.delete()
                }
                inputFile.writeText(JsonUtil.getObjectMapper().writeValueAsString(u))
                scmJsonComponent.upload(inputFilePath, inputFileName, ScmJsonComponent.AGGREGATE)
                try {

                    asyncExecuteUnixCommand(
                        "./pp-cluster --input $inputFilePath --output $outputFileName --pretty",
                        File("/opt"), resultHandler
                    )
                } catch (e: Exception) {
                    // 如果抛异常，则不加入map中
                    logger.error("execute unix command fail! stream name : $streamName, tool name: $toolName")
                    resultHandler.onProcessFailed(ExecuteException("execute failed!", 102, e))
                }
            } catch (e: Exception) {
                logger.error("cluster fail! number : $t", e)
                resultHandler.onProcessFailed(ExecuteException("execute failed!", 102, e))
            }
        }
        return resultHandlerMap
    }

    /**
     * 按告警文件分组
     */
    private fun groupByFilePath(defectHashList: List<AggregateDefectInputModel>): MutableList<AggregateDefectGroupModel> {
        return defectHashList.groupingBy { (if (it.relPath.isNullOrBlank()) it.filePath else it.relPath) }.eachCount()
            .entries.fold(
                mutableListOf(
                    AggregateDefectGroupModel(0, mutableListOf())
                )
            ) { acc, entry ->
                // 如果大于30000，则要重新加一个分组
                if (acc.last().count + entry.value > 30000) {
                    acc.add(AggregateDefectGroupModel(entry.value, mutableListOf(entry.key)))
                    acc
                }
                // 如果小于30000，则在原来分组中处理
                else {
                    if (!(entry.key.isBlank())) {
                        acc.last().filePathList.add(entry.key)
                        acc.last().count += entry.value
                    }
                    acc
                }
            }
    }

    /**
     * 通用读数据操作
     */
    private inline fun <reified E> getFileContent(fileName: String): E? {
        val file = File(fileName)
        if (!file.exists())
            return null
        val fileStr = file.readText()
        return JsonUtil.getObjectMapper().readValue(fileStr, object : TypeReference<E>() {})
    }

    /**
     * 获取文件md5值
     */
    protected fun getFileMD5(streamName: String, toolName: String, buildId: String): FileMD5TotalModel {
        return getFileContent(scmJsonComponent.getFileMD5Index(streamName, toolName, buildId))
            ?: throw CodeCCException(CommonMessageCode.SYSTEM_ERROR)
    }

    /**
     * 异步执行可执行文件
     */
    private fun asyncExecuteUnixCommand(
        command: String,
        sourceDir: File?,
        execResultHandler: DefaultExecuteResultHandler
    ): String {
        try {
            return CommandLineUtils.execute(command, sourceDir, true, "", execResultHandler)
        } catch (e: Exception) {
            logger.info("Fail to run the command because of error(${e.message})")
            throw e
        }
    }
}
