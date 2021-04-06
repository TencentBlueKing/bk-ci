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

package com.tencent.bkrepo.common.mongo.dao.util

import com.tencent.bkrepo.common.api.constant.DEFAULT_PAGE_NUMBER
import com.tencent.bkrepo.common.api.constant.DEFAULT_PAGE_SIZE
import com.tencent.bkrepo.common.api.pojo.Page
import org.springframework.data.domain.PageRequest

/**
 * 分页工具类
 */
object Pages {

    /**
     * 根据页码[page]和分页大小[size]构造[PageRequest]
     *
     * [page]从1开始，如果传入值小于1则置为1
     * [size]如果小于0则置为默认分页大小20
     *
     * [PageRequest]要求页码从0开始
     */
    fun ofRequest(page: Int, size: Int): PageRequest {
        val pageNumber = if (page <= 0) DEFAULT_PAGE_NUMBER else page
        val pageSize = if (page < 0) DEFAULT_PAGE_SIZE else size
        return PageRequest.of(pageNumber - 1, pageSize)
    }

    /**
     * 创建分页响应结果
     */
    inline fun <reified T> ofResponse(pageRequest: PageRequest, totalRecords: Long, records: List<T>): Page<T> {
        return Page(
            pageNumber = pageRequest.pageNumber + 1,
            pageSize = pageRequest.pageSize,
            totalRecords = totalRecords,
            records = records
        )
    }
}
