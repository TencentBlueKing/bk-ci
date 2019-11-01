package com.tencent.devops.process.engine.atom.task

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.element.DelayElement
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class DelayTaskAtom : IAtomTask<DelayElement> {

    override fun getParamElement(task: PipelineBuildTask): DelayElement {
        return JsonUtil.mapTo(task.taskParams, DelayElement::class.java)
    }

    override fun execute(task: PipelineBuildTask, param: DelayElement, runVariables: Map<String, String>): AtomResponse {
        logger.info("Execute delay task[$task], delay=($param.delaySeconds)")
        Thread.sleep(param.delaySeconds * 1000L)
        logger.info("Finish the delay task [$task]")
        return AtomResponse(BuildStatus.SUCCEED)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DelayTaskAtom::class.java)
    }
}
