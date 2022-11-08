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

package com.tencent.devops.scm.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
{
"file_name": "foo.h",
"file_path": "src/controller/",
"size": 15,
"ref": "master",
"blob_id": "37c36524713aa8083f787066a9ed0c0d2f82bbb4",
"commit_id": "b5e3f65af2fd6d2895414a679290cad7664217b3",
"content": "I2lmbmRlZiBXT1JLVFJFRV9ICiNkZWZpbmUgV09SS1RSRUVfSAoKI2luY2x1ZGUgInJlZnMua",
"encoding": "base64"
}
 */

@ApiModel("工蜂文件信息")
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitCodeFileInfo(
    @JsonProperty("file_name")
    @ApiModelProperty(name = "file_name")
    val fileName: String,
    @JsonProperty("file_path")
    @ApiModelProperty(name = "file_path")
    val filePath: String,
    @JsonProperty("size")
    @ApiModelProperty(name = "size")
    val size: Int,
    @JsonProperty("ref")
    @ApiModelProperty(name = "ref")
    val ref: String,
    @JsonProperty("blob_id")
    @ApiModelProperty(name = "blob_id")
    val blobId: String,
    @JsonProperty("commit_id")
    @ApiModelProperty(name = "commit_id")
    val commitId: String,
    @JsonProperty("content")
    @ApiModelProperty(name = "content")
    val content: String,
    @JsonProperty("encoding")
    @ApiModelProperty(name = "encoding")
    val encoding: String
)
