package com.tencent.devops.common.api.util.script

import com.tencent.devops.common.api.pojo.CommonEnv
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.charset.Charset

object BatScriptUtil {
    private val logger = LoggerFactory.getLogger(BatScriptUtil::class.java)

    fun executeEnhance(
        script: String,
        runtimeVariables: Map<String, String>,
        dir: File? = null
    ): String {
        val enhanceScript = CommandLineUtils.solveSpecialChar(script)
        return execute(enhanceScript, dir, runtimeVariables)
    }

    private fun execute(
        script: String,
        dir: File?,
        runtimeVariables: Map<String, String>
    ): String {
        try {
            val tmpDir = System.getProperty("java.io.tmpdir")
            val file = if (tmpDir.isNullOrBlank()) {
                File.createTempFile("paas_build_script_", ".bat")
            } else {
                File(tmpDir).mkdirs()
                File.createTempFile("paas_build_script_", ".bat", File(tmpDir))
            }
            file.deleteOnExit()

            val command = StringBuilder()

            command.append("@echo off")
                    .append("\r\n")
                    .append("set DEVOPS_BUILD_SCRIPT_FILE=${file.absolutePath}\r\n")
                    .append("\r\n")

            runtimeVariables.plus(CommonEnv.getCommonEnv())
                    .forEach { name, value ->
                        val clean = value.replace("\"", "\\\"") // 转义
                        command.append("set $name=\"$clean\"\r\n") // 双引号防止变量值有空格而意外截断定义
                        command.append("set $name=%$name:~1,-1%\r\n") // 去除又引号，防止被程序读到有双引号的变量值
                    }

            command.append(script.replace("\n", "\r\n"))
                    .append("\r\n")
                    .append("exit")
                    .append("\r\n")

            val charset = Charset.defaultCharset()

            file.writeText(command.toString(), charset)
            return CommandLineUtils.execute("cmd.exe /C \"${file.canonicalPath}\"", dir, true)
        } catch (e: Throwable) {
            logger.warn("Fail to execute bat script", e)
            throw e
        }
    }
}
