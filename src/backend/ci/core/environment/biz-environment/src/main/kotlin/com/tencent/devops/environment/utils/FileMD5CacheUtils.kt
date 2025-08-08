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

package com.tencent.devops.environment.utils

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.tencent.devops.common.api.util.FileUtil
import org.slf4j.LoggerFactory
import java.io.File
import jakarta.ws.rs.NotFoundException

object FileMD5CacheUtils {

    private val cache: LoadingCache<String/**File Name**/, FileMD5> =
        CacheBuilder.newBuilder().maximumSize(100).build(object : CacheLoader<String, FileMD5>() {
            override fun load(fileName: String): FileMD5 {
                return fileMD5(fileName)
            }
        })

    fun getUninstallScriptFile(os: String): File {
        val scriptFile = File("/data1/work/thirdPartyAgent/scripts/${os.toLowerCase()}/agent_uninstall.sh")
        if (!scriptFile.exists()) {
            logger.warn("The uninstall script file(${scriptFile.absolutePath}) is not exist")
            throw NotFoundException("The uninstall script file is not exist")
        }
        return scriptFile
    }

    fun getAgentJarFile(): File {
        val file = File("/data1/work/thirdPartyAgent/jar/agent.jar")
        if (!file.exists()) {
            logger.warn("The agent file(${file.absolutePath}) is not exist")
            throw NotFoundException("The agent file is not exist")
        }
        return file
    }

    fun getAgentUpgraderFile(): File {
        val file = File("/data1/work/thirdPartyAgent/jar/upgrader.jar")
        if (!file.exists()) {
            logger.warn("The agent file(${file.absolutePath}) is not exist")
            throw NotFoundException("The agent file is not exist")
        }
        return file
    }

    fun getJreFile(os: String): File {
        val file = File("/data1/work/thirdPartyAgent/jre/${os.toLowerCase()}/jre.tar.gz")
        if (!file.exists()) {
            logger.warn("The jre file(${file.absolutePath}) is not exist")
            throw NotFoundException("The jre file is not exist")
        }
        return file
    }

    fun getFileMD5(file: File): String {
        return try {
            var fileMD5 = cache.get(file.absolutePath)
            val lastModified = file.lastModified()
            if (lastModified != fileMD5.modifyTime) {
                logger.info(
                    "The file(${file.absolutePath}) modify time change from" +
                        " ${fileMD5.modifyTime} to $lastModified"
                )
                cache.invalidate(file.absolutePath)
                fileMD5 = cache.get(file.absolutePath)
            }
            fileMD5.md5
        } catch (ignored: Exception) {
            fileMD5(file.absolutePath).md5
        }
    }

    private fun fileMD5(fileName: String): FileMD5 {
        val file = File(fileName)
        val m = FileUtil.getMD5(file)
        logger.info("Add the file(${file.absolutePath}) md5($m) to cache")
        return FileMD5(file.lastModified(), m)
    }

    data class FileMD5(
        val modifyTime: Long,
        val md5: String
    )

    private val logger = LoggerFactory.getLogger(FileMD5CacheUtils::class.java)
}
