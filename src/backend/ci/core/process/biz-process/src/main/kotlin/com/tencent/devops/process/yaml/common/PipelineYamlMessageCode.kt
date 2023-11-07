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
 *
 */

package com.tencent.devops.process.yaml.common

object PipelineYamlMessageCode {
    const val CI_DISABLED = "ciDisabled"
    const val CI_YAML_NOT_FOUND = "ciYamlNotFound"

    const val GET_PROJECT_INFO_ERROR = "getProjectInfoError"
    const val GET_GIT_MERGE_INFO = "getGitMergeInfo"
    const val GET_GIT_MERGE_REVIEW_INFO = "getGitMergeReviewInfo"
    const val GET_GIT_MERGE_CHANGE_INFO = "getGitMergeChangeInfo"
    const val GET_GIT_FILE_TREE_ERROR = "getGitFileTreeError"
    const val GET_YAML_CONTENT_ERROR = "getYamlContentError"
    const val GET_GIT_FILE_INFO_ERROR = "getGitFileInfoError"


    const val CI_YAML_NEED_MERGE_OR_REBASE = "ciYamlNeedMergeOrRebase"

    const val DEVNET_TIMEOUT_ERROR = "devnetTimeoutError"

    const val UNKNOWN_ERROR = "unknownError"
}
