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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.composer.util

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.tencent.bkrepo.composer.ARTIFACT_DIRECT_DOWNLOAD_PREFIX
import com.tencent.bkrepo.composer.COMPOSER_JSON
import com.tencent.bkrepo.composer.exception.ComposerUnSupportCompressException
import com.tencent.bkrepo.composer.pojo.ComposerMetadata
import com.tencent.bkrepo.composer.util.JsonUtil.jsonValue
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream
import com.tencent.bkrepo.composer.util.pojo.ComposerJsonNode
import org.apache.commons.compress.archivers.ArchiveEntry
import java.util.UUID

object DecompressUtil {

    private const val BUFFER_SIZE = 2048
    // 支持的压缩格式
    private const val TAR = "tar"
    private const val ZIP = "zip"
    private const val WHL = "whl"
    private const val GZ = "tar.gz"
    private const val TGZ = "tgz"

    // json属性
    private const val UID = "uid"
    private const val TYPE = "type"
    private const val URL = "url"
    private const val DIST = "dist"
    private const val NAME = "name"
    private const val VERSION = "version"

    private const val LIBRARY = "library"

    /**
     * 获取composer 压缩包中'composer.json'文件
     * @param InputStream 文件流
     * @param format 文件压缩格式
     * @exception
     */
    private fun InputStream.getComposerJson(format: String): String {
        return when (format) {
            TAR -> {
                getTarComposerJson(this)
            }
            ZIP, WHL -> {
                getZipComposerJson(this)
            }
            GZ, TGZ -> {
                getTgzComposerJson(this)
            }
            else -> {
                throw ComposerUnSupportCompressException("Can not support compress format!")
            }
        }
    }

    fun InputStream.getComposerMetadata(uri: String): ComposerMetadata {
        val uriArgs = UriUtil.getUriArgs(uri)
        val json = this.getComposerJson(uriArgs.format)
        return JsonUtil.mapper.readValue(json, ComposerMetadata::class.java)
    }

    /**
     * 读取composer deploy 文件流中name , version 信息
     * composer package 中'composer.json'添加到服务器上对应%package%.json 需要增加一些信息
     * @param uri 请求中的全文件名
     * @return composerJsonNode
     */
    fun InputStream.wrapperJson(uri: String): ComposerJsonNode {
        val uriArgs = UriUtil.getUriArgs(uri)
        val json = this.getComposerJson(uriArgs.format)
        JsonParser.parseString(json).asJsonObject.let {
            it.addProperty(UID, UUID.randomUUID().leastSignificantBits)
            val distObject = JsonObject()
            distObject.addProperty(TYPE, uriArgs.format)
            distObject.addProperty(URL, "$ARTIFACT_DIRECT_DOWNLOAD_PREFIX$uri")
            it.add(DIST, distObject)
            it.addProperty(TYPE, LIBRARY)

            return ComposerJsonNode(
                packageName = (json jsonValue NAME),
                version = (json jsonValue VERSION),
                json = GsonBuilder().create().toJson(it)
            )
        }
    }

    private fun getZipComposerJson(inputStream: InputStream): String {
        return getCompressComposerJson(ZipArchiveInputStream(inputStream))
    }

    private fun getTgzComposerJson(inputStream: InputStream): String {
        return getCompressComposerJson(TarArchiveInputStream(GZIPInputStream(inputStream)))
    }

    private fun getTarComposerJson(inputStream: InputStream): String {
        return getCompressComposerJson(TarArchiveInputStream(inputStream))
    }

    /**
     * 获取Composer package 压缩中的'composer.json'文件
     * @param tarInputStream 压缩文件流
     * @return 以字符串格式返回 composer.json 文件内容
     */
    private fun getCompressComposerJson(archiveInputStream: ArchiveInputStream): String {
        val stringBuilder = StringBuffer("")
        var zipEntry: ArchiveEntry
        archiveInputStream.use {
            while (archiveInputStream.nextEntry.also { zipEntry = it } != null) {
                if ((!zipEntry.isDirectory) && zipEntry.name.split("/").last() == COMPOSER_JSON) {
                    var length: Int
                    val bytes = ByteArray(BUFFER_SIZE)
                    while ((archiveInputStream.read(bytes).also { length = it }) != -1) {
                        stringBuilder.append(String(bytes, 0, length))
                    }
                }
            }
        }
        return stringBuilder.toString()
    }
}
