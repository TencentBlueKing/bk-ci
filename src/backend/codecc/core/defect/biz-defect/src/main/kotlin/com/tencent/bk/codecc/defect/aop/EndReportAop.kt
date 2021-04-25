package com.tencent.bk.codecc.defect.aop

import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerSetTaskRelationshipRepository
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetTaskRelationshipEntity
import com.tencent.bk.codecc.defect.service.AbstractCodeScoringService
import com.tencent.bk.codecc.defect.service.impl.ClusterDefectServiceImpl
import com.tencent.bk.codecc.defect.vo.CommitDefectVO
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource
import com.tencent.bk.codecc.task.vo.AnalyzeConfigInfoVO
import com.tencent.bk.codecc.task.vo.TaskDetailVO
import com.tencent.devops.common.api.BaseDataVO
import com.tencent.devops.common.api.OpenSourceCheckerSetVO
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.ComConstants.CodeLang
import com.tencent.devops.common.service.BaseDataCacheService
import com.tencent.devops.common.util.ThreadPoolUtil
import org.apache.commons.lang.StringUtils
import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import java.util.stream.Collectors

@Aspect
@Component
class EndReportAop @Autowired constructor(
        private val client: Client,
        private val applicationContext: ApplicationContext,
        private val baseDataCacheService: BaseDataCacheService,
        private val clusterDefectServiceImpl: ClusterDefectServiceImpl,
        private val checkerSetTaskRelationshipRepository: CheckerSetTaskRelationshipRepository
) {

    companion object {
        private val logger = LoggerFactory.getLogger(EndReportAop::class.java)
    }

    @Pointcut("@annotation(com.tencent.devops.common.web.aop.annotation.EndReport)")
    fun endReport() {
    }

    /**
     * 普通告警上报后代码度量计算逻辑
     * @param commitDefectVO 告警上报信息，从中获取任务ID和构建号
     */
    @After("endReport()&&args(commitDefectVO)")
    fun scoring(commitDefectVO: CommitDefectVO) {
        ThreadPoolUtil.addRunnableTask {
            logger.info(
                    "begin to scoring！taskId: {} | buildId: {} ｜ toolName: {}",
                    commitDefectVO.taskId,
                    commitDefectVO.buildId,
                    commitDefectVO.toolName
            )

            try {
                val taskDetailVO = getTaskDetail(commitDefectVO.taskId)
                val codeScoringService: AbstractCodeScoringService = applicationContext.getBean(
                        getScoringServiceName(taskDetailVO),
                        AbstractCodeScoringService::class.java)
                codeScoringService.scoring(taskDetailVO,
                        buildId = commitDefectVO.buildId,
                        toolName = commitDefectVO.toolName,
                        type = "Normal")
            } catch (e: Throwable) {
                logger.info("", e)
            }
        }
    }

    /**
     * 超快增量扫描告警上报后代码度量计算逻辑
     * @param analyzeConfigInfoVO 分析配置信息，从中获取任务ID和构建号
     */
    @After("endReport()&&args(analyzeConfigInfoVO)")
    fun scoring(analyzeConfigInfoVO: AnalyzeConfigInfoVO) {
        ThreadPoolUtil.addRunnableTask {
            logger.info(
                    "begin to scoring by fast increment！taskId: {} | buildId: {} ｜ toolName: {}",
                    analyzeConfigInfoVO.taskId,
                    analyzeConfigInfoVO.buildId,
                    analyzeConfigInfoVO.multiToolType
            )

            try {
                val taskDetailVO = getTaskDetail(analyzeConfigInfoVO.taskId)
                val codeScoringService: AbstractCodeScoringService = applicationContext.getBean(
                        getScoringServiceName(taskDetailVO),
                        AbstractCodeScoringService::class.java)
                codeScoringService.scoring(taskDetailVO,
                        buildId = analyzeConfigInfoVO.buildId,
                        toolName = analyzeConfigInfoVO.multiToolType,
                        type = "Normal")
            } catch (e: Throwable) {
                logger.info("", e)
            }
        }
    }

    /**
     * 普通告警上报后按照告警类型聚类统计信息
     * @param commitDefectVO 告警上报信息，从中获取任务ID和构建号
     */
    @After("endReport()&&args(commitDefectVO)")
    fun clusterDefect(commitDefectVO: CommitDefectVO) {
        logger.info("cluster aop")
        ThreadPoolUtil.addRunnableTask {
            clusterDefectServiceImpl.cluster(
                    taskId = commitDefectVO.taskId,
                    buildId = commitDefectVO.buildId,
                    toolName = commitDefectVO.toolName
            )
        }

    }

    /**
     * 超快增量扫描告警上报后按照告警类型聚类统计信息
     * @param analyzeConfigInfoVO 分析配置信息，从中获取任务ID和构建号
     */
    @After("endReport()&&args(analyzeConfigInfoVO)")
    fun clusterDefect(analyzeConfigInfoVO: AnalyzeConfigInfoVO) {
        logger.info("cluster aop")
        ThreadPoolUtil.addRunnableTask {
            clusterDefectServiceImpl.cluster(
                    taskId = analyzeConfigInfoVO.taskId,
                    buildId = analyzeConfigInfoVO.buildId,
                    toolName = analyzeConfigInfoVO.multiToolType
            )
        }
    }

    private fun getScoringServiceName(taskDetailVO: TaskDetailVO): String {
        val isOpenScan = isOpenScan(taskDetailVO.taskId, taskDetailVO.codeLang)
        return if (ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value()
                        .equals(taskDetailVO.createFrom, ignoreCase = true) || isOpenScan) {
            "TStandard"
        } else {
            "Custom"
        }
    }

    /**
     * 判断当前度量计算的环境是否符合开源扫描的场景
     * 规则集是否符合开源扫描规则集要求
     * 从缓存中根据当前项目语言获取相应的全量规则集信息与当前 Task 的规则集比对
     *
     * @param taskId
     * @param codeLang
     */
    private fun isOpenScan(taskId: Long, codeLang: Long): Boolean {
        val checkerSetTaskRelationshipEntityList: List<CheckerSetTaskRelationshipEntity> = checkerSetTaskRelationshipRepository.findByTaskId(taskId)
        val baseDataVOList: List<BaseDataVO> = baseDataCacheService.getLanguageBaseDataFromCache(codeLang)
        // 过滤 OTHERS 的开源规则集
        val openSourceCheckerSet = baseDataVOList.stream()
                .filter { baseDataVO: BaseDataVO -> CodeLang.OTHERS.langName() != baseDataVO.langFullKey }
                .flatMap { baseDataVO: BaseDataVO ->
                    baseDataVO.openSourceCheckerListVO.stream()
                            .filter { openSourceCheckerSetVO: OpenSourceCheckerSetVO ->
                                (openSourceCheckerSetVO.checkerSetType == null
                                        || "FULL" == openSourceCheckerSetVO.checkerSetType)
                            }.map { obj: OpenSourceCheckerSetVO -> obj.checkerSetId }
                }.collect(Collectors.toSet())
        val checkerSetIdSet = checkerSetTaskRelationshipEntityList.stream()
                .map { obj: CheckerSetTaskRelationshipEntity -> obj.checkerSetId }
                .collect(Collectors.toSet())
        return checkerSetIdSet.containsAll(openSourceCheckerSet)
    }

    private fun getTaskDetail(taskId: Long): TaskDetailVO {
        val result: Result<TaskDetailVO?> = client.get(ServiceTaskRestResource::class.java)
                .getTaskInfoById(taskId)
        if (result.isNotOk() || result.data == null) {
            logger.error("scoring fail to get project id {}", taskId)
            throw CodeCCException("scoring fail to get project id")
        }
        return result.data!!
    }
}
