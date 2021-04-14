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

package com.tencent.bkrepo.common.query.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.tencent.bkrepo.common.api.constant.DEFAULT_PAGE_NUMBER
import com.tencent.bkrepo.common.api.constant.DEFAULT_PAGE_SIZE
import com.tencent.bkrepo.common.api.util.CompatibleUtils
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("分页参数")
data class PageLimit(
    @ApiModelProperty("当前页")
    val pageNumber: Int = DEFAULT_PAGE_NUMBER,
    @ApiModelProperty("每页数量")
    val pageSize: Int = DEFAULT_PAGE_SIZE,

    @Deprecated("Replace with pageNumber", replaceWith = ReplaceWith("pageNumber"))
    @ApiModelProperty("当前页")
    val current: Int? = null,
    @Deprecated("Replace with pageSize", replaceWith = ReplaceWith("pageSize"))
    @ApiModelProperty("每页数量")
    val size: Int? = null
) {
    @JsonIgnore
    fun getNormalizedPageNumber(): Int {
        val pageNumber = CompatibleUtils.getValue(pageNumber, current, "PageLimit.current")
        return if (pageNumber <= 0) DEFAULT_PAGE_NUMBER else pageNumber
    }

    @JsonIgnore
    fun getNormalizedPageSize(): Int {
        val pageSize = CompatibleUtils.getValue(pageSize, size, "PageLimit.size")
        return if (pageSize <= 0) DEFAULT_PAGE_SIZE else pageSize
    }
}
