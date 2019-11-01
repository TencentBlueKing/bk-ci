package com.tencent.devops.process.bkjob

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.listener.pipeline.BaseListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.File

@Component
class BKJobClearTempFileListener @Autowired constructor(
    pipelineEventDispatcher: PipelineEventDispatcher
) : BaseListener<ClearJobTempFileEvent>(pipelineEventDispatcher) {

    override fun run(event: ClearJobTempFileEvent) {
        event.clearFileSet.forEach { filePath ->
            val file = File(filePath)
            if (file.exists()) {
                if (file.isDirectory) {
                    logger.info("[${event.buildId}]| delete temp dir $filePath : ${file.deleteRecursively()}")
                } else {
                    logger.info("[${event.buildId}]| delete temp file $filePath : ${file.delete()}")
                }
            }
        }
    }
}
