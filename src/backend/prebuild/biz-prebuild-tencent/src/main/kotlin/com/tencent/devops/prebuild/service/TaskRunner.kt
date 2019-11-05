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

package com.tencent.devops.prebuild.service

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.client.Client
import com.tencent.devops.environment.api.thirdPartyAgent.ServicePreBuildAgentResource
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentStaticInfo
import com.tencent.devops.prebuild.dao.PrebuildPersonalVmDao
import com.tencent.devops.prebuild.dao.PrebuildProjectDao
import com.tencent.devops.prebuild.pojo.InitPreProjectTask
import com.tencent.devops.prebuild.pojo.enums.TaskStatus
import com.tencent.devops.prebuild.utils.RedisUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(SCOPE_PROTOTYPE)
class TaskRunner @Autowired constructor(
    private val redisUtils: RedisUtils,
    private val devCloudVmService: DevCloudVmService,
    private val prebuildPersonalVmDao: PrebuildPersonalVmDao,
    private val prebuildProjectDao: PrebuildProjectDao,
    private val dslContext: DSLContext,
    private val client: Client
) : Runnable {

    companion object {
        private val logger = LoggerFactory.getLogger(TaskRunner::class.java)
    }

    lateinit var task: InitPreProjectTask

    override fun run() {
        try {
            // step 1, 创建节点
            val vm = prebuildPersonalVmDao.get(dslContext, task.userId)
            lateinit var vmName: String
            lateinit var vmPwd: String
            if (null == vm) {
                val (vmIP, pwd, containerName) = devCloudVmService.createVm(task.userId, "public/tlinux-2.2-base:latest")
                prebuildPersonalVmDao.create(dslContext, task.userId, vmIP, containerName, pwd)
                logger.info("Create vm success, ip:$vmIP, vmName: $containerName")
                appendLog("Create vm success, ip:$vmIP, vmName: $containerName")
                task.password = pwd
                vmPwd = pwd
                task.ip = vmIP
                redisUtils.setPreBuildInitTask(task.taskId, task)
                vmName = containerName
            } else {
                logger.info("VM has already exists, ip: ${vm.vmIp}, vmName: ${vm.vmName}")

                appendLog("VM has already exists, ip: ${vm.vmIp}, vmName: ${vm.vmName}")
                task.password = vm.rsyncPwd
                vmPwd = vm.rsyncPwd
                task.ip = vm.vmIp
                redisUtils.setPreBuildInitTask(task.taskId, task)
                vmName = vm.vmName
            }

            // step 2, 在服务端生成agent
            appendLog("Create agent in server...")
            val agent = getOrCreatePreAgent(task.ip)
            appendLog("Create agent in server success.")

            // step 3. 安装agent
            appendLog("Install agent in vm:(${task.ip})")
            val installAgentScript = createAgentInstallScript(agent.script)
            var commandResp = devCloudVmService.executeContainerCommand(task.userId, vmName, installAgentScript)
            if (commandResp.first == 0) {
                logger.info("Install agent success")
                appendLog("Install agent success, message: ${commandResp.second}")
                redisUtils.setPreBuildInitTask(task.taskId, task)
            } else {
                logger.info("Install agent failed, retCode: ${commandResp.first}, msg: ${commandResp.second}")
                appendLog("Install agent success, retCode: ${commandResp.first}, message: ${commandResp.second}")
                appendLog("Please checkout agent process.")
                task.taskStatus = TaskStatus.ERROR
                redisUtils.setPreBuildInitTask(task.taskId, task)

                return
            }

            // step 4, rsync拉起
            appendLog("Config rsync server...")
            commandResp = devCloudVmService.executeContainerCommand(task.userId, vmName, getRsyncScript(vmPwd))
            if (commandResp.first == 0) {
                logger.info("rsync config success")
                appendLog("rsync config success, message: ${commandResp.second}")
                redisUtils.setPreBuildInitTask(task.taskId, task)
            } else {
                logger.info("rsync config failed, retCode: ${commandResp.first}, msg: ${commandResp.second}")
                appendLog("rsync config success, retCode: ${commandResp.first}, message: ${commandResp.second}")
                appendLog("Please checkout rsync config")
                task.taskStatus = TaskStatus.ERROR
                redisUtils.setPreBuildInitTask(task.taskId, task)
                return
            }

            // step 4, mount codeCC
            appendLog("Config CodeCC...")
            commandResp = devCloudVmService.executeContainerCommand(task.userId, vmName, getCodeccScript())
            if (commandResp.first == 0) {
                logger.info("Config codeCC success")
                appendLog("Config codeCC success, message: ${commandResp.second}")
                redisUtils.setPreBuildInitTask(task.taskId, task)
            } else {
                logger.info("Config codeCC failed, retCode: ${commandResp.first}, msg: ${commandResp.second}")
                appendLog("Config codeCC success, retCode: ${commandResp.first}, message: ${commandResp.second}")
                appendLog("Please checkout config codeCC")
                task.taskStatus = TaskStatus.ERROR
                redisUtils.setPreBuildInitTask(task.taskId, task)
                return
            }

            appendLog("Init finished and success.")
            task.taskStatus = TaskStatus.SUCCESS
            redisUtils.setPreBuildInitTask(task.taskId, task)
        } catch (ex: Exception) {
            logger.error("init failed, error:", ex)
            task.taskStatus = TaskStatus.ERROR
            appendLog("Init failed, message: ${ex.message}")
            redisUtils.setPreBuildInitTask(task.taskId, task)
        }
    }

    private fun createAgentInstallScript(agentInstallScript: String): List<String> {
        val command = mutableListOf<String>()
        val configFile = "/root/installAgent.sh"
        command.add("echo 'export LANG=zh_CN.UTF-8' > $configFile")
        command.add("echo 'export LANGUAGE=zh_CN.UTF-8' >> $configFile")
        command.add("echo 'export LC_ALL=zh_CN.UTF-8' >> $configFile")
        command.add("echo 'mkdir -p /data/agent/' >> $configFile")
        command.add("echo 'cd /data/agent/' >> $configFile")
        command.add("echo '$agentInstallScript' >> $configFile")

        command.add("nohup sh $configFile > /dev/null 2>&1 &")
        return command
    }

    private fun appendLog(line: String) {
        redisUtils.appendPreBuildInitTaskLogs(task.taskId, listOf(line))
    }

    private fun getCodeccScript(): List<String> {
        return listOf(
            "if [ -d \"/data/codecc_software\" ];then echo mount codecc software success ; else ln -s /tools/codecc_software/ /data/codecc_software;  fi"
        )
    }

    private fun getRsyncScript(vmPwd: String): List<String> {
        val command = mutableListOf<String>()
        val secretFile = "/etc/rsyncd.secrets"
        command.add("echo 'root:$vmPwd' > $secretFile")

        val configFile = "/etc/rsyncd.conf"
        command.add("echo 'uid = root' > $configFile")
        command.add("echo 'gid = root' >> $configFile")
        command.add("echo 'read only = no' >> $configFile")
        command.add("echo 'use chroot =no' >> $configFile")
        command.add("echo 'transfer logging = true' >> $configFile")
        command.add("echo 'log format = %h %o %f %l %b' >> $configFile")
        command.add("echo 'log file = /var/log/rsyncd.log' >> $configFile")
        command.add("echo 'hosts allow = *' >> $configFile")
        command.add("echo 'slp refresh = 300' >> $configFile")
        command.add("echo '' >> $configFile")

        val preProjects = prebuildProjectDao.list(dslContext, task.userId, task.projectId)
        preProjects.forEach {
            command.add("echo '[${it.prebuildProjectId}]' >> $configFile")
            command.add("echo '  path = ${it.workspace}' >> $configFile")
            command.add("echo '  secret file = $secretFile' >> $configFile")

            command.add("mkdir -p ${it.workspace}")
        }

        command.add("rsync --daemon --config=/etc/rsyncd.conf")
        return command
    }

    fun getOrCreatePreAgent(ip: String): ThirdPartyAgentStaticInfo {
        val listPreAgentResult =
            client.get(ServicePreBuildAgentResource::class).listPreBuildAgent(task.userId, task.projectId, null)
        if (listPreAgentResult.isNotOk()) {
            logger.error("list prebuild agent failed")
            throw OperationException("list prebuild agent failed")
        }
        val preAgents = listPreAgentResult.data!!
        preAgents.forEach {
            if (it.ip == ip) {
                logger.info("Get user personal vm, ip: $ip")
                return it
            }
        }

        val createResult = client.get(ServicePreBuildAgentResource::class)
            .createPrebuildAgent(task.userId, task.projectId, OS.LINUX, null, ip)
        if (createResult.isNotOk()) {
            logger.error("create prebuild agent failed")
            throw OperationException("create prebuild agent failed")
        }
        logger.info("create prebuild agent success")
        return createResult.data!!
    }
}
