package com.tencent.devops.plugin.task

import com.tencent.devops.plugin.pojo.FileTaskData
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@Task
class FileTask @Autowired constructor() : BaseTask<FileTaskData> {
    override fun taskDataClass(): Class<FileTaskData> {
        return FileTaskData::class.java
    }

    override fun process(taskData: FileTaskData) {
        logger.error("FileTask fileName: ${taskData.fileName}")
        logger.error("FileTask url: ${taskData.url}")
        logger.error("FileTask process")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FileTask::class.java)
    }
}