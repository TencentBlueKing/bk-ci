/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.process.pojo.trigger

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "yaml文件变更列表")
data class WebhookChangeFiles(
    @get:Schema(title = "所有的变更文件,包含新增,修改,删除,重命名", required = true)
    val allFiles: Set<String>,
    @get:Schema(title = "新增文件列表", required = true)
    val addedFiles: Set<String>,
    @get:Schema(title = "更新文件列表", required = true)
    val updatedFiles: Set<String>,
    @get:Schema(title = "删除文件列表", required = true)
    val deletedFiles: Set<String>,
    @get:Schema(title = "重命名新旧文件映射", required = true)
    val renamedFiles: Map<String /*new*/, String /*old*/>,
    @get:Schema(title = "重命名旧新文件映射", required = true)
    val renamedOldFiles: Map<String, String>
)
