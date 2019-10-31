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