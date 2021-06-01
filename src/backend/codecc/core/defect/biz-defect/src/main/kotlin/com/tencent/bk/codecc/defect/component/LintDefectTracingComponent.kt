package com.tencent.bk.codecc.defect.component

import com.tencent.bk.codecc.defect.component.abstract.AbstractDefectTracingClass
import com.tencent.bk.codecc.defect.model.BuildEntity
import com.tencent.bk.codecc.defect.model.LintDefectEntity
import com.tencent.bk.codecc.defect.model.LintFileEntity
import com.tencent.bk.codecc.defect.pojo.AggregateDefectInputModel
import com.tencent.bk.codecc.defect.pojo.AggregateDefectOutputModel
import com.tencent.bk.codecc.task.vo.TaskDetailVO
import com.tencent.devops.common.constant.ComConstants
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.util.CollectionUtils

/**
 * 告警跟踪
 */
@Deprecated(message = "已废弃", replaceWith = ReplaceWith("NewLintDefectTracingComponent.java"))
@Component
class LintDefectTracingComponent @Autowired constructor(
    scmJsonComponent: ScmJsonComponent
) :
    AbstractDefectTracingClass<LintFileEntity>(scmJsonComponent) {

    companion object {
        private val logger = LoggerFactory.getLogger(LintDefectTracingComponent::class.java)
    }

    /**
     * lint类告警跟踪获取最新告警
     */
    override fun defectTracing(
        taskDetailVO: TaskDetailVO,
        toolName: String,
        buildEntity: BuildEntity?,
        originalFileList: List<LintFileEntity>,
        currentFileList: List<LintFileEntity>
    ): List<LintFileEntity> {
        //1. 告警去重
        val defectList =
            distinctLintDefect(taskDetailVO.nameEn, toolName, buildEntity!!.buildId, originalFileList, currentFileList)
        logger.info("distinct defect list: ${defectList.size}")
        //如果代码没有变化，则返回original的，并且把entityId拷贝给current的
        if (defectList.isNullOrEmpty()) {
            val md5Map = originalFileList.associate { it.md5 to it }

            currentFileList.forEach {
                val originalFile = md5Map[it.md5]
                if (null != originalFile) {
                    val lineNumMap = originalFile.defectList.associate { lintDefectEntity ->
                        Pair(
                            lintDefectEntity.lineNum,
                            lintDefectEntity
                        )
                    }
                    it.defectList.forEach { currentDefect ->
                        val originalDefect = lineNumMap[currentDefect.lineNum]
                        if (null != originalDefect) {
                            currentDefect.defectId = originalDefect.defectId
                            currentDefect.mark = originalDefect.mark
                            currentDefect.markTime = originalDefect.markTime
                            currentDefect.fixedTime = originalDefect.fixedTime
                            currentDefect.fixedBuildNumber = originalDefect.fixedBuildNumber
                            currentDefect.createTime = originalDefect.createTime
                            currentDefect.createBuildNumber = originalDefect.createBuildNumber
                            currentDefect.ignoreAuthor = originalDefect.ignoreAuthor
                            currentDefect.ignoreReason = originalDefect.ignoreReason
                            currentDefect.ignoreReasonType = originalDefect.ignoreReasonType
                            currentDefect.ignoreTime = originalDefect.ignoreTime
                            currentDefect.author = originalDefect.author
                            currentDefect.codeComment = originalDefect.codeComment
                            currentDefect.excludeTime = originalDefect.excludeTime
                            currentDefect.defectType = originalDefect.defectType
                        }
                    }
                }
            }
            logger.info("no file changed since last scan!")
            return originalFileList
        }
        //2. 拼装入参
        val defectHashList = defectList.map {
            AggregateDefectInputModel(
                id = it.defectId,
                checkerName = it.checker,
                pinpointHash = it.pinpointHash,
                filePath = it.filePath,
                relPath = it.relPath
            )
        }
        //3. 做聚类
        val clusteredDefectList = aggregateDefectByHash(taskDetailVO, toolName, buildEntity!!.buildId, defectHashList)

        logger.info("clustered defect list: ${clusteredDefectList.size}")
        val newFilePathSet = mutableSetOf<String>()

        val clusteredHandledDefectList = handleWithOutputModel(
            buildEntity,
            newFilePathSet,
            clusteredDefectList,
            originalFileList.flatMap { it.defectList },
            currentFileList.flatMap { it.defectList })
        logger.info("current handled defect list: ${clusteredHandledDefectList.size}")
        var clusteredMd5Map =
            clusteredHandledDefectList.groupBy { (if (it.relPath.isNullOrBlank()) it.filePath else it.relPath) }
        val currentKeys = mutableListOf<String>()
        val finalFileList = mutableListOf<LintFileEntity>()
        //处理本次上报的告警文件
        handleWithCurrentFileList(
            currentFileList = currentFileList,
            originalFileList = originalFileList,
            finalFileList = finalFileList,
            newFilePathSet = newFilePathSet,
            clusteredMd5Map = clusteredMd5Map,
            currentKeys = currentKeys
        )
        clusteredMd5Map = clusteredMd5Map.minus(currentKeys)
        //处理存量的告警文件
        originalFileList.forEach {
            if (newFilePathSet.contains(if (it.relPath.isNullOrBlank()) it.filePath else it.relPath)) {
                it.status = ComConstants.DefectStatus.NEW.value()
            }
            val originalDefectList = clusteredMd5Map[if (it.relPath.isNullOrBlank()) it.filePath else it.relPath]
            if (null != originalDefectList) {
                it.defectList = originalDefectList
                finalFileList.add(it)
            }
        }

        return finalFileList
    }


    /**
     * 处理本次上报的告警文件
     */
    private fun handleWithCurrentFileList(
        currentFileList: List<LintFileEntity>,
        originalFileList: List<LintFileEntity>,
        finalFileList: MutableList<LintFileEntity>,
        newFilePathSet: MutableSet<String>,
        clusteredMd5Map: Map<String, List<LintDefectEntity>>,
        currentKeys: MutableList<String>
    ) {
        //新建文件路径map，用于判断首次上报的文件赋值创建时间
        val originalFileMap =
            originalFileList.associate { (if (it.relPath.isNullOrBlank()) it.filePath else it.relPath) to it }
        currentFileList.forEach {
            //如果有新告警，则设置为new状态，如果没有则设置为已修复
            if (newFilePathSet.contains(if (it.relPath.isNullOrBlank()) it.filePath else it.relPath)) {
                it.status = ComConstants.DefectStatus.NEW.value()
            }
            val currentDefectList = clusteredMd5Map[if (it.relPath.isNullOrBlank()) it.filePath else it.relPath]
            if (null != currentDefectList) {
                it.defectList = currentDefectList
                currentKeys.add(if (it.relPath.isNullOrBlank()) it.filePath else it.relPath)
            }
            //如果判断是新文件，则设置创建时间
            val originalRespondDefect = originalFileMap[if (it.relPath.isNullOrBlank()) it.filePath else it.relPath]
            if (null == originalRespondDefect) {
                it.createTime = System.currentTimeMillis()
            } else {
                it.createTime = originalRespondDefect.createTime
            }
            finalFileList.add(it)
        }
    }

    /**
     * 告警去重后，得到并集的告警清单
     */
    private fun distinctLintDefect(
        streamName: String,
        toolName: String,
        buildId: String,
        originalLintFile: List<LintFileEntity>,
        currentLintFile: List<LintFileEntity>
    ): List<LintDefectEntity> {
        val finalDefectFile = mutableListOf<LintDefectEntity>()
        val fileMd5Info = getFileMD5(streamName, toolName, buildId).fileList.associate { it.filePath to it.md5 }
        currentLintFile.forEach {
            if (it == null) {
                return@forEach
            }
            it.md5 = fileMd5Info[it.filePath]
            if (!it.defectList.isNullOrEmpty()) {
                it.defectList.forEach { defect ->
                    defect.fileMd5 = it.md5
                    defect.relPath = it.relPath
                    defect.filePath = it.filePath
                    defect.defectId = ObjectId().toString()
                    defect.newDefect = true
                    defect.fileRevision = it.revision
                    defect.fileRepoId = it.repoId
                    defect.fileBranch = it.branch
                    if (0 == defect.status) {
                        defect.status = ComConstants.DefectStatus.NEW.value()
                    }
                }
            }
        }

        originalLintFile.forEach {
            if (!it.defectList.isNullOrEmpty()) {
                it.defectList.forEach { defect ->
                    defect.fileMd5 = it.md5
                    defect.relPath = it.relPath
                    defect.filePath = it.filePath
                    defect.newDefect = false
                    defect.fileRevision = it.revision
                    defect.fileRepoId = it.repoId
                    defect.fileBranch = it.branch
                    //第一次告警跟踪，原有id为空需要兼容
                    if (defect.defectId.isNullOrBlank()) {
                        defect.defectId = ObjectId().toString()
                    }
                    if (0 == defect.status) {
                        defect.status = ComConstants.DefectStatus.NEW.value()
                    }
                }
            }
        }

        //todo 注意pinpointHash初始化
        val originalDefectList =
            originalLintFile.flatMap { it.defectList }.filterNot { it.pinpointHash.isNullOrBlank() }
        //todo 要确认过滤范围
        val originalFileMd5Info = originalDefectList.filterNot { it.fileMd5.isNullOrBlank() }
            .associate { "${it.fileMd5}_${it.lineNum}_${it.checker}" to it }
        //去重1：过滤掉pinpointHash为空的告警，用于兼容（原来的没有上报的告警也要进行）
        finalDefectFile.addAll(originalDefectList)
        //去重2：md5及行号一样的，直接去掉
        val currDefectList =
            currentLintFile.filterNot { it == null || it.defectList.isNullOrEmpty() }.flatMap { it.defectList }
                .filterNot {
                    originalFileMd5Info.containsKey("${it.fileMd5}_${it.lineNum}_${it.checker}") || it.pinpointHash.isNullOrBlank()
                }
        if (!CollectionUtils.isEmpty(currDefectList)) {
            finalDefectFile.addAll(currDefectList)
        }
        //没有defectId的统一赋值defectId
        /*finalDefectFile.forEach {
            if (it.defectId.isBlank())
                it.defectId = ObjectId().toString()
        }*/

        return finalDefectFile
    }

    /**
     * 整理聚类输出
     */
    private fun handleWithOutputModel(
        buildEntity: BuildEntity?,
        newFilePathSet: MutableSet<String>,
        clusteredOutput: List<AggregateDefectOutputModel>,
        originalDefectList: List<LintDefectEntity>,
        currentDefectList: List<LintDefectEntity>
    ): List<LintDefectEntity> {
        val finalNewDefectList = mutableListOf<LintDefectEntity>()
        val defectMap = mutableMapOf<String, LintDefectEntity>()
        defectMap.putAll(originalDefectList.associate { it.defectId to it })
        defectMap.putAll(currentDefectList.filterNot { it.defectId.isNullOrBlank() }.associate { it.defectId to it })
        //todo 可以复用
        val lineMd5Map = originalDefectList.filterNot { it.fileMd5.isNullOrBlank() }
            .groupBy { "${it.fileMd5}_${it.lineNum}_${it.checker}" }
        //将currentdefect的md5作为过滤条件
        val currentMd5List = currentDefectList.map { it.fileMd5 }
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
            //如果聚类中只有老的告警，如果对应去重的新告警，则不变，否则作为修复告警也上报.
            //对于老的已修复告警，分为以下情况：
            // 1.没有新告警与之对应，则持续作为已修复老告警上报(不管md5值有没有改变)
            // 2.如果有新告警与之对应，则设置为重新打开
            if (newDefectList.isNullOrEmpty()) {
                oldDefectList.forEach { oldDefect ->
                    //如果是原有的已修复的或者是处理过的告警，才上报
                    if (oldDefect.status and ComConstants.DefectStatus.FIXED.value() > 0 ||
                        oldDefect.status and ComConstants.DefectStatus.IGNORE.value() > 0 ||
                        oldDefect.status and ComConstants.DefectStatus.CHECKER_MASK.value() > 0 ||
                        oldDefect.status and ComConstants.DefectStatus.PATH_MASK.value() > 0
                    ) {
                        //如果本次上报中没有的才加入,本次去重的后续再处理
                        if (!currentMd5List.contains(oldDefect.fileMd5)) {
                            finalNewDefectList.add(oldDefect)
                        }
                    } else if (oldDefect.status and ComConstants.DefectStatus.IGNORE.value() == 0) {
                        if (!currentMd5List.contains(oldDefect.fileMd5)) {
                            //已修复的不会重新赋值，只有为待修复的状态，才会赋值为已修复
                            if (oldDefect.status == ComConstants.DefectStatus.NEW.value()) {
                                oldDefect.status = oldDefect.status or ComConstants.DefectStatus.FIXED.value()
                                if (null != buildEntity) {
                                    oldDefect.fixedBuildNumber = buildEntity.buildNo
                                }
                                oldDefect.fixedRevision = oldDefect.fileRevision
                                oldDefect.fixedRepoId = oldDefect.fileRepoId
                                oldDefect.fixedBranch = oldDefect.fileBranch
                                oldDefect.fixedTime = System.currentTimeMillis()
                            } else {
                                newFilePathSet.add(if (oldDefect.relPath.isNullOrBlank()) oldDefect.filePath else oldDefect.relPath)
                            }
                            finalNewDefectList.add(oldDefect)
                            //如果文件不变，但是是已修复告警，则也要上报
                        }
                    }
                }
//                finalNewDefectList.addAll(oldDefectList)
            } else {
                newDefectList.forEachIndexed { index, currentDefect ->
                    //如果在老告警清单里面能找到和新告警对应文件路径的，则赋值
                    val selectedOldDefect = when {
                        oldDefectList.isNullOrEmpty() -> null
                        oldDefectList.lastIndex >= index -> oldDefectList[index]
                        else -> oldDefectList.last()
                    }
                    if (null != selectedOldDefect) {
                        currentDefect.defectId = selectedOldDefect.defectId
                        currentDefect.mark = selectedOldDefect.mark
                        currentDefect.markTime = selectedOldDefect.markTime
                        currentDefect.author = selectedOldDefect.author
                        currentDefect.fixedBuildNumber = selectedOldDefect.fixedBuildNumber
                        currentDefect.fixedTime = selectedOldDefect.fixedTime
                        currentDefect.createBuildNumber = selectedOldDefect.createBuildNumber
                        currentDefect.createTime = selectedOldDefect.createTime
                        currentDefect.ignoreAuthor = selectedOldDefect.ignoreAuthor
                        currentDefect.ignoreReason = selectedOldDefect.ignoreReason
                        currentDefect.ignoreReasonType = selectedOldDefect.ignoreReasonType
                        currentDefect.ignoreTime = selectedOldDefect.ignoreTime
                        currentDefect.codeComment = selectedOldDefect.codeComment
                        currentDefect.excludeTime = selectedOldDefect.excludeTime
                        currentDefect.defectType = selectedOldDefect.defectType
                        if (((selectedOldDefect.status and ComConstants.DefectStatus.IGNORE.value()) > 0) ||
                            ((selectedOldDefect.status and ComConstants.DefectStatus.CHECKER_MASK.value()) > 0) ||
                            ((selectedOldDefect.status and ComConstants.DefectStatus.PATH_MASK.value()) > 0)
                        ) {
                            currentDefect.status = selectedOldDefect.status
                        } else {
                            currentDefect.status = ComConstants.DefectStatus.NEW.value()
                            newFilePathSet.add(if (currentDefect.relPath.isNullOrBlank()) currentDefect.filePath else currentDefect.relPath)
                        }

                        //如果没找到对应的，算作新告警上报
                    } else {
                        if (null != buildEntity) {
                            currentDefect.createBuildNumber = buildEntity.buildNo
                        }
                        currentDefect.createTime = System.currentTimeMillis()
                        currentDefect.status = ComConstants.DefectStatus.NEW.value()
                        newFilePathSet.add(if (currentDefect.relPath.isNullOrBlank()) currentDefect.filePath else currentDefect.relPath)
                    }
                }
                finalNewDefectList.addAll(newDefectList)
            }
        }
        //新告警被去重的一部分也需要赋值
        currentDefectList.forEach {
            val originalDefects = lineMd5Map["${it.fileMd5}_${it.lineNum}_${it.checker}"]
            if (!originalDefects.isNullOrEmpty()) {
                val originalDefect = originalDefects.find { singleOriginalDefect ->
                    singleOriginalDefect.message == it.message
                }
                //todo 看下要不要过滤已忽略和已修复
                if (null != originalDefect) {
                    it.defectId = originalDefect.defectId
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
                    it.defectType = originalDefect.defectType
                    //告警状态赋值原则：只要本次上报告警的，没有屏蔽或者忽略的，就要赋值新的状态
                    if (((originalDefect.status and ComConstants.DefectStatus.IGNORE.value()) == 0) &&
                        ((originalDefect.status and ComConstants.DefectStatus.CHECKER_MASK.value()) == 0) &&
                        ((originalDefect.status and ComConstants.DefectStatus.PATH_MASK.value()) == 0)
                    ) {
                        it.status = ComConstants.DefectStatus.NEW.value()
                    } else {
                        it.status = originalDefect.status
                    }
                    if (it.status and ComConstants.DefectStatus.FIXED.value() > 0) {
                        it.fixedRevision = originalDefect.fixedRevision
                        it.fixedRepoId = originalDefect.fixedRepoId
                        it.fixedBranch = originalDefect.fixedBranch
                        it.fixedTime = originalDefect.fixedTime
                    }
//                    it.status = ComConstants.DefectStatus.NEW.value()
                    if (it.status == ComConstants.DefectStatus.NEW.value()) {
                        newFilePathSet.add(if (it.relPath.isNullOrBlank()) it.filePath else it.relPath)
                    }
                    finalNewDefectList.add(it)
                }
            }
        }
        return finalNewDefectList.distinctBy { it.defectId }
    }
}