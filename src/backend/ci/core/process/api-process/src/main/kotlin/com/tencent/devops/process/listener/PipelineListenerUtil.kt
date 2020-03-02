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

    val pipelineHardDeleteListeners: MutableList<PipelineHardDeleteListener> = mutableListOf()

    init {
        val applicationContext = SpringContextUtil.getApplicationCtx()!!
        val beanNames = applicationContext.beanDefinitionNames
        beanNames.forEach { beanName ->
            val beanInstance = applicationContext.getBean(beanName)
            logger.info("check $beanName")
            if (beanInstance is PipelineHardDeleteListener) {
                logger.info("add listener:$beanName")
                pipelineHardDeleteListeners.add(beanInstance)
            }
        }
        logger.info("There are ${pipelineHardDeleteListeners.size} pipelineHardDeleteListeners")
    }
}
