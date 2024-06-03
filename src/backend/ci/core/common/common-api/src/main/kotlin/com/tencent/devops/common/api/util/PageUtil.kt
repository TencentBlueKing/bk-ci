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

package com.tencent.devops.common.api.util

import com.tencent.devops.common.api.model.SQLLimit
import com.tencent.devops.common.api.pojo.Page

/**
 * Powered By Tencent
 */
object PageUtil {
    fun convertPageSizeToSQLLimit(page: Int, pageSize: Int): SQLLimit {
        val oneOffsetPage = if (page <= 0) DEFAULT_PAGE else page
        val defaultPageSize = if (pageSize <= 0) DEFAULT_PAGE_SIZE else pageSize
        return SQLLimit((oneOffsetPage - 1) * defaultPageSize, defaultPageSize)
    }

    fun convertPageSizeToSQLMAXLimit(page: Int, pageSize: Int): SQLLimit {
        val oneOffsetPage = if (page <= 0) DEFAULT_PAGE else page
        val defaultPageSize = when {
            pageSize <= 0 -> {
                DEFAULT_PAGE_SIZE
            }
            pageSize > MAX_PAGE_SIZE -> {
                MAX_PAGE_SIZE
            }
            else -> {
                pageSize
            }
        }
        return SQLLimit((oneOffsetPage - 1) * defaultPageSize, defaultPageSize)
    }

    // page & pageSize为空则不分页
    fun convertPageSizeToSQLLimit(page: Int?, pageSize: Int?): SQLLimit {
        val oneOffsetPage = if (page == null || page <= 0) DEFAULT_PAGE else page
        val defaultPageSize = if (pageSize == null || pageSize <= 0) -1 else pageSize
        return SQLLimit((oneOffsetPage - 1) * defaultPageSize, defaultPageSize)
    }

    // 限制pageSize大小最大为100
    fun convertPageSizeToSQLLimitMaxSize(page: Int, pageSize: Int): SQLLimit {
        val oneOffsetPage = if (page <= 0) 1 else page
        val defaultPageSize = when {
            pageSize <= 0 -> 10
            pageSize > 50 -> 50
            else -> pageSize
        }
        return SQLLimit((oneOffsetPage - 1) * defaultPageSize, defaultPageSize)
    }

    /**
     * 计算总页数
     * @param pageSize 分页数据大小
     * @param totalSize 总数
     */
    fun calTotalPage(pageSize: Int?, totalSize: Long): Int {
        var totalPages = 1L
        if (null != pageSize && -1 != pageSize) {
            val flag = totalSize % pageSize
            totalPages = if (flag == 0L) {
                totalSize / pageSize
            } else {
                totalSize / pageSize + 1
            }
        }
        return totalPages.toInt()
    }

    const val DEFAULT_PAGE = 1
    const val DEFAULT_PAGE_SIZE = 10
    const val MAX_PAGE_SIZE = 100

    fun getValidPage(page: Int?): Int {
        var validPage = page
        if (validPage == null || validPage <= 0) {
            validPage = DEFAULT_PAGE
        }
        return validPage
    }

    fun getValidPageSize(pageSize: Int?): Int {
        var validPageSize = pageSize
        if (validPageSize == null || validPageSize <= 0) {
            validPageSize = DEFAULT_PAGE_SIZE
        }
        return validPageSize
    }

    /**
     * 本地内存中分页，返回Page对象
     */
    fun <T> pageList(
        list: List<T>,
        page: Int? = DEFAULT_PAGE,
        pageSize: Int? = DEFAULT_PAGE_SIZE,
        totalCount: Long?
    ): Page<T> {
        // 参数校验
        val validPage = getValidPage(page)
        val validPageSize = getValidPageSize(pageSize)
        var validTotalCount = totalCount
        if (validTotalCount == null || validTotalCount <= 0) {
            validTotalCount = list.size.toLong()
        }
        val offset = (validPage - 1) * validPageSize
        // 分页
        val pagedList = when {
            offset >= list.size -> emptyList()
            else -> {
                val toIndex = if (list.size <= (offset + validPageSize)) list.size else offset + validPageSize
                list.subList(offset, toIndex)
            }
        }
        var totalPages = validTotalCount / validPageSize
        if (totalPages * validPage < validTotalCount) {
            totalPages++
        }
        return Page(
            count = validTotalCount,
            page = validPage,
            pageSize = validPageSize,
            totalPages = totalPages.toInt(),
            records = pagedList
        )
    }
}
