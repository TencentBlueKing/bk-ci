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

package com.tencent.devops.plugin.codecc.service

import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.util.FileUtil
import com.tencent.devops.plugin.codecc.exception.CodeccDownloadException
import com.tencent.devops.plugin.codecc.pojo.CodeccToolType
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileNotFoundException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput

@Service
class CodeccToolDownloaderService {

    @Value("\${plugin.codecc.path:#{null}}")
    private val codeccPath: String? = null

    @Value("\${plugin.codecc.toolFile}")
    private lateinit var toolScriptFile: String

    fun downloadTool(toolName: String, osType: OSType, fileMd5: String, is32Bit: Boolean?): Response {
        return when (toolName) {
            CodeccToolType.PYTHON2.name -> downloadPython2(osType, fileMd5, is32Bit)
            CodeccToolType.PYTHON3.name -> downloadPython3(osType, fileMd5, is32Bit)
            CodeccToolType.COVERITY.name -> downloadCoverity(osType, fileMd5, is32Bit)
            CodeccToolType.KLOCWORK.name -> downloadKlocwork(osType, fileMd5, is32Bit)
            CodeccToolType.ESLINT.name -> downloadEslint(osType, fileMd5, is32Bit)
            CodeccToolType.PYLINT2.name -> downloadPylint2(osType, fileMd5, is32Bit)
            CodeccToolType.PYLINT3.name -> downloadPylint3(osType, fileMd5, is32Bit)
            CodeccToolType.GOLANG.name -> downloadGolang(osType, fileMd5, is32Bit)
            CodeccToolType.GOMETALINTER.name -> downloadGoMetaLinter(osType, fileMd5, is32Bit)
            CodeccToolType.JDK8.name -> downloadJdk8(osType, fileMd5, is32Bit)
            else -> throw InvalidParamException("unsupported tool name: $toolName")
        }
    }

    private fun downloadJdk8(osType: OSType, fileMd5: String, is32Bit: Boolean?): Response {
        val file = when (osType) {
            OSType.LINUX -> {
                if (is32Bit != null && is32Bit) {
                    "jdk-8u191-linux-i586.tar.gz"
                } else {
                    "jdk-8u191-linux-x64.tar.gz"
                }
            }
            OSType.MAC_OS -> {
                "jdk-8u191-macosx-x64.dmg"
            }
            else -> {
                throw CodeccDownloadException(osType)
            }
        }
        return download(file, fileMd5)
    }

    private fun downloadGoMetaLinter(osType: OSType, fileMd5: String, is32Bit: Boolean?): Response {
        val file = if (osType == OSType.MAC_OS) "gometalinter_macos.zip" else "gometalinter_linux.zip"
        return download(file, fileMd5)
    }

    private fun downloadGolang(osType: OSType, fileMd5: String, is32Bit: Boolean?): Response {
        val file = when (osType) {
            OSType.LINUX -> {
                if (is32Bit != null && is32Bit) {
                    "go1.9.2.linux-386.tar.gz"
                } else {
                    "go1.9.2.linux-amd64.tar.gz"
                }
            }
            OSType.MAC_OS -> {
                "go1.9.2.darwin-amd64.tar.gz"
            }
            else -> {
                throw CodeccDownloadException(osType)
            }
        }
        return download(file, fileMd5)
    }

    private fun downloadPylint3(osType: OSType, fileMd5: String, is32Bit: Boolean?): Response {
        val file = "pylint_3.5.zip"
        return download(file, fileMd5)
    }

    private fun downloadPylint2(osType: OSType, fileMd5: String, is32Bit: Boolean?): Response {
        val file = "pylint_2.7.zip"
        return download(file, fileMd5)
    }

    private fun downloadEslint(osType: OSType, fileMd5: String, is32Bit: Boolean?): Response {
        val file = "node-v8.9.0-linux-x64_eslint.tar.gz"
        return download(file, fileMd5)
    }

    fun downloadKlocwork(osType: OSType, fileMd5: String, is32Bit: Boolean?): Response {
        val klocFile = when (osType) {
            OSType.LINUX -> {
                if (is32Bit != null && is32Bit) {
                    "kw-analysis-linux-12.3.tar.gz"
                } else {
                    "kw-analysis-linux64-12.3.tar.gz"
                }
            }
            OSType.MAC_OS -> {
                "cov-analysis-macosx-2018.06.tar.gz"
            }
            else -> {
                throw CodeccDownloadException(osType)
            }
        }
        return download(klocFile, fileMd5)
    }

    fun downloadCoverity(osType: OSType, fileMd5: String, is32Bit: Boolean?): Response {
        val covFile = when (osType) {
            OSType.LINUX -> {
                if (is32Bit != null && is32Bit) {
                    "cov-analysis-linux-2018.03.tar.gz"
                } else {
                    "cov-analysis-linux64-2018.03.tar.gz"
                }
            }
            OSType.MAC_OS -> {
                "cov-analysis-macosx-2018.06.tar.gz"
            }
            else -> {
                throw CodeccDownloadException(osType)
            }
        }
        return download(covFile, fileMd5)
    }

    fun downloadPython2(osType: OSType, fileMd5: String, is32Bit: Boolean?): Response {
        val pyFile = "Python-2.7.12.tgz"
        return download(pyFile, fileMd5)
    }

    fun downloadPython3(osType: OSType, fileMd5: String, is32Bit: Boolean?): Response {
        val pyFile = "Python-3.5.1.tgz"
        return download(pyFile, fileMd5)
    }

    fun downloadToolsScript(osType: OSType, fileMd5: String): Response {
        return download(toolScriptFile, fileMd5)
    }

    private fun download(file: String, eTag: String): Response {
        val target = File(codeccPath, file)
        if (!target.exists()) {
            throw FileNotFoundException("${target.absolutePath} not exist")
        }

        if (!target.isFile) {
            throw FileNotFoundException("${target.absolutePath} not file")
        }

        if (eTag.isNotBlank()) {
            // 检查文件的MD5值是否和客户端一致
            if (FileUtil.getMD5(target) == eTag) {
                return Response.status(Response.Status.NOT_MODIFIED).build()
            }
        }
        val bufSize = 40960
        val buf = ByteArray(bufSize)
        val inputStream = target.inputStream()
        val fileStream = StreamingOutput { output ->
            while (true) {
                val len = inputStream.read(buf)
                if (len == -1) break
                output.write(buf, 0, len)
                output.flush()
            }
        }
        return Response
            .ok(fileStream, MediaType.APPLICATION_OCTET_STREAM_TYPE)
            .header("content-disposition", "attachment; filename = ${target.name}")
            .build()
    }
}
