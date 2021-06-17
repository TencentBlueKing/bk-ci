package com.tencent.bk.codecc.defect.component.abstract

import com.tencent.bk.codecc.defect.component.ScmJsonComponent
import com.tencent.bk.codecc.defect.model.BuildEntity
import com.tencent.bk.codecc.defect.model.TransferAuthorEntity
import com.tencent.bk.codecc.defect.pojo.AggregateDefectNewInputModel
import com.tencent.bk.codecc.defect.pojo.AggregateDefectOutputModelV2
import com.tencent.bk.codecc.defect.pojo.DefectClusterDTO
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.constant.CommonMessageCode
import org.slf4j.LoggerFactory

abstract class AbstractDefectCommitComponent<T>(
    private val scmJsonComponent: ScmJsonComponent
) {

    companion object{
        private val logger = LoggerFactory.getLogger(AbstractDefectCommitComponent::class.java)
    }

    /**
     * 运行聚类业务流程
     */
    abstract fun processCluster(defectClusterDTO: DefectClusterDTO)

    /**
     * 获取本次上报告警
     */
    abstract fun getCurrentDefectList(
        inputFileName: String
    ): AggregateDefectNewInputModel<T>

    /**
     * 告警清单预处理
     */
    abstract fun preHandleDefectList(
        streamName: String,
        toolName: String,
        buildId: String,
        currentDefectList: List<T>,
        preDefectList: List<T>,
        md5SameMap: MutableMap<String, Boolean>
    ): List<T>


    /**
     * 获取原有数据库中的告警清单
     */
    abstract fun getPreDefectList(
        defectClusterDTO: DefectClusterDTO,
        relPathSet: Set<String>?,
        filePathSet : Set<String>?
    ): List<T>


    /**
     * 告警清单后处理
     */
    abstract fun postHandleDefectList(
        outputDefectList: List<AggregateDefectOutputModelV2<T>>,
        buildEntity: BuildEntity?,
        transferAuthorList: List<TransferAuthorEntity.TransferAuthorPair>?
    ): List<T>

    /**
     * 获取md5映射对象
     */
    protected fun getMd5Map(streamName: String, toolName: String, buildId: String): Map<String, String> {
        val fileMD5TotalModel = scmJsonComponent.loadFileMD5(streamName, toolName, buildId)
        if (null == fileMD5TotalModel || fileMD5TotalModel.fileList.isNullOrEmpty()) {
            logger.info("md5 file is empty: ${streamName}_${toolName}_${buildId}_md5.json")
            throw CodeCCException(CommonMessageCode.SYSTEM_ERROR)
        }
        return fileMD5TotalModel.fileList.associate { it.filePath to it.md5 }
    }
}
