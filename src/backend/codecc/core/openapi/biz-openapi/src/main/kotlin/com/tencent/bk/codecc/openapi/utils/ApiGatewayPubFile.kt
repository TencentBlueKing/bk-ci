package com.tencent.bk.codecc.openapi.utils

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File

@Component
class ApiGatewayPubFile {

    @Value("\${api.gateway.pub.file.outer:#{null}}")
    private val pubFileOuter: String? = null

    @Value("\${api.gateway.pub.file.inner:#{null}}")
    private val pubFileInner: String? = null

    private var pubOuter: String? = null
    private var pubInner: String? = null

    fun getPubOuter(): String {
        if (pubOuter == null) {
            synchronized(this) {
                if (pubOuter != null) {
                    return pubOuter!!
                }
                if (pubFileOuter == null) {
                    throw RuntimeException("Api gateway pub file is not settle")
                }

                val file = File(pubFileOuter)
                if (!file.exists()) {
                    throw RuntimeException("The pub file (${file.absolutePath}) is not exist")
                }
                pubOuter = file.readText()
                if (pubOuter == null) {
                    throw RuntimeException("Can't read the pub content from ${file.absolutePath}")
                }

                if (pubOuter!!.trim().isEmpty()) {
                    throw RuntimeException("The pub file is empty from ${file.absolutePath}")
                }
                logger.info("Get the pub($pubOuter) from ${file.absolutePath}")
            }
        }

        return pubOuter!!
    }

    fun getPubInner(): String {
        if (pubInner == null) {
            synchronized(this) {
                if (pubInner != null) {
                    return pubInner!!
                }
                if (pubFileInner == null) {
                    throw RuntimeException("Api gateway pub file is not settle")
                }

                val file = File(pubFileInner)
                if (!file.exists()) {
                    throw RuntimeException("The pub file (${file.absolutePath}) is not exist")
                }
                pubInner = file.readText()
                if (pubInner == null) {
                    throw RuntimeException("Can't read the pub content from ${file.absolutePath}")
                }

                if (pubInner!!.trim().isEmpty()) {
                    throw RuntimeException("The pub file is empty from ${file.absolutePath}")
                }
                logger.info("Get the pub($pubInner) from ${file.absolutePath}")
            }
        }

        return pubInner!!
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApiGatewayPubFile::class.java)
    }
}