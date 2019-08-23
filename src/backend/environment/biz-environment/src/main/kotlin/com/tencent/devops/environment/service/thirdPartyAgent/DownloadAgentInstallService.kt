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

package com.tencent.devops.environment.service.thirdPartyAgent

import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.common.service.Profile
import com.tencent.devops.environment.dao.thirdPartyAgent.ThirdPartyAgentDao
import com.tencent.devops.environment.service.slave.SlaveGatewayService
import com.tencent.devops.environment.utils.FileMD5CacheUtils.getFileMD5
import com.tencent.devops.model.environment.tables.records.TEnvironmentThirdpartyAgentRecord
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.utils.IOUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.nio.charset.Charset
import javax.ws.rs.NotFoundException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput

@Service
class DownloadAgentInstallService @Autowired constructor(
    private val dslContext: DSLContext,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val profile: Profile,
    private val slaveGatewayService: SlaveGatewayService
) {

    @Value("\${environment.agent-package}")
    private val agentPackage = ""

    @Value("\${environment.agentCollectorOn:false}")
    private val agentCollectorOn = ""

    fun downloadInstallScript(agentId: String): Response {
        logger.info("Trying to download the agent($agentId) install script")
        val agentRecord = getAgentRecord(agentId)
        /**
         * agent_url
         * jre_url
         * projectId
         * agentId
         * agentSecretKey
         * gateWay
         */
        val fileName = if (agentRecord.os == OS.WINDOWS.name) {
            "install.bat"
        } else {
            "install.sh"
        }
        val scriptFile = File(agentPackage, "script/${agentRecord.os.toLowerCase()}/$fileName")

        if (!scriptFile.exists()) {
            logger.warn("The install script file(${scriptFile.absolutePath}) is not exist")
            throw FileNotFoundException("The install script file is not exist")
        }
        val map = getAgentReplaceProperties(agentRecord)
        var result = scriptFile.readText(Charset.forName("UTF-8"))

        map.forEach { (t, u) ->
            result = result.replace("##$t##", u)
        }

        return Response.ok(StreamingOutput { output ->
            output.write(result.toByteArray())
            output.flush()
        }, MediaType.APPLICATION_OCTET_STREAM_TYPE)
            .header("content-disposition", "attachment; filename = $fileName")
            .build()
    }

    fun downloadGoAgent(agentId: String, record: TEnvironmentThirdpartyAgentRecord): Response {
        logger.info("Trying to download the agent($agentId)")

        val jarFiles = getGoAgentJarFiles(record.os)
        val goDaemonFile = getGoDaemonFile(record.os)
        val goAgentFile = getGoAgentFile(record.os)
        val scriptFiles = getGoAgentScriptFiles(record)
        val propertyFile = getPropertyFile(record)

        logger.info("Get the script files (${scriptFiles.keys})")

        return Response.ok(StreamingOutput { output ->
            val zipOut = ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.ZIP, output)

            jarFiles.forEach {
                zipOut.putArchiveEntry(ZipArchiveEntry(it, it.name))
                IOUtils.copy(FileInputStream(it), zipOut)
                zipOut.closeArchiveEntry()
            }

            val goAgentFileName = if (record.os == OS.WINDOWS.name) {
                "devopsAgent.exe"
            } else {
                "devopsAgent"
            }
            val devopsAgentEntry = ZipArchiveEntry(goAgentFile, goAgentFileName)
            devopsAgentEntry.unixMode = AGENT_FILE_MODE
            zipOut.putArchiveEntry(devopsAgentEntry)
            IOUtils.copy(FileInputStream(goAgentFile), zipOut)
            zipOut.closeArchiveEntry()

            val goDaemonFileName = if (record.os == OS.WINDOWS.name) {
                "devopsDaemon.exe"
            } else {
                "devopsDaemon"
            }
            val devopsDaemonEntry = ZipArchiveEntry(goDaemonFile, goDaemonFileName)
            devopsDaemonEntry.unixMode = AGENT_FILE_MODE
            zipOut.putArchiveEntry(devopsDaemonEntry)
            IOUtils.copy(FileInputStream(goDaemonFile), zipOut)
            zipOut.closeArchiveEntry()

            scriptFiles.forEach { (name, content) ->
                val entry = ZipArchiveEntry(name)
                val bytes = content.toByteArray()
                entry.size = bytes.size.toLong()
                entry.unixMode = AGENT_FILE_MODE
                zipOut.putArchiveEntry(entry)
                IOUtils.copy(ByteArrayInputStream(bytes), zipOut)
                zipOut.closeArchiveEntry()
            }

            propertyFile.forEach { (name, content) ->
                val entry = ZipArchiveEntry(name)
                val bytes = content.toByteArray()
                entry.size = bytes.size.toLong()
                zipOut.putArchiveEntry(entry)
                IOUtils.copy(ByteArrayInputStream(bytes), zipOut)
                zipOut.closeArchiveEntry()
            }

            zipOut.close()
        }, MediaType.APPLICATION_OCTET_STREAM_TYPE)
            .header("content-disposition", "attachment; filename = agent.zip")
            .build()
    }

    fun downloadAgent(agentId: String): Response {
        val agentRecord = getAgentRecord(agentId)
        return downloadGoAgent(agentId, agentRecord)
    }

    private fun getGoAgentJarFiles(os: String): List<File> {
        val agentJar = getAgentJarFile()
        val jreFile = getJreZipFile(os)
        return listOf(agentJar, jreFile)
    }

    private fun getGoDaemonFile(os: String): File {
        val daemonFileName = when (os) {
            OS.WINDOWS.name -> "upgrade/devopsDaemon.exe"
            OS.MACOS.name -> "upgrade/devopsDaemon_macos"
            else -> "upgrade/devopsDaemon_linux"
        }
        val daemonFile = File(agentPackage, daemonFileName)
        if (!daemonFile.exists()) {
            throw NotFoundException("go daemon file not exists")
        }
        return daemonFile
    }

    private fun getGoAgentFile(os: String): File {
        val agentFileName = when (os) {
            OS.WINDOWS.name -> "upgrade/devopsAgent.exe"
            OS.MACOS.name -> "upgrade/devopsAgent_macos"
            else -> "upgrade/devopsAgent_linux"
        }
        val agentFile = File(agentPackage, agentFileName)
        if (!agentFile.exists()) {
            throw NotFoundException("go agent file not exists")
        }
        return agentFile
    }

    private fun getAgentPackageFiles(os: String) =
        File(agentPackage, "packages/${os.toLowerCase()}/").listFiles()

    private fun getGoAgentScriptFiles(agentRecord: TEnvironmentThirdpartyAgentRecord): Map<String/*Name*/, String> {
        val file = File(agentPackage, "script/${agentRecord.os.toLowerCase()}")
        val scripts = file.listFiles()
        val map = getAgentReplaceProperties(agentRecord)
        return scripts?.map {
            var content = it.readText(Charsets.UTF_8)
            map.forEach { (key, value) ->
                content = content.replace("##$key##", value)
            }
            it.name to content
        }?.toMap() ?: emptyMap()
    }

    private fun getPropertyFile(agentRecord: TEnvironmentThirdpartyAgentRecord): Map<String, String> {
        val file = File(agentPackage, "config").listFiles()
        val map = getAgentReplaceProperties(agentRecord)
        return file?.filter { it.isFile }?.map {
            var content = it.readText(Charsets.UTF_8)
            map.forEach { (key, value) ->
                content = content.replace("##$key##", value)
            }
            it.name to content
        }?.toMap() ?: emptyMap()
    }

    fun downloadJre(agentId: String, eTag: String?): Response {
        logger.info("Trying to download the jre($agentId)")
        val record = getAgentRecord(agentId)
        val file = getJreZipFile(record.os)

        if (!eTag.isNullOrBlank()) {
            if (eTag == getFileMD5(file)) {
                return Response.status(Response.Status.NOT_MODIFIED).build()
            }
        }
        return Response.ok(StreamingOutput { output ->
            output.write(file.readBytes())
            output.flush()
        }, MediaType.APPLICATION_OCTET_STREAM_TYPE)
            .header("content-disposition", "attachment; filename = jre.zip")
            .build()
    }

    private fun getAgentRecord(agentId: String): TEnvironmentThirdpartyAgentRecord {
        val id = HashUtil.decodeIdToLong(agentId)
        val agentRecord = thirdPartyAgentDao.getAgent(dslContext, id)
        if (agentRecord == null) {
            logger.warn("The agent($id) is not exist")
            throw NotFoundException("The agent is not exist")
        }
        return agentRecord
    }

    private fun getAgentReplaceProperties(agentRecord: TEnvironmentThirdpartyAgentRecord): Map<String, String> {
        val agentId = HashUtil.encodeLongId(agentRecord.id)
        val gw = slaveGatewayService.getGateway(agentRecord)
        return mapOf(
            "agent_url" to "$gw/ms/environment/api/external/thirdPartyAgent/$agentId/agent",
            "projectId" to agentRecord.projectId,
            "agentId" to agentId,
            "agentSecretKey" to SecurityUtil.decrypt(agentRecord.secretKey),
            "gateWay" to gw!!,
            "landun.env" to profile.getEnv().name,
            "agentCollectorOn" to agentCollectorOn
        )
    }

    fun getAgentJarFile(): File {
        val agentJar = File(agentPackage, "jar/worker-agent.jar")
        if (!agentJar.exists()) {
            throw NotFoundException("The worker-agent.jar is not exist")
        }

        return agentJar
    }

    fun getJreZipFile(os: String): File {
        val file = File(agentPackage, "/jre/${os.toLowerCase()}/jre.zip")
        if (!file.exists()) {
            logger.warn("The jre file(${file.absolutePath}) is not exist")
            throw FileNotFoundException("The jre file is not exist")
        }
        return file
    }

    fun getUpgradeFile(fileName: String): File {
        val file = File(agentPackage, fileName)
        if (!file.exists()) {
            logger.warn("The file(${file.absolutePath}) is not exist")
            throw FileNotFoundException("The file is not exist")
        }
        return file
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DownloadAgentInstallService::class.java)
        private const val AGENT_FILE_MODE = 0b111101101
    }
}