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

class Graph<T>(
    private val adj: MutableMap<String, MutableList<String>> = mutableMapOf()
) {

    fun addEdge(fromPath: String, toPath: String) {
        if (adj[fromPath] != null) {
            adj[fromPath]!!.add(toPath)
        } else {
            adj[fromPath] = mutableListOf(toPath)
        }
    }

    fun hasCyclic(): Boolean {
        val visited = adj.map { it.key to false }.toMap().toMutableMap()
        val recStack = adj.map { it.key to false }.toMap().toMutableMap()

        for (i in adj.keys) {
            if (hasCyclicUtil(i, visited, recStack)) {
                return true
            }
        }
        return false
    }

    private fun hasCyclicUtil(
        i: String,
        visited: MutableMap<String, Boolean>,
        recStack: MutableMap<String, Boolean>
    ): Boolean {

        if (recStack[i] == null || visited[i] == null) {
            return false
        }

        if (recStack[i]!!) {
            return true
        }

        if (visited[i]!!) {
            return false
        }

        visited[i] = true

        recStack[i] = true

        val children = adj[i]!!

        for (c in children) {
            if (hasCyclicUtil(c, visited, recStack)) {
                return true
            }
        }

        recStack[i] = false

        return false
    }
}
