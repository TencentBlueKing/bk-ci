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

package com.tencent.bkrepo.pypi.util

import com.tencent.bkrepo.common.api.util.DecompressUtils
import com.tencent.bkrepo.pypi.exception.PypiUnSupportCompressException
import com.tencent.bkrepo.pypi.util.JsonUtil.jsonValue
import com.tencent.bkrepo.pypi.util.PropertiesUtil.propInfo
import com.tencent.bkrepo.pypi.util.pojo.PypiInfo
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream

object DecompressUtil {

    // 支持的压缩格式
    private const val TAR = "tar"
    private const val ZIP = "zip"
    private const val WHL = "whl"
    private const val GZ = "tar.gz"
    private const val TGZ = "tgz"

    // 目标属性
    private const val name = "name"
    private const val version = "version"
    private const val summary = "summary"

    // 目标文件
    private const val metadata = "metadata.json"
    private const val pkgInfo = "PKG-INFO"

    /**
     * @param format 文件格式
     * @return PypiInfo 包文件信息
     */
    fun InputStream.getPkgInfo(format: String): PypiInfo {
        return when (format) {
            TAR -> {
                getTarPkgInfo(this)
            }
            WHL -> {
                getWhlMetadata(this)
            }
            ZIP -> {
                getZipMetadata(this)
            }
            GZ, TGZ -> {
                getTgzPkgInfo(this)
            }
            else -> {
                throw PypiUnSupportCompressException("Can not support compress format!")
            }
        }
    }

    private fun getWhlMetadata(inputStream: InputStream): PypiInfo {
        val metadata = DecompressUtils.getContent(ZipArchiveInputStream(inputStream), metadata)
        return PypiInfo(metadata jsonValue name, metadata jsonValue version, metadata jsonValue summary)
    }

    private fun getZipMetadata(inputStream: InputStream): PypiInfo {
        val propStr = DecompressUtils.getContent(ZipArchiveInputStream(inputStream), pkgInfo)
        return propStr.propInfo()
    }

    private fun getTgzPkgInfo(inputStream: InputStream): PypiInfo {
        val propStr = DecompressUtils.getContent(TarArchiveInputStream(GZIPInputStream(inputStream, 512)), pkgInfo)
        return propStr.propInfo()
    }

    private fun getTarPkgInfo(inputStream: InputStream): PypiInfo {
        val propStr = DecompressUtils.getContent(TarArchiveInputStream(inputStream), pkgInfo)
        return propStr.propInfo()
    }
}
