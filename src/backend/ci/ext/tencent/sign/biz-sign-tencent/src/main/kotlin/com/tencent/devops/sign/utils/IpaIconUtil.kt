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
