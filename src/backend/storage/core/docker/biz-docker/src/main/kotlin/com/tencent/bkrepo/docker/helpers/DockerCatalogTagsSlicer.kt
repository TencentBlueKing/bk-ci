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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.docker.helpers

import java.util.TreeSet

/**
 * catalog slice tags pagination helper
 */
object DockerCatalogTagsSlicer {

    /**
     * slice catalog
     * @param elementsHolder elements holder
     * @param maxEntries max entity
     * @param lastEntry the last entry
     */
    fun sliceCatalog(elementsHolder: DockerPaginationElementsHolder, maxEntries: Int, lastEntry: String) {
        if (elementsHolder.elements.isEmpty()) return

        val fromElement = calcFromElement(elementsHolder, lastEntry)
        if (fromElement.isBlank()) {
            elementsHolder.elements = TreeSet()
            return
        }
        val toElement = calcToElement(elementsHolder, fromElement, maxEntries)
        val elements = elementsHolder.elements
        val lastElement = elementsHolder.elements.last() as String
        val firstElement = elementsHolder.elements.first() as String
        if (fromElement == toElement) {
            elementsHolder.hasMoreElements = (lastElement != toElement)
            elementsHolder.elements = elements.subSet(fromElement, true, toElement, true) as TreeSet<String>
        }
        if (toElement != lastElement) {
            if (toElement.isBlank()) {
                elementsHolder.elements = TreeSet()
            } else {
                elementsHolder.hasMoreElements = lastElement != toElement
                elementsHolder.elements = elements.subSet(fromElement, true, toElement, true) as TreeSet<String>
            }
        }
        if (fromElement != firstElement) {
            elementsHolder.elements = elements.subSet(fromElement, true, toElement, true) as TreeSet<String>
        }
        return
    }

    private fun calcToElement(holder: DockerPaginationElementsHolder, element: String, maxEntries: Int): String {
        var toElement = holder.elements.last() as String
        if (maxEntries <= 0) return toElement

        val repos = holder.elements.tailSet(element)
        var repoIndex = 1
        val iter = repos.iterator()

        while (iter.hasNext()) {
            val repo = iter.next() as String
            if (repoIndex++ == maxEntries) {
                toElement = repo
                break
            }
        }
        return toElement
    }

    private fun calcFromElement(elementsHolder: DockerPaginationElementsHolder, lastEntry: String): String {
        var fromElement = elementsHolder.elements.first() as String
        if (lastEntry.isNotBlank()) {
            fromElement = elementsHolder.elements.higher(lastEntry) as String
        }
        return fromElement
    }
}
