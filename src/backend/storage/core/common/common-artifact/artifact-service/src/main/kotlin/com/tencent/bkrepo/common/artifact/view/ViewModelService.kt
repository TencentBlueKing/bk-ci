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

package com.tencent.bkrepo.common.artifact.view

import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.common.artifact.path.PathUtils.UNIX_SEPARATOR
import com.tencent.bkrepo.common.artifact.util.http.UrlFormatter
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.repository.pojo.list.HeaderItem
import com.tencent.bkrepo.repository.pojo.list.ListViewObject
import com.tencent.bkrepo.repository.pojo.list.RowItem
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import org.apache.commons.text.StringEscapeUtils
import java.io.PrintWriter
import java.net.URLEncoder.encode

class ViewModelService(
    private val viewModelProperties: ViewModelProperties
) {

    fun trailingSlash(serviceName: String) {
        val host = viewModelProperties.domain
        val builder = StringBuilder(UrlFormatter.format(host, serviceName))
        val url = builder.append(HttpContextHolder.getRequest().requestURI).toString()
        if (!url.endsWith(UNIX_SEPARATOR)) {
            HttpContextHolder.getResponse().sendRedirect("$url/")
        }
    }

    fun computeCurrentPath(currentNode: NodeDetail): String {
        val builder = StringBuilder()
        builder.append(UNIX_SEPARATOR)
            .append(currentNode.projectId)
            .append(UNIX_SEPARATOR)
            .append(currentNode.repoName)
            .append(currentNode.fullPath)
        if (!PathUtils.isRoot(currentNode.fullPath)) {
            builder.append(UNIX_SEPARATOR)
        }
        return builder.toString()
    }

    fun render(title: String, headerList: List<HeaderItem>, rowList: List<RowItem>) {
        writePageContent(ListViewObject(title, headerList, rowList, FOOTER, true))
    }

    private fun writePageContent(listViewObject: ListViewObject) {
        with(listViewObject) {
            val response = HttpContextHolder.getResponse()
            response.contentType = MediaTypes.TEXT_HTML
            val writer = response.writer
            val headerContent = buildHeaderContent(this)
            writer.println(FIRST_PART.format(title, title, headerContent).trimIndent())
            writeListContent(this, writer)
            writer.println(LAST_PART.trimIndent())
        }
    }

    private fun buildHeaderContent(listViewObject: ListViewObject): String {
        with(listViewObject) {
            val builder = StringBuilder()
            headerList.forEachIndexed { index, header ->
                header.width = computeColumnWidth(header, rowList, index)
                builder.append(header.name.padEnd(header.width!! + GAP))
            }
            return builder.toString()
        }
    }

    private fun writeListContent(listViewObject: ListViewObject, writer: PrintWriter) {
        with(listViewObject) {
            if (backTo) {
                writer.println(BACK_TO)
            }
            if (rowList.isEmpty()) {
                writer.print(EMPTY_CONTENT)
            }
            rowList.forEachIndexed { rowIndex, row ->
                row.itemList.forEachIndexed { columnIndex, item ->
                    if (columnIndex == 0) {
                        val escapedItem = StringEscapeUtils.escapeXml11(item)
                        // 不对末尾的'/'进行URLEncode，避免在访问目录链接时触发重定向
                        val encodedItem = if (item.endsWith(UNIX_SEPARATOR)) {
                            encode(item.substring(0, item.length - 1), Charsets.UTF_8.name()) + UNIX_SEPARATOR
                        } else {
                            encode(item, Charsets.UTF_8.name())
                        }
                        writer.print("""<a href="$encodedItem">$escapedItem</a>""")
                        writer.print(" ".repeat(headerList[columnIndex].width!! - item.length))
                    } else {
                        writer.print(item.padEnd(headerList[columnIndex].width!!))
                    }
                    writer.print(" ".repeat(GAP))
                }
                if (rowIndex != rowList.size - 1) {
                    writer.println()
                }
            }
        }
    }

    private fun computeColumnWidth(header: HeaderItem, rowList: List<RowItem>, index: Int): Int {
        var maxLength = header.name.length
        rowList.forEach {
            if (it.itemList[index].length > maxLength) {
                maxLength = it.itemList[index].length
            }
        }
        return maxLength
    }

    companion object {
        private const val GAP = 4
        private const val FOOTER = "BlueKing Repository"
        private const val BACK_TO =
            """<a href="../">../</a>"""
        private const val EMPTY_CONTENT = "\nEmpty content."
        private const val FIRST_PART =
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Index of %s</title>
            </head>
            <body>
                <h1>Index of %s</h1>
                <hr/>
                <pre>%s</pre>
                <hr/>
                <pre>
        """

        private const val LAST_PART =
            """
                </pre>
                <hr/>
                <address style="font-size:small;">$FOOTER</address>
            </body>
            </html>
        """
    }
}
