/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.composer.util

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.tencent.bkrepo.common.api.util.DecompressUtils
import com.tencent.bkrepo.composer.DIRECT_DISTS
import com.tencent.bkrepo.composer.COMPOSER_JSON
import com.tencent.bkrepo.composer.exception.ComposerUnSupportCompressException
import com.tencent.bkrepo.composer.util.JsonUtil.jsonValue
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream
import com.tencent.bkrepo.composer.util.pojo.ComposerArtifact
import java.util.UUID
import kotlin.math.abs

object DecompressUtil {

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

    /**
     * 读取composer deploy 文件流中name , version 信息
     * composer package 中'composer.json'添加到服务器上对应%package%.json 需要增加一些信息
     * @param uri 请求中的全文件名
     * @return composerJsonNode
     */
    fun InputStream.wrapperJson(uri: String): ComposerArtifact {
        val uriArgs = UriUtil.getUriArgs(uri)
        val json = this.getComposerJson(uriArgs.format)
        JsonParser.parseString(json).asJsonObject.let {
            it.addProperty(UID, abs(UUID.randomUUID().leastSignificantBits))
            val distObject = JsonObject()
            distObject.addProperty(TYPE, uriArgs.format)
            distObject.addProperty(URL, "$DIRECT_DISTS$uri")
            it.add(DIST, distObject)
            it.addProperty(TYPE, LIBRARY)

            return ComposerArtifact(
                name = (json jsonValue NAME),
                version = (json jsonValue VERSION),
                json = GsonBuilder().create().toJson(it)
            )
        }
    }

    private fun getZipComposerJson(inputStream: InputStream): String {
        return DecompressUtils.getContent(ZipArchiveInputStream(inputStream), COMPOSER_JSON)
    }

    private fun getTgzComposerJson(inputStream: InputStream): String {
        return DecompressUtils.getContent(TarArchiveInputStream(GZIPInputStream(inputStream)), COMPOSER_JSON)
    }

    private fun getTarComposerJson(inputStream: InputStream): String {
        return DecompressUtils.getContent(TarArchiveInputStream(inputStream), COMPOSER_JSON)
    }
}
