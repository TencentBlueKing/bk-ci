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

package com.tencent.devops.common.auth.callback

import com.tencent.bk.sdk.iam.dto.callback.response.BaseDataResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.response.SearchInstanceResponseDTO

class SearchInstanceInfo : SearchInstanceResponseDTO() {

    fun buildSearchInstanceFailResult(): SearchInstanceInfo {
        val data = BaseDataResponseDTO<InstanceInfoDTO>()
        val result = SearchInstanceInfo()
        result.code = 0
        result.message = "empty data"
        result.data = data
        return result
    }

    // 关键词太短
    fun buildSearchInstanceKeywordFailResult(): SearchInstanceInfo {
        val result = SearchInstanceInfo()
        result.code = AuthConstants.KEYWORD_SHORT
        result.message = AuthConstants.KEYWORD_SHORT_MESSAGE
        return result
    }

    fun buildSearchInstanceResult(infos: List<InstanceInfoDTO>, count: Long): SearchInstanceInfo {
        val data = BaseDataResponseDTO<InstanceInfoDTO>()
        data.count = count
        data.result = infos
        val result = SearchInstanceInfo()
        result.code = 0L
        result.message = ""
        result.data = data
        return result
    }
}
