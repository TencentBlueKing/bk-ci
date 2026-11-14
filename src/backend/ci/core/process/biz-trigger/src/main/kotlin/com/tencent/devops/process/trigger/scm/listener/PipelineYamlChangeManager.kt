package com.tencent.devops.process.trigger.scm.listener

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineYamlChangeManager @Autowired constructor(
    private val pipelineYamlChangeListeners: List<PipelineYamlChangeListener>
) {

    fun fireChangeSuccess(context: PipelineYamlChangeContext) {
        pipelineYamlChangeListeners.forEach { listener ->
            listener.onChangeSuccess(context = context)
        }
    }

    fun fireChangeError(context: PipelineYamlChangeContext, exception: Exception) {
        pipelineYamlChangeListeners.forEach { listener ->
            listener.onChangeError(context = context, exception = exception)
        }
    }
}
