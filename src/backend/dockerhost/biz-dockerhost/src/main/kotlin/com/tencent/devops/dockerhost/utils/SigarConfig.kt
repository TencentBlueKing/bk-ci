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

package com.tencent.devops.dockerhost.utils

import org.hyperic.jni.ArchNotSupportedException
import org.hyperic.sigar.Sigar
import org.hyperic.sigar.SigarLoader
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.DefaultResourceLoader
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@Configuration
class SigarConfig {

    private val logger = LoggerFactory.getLogger(SigarConfig::class.java)

    // 静态代码块
    init {
        try {
            this.initSigar()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // 初始化sigar的配置文件
    @Throws(IOException::class)
    fun initSigar() {
        val loader = SigarLoader(Sigar::class.java)
        var lib: String? = null

        try {
            lib = loader.libraryName
            logger.info("init sigar so文件=====================" + lib!!)
        } catch (var7: ArchNotSupportedException) {
            logger.error("initSigar() error:{}", var7.message)
        }

        val resourceLoader = DefaultResourceLoader()
        val resource = resourceLoader.getResource("classpath:/sigar/" + lib!!)
        if (resource.exists()) {
            val inputStream = resource.inputStream
            val tempDir = File("/usr/lib64")
            if (!tempDir.exists()) {
                tempDir.mkdirs()
            }
            val os = BufferedOutputStream(FileOutputStream(File(tempDir, lib), false))
            inputStream.copyTo(os, DEFAULT_BUFFER_SIZE)
            inputStream.close()
            os.close()
            System.setProperty("org.hyperic.sigar.path", tempDir.canonicalPath)
            logger.info("======================org.hyperic.sigar.path:" + System.getProperty("org.hyperic.sigar.path"))
        }
    }
}