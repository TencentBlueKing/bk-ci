package com.tencent.bk.codecc.defect.aop

import com.tencent.bk.codecc.defect.service.TaskLogOverviewService
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO
import com.tencent.devops.common.util.ThreadPoolUtil
import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Aspect
@Component
class EndAnalyzeAop @Autowired constructor(
        private val taskLogOverviewService: TaskLogOverviewService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(EndReportAop::class.java)
    }

    @Pointcut("@annotation(com.tencent.devops.common.web.aop.annotation.EndAnalyze)")
    fun endAnalyze() {
    }

    /**
     * 告警分析提单后的处理逻辑
     * @param uploadTaskLogStepVO 告警上报信息，从中获取任务ID和构建号
     */
    @After("endAnalyze()&&args(uploadTaskLogStepVO)")
    fun scoring(uploadTaskLogStepVO: UploadTaskLogStepVO) {
        ThreadPoolUtil.addRunnableTask {
            logger.info("after upload task log, cal task status: " +
                    "${uploadTaskLogStepVO.taskId} ${uploadTaskLogStepVO.toolName} ${uploadTaskLogStepVO.pipelineBuildId}")
            taskLogOverviewService.calTaskStatus(uploadTaskLogStepVO)
        }
    }
}
