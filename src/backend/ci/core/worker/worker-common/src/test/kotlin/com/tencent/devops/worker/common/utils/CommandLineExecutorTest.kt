package com.tencent.devops.worker.common.utils

import java.io.File
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.LogOutputStream
import org.apache.commons.exec.PumpStreamHandler
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test

class CommandLineExecutorTest {

    @Test
    fun launchMergesStderrInWriteOrder() {
        Assumptions.assumeFalse(
            System.getProperty("os.name").orEmpty().lowercase().contains("windows"),
            "合流动序依赖 POSIX shell"
        )
        val collected = StringBuilder()
        val outputStream = object : LogOutputStream() {
            override fun processLine(line: String?, level: Int) {
                if (line != null) {
                    collected.append(line).append('\n')
                }
            }
        }
        val executor = CommandLineExecutor()
        executor.workingDirectory = File(System.getProperty("java.io.tmpdir"))
        executor.streamHandler = PumpStreamHandler(outputStream, null)
        val command = CommandLine("/bin/sh")
        command.addArgument("-c", false)
        command.addArgument("echo out1; echo err1 >&2; echo out2", false)

        Assertions.assertEquals(0, executor.execute(command))
        Assertions.assertEquals("out1\nerr1\nout2\n", collected.toString())
    }
}
