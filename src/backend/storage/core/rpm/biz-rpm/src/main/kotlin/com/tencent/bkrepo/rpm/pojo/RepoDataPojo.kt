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

package com.tencent.bkrepo.rpm.pojo

import com.tencent.bkrepo.rpm.REPODATA
import com.tencent.bkrepo.rpm.exception.RpmRepoDataException
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * repodataPath : 索引目录的父级路径 `/`开头，`/`结尾
 * artifactRelativePath：保存在索引中的路径 ， 不要`/`
 */
@ApiModel("索引目录数据类")
data class RepoDataPojo(
    @ApiModelProperty("契合本次请求的repodata_depth 目录路径")
    val repoDataPath: String,
    @ApiModelProperty("构件相对于索引文件的保存路径")
    val artifactRelativePath: String
) {

    init {
        checkRepoDataPathFormat(repoDataPath)
        checkArtifactRelativePathFormat(artifactRelativePath)
    }

    private fun checkRepoDataPathFormat(repoDataPath: String) {
        if (!(repoDataPath.endsWith("/") && repoDataPath.startsWith("/"))) {
            throw RpmRepoDataException("$repoDataPath is invalid")
        }
    }
    private fun checkArtifactRelativePathFormat(artifactRelativePath: String) {
        if (artifactRelativePath.startsWith("/")) {
            throw RpmRepoDataException("$repoDataPath is invalid")
        }
    }

    override fun toString(): String {
        return "$repoDataPath$artifactRelativePath"
    }

    fun getMarkPath(indexType: IndexType): String {
        return if (repoDataPath == "/") {
            "/$REPODATA/${indexType.value}/$artifactRelativePath"
        } else {
            "$repoDataPath$REPODATA/${indexType.value}/$artifactRelativePath"
        }
    }
}
