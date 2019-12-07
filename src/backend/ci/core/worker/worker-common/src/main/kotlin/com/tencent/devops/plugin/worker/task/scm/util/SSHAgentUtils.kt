/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.plugin.worker.task.scm.util

import com.tencent.devops.common.log.Ansi
import com.tencent.devops.worker.common.CommonEnv
import com.tencent.devops.worker.common.logger.LoggerService
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecutor
import org.apache.commons.exec.ExecuteWatchdog
import org.apache.commons.exec.LogOutputStream
import org.apache.commons.exec.PumpStreamHandler
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.util.HashMap
import java.util.concurrent.TimeUnit

class SSHAgentUtils constructor(private val privateKey: String, private val passPhrase: String?) {

    companion object {
        private const val AUTH_SOCKET_VAR = "SSH_AUTH_SOCK"
        private const val AGENT_PID_VAR = "SSH_AGENT_PID"
        private val logger = LoggerFactory.getLogger(SSHAgentUtils::class.java)
    }

    fun addIdentity() {
        try {
            val agentEnv = parseAgentEnv(executeCommand("ssh-agent", null))
            // 600
            val perms = PosixFilePermissions.fromString("rw-------")
            val fileAttributes = PosixFilePermissions
                .asFileAttribute(perms)

            val keyFile = Files.createTempFile("private_key_", ".key", fileAttributes).toFile()
            keyFile.writeText(privateKey)
            try {

                val askpass = if (passPhrase != null) createAskpassScript() else null
                try {
                    val env = HashMap(agentEnv)
                    if (passPhrase != null) {
                        env.put("SSH_PASSPHRASE", passPhrase)
                        env.put("DISPLAY", ":0") // just to force using SSH_ASKPASS
                        env.put("SSH_ASKPASS", askpass!!.absolutePath)
                    }
                    val sshAddShell = File.createTempFile("ssh-add-", ".sh")
                    sshAddShell.writeText("ssh-add ${keyFile.absolutePath}")
                    val output = executeCommand("sh ${sshAddShell.absolutePath}", env)
                    println("Finish add the ssh-agent - ($output)")
                    if (agentEnv.isNotEmpty()) {
                        CommonEnv.addCommonEnv(agentEnv)
                    }
                } finally {
                    askpass?.delete()
                }
            } finally {
                keyFile.delete()
            }
        } catch (t: Throwable) {
            logger.warn("Fail to add the ssh key to ssh-agent", t)
            LoggerService.addNormalLine(Ansi().fgYellow().a("Fail to add the ssh-key to ssh agent").reset().toString())
        }
    }

    private fun executeCommand(commandLine: String, env: Map<String, String>?): String {
        val cmdLine = CommandLine.parse(commandLine)
        val executor = DefaultExecutor()
        val output = StringBuilder()
        val outputStream = object : LogOutputStream() {
            override fun processLine(line: String?, level: Int) {
                output.append(line).append("\n")
            }
        }
        executor.streamHandler = PumpStreamHandler(outputStream)
        executor.watchdog = ExecuteWatchdog(TimeUnit.MINUTES.toMillis(5))
        try {
            val exitCode = executor.execute(cmdLine, env)
            if (exitCode != 0) {
                logger.warn("Fail to execute the command($commandLine) because of exitCode($exitCode) and output($output)")
            }
        } catch (t: Throwable) {
            logger.warn("Error message($output)", t)
        }
        return output.toString()
    }

    /**
     * Parses ssh-agent output.
     */
    private fun parseAgentEnv(agentOutput: String): Map<String, String> {
        val env = HashMap<String, String>()

        // get SSH_AUTH_SOCK
        env.put(AUTH_SOCKET_VAR, getAgentValue(agentOutput, AUTH_SOCKET_VAR))
        logger.info(AUTH_SOCKET_VAR + "=" + env[AUTH_SOCKET_VAR])

        // get SSH_AGENT_PID
        env.put(AGENT_PID_VAR, getAgentValue(agentOutput, AGENT_PID_VAR))
        logger.info(AGENT_PID_VAR + "=" + env[AGENT_PID_VAR])

        return env
    }

    /**
     * Parses a value from ssh-agent output.
     */
    private fun getAgentValue(agentOutput: String, envVar: String): String {
        val pos = agentOutput.indexOf(envVar) + envVar.length + 1 // +1 for '='
        val end = agentOutput.indexOf(';', pos)
        return agentOutput.substring(pos, end)
    }

    /**
     * Creates a self-deleting script for SSH_ASKPASS. Self-deleting to be able to detect a wrong passphrase.
     */
    private fun createAskpassScript(): File {
        // TODO: assuming that ssh-add runs the script in shell even on Windows, not cmd
        //       for cmd following could work
        //       suffix = ".bat";
        //       script = "@ECHO %SSH_PASSPHRASE%\nDEL \"" + askpass.getAbsolutePath() + "\"\n";
        // 700
        val perms = PosixFilePermissions.fromString("rwx------")
        val fileAttributes = PosixFilePermissions
            .asFileAttribute(perms)
        val askpass = Files.createTempFile("askpass_", ".sh", fileAttributes).toFile()
        logger.info("Create the askpass file(${askpass.absolutePath})")
        askpass.writeText("#!/bin/sh\necho \"\$SSH_PASSPHRASE\"\nrm \"$0\"\n")
        executeCommand("chmod +x ${askpass.absolutePath}", null)
        return askpass
    }
}