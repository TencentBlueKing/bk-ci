package com.tencent.devops.common.api.util.script

import org.slf4j.LoggerFactory
import java.io.File

object CommonScriptUtils {

    private val logger = LoggerFactory.getLogger(CommonScriptUtils::class.java)

    fun execute(
            script: String,
            dir: File? = null,
            runtimeVariables: Map<String, String> = mapOf()
    ): String {
        logger.info("execute script: ${SensitiveLineParser.onParseLine(script)}")
        val isWindows = System.getProperty("os.name").startsWith("Windows", true)
        return if (isWindows) BatScriptUtil.executeEnhance(script, runtimeVariables, dir)
        else ShellUtil.executeEnhance(script, runtimeVariables, dir)
    }
}
