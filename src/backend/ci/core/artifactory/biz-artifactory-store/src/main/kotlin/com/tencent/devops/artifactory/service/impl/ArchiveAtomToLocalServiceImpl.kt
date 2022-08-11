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

package com.tencent.devops.artifactory.service.impl

import com.tencent.devops.artifactory.constant.BK_CI_ATOM_DIR
import com.tencent.devops.artifactory.constant.REALM_LOCAL
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import org.apache.commons.io.FileUtils
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.util.FileSystemUtils
import java.io.File
import java.io.InputStream
import java.net.URLDecoder

@Service
@ConditionalOnProperty(prefix = "artifactory", name = ["realm"], havingValue = REALM_LOCAL)
class ArchiveAtomToLocalServiceImpl : ArchiveAtomServiceImpl() {

    private val logger = LoggerFactory.getLogger(ArchiveAtomToLocalServiceImpl::class.java)

    @Value("\${artifactory.archiveLocalBasePath:#{null}}")
    private lateinit var atomArchiveLocalBasePath: String

    override fun getAtomFileContent(filePath: String): String {
        logger.info("getAtomFileContent, filePath: $filePath")
        if (filePath.contains("../")) {
            throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf(filePath))
        }
        val file = File("$atomArchiveLocalBasePath/$BK_CI_ATOM_DIR/${URLDecoder.decode(filePath, "UTF-8")}")
        val content = if (file.exists()) FileUtils.readFileToString(file) else ""
        logger.info("getAtomFileContent content: $content")
        return content
    }

    override fun deleteAtom(userId: String, projectCode: String, atomCode: String) {
        val filePath = "$BK_CI_ATOM_DIR/$projectCode/$atomCode"
        FileSystemUtils.deleteRecursively(File(atomArchiveLocalBasePath, filePath))
    }

    override fun handleArchiveFile(
        disposition: FormDataContentDisposition,
        inputStream: InputStream,
        projectCode: String,
        atomCode: String,
        version: String
    ) {
        unzipFile(
            disposition = disposition,
            inputStream = inputStream,
            projectCode = projectCode,
            atomCode = atomCode,
            version = version
        )
    }

    override fun getAtomArchiveBasePath(): String {
        return atomArchiveLocalBasePath
    }

    override fun clearServerTmpFile(projectCode: String, atomCode: String, version: String) {
        // 插件文件存在本地硬盘，不需要清理
    }
}
