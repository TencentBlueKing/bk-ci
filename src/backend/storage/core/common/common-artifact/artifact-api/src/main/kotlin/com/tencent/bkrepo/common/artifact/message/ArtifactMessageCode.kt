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

package com.tencent.bkrepo.common.artifact.message

import com.tencent.bkrepo.common.api.message.MessageCode

/**
 * 构件相关错误码
 */
enum class ArtifactMessageCode(private val key: String) : MessageCode {
    PROJECT_NOT_FOUND("artifact.project.notfound"),
    PROJECT_EXISTED("artifact.project.existed"),
    REPOSITORY_NOT_FOUND("artifact.repository.notfound"),
    REPOSITORY_EXISTED("artifact.repository.existed"),
    REPOSITORY_CONTAINS_FILE("artifact.repository.contains-file"),
    NODE_NOT_FOUND("artifact.node.notfound"),
    NODE_PATH_INVALID("artifact.node.path.invalid"),
    NODE_EXISTED("artifact.node.existed"),
    NODE_CONFLICT("artifact.node.conflict"),
    NODE_LIST_TOO_LARGE("artifact.node.list.too-large");

    override fun getBusinessCode() = ordinal + 1
    override fun getKey() = key
    override fun getModuleCode() = 10
}
