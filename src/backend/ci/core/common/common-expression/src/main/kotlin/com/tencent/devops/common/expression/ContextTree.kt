package com.tencent.devops.common.expression

import java.util.LinkedList
import java.util.Queue
import java.util.Stack

/**
 * 用来将流水线变量转为树的形式，来对其转换到表达式引擎做兼容处理
 * 如 a.b.c = 1 的变量转换后就是 a->b->c = 1
 * 树中节点转换时默认会使用 map，除非其节点对应的json明确声明了当前节点是数组
 * @param nodes 所有的树根节点
 */
class ContextTree(
    val nodes: MutableMap<String, ContextTreeNode> = mutableMapOf()
) {
    fun addNode(key: String, value: String) {
        val tokens = key.split(".")
        // 不包含 . 计算符
        if (tokens.size == 1) {
            nodes[key] = ContextTreeNode(key, value)
            return
        }

        val rootKey = tokens.first()
        // 如果之前的节点中没有根节点直接添加
        val rootNode = nodes[rootKey]
        if (rootNode == null) {
            nodes[rootKey] = toTree(tokens, value)
            return
        }
        // 根节点已经存在了需要加入进去，同时做校验
        // 1、命中存在 key 的节点，无值赋值，有值报错
        // 2、命中没有 key 的节点，在最后一个找不到 key 的父结点下添加没找到的 key 的树
        var tokenIdx = 1
        var parentNode: ContextTreeNode = rootNode
        var valueNode: ContextTreeNode? = null
        while (tokenIdx < tokens.size) {
            var endFlag = true
            parentNode.breadthFirstTraversal { n ->
                // 还没有找到当前层
                if (n.depth < tokenIdx) {
                    return@breadthFirstTraversal false
                }
                // 过了当前层还没找到说明需要在这一层的父结点上面加从这一层开始的内容
                if (n.depth > tokenIdx) {
                    return@breadthFirstTraversal true
                }
                // 在当前层找,如果能找到就继续找这个节点的下一层
                // 如果是token最后一层则赋值，如果存在原本有值的情况则报错
                if (n.key == tokens[tokenIdx]) {
                    if (tokenIdx != tokens.lastIndex) {
                        tokenIdx++
                        parentNode = n
                        endFlag = false
                        return@breadthFirstTraversal true
                    }
                    if (n.value != null) {
                        // TODO: 自定异常
                        throw RuntimeException()
                    }
                    valueNode = n
                    return@breadthFirstTraversal true
                }
                parentNode = n.parent ?: rootNode
                return@breadthFirstTraversal false
            }
            if (endFlag) {
                break
            }
        }

        if (valueNode != null) {
            valueNode!!.value = value
        } else {
            parentNode.addChild(toTree(tokens.subList(tokenIdx, tokens.size), value))
        }
    }

    private fun toTree(tokens: List<String>, value: String): ContextTreeNode {
        if (tokens.size == 1) {
            return ContextTreeNode(tokens[0], value)
        }
        val node = ContextTreeNode(tokens[0], null)
        val valueTokens = tokens.subList(1, tokens.size)
        valueTokens.forEachIndexed { idx, t ->
            node.addChild(
                ContextTreeNode(
                    key = t,
                    value = if (idx == valueTokens.lastIndex) {
                        value
                    } else {
                        null
                    }
                )
            )
        }
        return node
    }
}

/**
 * 树节点
 * @param key 变量名
 * @param value 变量值，存在为空情况即用户没有具体存入值但是拥有计算逻辑的节点，如 a.b.c = 1 中的a和b
 * @param array 当前节点是否是数组节点
 * @param parent 父节点
 * @param children 所有子节点
 * @param depth 深度
 */
open class ContextTreeNode(
    val key: String,
    var value: String?,
    private val array: Boolean = false,
    var parent: ContextTreeNode? = null,
    private val children: MutableList<ContextTreeNode> = mutableListOf(),
    var depth: Int = 0
) {

    fun addChild(child: ContextTreeNode) {
        child.parent = this
        child.depth = depth + 1
        children.add(child)
    }

    fun depthFirstTraversal(run: (node: ContextTreeNode) -> Unit) {
        val stack = Stack<ContextTreeNode>()
        stack.push(this)

        while (!stack.isEmpty()) {
            val node = stack.pop()

            run(node)

            // 将子节点逆序入栈，保证先访问左边的子节点
            for (i in node.children.size - 1 downTo 0) {
                stack.push(node.children[i])
            }
        }
    }

    fun breadthFirstTraversal(run: (node: ContextTreeNode) -> Boolean) {
        val queue: Queue<ContextTreeNode> = LinkedList()
        queue.offer(this)

        while (!queue.isEmpty()) {
            val node = queue.poll()

            if (run(node)) {
                return
            }

            for (child in node.children) {
                queue.offer(child)
            }
        }
    }
}