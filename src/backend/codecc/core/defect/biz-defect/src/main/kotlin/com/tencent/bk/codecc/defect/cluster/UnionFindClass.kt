package com.tencent.bk.codecc.defect.cluster

import com.tencent.bk.codecc.defect.pojo.UnionFindNodeInfo

/**
 * 并查集工具类
 */
class UnionFindClass<T> {

    //用数组加快效率
    private var nodeList: Array<UnionFindNodeInfo<T>>? = null

    /**
     * 初始化并查集分组
     * @param size 并查集数组的长度
     */
    @ExperimentalUnsignedTypes
    fun initArray(size: Int) {
        nodeList = Array(size) { UnionFindNodeInfo<T>(-1, null, null, null) }
    }

    /**
     * 获取特定并查集元素信息
     * @param index 数组所在的位置
     */
    fun getArrayElement(index: Int): UnionFindNodeInfo<T>? {
        return if (index > nodeList!!.size - 1) {
            null
        } else {
            nodeList!![index]
        }
    }

    /**
     * 查找父节点
     * @param x 被查找节点的索引
     */
    fun findRoot(x: Int): Int {
        if (nodeList!![x].parentIndex < 0) {
            return x
        }
        val incurRoot = findRoot(nodeList!![x].parentIndex)
        nodeList!![x].parentIndex = incurRoot
        return incurRoot
    }

    /**
     * 链接集合
     * @param child1 节点一的索引
     * @param child2 节点二的索引
     */
    fun unionCollection(child1: Int, child2: Int) {
        if (findRoot(child1) == findRoot(child2)) {
            return
        }

        if (nodeList!![findRoot(child1)].parentIndex < nodeList!![findRoot(child2)].parentIndex) {
            nodeList!![findRoot(child2)].parentIndex = findRoot(child1)
        } else {
            if (nodeList!![findRoot(child1)].parentIndex == nodeList!![findRoot(child2)].parentIndex) {
                nodeList!![findRoot(child2)].parentIndex = nodeList!![findRoot(child2)].parentIndex - 1
            }
            nodeList!![findRoot(child1)].parentIndex = findRoot(child2)
        }
    }
}