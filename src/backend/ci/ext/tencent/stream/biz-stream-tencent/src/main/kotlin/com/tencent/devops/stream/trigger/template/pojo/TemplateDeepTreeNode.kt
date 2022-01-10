package com.tencent.devops.stream.trigger.template.pojo

import com.tencent.devops.common.ci.v2.exception.YamlFormatException

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
