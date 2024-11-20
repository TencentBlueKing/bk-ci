package com.tencent.devops.common.expression

import com.tencent.devops.common.expression.context.ContextValueNode
import com.tencent.devops.common.expression.context.DictionaryContextData
import com.tencent.devops.common.expression.context.DictionaryContextDataWithVal
import com.tencent.devops.common.expression.context.PipelineContextData
import com.tencent.devops.common.expression.context.StringContextData
import com.tencent.devops.common.expression.expression.sdk.NamedValueInfo
import java.util.LinkedList
import java.util.Queue

/**
 * 用来将流水线变量转为树的形式，来对其转换到表达式引擎做兼容处理
 * 如 a.b.c = 1 的变量转换后就是 a->b->c = 1
 * 树中节点转换时默认会使用 map，除非其节点对应的json明确声明了当前节点是数组
 * @param nodes 所有的树根节点
 */
@Suppress("ComplexMethod")
class ContextTree(
    val nodes: MutableMap<String, ContextTreeNode> = mutableMapOf()
) {
    fun addNode(key: String, value: String) {
        val tokens = key.split(".")
        val rootKey = tokens.first()
        // 如果之前的节点中没有根节点直接添加
        val rootNode = nodes[rootKey]
        if (rootNode == null) {
            nodes[rootKey] = toTree(tokens, value)
            return
        } else if (tokens.size == 1) {
            rootNode.value = value
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
                    if (n.value != null && n.value != value) {
                        throw ContextDataRuntimeException("duplicate key ${n.key} value ${n.value}|$value not equal")
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
            parentNode.addChild(toTree(tokens.subList(tokenIdx, tokens.size), value, parentNode.depth + 1))
        }
    }

    /**
     * 将树中节点转换为上下文并添加到输入参数
     * @param context 被添加的上下文
     * @param nameValue 被添加的命名
     */
    fun toContext(context: DictionaryContextData, nameValue: MutableList<NamedValueInfo>) {
        nodes.forEach { (key, value) ->
            context[key] = value.toContext()
            nameValue.add(NamedValueInfo(key, ContextValueNode()))
        }
    }

    private fun toTree(tokens: List<String>, value: String, depth: Int = 0): ContextTreeNode {
        if (tokens.size == 1) {
            return ContextTreeNode(tokens[0], value)
        }
        val rNode = ContextTreeNode(tokens[0], null, depth = depth)
        val valueTokens = tokens.subList(1, tokens.size)
        var node = rNode
        valueTokens.forEachIndexed { idx, t ->
            val child = ContextTreeNode(
                key = t,
                value = if (idx == valueTokens.lastIndex) {
                    value
                } else {
                    null
                }
            )
            node.addChild(child)
            node = child
        }
        return rNode
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

    @Throws(ContextJsonFormatException::class)
    fun toContext(): PipelineContextData {
        if (this.children.isEmpty()) {
            return StringContextData(this.value ?: "")
        }
        val dict = value?.let { DictionaryContextDataWithVal(it) } ?: DictionaryContextData()
        this.children.forEach { child ->
            dict[child.key] = child.toContext()
        }
        return dict
    }
}
