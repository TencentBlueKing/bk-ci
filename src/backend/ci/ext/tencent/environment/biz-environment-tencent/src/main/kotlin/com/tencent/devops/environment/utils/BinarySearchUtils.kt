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

package com.tencent.devops.environment.utils

object BinarySearchUtils {

    /**
     * 从有序列表中二分查找出指定的值并返回对应下标
     * @param sortedValueList 有序列表
     * @param targetValue 目标值
     * @return 如果存在恰好相等的值，返回对应下标
     *         如果所有值比目标值小，返回-1
     *         如果所有值比目标值大，返回0
     *         如果目标值位于首尾值之间但不存在恰好相等的值，返回与目标值距离最近且比目标值大的值的下标
     */
    fun <T : Comparable<T>> binarySearchIndexOrCeil(
        sortedValueList: List<T>,
        targetValue: T
    ): Int {
        return binarySearchIndexOrCeil(sortedValueList, targetValue, 0, sortedValueList.size - 1)
    }

    /**
     * 从有序列表中的指定位置之间二分查找出指定的值并返回对应下标
     * @param sortedValueList 有序列表
     * @param targetValue 目标值
     * @param startIndex 起始下标
     * @param endIndex 结束下标
     * @return 如果存在恰好相等的值，返回对应下标
     *         如果所有值比目标值小或列表为空，返回-1
     *         如果所有值比目标值大，返回0
     *         如果目标值位于首尾值之间但不存在恰好相等的值，返回与目标值距离最近且比目标值大的值的下标
     */
    private fun <T : Comparable<T>> binarySearchIndexOrCeil(
        sortedValueList: List<T>,
        targetValue: T,
        startIndex: Int,
        endIndex: Int
    ): Int {
        if (sortedValueList.isEmpty()) {
            return -1
        }
        if (startIndex == endIndex) {
            val startValue = sortedValueList[startIndex]
            return if (targetValue == startValue) {
                startIndex
            } else if (targetValue < startValue) {
                startIndex
            } else {
                -1
            }
        }
        val midIndex = (startIndex + endIndex) / 2
        val valueAtMidIndex = sortedValueList[midIndex]
        return if (targetValue == valueAtMidIndex) {
            midIndex
        } else if (targetValue < valueAtMidIndex) {
            binarySearchIndexOrCeil(sortedValueList, targetValue, startIndex, midIndex)
        } else {
            binarySearchIndexOrCeil(sortedValueList, targetValue, midIndex + 1, endIndex)
        }
    }
}
