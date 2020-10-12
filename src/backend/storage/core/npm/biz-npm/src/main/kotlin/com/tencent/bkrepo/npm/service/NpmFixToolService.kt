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

package com.tencent.bkrepo.npm.service

import com.google.gson.JsonObject
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.artifact.constant.OCTET_STREAM
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactSearchContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.context.RepositoryHolder
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo
import com.tencent.bkrepo.npm.constants.NAME
import com.tencent.bkrepo.npm.constants.NPM_FILE_FULL_PATH
import com.tencent.bkrepo.npm.constants.NPM_PKG_FULL_PATH
import com.tencent.bkrepo.npm.constants.TIME
import com.tencent.bkrepo.npm.pojo.fixtool.DateTimeFormatResponse
import com.tencent.bkrepo.npm.utils.GsonUtils
import com.tencent.bkrepo.npm.utils.TimeUtil
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class NpmFixToolService {

    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    fun fixDateFormat(artifactInfo: NpmArtifactInfo, pkgName: String): DateTimeFormatResponse {
        val pkgNameSet = pkgName.split(',').filter { it.isNotBlank() }.map { it.trim() }.toMutableSet()
        logger.info("fix time format with package: $pkgNameSet, size : [${pkgNameSet.size}]")
        val successSet = mutableSetOf<String>()
        val errorSet = mutableSetOf<String>()
        val context = ArtifactSearchContext()
        val repository = RepositoryHolder.getRepository(context.repositoryInfo.category)
        pkgNameSet.forEach { it ->
            try {
                val fullPath = String.format(NPM_PKG_FULL_PATH, it)
                context.contextAttributes[NPM_FILE_FULL_PATH] = fullPath
                val pkgFileInfo = repository.search(context) as? JsonObject
                if (pkgFileInfo == null) {
                    errorSet.add(it)
                    return@forEach
                }
                val timeJsonObject = pkgFileInfo[TIME].asJsonObject
                timeJsonObject.entrySet().forEach {
                    if (!it.value.asString.contains('T')) {
                        timeJsonObject.add(it.key, GsonUtils.gson.toJsonTree(formatDateTime(it.value.asString)))
                    }
                }
                reUploadPkgJson(pkgFileInfo)
                successSet.add(it)
            } catch (ignored: Exception) {
                errorSet.add(it)
            }
        }
        return DateTimeFormatResponse(successSet, errorSet)
    }

    private fun reUploadPkgJson(pkgFileInfo: JsonObject) {
        val name = pkgFileInfo[NAME].asString
        val pkgMetadata = ArtifactFileFactory.build(GsonUtils.gsonToInputStream(pkgFileInfo))
        val context = ArtifactUploadContext(pkgMetadata)
        val fullPath = String.format(NPM_PKG_FULL_PATH, name)
        context.contextAttributes[OCTET_STREAM + "_full_path"] = fullPath
        val repository = RepositoryHolder.getRepository(context.repositoryInfo.category)
        repository.upload(context)
    }

    fun formatDateTime(time: String): String {
        val dateFormat = "yyyy-MM-dd HH:mm:ss.SSS'Z'"
        val dateTime = LocalDateTime.parse(time, DateTimeFormatter.ofPattern(dateFormat))
        return TimeUtil.getGMTTime(dateTime)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NpmFixToolService::class.java)
    }
}
