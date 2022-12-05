/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.scanner.task.iterator

import com.tencent.bkrepo.common.api.constant.DEFAULT_PAGE_NUMBER
import com.tencent.bkrepo.common.api.constant.DEFAULT_PAGE_SIZE

/**
 * 分页数据迭代器
 */
abstract class PageableIterator<T>(
    open val position: PageIteratePosition = PageIteratePosition()
) : Iterator<T> {

    private var data: List<T> = emptyList()

    /**
     * 没有下一项标记位，true表示没有下一项，false表示未知或者还有下一项
     */
    private var noNext: Boolean = false

    override fun hasNext(): Boolean {
        if (noNext) {
            return false
        }
        val pageData = pageData()
        if (pageData.isEmpty()) {
            noNext = true
            return false
        }
        return true
    }

    override fun next(): T {
        try {
            return pageData()[++position.index]
        } catch (e: IndexOutOfBoundsException) {
            throw NoSuchElementException()
        }
    }

    /**
     * 当前正在遍历的分页数据，如果已经遍历完了则拉取新的分页数据，没有新的分页数据则返回empty list
     */
    private fun pageData(): List<T> {
        with(position) {
            if (index == data.size - 1) {
                data = nextPageData(++page, pageSize)
                index = INITIAL_INDEX
            }
        }

        return data
    }

    /**
     * 获取下一页数据
     *
     * @param page 请求的数据页码
     * @param pageSize 请求的数据页大小
     * @return 下一页数据，返回empty list时表示没有下一页数据
     */
    abstract fun nextPageData(page: Int, pageSize: Int): List<T>

    /**
     * 分页数据遍历位置
     */
    open class PageIteratePosition(
        /**
         * 当前正在遍历的页
         */
        open var page: Int = INITIAL_PAGE,
        /**
         * 遍历页大小
         */
        open var pageSize: Int = DEFAULT_PAGE_SIZE,
        /**
         * 当前正在遍历的页数据下标
         */
        open var index: Int = INITIAL_INDEX
    )

    companion object {
        /**
         * 初始页DEFAULT_PAGE_NUMBER - 1表示没有已扫描的页
         */
        const val INITIAL_PAGE = DEFAULT_PAGE_NUMBER - 1

        /**
         * 初始-1表示没有已扫描的文件
         */
        const val INITIAL_INDEX = -1
    }
}
