package com.tencent.devops.process.listener

import com.tencent.devops.common.service.utils.SpringContextUtil
import org.slf4j.LoggerFactory

/**
 * @Description
 * @Date 2020/3/2
 * @Version 1.0
 */
object PipelineListenerUtil {

    val logger = LoggerFactory.getLogger(PipelineListenerUtil::class.java)!!

    private val pipelineHardDeleteListeners: MutableList<PipelineHardDeleteListener> = mutableListOf()

    fun getHardDeleteListeners(): MutableList<PipelineHardDeleteListener> {
        if (pipelineHardDeleteListeners.isEmpty()) {
            synchronized(PipelineListenerUtil::class.java) {
                if (pipelineHardDeleteListeners.isEmpty()) {
                    findListeners()
                }
            }
        }
        return pipelineHardDeleteListeners
    }

    private fun findListeners() {
        pipelineHardDeleteListeners.addAll(SpringContextUtil.getBeansWithClass(PipelineHardDeleteListener::class.java))
        logger.info("There are ${pipelineHardDeleteListeners.size} pipelineHardDeleteListeners")
    }
}
