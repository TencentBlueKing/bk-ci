package com.tencent.devops.sign.utils

import com.dd.plist.NSArray
import com.dd.plist.NSDictionary
import com.dd.plist.PropertyListParser
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object IpaIconUtil {
    private val logger = LoggerFactory.getLogger(IpaIconUtil::class.java)

    fun resolveIpaIcon(file: File): ByteArray? {
        try {
            val plistPattern = Pattern.compile("Payload/[\\w.-]+\\.app/Info.plist")
            var iconName: String? = null
            ZipInputStream(BufferedInputStream(file.inputStream())).use { inputSteam ->
                var entry: ZipEntry? = null
                while (inputSteam.nextEntry.also { entry = it } != null) {
                    if (plistPattern.matcher(entry!!.name).matches()) {
                        val buffer = ByteArrayOutputStream()
                        IOUtils.copy(inputSteam, buffer)
                        val rootDict = PropertyListParser.parse(buffer.toByteArray()) as NSDictionary
                        val cfBundleIcons = rootDict.objectForKey("CFBundleIcons") as NSDictionary
                        val cfBundlePrimaryIcon = cfBundleIcons.objectForKey("CFBundlePrimaryIcon") as NSDictionary
                        val cfBundleIconFiles = cfBundlePrimaryIcon.objectForKey("CFBundleIconFiles") as NSArray
                        iconName = cfBundleIconFiles.lastObject().toString()
                    }
                }
            }

            if (!iconName.isNullOrBlank()) {
                ZipInputStream(BufferedInputStream(file.inputStream())).use { inputSteam ->
                    var entry: ZipEntry? = null
                    while (inputSteam.nextEntry.also { entry = it } != null) {
                        if (entry!!.name.contains(iconName!!)) {
                            val buffer = ByteArrayOutputStream()
                            IOUtils.copy(inputSteam, buffer)
                            return buffer.toByteArray()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("resolve Ipa(${file.absolutePath}) icon failed, cause: ${e.message}")
        }
        return null
    }
}
