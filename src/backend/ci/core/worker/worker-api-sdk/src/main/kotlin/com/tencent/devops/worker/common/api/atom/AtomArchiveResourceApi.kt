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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.worker.common.api.atom

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.JsonParser
import com.tencent.devops.artifactory.constant.BK_CI_ATOM_DIR
import com.tencent.devops.common.api.exception.ExecuteException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.store.pojo.atom.AtomEnv
import com.tencent.devops.store.pojo.atom.AtomEnvRequest
import com.tencent.devops.store.pojo.common.SensitiveConfResp
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import com.tencent.devops.worker.common.api.archive.ARCHIVE_PROPS_BUILD_ID
import com.tencent.devops.worker.common.api.archive.ARCHIVE_PROPS_BUILD_NO
import com.tencent.devops.worker.common.api.archive.ARCHIVE_PROPS_PIPELINE_ID
import com.tencent.devops.worker.common.api.archive.ARCHIVE_PROPS_PROJECT_ID
import com.tencent.devops.worker.common.api.archive.ARCHIVE_PROPS_SOURCE
import com.tencent.devops.worker.common.api.archive.ARCHIVE_PROPS_USER_ID
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.utils.ArchiveUtils
import okhttp3.MediaType
import okhttp3.RequestBody
import java.io.File
import java.net.URLEncoder

class AtomArchiveResourceApi : AbstractBuildResourceApi(), AtomArchiveSDKApi {

    /**
     * 获取插件信息
     */
    override fun getAtomEnv(projectCode: String, atomCode: String, atomVersion: String): Result<AtomEnv> {
        val path = "/ms/store/api/build/market/atom/env/$projectCode/$atomCode/$atomVersion"
        val request = buildGet(path)

        val responseContent = request(request, "获取插件执行环境信息失败")
        return objectMapper.readValue(responseContent)
    }

    /**
     * 更新插件执行环境信息
     */
    override fun updateAtomEnv(
        projectCode: String,
        atomCode: String,
        atomVersion: String,
        atomEnvRequest: AtomEnvRequest
    ): Result<Boolean> {
        val path = "/ms/store/api/build/market/atom/env/$projectCode/$atomCode/$atomVersion"
        val body = RequestBody.create(
            MediaType.parse("application/json; charset=utf-8"),
            objectMapper.writeValueAsString(atomEnvRequest)
        )
        val request = buildPut(path, body)
        val responseContent = request(request, "更新插件执行环境信息失败")
        return objectMapper.readValue(responseContent)
    }

    /**
     * 获取插件插件敏感信息
     */
    override fun getAtomSensitiveConf(atomCode: String): Result<List<SensitiveConfResp>?> {
        val path = "/ms/store/api/build/store/sensitiveConf/types/ATOM/codes/$atomCode"
        val request = buildGet(path)
        val responseContent = request(request, "获取插件敏感信息失败")
        return objectMapper.readValue(responseContent)
    }

    override fun archiveAtom(
        filePath: String,
        destPath: String,
        workspace: File,
        buildVariables: BuildVariables
    ): String? {
        val files = ArchiveUtils.matchFiles(workspace, filePath.trim())
        if (files.isEmpty()) {
            throw ExecuteException("no found atom file: $filePath")
        }
        if (files.size > 1) {
            throw ExecuteException("too many(${files.size}) atom file: $filePath")
        }
        val file = files[0]
        if (!ArchiveUtils.isFileLegal(file.name)) throw ExecuteException("not allow to archive ${file.name} file")
        uploadAtom(file, destPath, buildVariables)
        return ShaUtils.sha1(file.readBytes())
    }

    override fun uploadAtom(file: File, destPath: String, buildVariables: BuildVariables) {
        val path = if (destPath.trim().endsWith(file.name)) {
            destPath.trim()
        } else {
            destPath.trim().removePrefix("/") + "/" + file.name
        }

        LoggerService.addNormalLine("归档插件文件 >>> ${file.name}")

        val url = StringBuilder("/ms/artifactory/build/atom/result/$path")
        with(buildVariables) {
            url.append(";$ARCHIVE_PROPS_PROJECT_ID=${encodeProperty(projectId)}")
            url.append(";$ARCHIVE_PROPS_PIPELINE_ID=${encodeProperty(pipelineId)}")
            url.append(";$ARCHIVE_PROPS_BUILD_ID=${encodeProperty(buildId)}")
            url.append(";$ARCHIVE_PROPS_USER_ID=${encodeProperty(variables[PIPELINE_START_USER_ID] ?: "")}")
            url.append(";$ARCHIVE_PROPS_BUILD_NO=${encodeProperty(variables[PIPELINE_BUILD_NUM] ?: "")}")
            url.append(";$ARCHIVE_PROPS_SOURCE=pipeline")
        }

        val request = buildPut(url.toString(), RequestBody.create(MediaType.parse("application/octet-stream"), file))
        val responseContent = request(request, "归档插件文件失败")
        try {
            val obj = JsonParser().parse(responseContent).asJsonObject
            if (obj.has("code") && obj["code"].asString != "200") throw RemoteServiceException("${obj["code"]}")
        } catch (ignored: Exception) {
            LoggerService.addNormalLine(ignored.message ?: "")
            throw RemoteServiceException("AtomArchive fail: $responseContent")
        }
    }

    override fun downloadAtom(atomFilePath: String, file: File) {
        val path = "/ms/artifactory/api/build/artifactories/file/download?filePath=${URLEncoder.encode(
            "$BK_CI_ATOM_DIR/$atomFilePath",
            "UTF-8"
        )}"
        val request = buildGet(path)
        download(request, file)
    }
}
