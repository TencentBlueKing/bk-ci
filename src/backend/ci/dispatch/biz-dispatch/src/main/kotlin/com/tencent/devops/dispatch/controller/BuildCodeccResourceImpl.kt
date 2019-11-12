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

package com.tencent.devops.dispatch.controller

import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.api.BuildCodeccResource
import com.tencent.devops.dispatch.service.CodeccDownloaderService
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
class BuildCodeccResourceImpl @Autowired constructor(
    private val codeccDownloaderService: CodeccDownloaderService
) : BuildCodeccResource {
    override fun downloadTool(toolName: String, osType: OSType, fileMd5: String, is32Bit: Boolean?): Response {
        return codeccDownloaderService.downloadTool(toolName, osType, fileMd5, is32Bit)
    }

    override fun downloadCovScript(osType: OSType, fileMd5: String): Response {
        return codeccDownloaderService.downloadCovScript(osType, fileMd5)
    }

    override fun downloadToolsScript(osType: OSType, fileMd5: String): Response {
        return codeccDownloaderService.downloadToolsScript(osType, fileMd5)
    }
}