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

package com.tencent.devops.worker.common.utils

import com.dd.plist.NSArray
import com.dd.plist.NSDictionary
import com.dd.plist.NSString
import com.dd.plist.PropertyListParser
import com.tencent.devops.common.api.exception.ExecuteException
import org.slf4j.LoggerFactory
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

@SuppressWarnings("SwallowedException", "TooGenericExceptionCaught")
object IosUtils {
    private val logger = LoggerFactory.getLogger(IosUtils::class.java)

    fun getIpaInfoMap(ipa: File): Map<String, String> {

        val map = mutableMapOf<String, String>()
        val infoFilePair = getInfoFilePair(ipa)
        if (infoFilePair.first == null) {
            throw ExecuteException("not Info.plist found")
        }
        // 第三方jar包提供
        val rootDict = PropertyListParser.parse(infoFilePair.first) as NSDictionary
        // 应用包名
        if (!rootDict.containsKey("CFBundleIdentifier")) {
            throw ExecuteException("no CFBundleIdentifier find in plist")
        }
        var parameters = rootDict.objectForKey("CFBundleIdentifier") as NSString
        map["bundleIdentifier"] = parameters.toString()
        // 应用标题
        if (!rootDict.containsKey("CFBundleName")) {
            throw ExecuteException("no CFBundleName find in plist")
        }
        parameters = rootDict.objectForKey("CFBundleName") as NSString
        map["appTitle"] = parameters.toString()
        // 应用版本
        if (!rootDict.containsKey("CFBundleShortVersionString")) {
            throw ExecuteException("no CFBundleShortVersionString find in plist")
        }
        parameters = rootDict.objectForKey("CFBundleShortVersionString") as NSString
        map["bundleVersion"] = parameters.toString()
        // scheme
        val scheme = try {
            val schemeArray = rootDict.objectForKey("CFBundleURLTypes") as NSArray
            schemeArray.array
                .mapNotNull { it as NSDictionary }
                .mapNotNull { it.objectForKey("CFBundleURLSchemes") }
                .map { it as NSArray }
                .mapNotNull { it.array }
                .flatMap { it.toList() }
                .mapNotNull { it as NSString }
                .map { it.toString() }
                .maxByOrNull { it.length } ?: ""
        } catch (e: Exception) {
            logger.warn("get scheme failed", e)
            ""
        }
        map["scheme"] = scheme
        // 应用名称
        val appName = try {
            val nameDictionary = if (infoFilePair.second != null) {
                PropertyListParser.parse(infoFilePair.second) as NSDictionary
            } else {
                rootDict
            }
            nameDictionary.objectForKey("CFBundleDisplayName").toString()
        } catch (e: Exception) {
            logger.warn("get app name failed", e)
            ""
        }
        map["appName"] = appName

        // 如果没有图标，捕获异常，不影响接下步骤
        try {
            val cfBundleIcons = rootDict.objectForKey("CFBundleIcons") as NSDictionary
            val cfBundlePrimaryIcon = cfBundleIcons.objectForKey("CFBundlePrimaryIcon") as NSDictionary
            val cfBundleIconFiles = cfBundlePrimaryIcon.objectForKey("CFBundleIconFiles") as NSArray
            val size = cfBundleIconFiles.array.size
            map["image"] = (cfBundleIconFiles.array[0] as NSString).toString()
            map["fullImage"] = (cfBundleIconFiles.array[size - 1] as NSString).toString()
        } catch (e: Exception) {
            logger.warn("get image failed", e)
        }

        return map
    }

    private fun getInfoFilePair(srcZipFile: File): Pair<File?/*Info.plist*/, File?/*InfoPlist.strings*/> {
        val pattern = Pattern.compile("Payload/[\\w.-]+\\.app/Info.plist")
        val zhPattern = Pattern.compile("Payload/[\\w.-]+\\.app/zh-Hans.lproj/InfoPlist.strings")
        var file: File? = null
        var zhFile: File? = null
        ZipInputStream(BufferedInputStream(FileInputStream(srcZipFile))).use { zis ->
            var entry: ZipEntry?
            while (file == null || zhFile == null) {
                entry = zis.nextEntry
                if (entry == null) break
                file = file ?: toTmpFile(pattern, entry, zis, "Info", ".plist")
                zhFile = zhFile ?: toTmpFile(zhPattern, entry, zis, "InfoPlist", ".strings")
            }
        }

        return Pair(file, zhFile)
    }

    @SuppressWarnings("NestedBlockDepth")
    private fun toTmpFile(
        pattern: Pattern,
        entry: ZipEntry,
        zis: ZipInputStream,
        prefix: String,
        suffix: String
    ): File? {
        return if (pattern.matcher(entry.name).matches()) {
            val file = File.createTempFile(prefix, suffix)
            BufferedOutputStream(FileOutputStream(file.canonicalFile)).use { bos ->
                var b: Int
                val buf = ByteArray(4096)
                while (true) {
                    b = zis.read(buf)
                    if (b == -1) break
                    bos.write(buf, 0, b)
                }
            }
            file
        } else {
            null
        }
    }
}
