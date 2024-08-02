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

package com.tencent.devops.process.yaml.v2.parsers.template.models

import com.tencent.devops.process.yaml.v2.exception.YamlFormatException

class TemplateDeepTreeNode(
    val path: String,
    val parent: TemplateDeepTreeNode?,
    val children: MutableList<TemplateDeepTreeNode>
) {
    companion object {
        // 模板最多引用数和最大深度
        const val MAX_TEMPLATE_NUMB = 10
        const val MAX_TEMPLATE_DEEP = 5

        const val TEMPLATE_NUMB_BEYOND =
            "[%s]The number of referenced template files exceeds the threshold [$MAX_TEMPLATE_NUMB] "
        const val TEMPLATE_DEEP_BEYOND =
            "[%s]The template nesting depth exceeds the threshold [$MAX_TEMPLATE_DEEP}]"
    }

    fun add(nodePath: String): TemplateDeepTreeNode {
        val node = TemplateDeepTreeNode(
            path = nodePath,
            parent = this,
            children = mutableListOf()
        )
        children.add(
            node
        )
        if (node.getDeep() > MAX_TEMPLATE_DEEP) {
            throw YamlFormatException(TEMPLATE_DEEP_BEYOND.format(getRoot().toStr("", true)))
        }
        if (node.getWidth() > MAX_TEMPLATE_NUMB) {
            throw YamlFormatException(TEMPLATE_NUMB_BEYOND.format(getRoot().toStr("", true)))
        }
        return node
    }

    fun toStr(prefix: String?, isTail: Boolean): String {
        val str = StringBuilder()
        str.append(prefix + (if (isTail) "└── " else "├── ") + path + "\n")
        for (i in 0 until children.size - 1) {
            str.append(children[i].toStr(prefix + if (isTail) "    " else "│   " + "\n", false))
        }
        if (children.size > 0) {
            str.append(
                children[children.size - 1].toStr(prefix + if (isTail) "    " else "│   " + "\n", true)
            )
        }
        return str.toString()
    }

    private fun getDeep(): Int {
        var length = 1
        var itor = this
        while (itor.parent != null) {
            itor = itor.parent!!
            length++
        }
        return length
    }

    private fun getWidth(): Int {
        return children.size
    }

    private fun getRoot(): TemplateDeepTreeNode {
        var itor = this
        while (itor.parent != null) {
            itor = itor.parent!!
        }
        return itor
    }
}
