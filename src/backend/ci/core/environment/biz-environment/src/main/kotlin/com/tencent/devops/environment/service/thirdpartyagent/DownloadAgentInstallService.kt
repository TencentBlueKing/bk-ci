/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.environment.service.thirdpartyagent

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.agent.AgentArchType
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.common.service.Profile
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.pojo.thirdpartyagent.TPAInstallType
import com.tencent.devops.environment.service.AgentUrlService
import com.tencent.devops.environment.utils.FileMD5CacheUtils.getFileMD5
import com.tencent.devops.model.environment.tables.records.TEnvironmentThirdpartyAgentRecord
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.StreamingOutput
import org.apache.commons.compress.archivers.ArchiveOutputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
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
import java.util.Locale

@Service
@Suppress("TooManyFunctions", "LongMethod")
class DownloadAgentInstallService @Autowired constructor(
    private val dslContext: DSLContext,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val agentUrlService: AgentUrlService,
    private val profile: Profile,
    private val commonConfig: CommonConfig
) {

    @Value("\${environment.agent-package}")
    private val agentPackage = ""

    @Value("\${environment.agentCollectorOn:false}")
    private val agentCollectorOn = ""

    @Value("\${environment.certFilePath:#{null}}")
    private val certFilePath: String? = null

    @Value("\${environment.jdkCompatibility:true}")
    private val jdkCompatibility = true

    fun downloadInstallScript(
        agentId: String,
        isWinDownload: Boolean,
        loginName: String?,
        loginPassword: String?,
        installType: TPAInstallType?
    ): Response {
        logger.info("Trying to download the agent($agentId) install script")
        val agentRecord = getAgentRecord(agentId)

        if (agentRecord.status == AgentStatus.IMPORT_OK.status) {
            throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_AGENT_ALREADY_INSTALL,
                defaultMessage = "Agent already installed. Please obtain the install url again"
            )
        }

        /**
         * agent_url
         * jre_url
         * projectId
         * agentId
         * agentSecretKey
         * gateWay
         */
        val fileName = if (agentRecord.os == OS.WINDOWS.name) {
            if (isWinDownload) {
                "download_install.ps1"
            } else {
                "install.bat"
            }
        } else {
            "install.sh"
        }
        val scriptFile = File(agentPackage, "script/${agentRecord.os.lowercase()}/$fileName")

        if (!scriptFile.exists()) {
            logger.warn("The install script file(${scriptFile.absolutePath}) is not exist")
            throw FileNotFoundException("The install script file is not exist")
        }
        val map = getAgentReplaceProperties(agentRecord, true, loginName, loginPassword, installType)
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

    fun downloadGoAgent(
        agentId: String,
        record: TEnvironmentThirdpartyAgentRecord,
        arch: AgentArchType?
    ): Response {
        logger.info("Trying to download the agent($agentId) arch($arch)")

        val jarFiles = getGoAgentJarFiles(record.os, arch)
        val goDaemonFile = getGoFile(record.os, "devopsDaemon", arch)
        val goAgentFile = getGoFile(record.os, "devopsAgent", arch)
        val goInstallerFile = getGoFile(record.os, "installer", arch)
        val goUpgraderFile = getGoFile(record.os, "upgrader", arch)
        val packageFiles = getAgentPackageFiles(record.os)
        val scriptFiles = getGoAgentScriptFiles(record, null, null, null)
        val propertyFile = getPropertyFile(record)

        logger.info("Get the script files (${scriptFiles.keys})")

        return Response.ok(StreamingOutput { output ->
            val zipOut = ArchiveStreamFactory().createArchiveOutputStream<ZipArchiveOutputStream>(
                ArchiveStreamFactory.ZIP,
                output
            )

            if (!certFilePath.isNullOrBlank()) {
                val certFile = File(certFilePath)
                if (certFile.exists() && certFile.isFile) {
                    zipOut.putArchiveEntry(ZipArchiveEntry(certFile, CERT_FILE_NAME))
                    IOUtils.copy(FileInputStream(certFile), zipOut)
                    zipOut.closeArchiveEntry()
                }
            }

            (packageFiles?.let { jarFiles.plus(packageFiles) } ?: jarFiles).forEach {
                zipOut.putArchiveEntry(ZipArchiveEntry(it, it.name))
                IOUtils.copy(FileInputStream(it), zipOut)
                zipOut.closeArchiveEntry()
            }

            zipBinaryFile(os = record.os, goAgentFile = goAgentFile, fileName = "devopsAgent", zipOut = zipOut)
            zipBinaryFile(os = record.os, goAgentFile = goDaemonFile, fileName = "devopsDaemon", zipOut = zipOut)
            zipBinaryFile(os = record.os, goAgentFile = goInstallerFile, fileName = "tmp/installer", zipOut = zipOut)
            zipBinaryFile(os = record.os, goAgentFile = goUpgraderFile, fileName = "tmp/upgrader", zipOut = zipOut)

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

    private fun zipBinaryFile(
        os: String,
        goAgentFile: File,
        fileName: String,
        zipOut: ArchiveOutputStream<ZipArchiveEntry>
    ) {
        val finalFilename = if (os == OS.WINDOWS.name) {
            "$fileName.exe"
        } else {
            fileName
        }
        val devopsAgentEntry = ZipArchiveEntry(goAgentFile, finalFilename)
        devopsAgentEntry.unixMode = AGENT_FILE_MODE
        zipOut.putArchiveEntry(devopsAgentEntry)
        IOUtils.copy(FileInputStream(goAgentFile), zipOut)
        zipOut.closeArchiveEntry()
    }

    fun downloadAgent(
        agentId: String,
        arch: AgentArchType?
    ): Response {
        val agentRecord = getAgentRecord(agentId)
        return downloadGoAgent(
            agentId = agentId,
            record = agentRecord,
            arch = arch
        )
    }

    private fun getAgentPackageFiles(os: String) =
        File(agentPackage, "packages/${os.toLowerCase()}/").listFiles()

    private fun getGoAgentJarFiles(os: String, arch: AgentArchType?): List<File> {
        val agentJar = getAgentJarFile()
        val jdk17File = getJreZipFile(os, arch, JDK17_FILENAME)
        if (jdkCompatibility) {
            // #10586 现阶段保持 8 和 17并行，直到没有 8 的使用
            val jreFile = getJreZipFile(os, arch, JDK8_FILENAME)
            return listOf(agentJar, jreFile, jdk17File)
        } else {
            return listOf(agentJar, jdk17File)
        }
    }

    private fun getGoFile(os: String, fileName: String, arch: AgentArchType?): File {
        val archStr = if (arch == null) {
            ""
        } else {
            "_${arch.arch}"
        }
        val daemonFileName = when (os) {
            OS.WINDOWS.name -> "upgrade/$fileName.exe"
            OS.MACOS.name -> "upgrade/${fileName}_macos$archStr"
            else -> "upgrade/${fileName}_linux$archStr"
        }
        val daemonFile = File(agentPackage, daemonFileName)
        if (!daemonFile.exists()) {
            throw NotFoundException("go $fileName file not exists")
        }
        return daemonFile
    }

    private fun getGoAgentScriptFiles(
        agentRecord: TEnvironmentThirdpartyAgentRecord,
        loginName: String?,
        loginPassword: String?,
        installType: TPAInstallType?
    ): Map<String, String> {
        val file = File(agentPackage, "script/${agentRecord.os.lowercase()}")
        val scripts = file.listFiles()
        val map = getAgentReplaceProperties(agentRecord, false, loginName, loginPassword, installType)
        return scripts?.associate {
            var content = it.readText(Charsets.UTF_8)
            map.forEach { (key, value) -> content = content.replace("##$key##", value) }
            it.name to content
        } ?: emptyMap()
    }

    private fun getPropertyFile(agentRecord: TEnvironmentThirdpartyAgentRecord): Map<String, String> {
        val file = File(agentPackage, "config").listFiles()
        val map = getAgentReplaceProperties(agentRecord, false, null, null, null)
        return file?.filter { it.isFile }?.associate {
            var content = it.readText(Charsets.UTF_8)
            map.forEach { (key, value) -> content = content.replace("##$key##", value) }
            it.name to content
        } ?: emptyMap()
    }

    fun downloadJre(agentId: String, eTag: String?, arch: AgentArchType?): Response {
        logger.info("downloadJre, agentId: $agentId, eTag: $eTag")
        val record = getAgentRecord(agentId)
        val file = getJreZipFile(record.os, arch, JDK17_FILENAME)

        if (!eTag.isNullOrBlank()) {
            if (eTag == getFileMD5(file)) {
                return Response.status(Response.Status.NOT_MODIFIED).build()
            }
        }
        return Response.ok(file.inputStream(), MediaType.APPLICATION_OCTET_STREAM_TYPE)
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

    private fun getAgentReplaceProperties(
        agentRecord: TEnvironmentThirdpartyAgentRecord,
        enableCheckFiles: Boolean,
        loginName: String?,
        loginPassword: String?,
        installType: TPAInstallType?
    ): Map<String, String> {
        val agentId = HashUtil.encodeLongId(agentRecord.id)
        val agentUrl = agentUrlService.genAgentUrl(agentRecord)

        val gateWay = agentUrlService.genGateway(agentRecord)
        val fileGateway = agentUrlService.genFileGateway(agentRecord)
        return mapOf(
            "agent_url" to agentUrl,
            "projectId" to agentRecord.projectId,
            "agentId" to agentId,
            "agentSecretKey" to SecurityUtil.decrypt(agentRecord.secretKey),
            "gateWay" to gateWay,
            "fileGateway" to fileGateway,
            "landun.env" to profile.getEnv().name,
            "agentCollectorOn" to agentCollectorOn,
            "language" to commonConfig.devopsDefaultLocaleLanguage,
            "enableCheckFiles" to enableCheckFiles.toString(),
            "serviceUsername" to (loginName ?: ""),
            "servicePassword" to (loginPassword ?: ""),
            "installType" to (installType?.name ?: "")
        )
    }

    fun getAgentJarFile(): File {
        val agentJar = File(agentPackage, "jar/worker-agent.jar")
        if (!agentJar.exists()) {
            throw NotFoundException("The worker-agent.jar is not exist")
        }

        return agentJar
    }

    fun getJreZipFile(os: String, arch: AgentArchType?, fileName: String): File {
        val archStr = if (arch == null) {
            ""
        } else {
            "_${arch.arch}"
        }
        val file = File(agentPackage, "/jre/${os.lowercase(Locale.getDefault())}$archStr/$fileName")
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

    /**
     * 为指定[agentHashId]的agent生成并下载安装该台agent所需要的动态脚本和配置批次文件
     */
    fun downloadInstallAgentBatchFile(agentHashId: String): Response {
        logger.info("Trying to gen the new agent batch.zip from($agentHashId)")
        val record = getAgentRecord(agentHashId)

        return Response.ok(StreamingOutput { output ->
            val zipOut = ArchiveStreamFactory().createArchiveOutputStream<ZipArchiveOutputStream>(
                ArchiveStreamFactory.ZIP,
                output
            )

            if (!certFilePath.isNullOrBlank()) {
                val certFile = File(certFilePath)
                if (certFile.exists() && certFile.isFile) {
                    zipOut.putArchiveEntry(ZipArchiveEntry(certFile, CERT_FILE_NAME))
                    IOUtils.copy(FileInputStream(certFile), zipOut)
                    zipOut.closeArchiveEntry()
                }
            }

            getGoAgentScriptFiles(record, null, null, null).forEach { (name, content) ->
                logger.info("zip the script files ($name)")
                val entry = ZipArchiveEntry(name)
                val bytes = content.toByteArray()
                entry.size = bytes.size.toLong()
                entry.unixMode = AGENT_FILE_MODE
                zipOut.putArchiveEntry(entry)
                IOUtils.copy(ByteArrayInputStream(bytes), zipOut)
                zipOut.closeArchiveEntry()
            }

            getPropertyFile(record).forEach { (name, content) ->
                logger.info("zip the properties files ($name)")
                val entry = ZipArchiveEntry(name)
                val bytes = content.toByteArray()
                entry.size = bytes.size.toLong()
                zipOut.putArchiveEntry(entry)
                IOUtils.copy(ByteArrayInputStream(bytes), zipOut)
                zipOut.closeArchiveEntry()
            }

            zipOut.close()
        }, MediaType.APPLICATION_OCTET_STREAM_TYPE)
            .header("content-disposition", "attachment; filename = batch.zip")
            .build()
    }

    fun downloadUpgradeFile(
        projectId: String,
        agentId: String,
        secretKey: String,
        file: String,
        md5: String?
    ): Response {

        if (!checkAgent(projectId, agentId, secretKey)) {
            logger.warn("The agent($agentId)'s is DELETE")
            return Response.status(Response.Status.NOT_FOUND).build()
        }

        val upgradeFile = getUpgradeFile(file)
        val fileName = upgradeFile.name

        var modify = true
        val existMD5 = getFileMD5(upgradeFile)
        if (!upgradeFile.exists()) {
            logger.warn("The upgrade of agent($agentId) file(${upgradeFile.absolutePath}) is not exist")
            modify = false
        } else {
            if (md5 != null && existMD5 == md5) {
                modify = false
            }
        }

        return if (modify) {
            logger.info("upgrade file($file) changed, server file md5: $existMD5")
            Response.ok(upgradeFile.inputStream(), MediaType.APPLICATION_OCTET_STREAM_TYPE)
                .header("content-disposition", "attachment; filename = $fileName")
                .header("X-Checksum-Md5", existMD5)
                .build()
        } else {
            Response.status(Response.Status.NOT_MODIFIED).build()
        }
    }

    private fun checkAgent(projectId: String, agentId: String, secretKey: String): Boolean {
        val id = HashUtil.decodeIdToLong(agentId)
        val agentRecord = thirdPartyAgentDao.getAgent(dslContext, id)
            ?: return false

        val key = SecurityUtil.decrypt(agentRecord.secretKey)
        if (key != secretKey) {
            return false
        }

        return AgentStatus.fromStatus(agentRecord.status) != AgentStatus.DELETE
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DownloadAgentInstallService::class.java)
        private const val AGENT_FILE_MODE = 0b111101101
        private const val CERT_FILE_NAME = ".cert"

        private const val JDK8_FILENAME = "jre.zip"
        private const val JDK17_FILENAME = "jdk17.zip"
    }
}
