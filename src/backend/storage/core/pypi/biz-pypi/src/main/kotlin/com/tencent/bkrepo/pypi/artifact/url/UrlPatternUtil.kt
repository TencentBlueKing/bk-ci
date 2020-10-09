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

package com.tencent.bkrepo.pypi.artifact.url

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.bkrepo.pypi.artifact.PypiArtifactInfo
import javax.servlet.http.HttpServletRequest

/**
 * pypi部分请求路径的信息无法定位一个文件，因此在PypiService之前构建一个完整坐标。
 * {package}/{version}/{filename}
 */
object UrlPatternUtil {

    fun HttpServletRequest.parameterMaps(): MutableMap<String, String> {
        val map = this.parameterMap
        val metadata: MutableMap<String, String> = mutableMapOf()
        // 对字符串数组做处理
        for (entry in map) {
            metadata[entry.key] = ObjectMapper().writeValueAsString(entry.value)
        }
        return metadata
    }

    fun fileUpload(
        projectId: String,
        repoName: String,
        request: HttpServletRequest
    ): PypiArtifactInfo {
        val packageName: String = request.getParameter("name")
        val version: String = request.getParameter("version")
        val map = request.parameterMap
        val metadata: MutableMap<String, String> = mutableMapOf()
        // 对字符串数组做处理
        for (entry in map) {
            metadata[entry.key] = ObjectMapper().writeValueAsString(entry.value)
        }
        return PypiArtifactInfo(projectId, repoName, "/$packageName/$version")
    }
}
