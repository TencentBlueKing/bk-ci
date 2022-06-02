package com.tencent.bk.codecc.defect.component

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.bk.codecc.defect.cluster.ClusterLintCompareProcess
import com.tencent.bk.codecc.defect.component.abstract.AbstractDefectCommitComponent
import com.tencent.bk.codecc.defect.dao.mongorepository.LintDefectV2Repository
import com.tencent.bk.codecc.defect.model.BuildEntity
import com.tencent.bk.codecc.defect.model.LintDefectV2Entity
import com.tencent.bk.codecc.defect.model.TransferAuthorEntity
import com.tencent.bk.codecc.defect.pojo.AggregateDefectNewInputModel
import com.tencent.bk.codecc.defect.pojo.AggregateDefectOutputModelV2
import com.tencent.bk.codecc.defect.pojo.DefectClusterDTO
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.api.codecc.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.CommonMessageCode
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.File

@Component("LINTDefectCommitComponent")
class LintDefectCommitComponent @Autowired constructor(
    private val lintDefectV2Repository: LintDefectV2Repository,
    private val clusterLintCompareProcess: ClusterLintCompareProcess,
    private val newLintDefectTracingComponent: NewLintDefectTracingComponent,
    scmJsonComponent: ScmJsonComponent
) : AbstractDefectCommitComponent<LintDefectV2Entity>(scmJsonComponent) {
    companion object {
        private val logger = LoggerFactory.getLogger(LintDefectCommitComponent::class.java)
    }

    @ExperimentalUnsignedTypes
    override fun processCluster(defectClusterDTO: DefectClusterDTO) {
        with(defectClusterDTO) {
            val startTime = System.currentTimeMillis()
            logger.info("[lint cluster process] cluster process begin! defect cluster info: $defectClusterDTO")
            //1. 从nfs中读取本次上报告警清单

            val aggregateDefectNewInputModel = getCurrentDefectList(inputFilePath)
            val inputDefects = aggregateDefectNewInputModel.defectList
            val relPathSet = aggregateDefectNewInputModel.relPathSet
            val filePathSet = aggregateDefectNewInputModel.filePathSet
            logger.info("[lint cluster process] current defect size: ${inputDefects.size}, rel path set size: ${relPathSet?.size}, file path set size: ${filePathSet?.size}")
            //2. 获取原有告警清单
            val preDefectList = getPreDefectList(this, relPathSet, filePathSet)
            logger.info("[lint cluster process] pre defect size: ${preDefectList.size}")
            //3.声明判断md5是否一致的映射，用于比较时简化流程
            val md5SameMap = mutableMapOf<String, Boolean>()
            //4.组装聚类入参
            val inputDefectList = preHandleDefectList(
                streamName = commitDefectVO.streamName,
                toolName = commitDefectVO.toolName,
                buildId = commitDefectVO.buildId,
                currentDefectList = inputDefects,
                preDefectList = preDefectList,
                md5SameMap = md5SameMap
            )
            logger.info("[lint cluster process] pre handle result, input defect size: ${inputDefectList.size}")
            //5. 聚类得到结果
            val outputDefectList = clusterLintCompareProcess.clusterMethod(inputDefectList, md5SameMap)
            logger.info("[lint cluster process] cluster result, output group size: ${outputDefectList.size}")
            //6. 对于聚类结果进行后处理
            val upsertDefectList = postHandleDefectList(outputDefectList, buildEntity, transferAuthorList)
            logger.info("[lint cluster process] post handle result, result defect size: ${upsertDefectList.size}")
            //7. 分批保存告警
            newLintDefectTracingComponent.saveDefectFile(
                commitDefectVO.taskId,
                commitDefectVO.toolName,
                aggregateDefectNewInputModel.filterPath,
                aggregateDefectNewInputModel.pathList,
                buildEntity,
                upsertDefectList
            )
            logger.info("[lint cluster process] post handle result, save defect file success! time cost: ${System.currentTimeMillis() - startTime}")
        }
    }

    override fun preHandleDefectList(
        streamName: String,
        toolName: String,
        buildId: String,
        currentDefectList: List<LintDefectV2Entity>,
        preDefectList: List<LintDefectV2Entity>,
        md5SameMap: MutableMap<String, Boolean>
    ): List<LintDefectV2Entity> {
        val md5Map = getMd5Map(streamName, toolName, buildId)
        val relPathMap = mutableMapOf<String, String>()
        val finalDefectList = mutableListOf<LintDefectV2Entity>()
        //1.先处理本次上报的告警
        currentDefectList.forEach {
            if (!it.relPath.isNullOrBlank()) {
                relPathMap[it.relPath] = it.filePath
            }
            it.fileMd5 = md5Map[it.filePath]
            it.entityId = ObjectId.get().toString()
            it.newDefect = true
            if (it.status == 0) {
                it.status = ComConstants.DefectStatus.NEW.value()
            }
        }
        finalDefectList.addAll(currentDefectList)
        //2.再处理原有的告警
        val processedPreDefectList = preDefectList.filter { !it.pinpointHash.isNullOrBlank() }
        processedPreDefectList.forEach {
            it.newDefect = false
            if (it.status == 0) {
                it.status = ComConstants.DefectStatus.NEW.value()
            }
            val filePath = relPathMap[it.relPath]
            if (!filePath.isNullOrBlank()) {
                it.filePath = filePath
            }
            val fileMd5 = md5Map[it.filePath]
            //判断文件的md5是否相同的映射，如果相同的话则不需要比较pinpoint哈希值了（默认为false）
            md5SameMap.compute(it.filePath) { _, u ->
                //只有当没有值或者值为真的情况下才会更新，因为默认值是false
                if (null == u || u) {
                    !fileMd5.isNullOrBlank() && fileMd5 == it.fileMd5
                } else {
                    u
                }
            }
        }
        finalDefectList.addAll(processedPreDefectList)
        logger.info("pre defect size: ${preDefectList.size}, current defect size: ${currentDefectList.size}")
        return finalDefectList
    }

    override fun getCurrentDefectList(
        inputFileName: String
    ): AggregateDefectNewInputModel<LintDefectV2Entity> {
        val inputFile = File(inputFileName)
        var i=0
        while (!inputFile.exists() && i < 3){
            Thread.sleep(10000L)
            i++
        }
        if(!inputFile.exists()){
            logger.info("input file not exists! will return null")
            throw CodeCCException(CommonMessageCode.SYSTEM_ERROR)
        }
        return JsonUtil.to(inputFile.readText(), object : TypeReference<AggregateDefectNewInputModel<LintDefectV2Entity>>() {})
    }

    override fun getPreDefectList(
        defectClusterDTO: DefectClusterDTO,
        relPathSet : Set<String>?,
        filePathSet : Set<String>?
    ): List<LintDefectV2Entity> {
        val taskId = defectClusterDTO.commitDefectVO.taskId
        val toolName = defectClusterDTO.commitDefectVO.toolName
        val finalDefects = mutableListOf<LintDefectV2Entity>()
        if (!filePathSet.isNullOrEmpty()) {
            finalDefects.addAll(
                lintDefectV2Repository.findByTaskIdAndToolNameAndFilePathIn(
                    taskId,
                    toolName,
                    filePathSet
                )
            )
        }
        if (!relPathSet.isNullOrEmpty()) {
            finalDefects.addAll(
                lintDefectV2Repository.findByTaskIdAndToolNameAndRelPathIn(
                    taskId,
                    toolName,
                    relPathSet
                )
            )
        }
        return finalDefects
    }

    override fun postHandleDefectList(
        outputDefectList: List<AggregateDefectOutputModelV2<LintDefectV2Entity>>,
        buildEntity: BuildEntity?,
        transferAuthorList: List<TransferAuthorEntity.TransferAuthorPair>?
    ): List<LintDefectV2Entity> {
        if (outputDefectList.isNullOrEmpty()) {
            logger.info("output defect list is empty! build id: ${buildEntity?.buildId}")
            return emptyList()
        }
        val upsertDefectList = mutableListOf<LintDefectV2Entity>()
        outputDefectList.forEach {
            //将每一组的告警分为新告警和老告警
            val partitionedDefects = it.defects.partition { defect -> defect.newDefect }
            val newDefects = partitionedDefects.first
            val oldDefects = partitionedDefects.second
            val groupId = UUIDUtil.generate()
            /**
             * 如果聚类分组中只有老告警
             * 1. 将状态是NEW的老告警变成已修复
             * 2. 老告警是其他状态，则不变更直接上报
             */
            if (newDefects.isNullOrEmpty()) {
                oldDefects.forEach { oldDefect ->
                    if(oldDefect.status == ComConstants.DefectStatus.NEW.value()){
                        fixDefect(oldDefect, buildEntity)
                        oldDefect.pinpointHashGroup = groupId
                        upsertDefectList.add(oldDefect)
                    }
                }
            } else {
                /**
                 * 如果聚类分组中新老告警都有
                 * 1.有对应老告警：
                 *   1.1 老告警是已修复，则变为重新打开
                 *   1.2 老告警是其他状态，则不变更状态直接上报
                 * 2.无对应老告警
                 *   2.1 告警是首次创建的告警
                 * 3.老告警列表比新告警多，部分老告警没有对应的新告警
                 *   3.1 将状态是NEW的老告警变成已修复
                 *   3.2 老告警是其他状态，则不变更直接上报
                 */
                val sortedNewDefects = newDefects.sortedBy { newDefect -> newDefect.lineNum }
                val sortedOldDefects = oldDefects.sortedBy { oldDefect -> oldDefect.lineNum }
                sortedNewDefects.forEachIndexed { index, newDefect ->
                    var selectedOldDefect = if (!sortedOldDefects.isNullOrEmpty() && sortedOldDefects.size > index) {
                        sortedOldDefects[index]
                    } else {
                        null
                    }
                    if (null != selectedOldDefect) {
                        //用新告警信息更新老告警信息
                        updateOldDefectInfo(selectedOldDefect, newDefect, transferAuthorList)
                        if (selectedOldDefect.status and ComConstants.DefectStatus.FIXED.value() > 0) {
                            reopenDefect(selectedOldDefect)
                        }
                        if (selectedOldDefect.author.isNullOrBlank()) {
                            selectedOldDefect.author = newDefect.author
                        }
                    } else {
                        selectedOldDefect = newDefect
                        selectedOldDefect.createTime = System.currentTimeMillis()
                        selectedOldDefect.status = ComConstants.DefectStatus.NEW.value()
                        if (null != buildEntity) {
                            selectedOldDefect.createBuildNumber = buildEntity.buildNo
                        }
                        //作者转换
                        transferAuthor(selectedOldDefect, transferAuthorList)
                    }
                    selectedOldDefect.pinpointHashGroup = groupId
                    upsertDefectList.add(selectedOldDefect)
                }
                //老告警比新告警多出来的那部分告警变成已修复
                if (!oldDefects.isNullOrEmpty() && oldDefects.size > newDefects.size) {
                    val needToCloseDefects = oldDefects.subList(newDefects.size, oldDefects.size)
                    needToCloseDefects.forEach { closeDefect ->
                        if (closeDefect.status == ComConstants.DefectStatus.NEW.value()) {
                            fixDefect(closeDefect, buildEntity)
                            closeDefect.pinpointHashGroup = groupId
                            upsertDefectList.add(closeDefect)
                        }
                    }
                }
            }
        }
        return upsertDefectList
    }

    /**
     * 将告警设置为已修复
     *
     * @param defect
     * @param buildEntity
     */
    private fun fixDefect(
        defect: LintDefectV2Entity,
        buildEntity: BuildEntity?
    ) {
        defect.status = defect.status or ComConstants.DefectStatus.FIXED.value()
        defect.fixedTime = System.currentTimeMillis()
        if (null != buildEntity) {
            defect.fixedBuildNumber = buildEntity.buildNo
        }
    }

    /**
     * 更新老告警信息
     */
    private fun updateOldDefectInfo(
        selectOldDefect: LintDefectV2Entity, newDefect: LintDefectV2Entity,
        transferAuthorList: List<TransferAuthorEntity.TransferAuthorPair>?
    ) {
        with(selectOldDefect) {
            checker = newDefect.checker
            lineNum = newDefect.lineNum
            message = newDefect.message
            pinpointHash = newDefect.pinpointHash
            lineUpdateTime = newDefect.lineUpdateTime
            filePath = newDefect.filePath
            relPath = newDefect.relPath
            url = newDefect.url
            repoId = newDefect.repoId
            revision = newDefect.revision
            branch = newDefect.branch
            subModule = newDefect.subModule
            fileUpdateTime = newDefect.fileUpdateTime
            fileMd5 = newDefect.fileMd5
            if (author.isNullOrBlank()) {
                author = newDefect.author
                transferAuthor(this, transferAuthorList)
            }
        }
    }

    /**
     * 作者转换
     */
    private fun transferAuthor(
        selectOldDefect: LintDefectV2Entity,
        transferAuthorList: List<TransferAuthorEntity.TransferAuthorPair>?
    ) {
        transferAuthorList?.forEach {
            if (selectOldDefect.author == it.sourceAuthor) {
                selectOldDefect.author = it.targetAuthor
            }
        }
    }

    /**
     * 重新打开告警
     */
    private fun reopenDefect(oldDefect: LintDefectV2Entity) {
        oldDefect.status = ComConstants.DefectStatus.NEW.value()
        oldDefect.fixedTime = null
        oldDefect.fixedBuildNumber = null
    }
}
