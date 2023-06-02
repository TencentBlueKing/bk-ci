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

package com.tencent.devops.common.websocket.utils

object PageUtils {
    private fun buildTagPage(page: String, tagName: String): String {
        if (page.endsWith("/")) {
            return page + tagName
        } else {
            return "$page/$tagName"
        }
    }

    private fun replacePage(page: String, newPageTage: String, oldPageTage: String): String? {
        var newPage: String? = null
        if (page.contains(oldPageTage)) {
            newPage = page.replace(oldPageTage, newPageTage)
        }
        return newPage
    }

    fun replaceAssociationPage(page: String): String?
    {
        var newPage: String? = null
        if (page.contains("upgrade")) {
            newPage = replacePage(page, "shelf", "upgrade")
        }
        if (page.contains("shelf")) {
            newPage = replacePage(page, "upgrade", "shelf")
        }
        return newPage
    }

    // 因流水线列表页有三个tag页，故此处区别于另外两种情况，需要推三个页面
    fun createAllTagPage(page: String): MutableList<String> {
        val pageList = mutableListOf<String>()
        pageList.add(page)
        pageList.add(buildTagPage(page, "allPipeline"))
        pageList.add(buildTagPage(page, "collect"))
        pageList.add(buildTagPage(page, "myPipeline"))
        return pageList
    }
}
