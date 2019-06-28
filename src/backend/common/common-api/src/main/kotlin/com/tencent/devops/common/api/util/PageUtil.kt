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

package com.tencent.devops.common.api.util

import com.tencent.devops.common.api.model.SQLLimit

/**
 * Powered By Tencent
 */
object PageUtil {
    fun convertPageSizeToSQLLimit(page: Int, pageSize: Int): SQLLimit {
        val oneOffsetPage = if (page <= 0) 1 else page
        val defaultPageSize = if (pageSize <= 0) 10 else pageSize
        return SQLLimit((oneOffsetPage - 1) * defaultPageSize, defaultPageSize)
    }

    // page & pageSize为空则不分页
    fun convertPageSizeToSQLLimit(page: Int?, pageSize: Int?): SQLLimit {
        val oneOffsetPage = if (page == null || page <= 0) 1 else page
        val defaultPageSize = if (pageSize == null || pageSize <= 0) -1 else pageSize
        return SQLLimit((oneOffsetPage - 1) * defaultPageSize, defaultPageSize)
    }

    /**
     * 计算总页数
     * @param pageSize 分页数据大小
     * @param totalSize 总数
     */
    fun calTotalPage(pageSize: Int?, totalSize: Long): Int {
        var totalPages = 1L
        if (null != pageSize) {
            val flag = totalSize % pageSize
            totalPages = if (flag == 0L) {
                totalSize / pageSize
            } else {
                totalSize / pageSize + 1
            }
        }
        return totalPages.toInt()
    }
}