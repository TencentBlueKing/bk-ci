package com.tencent.devops.stream.util

import org.slf4j.LoggerFactory

/**
 * 此工具用于需要处理存量数据时，打入关键log日志进行定位, 在存量数据迁移完成之后可以直接删除。
 */
object HandsomeUtil {
    private val logger = LoggerFactory.getLogger(HandsomeUtil::class.java)
    private val key2Issue8910 = listOf("ci.head_ref", "ci.base_ref", "GIT_CI_BASE_REF", "GIT_CI_HEAD_REF")

    fun issue8910(yamlStr: String, projectId: String, pipelineId: String, filePath: String) {
        for (element in key2Issue8910) {
            if (yamlStr.contains(element)) {
                logger.info("issue8910|$projectId|$pipelineId|$filePath")
                return
            }
        }
    }
}
