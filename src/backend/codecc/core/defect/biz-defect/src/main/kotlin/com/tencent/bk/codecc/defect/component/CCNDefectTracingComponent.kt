package com.tencent.bk.codecc.defect.component

import com.tencent.bk.codecc.defect.component.abstract.AbstractDefectTracingClass
import com.tencent.bk.codecc.defect.model.BuildEntity
import com.tencent.bk.codecc.defect.model.CCNDefectEntity
import com.tencent.bk.codecc.defect.pojo.AggregateDefectInputModel
import com.tencent.bk.codecc.defect.pojo.AggregateDefectOutputModel
import com.tencent.bk.codecc.task.vo.TaskDetailVO
import com.tencent.devops.common.constant.ComConstants
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.AsyncRabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Deprecated(message = "已废弃", replaceWith = ReplaceWith("NewCCNDefectTracingComponent.java"))
@Component
class CCNDefectTracingComponent @Autowired constructor(
    scmJsonComponent: ScmJsonComponent
) : AbstractDefectTracingClass<CCNDefectEntity>(scmJsonComponent) {

    companion object {
        private val logger = LoggerFactory.getLogger(CCNDefectTracingComponent::class.java)
    }

    override fun defectTracing(
        taskDetailVO: TaskDetailVO,
        toolName: String,
        buildEntity: BuildEntity?,
        originalFileList: List<CCNDefectEntity>,
        currentFileList: List<CCNDefectEntity>
    ): List<CCNDefectEntity> {
        //1. 告警去重
        val defectList =
            distinctCCNDefect(taskDetailVO.nameEn, toolName, buildEntity!!.buildId, originalFileList, currentFileList)
        logger.info("distinct defect list: ${defectList.size}")
        //如果代码没有变化，则直接返回original的，并且把defectId拷贝给currentList
        if (defectList.isNullOrEmpty()) {
            val md5Map = originalFileList.associate { it.md5 to it }
            currentFileList.forEach {
                val originalDefect = md5Map[it.md5]
                if (null != originalDefect) {
                    it.entityId = originalDefect.entityId
                    it.mark = originalDefect.mark
                    it.markTime = originalDefect.markTime
                    it.fixedBuildNumber = it.fixedBuildNumber
                    it.fixedTime = it.fixedTime
                    it.createTime = it.createTime
                    it.createBuildNumber = it.createBuildNumber
                    it.ignoreAuthor = originalDefect.ignoreAuthor
                    it.ignoreReason = originalDefect.ignoreReason
                    it.ignoreReasonType = originalDefect.ignoreReasonType
                    it.ignoreTime = originalDefect.ignoreTime
                    it.author = originalDefect.author
                    it.codeComment = originalDefect.codeComment
                    it.excludeTime = originalDefect.excludeTime
                }
            }
            return originalFileList
        }
        //2. 拼装入参
        val defectHashList = defectList.map {
            AggregateDefectInputModel(
                id = it.entityId,
                checkerName = "",
                pinpointHash = it.pinpointHash,
                filePath = it.filePath,
                relPath = it.relPath
            )
        }

        //3. 做聚类
        val clusteredDefectList = aggregateDefectByHash(taskDetailVO, toolName, buildEntity!!.buildId, defectHashList)
        logger.info("clustered defect list: ${clusteredDefectList.size}")

        return handleWithOutputModel(
            buildEntity,
            clusteredDefectList,
            originalFileList,
            currentFileList
        )
    }

    private fun distinctCCNDefect(
        streamName: String,
        toolName: String,
        buildId: String,
        originalFileList: List<CCNDefectEntity>,
        currentFileList: List<CCNDefectEntity>
    ): List<CCNDefectEntity> {
        logger.info("start to distinct ccn defect: $streamName, $toolName, $buildId")

        val finalDefectList = mutableListOf<CCNDefectEntity>()
        val fileMd5Info = getFileMD5(streamName, toolName, buildId).fileList.associate { it.filePath to it.md5 }
        //todo 确定id已经有值的
        logger.info("start to distinct ccn defect in scan current file list: $streamName, $toolName, $buildId, ${currentFileList.size}")
        currentFileList.forEach {
            if (it == null) {
                return@forEach
            }
            it.entityId = ObjectId().toString()
            it.md5 = fileMd5Info[it.filePath]
            it.newDefect = true
            if (0 == it.status) {
                it.status = ComConstants.DefectStatus.NEW.value()
            }
        }

        logger.info("start to distinct ccn defect in scan origin file list: $streamName, $toolName, $buildId, ${originalFileList.size}")
        originalFileList.forEach {
            it.newDefect = false
            if (0 == it.status) {
                it.status = ComConstants.DefectStatus.NEW.value()
            }
        }

        //过滤map
        val originalFileMd5Info = originalFileList.filterNot { it.md5.isNullOrBlank() }
            .associate { "${it.md5}_${it.functionName}_${it.startLines}" to it }
        logger.info("start to distinct ccn defect in scan origin file md5 info list: $streamName, $toolName, $buildId, ${originalFileMd5Info.size}")

        //1. pinpointhash 为空的要去掉
        finalDefectList.addAll(originalFileList.filterNot { it.pinpointHash.isNullOrBlank() })
        //2. 过滤掉文件没变的
        finalDefectList.addAll(currentFileList.filterNot {
            logger.info("start to distinct ccn defect in cur file list filter: $streamName, $toolName, $buildId, $it")
            originalFileMd5Info.containsKey("${it.md5}_${it.functionName}_${it.startLines}") || it.pinpointHash.isNullOrBlank()
        })

        return finalDefectList
    }

    /**
     * 整理聚类输出
     */
    private fun handleWithOutputModel(
        buildEntity: BuildEntity?,
        clusteredOutput: List<AggregateDefectOutputModel>,
        originalDefectList: List<CCNDefectEntity>,
        currentDefectList: List<CCNDefectEntity>
    ): List<CCNDefectEntity> {
        val finalNewDefectList = mutableListOf<CCNDefectEntity>()
        val defectMap = mutableMapOf<String, CCNDefectEntity>()
        val originalDefectMap = originalDefectList.filterNot { it.md5.isNullOrBlank() }
            .associate { "${it.md5}_${it.functionName}_${it.startLines}" to it }
        val currentDefectMap = currentDefectList.associate { it.md5 to it }
        defectMap.putAll(originalDefectList.associate { it.entityId to it })
        defectMap.putAll(currentDefectList.filterNot { it.entityId.isNullOrBlank() }.associate { it.entityId to it })
        //将聚类输出格式改为defectEntity
        val clusteredDefectList = clusteredOutput.map {
            it.defects.map { aggregateDefectInputModel ->
                defectMap[aggregateDefectInputModel.id]!!
            }
        }
        //将聚类输出分为新告警和历史告警，新告警根据是否有历史告警聚类，有的话则将历史告警的属性赋值给新告警
        clusteredDefectList.forEach {
            val partitionedDefects = it.partition { defect -> defect.newDefect }
            //遍历新告警，看是否有老告警和他对应，如果有则赋值，如果没有就取自己的
            val newDefectList = partitionedDefects.first
            val oldDefectList = partitionedDefects.second
            //如果聚类中只有老的告警，则作为修复告警也上报
            if (newDefectList.isNullOrEmpty()) {
                oldDefectList.forEach { oldDefect ->
                    if (oldDefect.status and ComConstants.DefectStatus.FIXED.value() > 0 ||
                        oldDefect.status and ComConstants.DefectStatus.IGNORE.value() > 0 ||
                        oldDefect.status and ComConstants.DefectStatus.CHECKER_MASK.value() > 0 ||
                        oldDefect.status and ComConstants.DefectStatus.PATH_MASK.value() > 0
                    ) {
                        if (!currentDefectMap.containsKey(oldDefect.md5)) {
                            finalNewDefectList.add(oldDefect)
                        }
                    } else if (oldDefect.status and ComConstants.DefectStatus.IGNORE.value() == 0) {
                        if (!currentDefectMap.containsKey(oldDefect.md5)) {
                            if (oldDefect.status == ComConstants.DefectStatus.NEW.value()) {
                                oldDefect.status = oldDefect.status or ComConstants.DefectStatus.FIXED.value()
                                if (null != buildEntity) {
                                    oldDefect.fixedBuildNumber = buildEntity.buildNo
                                }
                                oldDefect.fixedTime = System.currentTimeMillis()
                            }
                            finalNewDefectList.add(oldDefect)
                        }
                    }
                }
            } else {
                newDefectList.forEachIndexed { index, currentDefect ->
                    //如果在老告警清单里面能找到和新告警对应文件路径的，则赋值
                    val selectedOldDefect = when {
                        oldDefectList.isNullOrEmpty() -> null
                        oldDefectList.lastIndex >= index -> oldDefectList[index]
                        else -> oldDefectList.last()
                    }
                    if (null != selectedOldDefect) {
                        currentDefect.entityId = selectedOldDefect.entityId
                        currentDefect.mark = selectedOldDefect.mark
                        currentDefect.markTime = selectedOldDefect.markTime
                        currentDefect.fixedTime = selectedOldDefect.fixedTime
                        currentDefect.fixedBuildNumber = selectedOldDefect.fixedBuildNumber
                        currentDefect.createTime = selectedOldDefect.createTime
                        currentDefect.createBuildNumber = selectedOldDefect.createBuildNumber
                        currentDefect.ignoreAuthor = selectedOldDefect.ignoreAuthor
                        currentDefect.ignoreReason = selectedOldDefect.ignoreReason
                        currentDefect.ignoreReasonType = selectedOldDefect.ignoreReasonType
                        currentDefect.ignoreTime = selectedOldDefect.ignoreTime
                        currentDefect.author = selectedOldDefect.author
                        currentDefect.codeComment = selectedOldDefect.codeComment
                        currentDefect.excludeTime = selectedOldDefect.excludeTime
                        if (((selectedOldDefect.status and ComConstants.DefectStatus.IGNORE.value()) > 0) ||
                            ((selectedOldDefect.status and ComConstants.DefectStatus.CHECKER_MASK.value()) > 0) ||
                            ((selectedOldDefect.status and ComConstants.DefectStatus.PATH_MASK.value()) > 0)
                        ) {
                            currentDefect.status = selectedOldDefect.status
                        } else {
                            currentDefect.status = ComConstants.DefectStatus.NEW.value()
                        }
                        //如果没找到对应的，算作新告警上报
                    } else {
                        if (null != buildEntity) {
                            currentDefect.createBuildNumber = buildEntity.buildNo
                        }
                        currentDefect.createTime = System.currentTimeMillis()
                        currentDefect.status = ComConstants.DefectStatus.NEW.value()
                    }
                }
                finalNewDefectList.addAll(newDefectList)
            }
        }
        //新告警被过滤的一部分也需要赋值
        currentDefectList.forEach {
            val originalDefect = originalDefectMap["${it.md5}_${it.functionName}_${it.startLines}"]
            if (null != originalDefect) {
                it.entityId = originalDefect.entityId
                it.mark = originalDefect.mark
                it.markTime = originalDefect.markTime
                it.fixedBuildNumber = originalDefect.fixedBuildNumber
                it.fixedTime = originalDefect.fixedTime
                it.createBuildNumber = originalDefect.createBuildNumber
                it.createTime = originalDefect.createTime
                it.ignoreAuthor = originalDefect.ignoreAuthor
                it.ignoreReason = originalDefect.ignoreReason
                it.ignoreReasonType = originalDefect.ignoreReasonType
                it.ignoreTime = originalDefect.ignoreTime
                it.author = originalDefect.author
                it.codeComment = originalDefect.codeComment
                it.excludeTime = originalDefect.excludeTime
                //告警状态赋值原则：只要本次上报告警的，没有屏蔽或者忽略的，就要赋值新的状态
                if (((originalDefect.status and ComConstants.DefectStatus.IGNORE.value()) == 0) &&
                    ((originalDefect.status and ComConstants.DefectStatus.CHECKER_MASK.value()) == 0) &&
                    ((originalDefect.status and ComConstants.DefectStatus.PATH_MASK.value()) == 0)
                ) {
                    it.status = ComConstants.DefectStatus.NEW.value()
                } else {
                    it.status = originalDefect.status
                }
//                it.status = ComConstants.DefectStatus.NEW.value()
                finalNewDefectList.add(it)
            }
        }
        return finalNewDefectList.distinctBy { it.entityId }
//            .filterNot { it.createBuildNumber.isNullOrBlank() }.distinctBy { "${it.md5}_${it.functionName}_${it.startLines}" }
    }
}