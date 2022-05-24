package com.tencent.bk.codecc.defect.component

import com.tencent.bk.codecc.defect.cluster.ClusterCompareProcess
import com.tencent.bk.codecc.defect.component.abstract.AbstractDefectCommitComponent
import com.tencent.bk.codecc.defect.pojo.AggregateDispatchFileName
import com.tencent.bk.codecc.defect.pojo.DefectClusterDTO
import com.tencent.devops.common.service.ToolMetaCacheService
import com.tencent.devops.common.service.utils.SpringContextUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.File

@Component
class DefectClusterComponent @Autowired constructor(
    private val toolMetaCacheService: ToolMetaCacheService,
    private val scmJsonComponent: ScmJsonComponent
){

    companion object {
        private val logger = LoggerFactory.getLogger(DefectClusterComponent::class.java)
    }

    @ExperimentalUnsignedTypes
    fun executeCluster(aggregateDispatchFileName: AggregateDispatchFileName): Boolean {

        return try {
            logger.info("start to execute cluster! input file: ${aggregateDispatchFileName.inputFileName}, " +
                    "output file ${aggregateDispatchFileName.outputFileName}")
            scmJsonComponent.getFileIndex(aggregateDispatchFileName.inputFileName,ScmJsonComponent.AGGREGATE)
            ClusterCompareProcess.clusterMethod(aggregateDispatchFileName)
            val outputFile = File(aggregateDispatchFileName.outputFilePath)
            //排除读延时因素
            var i = 0
            while (!outputFile.exists() && i < 3){
                logger.info("waiting for generating output file")
                Thread.sleep(2000L)
                i++
            }
            if(outputFile.exists()){
                scmJsonComponent.upload(
                    aggregateDispatchFileName.outputFilePath,
                    aggregateDispatchFileName.outputFileName, ScmJsonComponent.AGGREGATE
                )
                return true
            }
            return false
        } catch (t: Throwable) {
            logger.error("execute cluster fail! error : ${t.message}", t)
            false
        }
    }


    @ExperimentalUnsignedTypes
    fun executeClusterNew(defectClusterDTO : DefectClusterDTO) : Boolean{
        return try{
            with(defectClusterDTO){
                //判断文件是否存在
                scmJsonComponent.getFileIndex(inputFileName, ScmJsonComponent.AGGREGATE)
                val toolPattern = toolMetaCacheService.getToolPattern(commitDefectVO.toolName)
                val processComponent = SpringContextUtil.getBean(AbstractDefectCommitComponent::class.java,
                    "${toolPattern}DefectCommitComponent")
                processComponent.processCluster(defectClusterDTO)
            }
            true
        } catch (t : Throwable){
            logger.info("cluster and save defect fail! input file path: ${defectClusterDTO.inputFilePath}, " +
                    "error message: ${t.message}")
            false
        }

    }

}
