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

package com.tencent.devops.common.api.enums

import com.fasterxml.jackson.annotation.JsonIgnore
import com.tencent.devops.common.api.exception.ParamBlankException
import io.swagger.annotations.ApiModelProperty
import java.net.URLEncoder

/**
 * deng
 * 2019-03-01
 */
class RepositoryConfig(
    val repositoryHashId: String?,
    @ApiModelProperty("新版的git代码库名")
    val repositoryName: String?,
    @ApiModelProperty("新版的git插件的类型")
    val repositoryType: RepositoryType
) {
    @JsonIgnore
    fun getRepositoryId(): String {
        return when (repositoryType) {
            RepositoryType.ID -> if (repositoryHashId.isNullOrBlank()) {
                throw ParamBlankException("代码库HashId为空")
            } else {
                repositoryHashId!!
            }
            RepositoryType.NAME -> if (repositoryName.isNullOrBlank()) {
                throw ParamBlankException("代码库名为空")
            } else {
                repositoryName!!
            }
        }
    }

    @JsonIgnore
    fun getURLEncodeRepositoryId(): String = URLEncoder.encode(getRepositoryId(), "UTF-8")

    override fun toString(): String {
        return "[repositoryHashId=$repositoryHashId, repositoryName=$repositoryName, repositoryType=$repositoryType]"
    }
}