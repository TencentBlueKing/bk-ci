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

import com.tencent.bkrepo.common.api.exception.SystemErrorException
import com.tencent.bkrepo.repository.api.ProjectClient
import org.slf4j.LoggerFactory

class ProjectIdIterator(
    private val projectClient: ProjectClient,
    position: PageIteratePosition = PageIteratePosition()
) : PageableIterator<String>(position) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun nextPageData(page: Int, pageSize: Int): List<String> {
        return if (page == FIRST_PAGE) {
            val res = projectClient.listProject()
            if (res.isNotOk()) {
                logger.error("List projects failed: code[${res.code}], message[${res.message}]")
                throw SystemErrorException()
            }
            return res.data!!.map { it.name }
        } else {
            emptyList()
        }
    }

    companion object {
        private const val FIRST_PAGE = 1
    }
}
