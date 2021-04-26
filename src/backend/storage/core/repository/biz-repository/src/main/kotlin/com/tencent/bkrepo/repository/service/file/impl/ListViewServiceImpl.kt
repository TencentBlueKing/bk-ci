/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.repository.service.file.impl

import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.common.artifact.path.PathUtils.UNIX_SEPARATOR
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.repository.pojo.list.HeaderItem
import com.tencent.bkrepo.repository.pojo.list.ListViewObject
import com.tencent.bkrepo.repository.pojo.list.RowItem
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.NodeListOption
import com.tencent.bkrepo.repository.pojo.node.NodeListViewItem
import com.tencent.bkrepo.repository.pojo.project.ProjectListViewItem
import com.tencent.bkrepo.repository.pojo.repo.RepoListViewItem
import com.tencent.bkrepo.repository.service.file.ListViewService
import com.tencent.bkrepo.repository.service.node.NodeService
import com.tencent.bkrepo.repository.service.repo.ProjectService
import com.tencent.bkrepo.repository.service.repo.RepositoryService
import org.apache.commons.lang.StringEscapeUtils
import org.springframework.stereotype.Service
import java.io.PrintWriter

/**
 * 列表视图服务
 */
@Service
class ListViewServiceImpl(
    private val projectService: ProjectService,
    private val repositoryService: RepositoryService,
    private val nodeService: NodeService
) : ListViewService {

    override fun listNodeView(artifactInfo: ArtifactInfo) {
        val node = nodeService.getNodeDetail(artifactInfo)
            ?: throw ErrorCodeException(ArtifactMessageCode.NODE_NOT_FOUND, artifactInfo.getArtifactFullPath())
        val response = HttpContextHolder.getResponse()
        response.contentType = MediaTypes.TEXT_HTML
        if (node.folder) {
            trailingSlash()
            val listOption = NodeListOption(
                includeFolder = true,
                includeMetadata = false,
                deep = false
            )
            val nodeList = nodeService.listNode(artifactInfo, listOption)
            val currentPath = computeCurrentPath(node)
            val headerList = listOf(
                HeaderItem("Name"),
                HeaderItem("Created by"),
                HeaderItem("Last modified"),
                HeaderItem("Size"),
                HeaderItem("Sha256")
            )
            val itemList = nodeList.map { NodeListViewItem.from(it) }.sorted()
            val rowList = itemList.map { RowItem(listOf(it.name, it.createdBy, it.lastModified, it.size, it.sha256)) }
            writePageContent(ListViewObject(currentPath, headerList, rowList, FOOTER, true))
        } else {
            val context = ArtifactDownloadContext(useDisposition = false)
            ArtifactContextHolder.getRepository().download(context)
        }
    }

    override fun listRepoView(projectId: String) {
        trailingSlash()
        val itemList = repositoryService.listRepo(projectId).map { RepoListViewItem.from(it) }
        val title = "Repository[$projectId]"
        val headerList = listOf(
            HeaderItem("Name"),
            HeaderItem("Created by"),
            HeaderItem("Last modified"),
            HeaderItem("Category"),
            HeaderItem("Type"),
            HeaderItem("Public"),
            HeaderItem("Storage")
        )
        val rowList = itemList.sorted().map {
            RowItem(listOf(it.name, it.createdBy, it.lastModified, it.category, it.type, it.public, it.storage))
        }
        val listViewObject = ListViewObject(title, headerList, rowList, FOOTER, true)
        writePageContent(listViewObject)
    }

    override fun listProjectView() {
        trailingSlash()
        val itemList = projectService.listProject().map { ProjectListViewItem.from(it) }
        val headerList = listOf(
            HeaderItem("Name"),
            HeaderItem("Created by"),
            HeaderItem("Last modified"),
            HeaderItem("Sharding index")
        )
        val rowList = itemList.sorted().map {
            RowItem(listOf(it.name, it.createdBy, it.lastModified, it.shardingIndex))
        }
        val listViewObject = ListViewObject("Project", headerList, rowList, FOOTER, false)
        writePageContent(listViewObject)
    }

    private fun writePageContent(listViewObject: ListViewObject) {
        with(listViewObject) {
            val writer = HttpContextHolder.getResponse().writer
            val headerContent = buildHeaderContent(this)
            writer.println(FIRST_PART.format(title, title, headerContent).trimIndent())
            writeListContent(this, writer)
            writer.println(LAST_PART.trimIndent())
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
                        val escapedItem = StringEscapeUtils.escapeXml(item)
                        writer.print("""<a href="$item">$escapedItem</a>""")
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

    private fun computeCurrentPath(currentNode: NodeDetail): String {
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

    private fun computeColumnWidth(header: HeaderItem, rowList: List<RowItem>, index: Int): Int {
        var maxLength = header.name.length
        rowList.forEach {
            if (it.itemList[index].length > maxLength) {
                maxLength = it.itemList[index].length
            }
        }
        return maxLength
    }

    private fun trailingSlash() {
        val url = HttpContextHolder.getRequest().requestURL.toString()
        if (!url.endsWith(UNIX_SEPARATOR)) {
            HttpContextHolder.getResponse().sendRedirect("$url/")
        }
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
