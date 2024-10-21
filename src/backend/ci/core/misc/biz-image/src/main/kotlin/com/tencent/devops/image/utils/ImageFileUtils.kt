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

package com.tencent.devops.image.utils

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.image.constants.ImageMessageCode.PARSE_MIRROR_FILE_FAILED
import com.tencent.devops.image.pojo.DockerImage
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.BufferedInputStream
import java.io.FileInputStream

object ImageFileUtils {
    fun parseImageMeta(imageFilePath: String): List<DockerImage> {
        val fis = FileInputStream(imageFilePath)
        val tarInputStream = TarArchiveInputStream(BufferedInputStream(fis))
        var entry: ArchiveEntry? = tarInputStream.nextTarEntry
        while (entry != null) {
            val tarEntry = entry as TarArchiveEntry
            val entryName = tarEntry.name
            if (entryName == "repositories") {
                return parseImagesFromContent(tarInputStream)
            }
            entry = tarInputStream.nextTarEntry
        }
        throw TaskExecuteException(
            errorCode = PARSE_MIRROR_FILE_FAILED.toInt(),
            errorType = ErrorType.USER,
            errorMsg = I18nUtil.getCodeLanMessage(PARSE_MIRROR_FILE_FAILED)
        )
    }

    private fun parseImagesFromContent(tarInputStream: TarArchiveInputStream): List<DockerImage> {
        val data: Map<String, Any> = jacksonObjectMapper().readValue(tarInputStream)
        val imageList = mutableListOf<DockerImage>()
        for ((repoKey, repoValue) in data) {
            val imageTags = repoValue as Map<String, Any>
            for ((k) in imageTags) {
                imageList.add(DockerImage(repoKey, k, parseImageShortName(repoKey)))
            }
        }
        return imageList
    }

    // 从imageRepo中解析imageName(去掉域名和端口）
    private fun parseImageShortName(imageTag: String): String {
        val index = imageTag.indexOf('/')
        return if (index == -1) {
            imageTag
        } else {
            val pre = imageTag.substring(0, index)
            if (pre.contains(':') || pre.contains('.')) {
                imageTag.substring(index + 1, imageTag.length)
            } else {
                imageTag
            }
        }
    }
}
