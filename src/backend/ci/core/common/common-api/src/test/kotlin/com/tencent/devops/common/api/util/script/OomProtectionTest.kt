package com.tencent.devops.common.api.util.script

import org.apache.commons.exec.CommandLine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS

class OomProtectionTest {
    @Test
    fun disabledKeepsOriginalCommand() {
        val command = CommandLine("some-program").addArgument("a b", false)
        assertSame(command, OomProtection.wrap(command, false))
    }

    @Test
    fun wrappingKeepsArgumentsSeparate() {
        val args = arrayOf("", "a b", "quote'\"", "\$HOME", "a; touch should-not-exist", "中文")
        val command = CommandLine("/bin/echo").addArguments(args, false)
        assertEquals(
            command.toStrings().toList(),
            OomProtection.wrap(command, true).toStrings().drop(4)
        )
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    fun childAndGrandchildHaveNormalScore() {
        val original = java.io.File("/proc/self/oom_score_adj").readText()
        val command = CommandLine("/bin/sh").addArgument("-c", false)
            .addArgument("cat /proc/self/oom_score_adj; /bin/sh -c 'cat /proc/self/oom_score_adj'", false)
        val process = ProcessBuilder(*OomProtection.wrap(command, true).toStrings()).start()
        assertEquals("0\n0", process.inputStream.bufferedReader().readText().trim())
        assertEquals(0, process.waitFor())
        assertEquals(original, java.io.File("/proc/self/oom_score_adj").readText())
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    fun preservesSpecialArgumentsAndExitStatus() {
        val command = CommandLine("/bin/sh").addArgument("-c", false)
            .addArgument("printf '%s\\n' \"\$1\" \"\$2\"; exit 37", false)
            .addArguments(arrayOf("test", "a b", "\$(touch should-not-exist)"), false)
        val process = ProcessBuilder(*OomProtection.wrap(command, true).toStrings()).start()
        assertEquals("a b\n\$(touch should-not-exist)\n", process.inputStream.bufferedReader().readText())
        assertEquals(37, process.waitFor())
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    fun killedUserCommandReturnsToWorker() {
        val command = CommandLine("/bin/sh").addArgument("-c", false)
            .addArgument("kill -9 \$\$", false)
        val process = ProcessBuilder(*OomProtection.wrap(command, true).toStrings()).start()
        assertEquals(137, process.waitFor())
        // 这里只验证 SIGKILL 的退出传播，不把 137 误当成已复现内核 OOM。
    }
}
