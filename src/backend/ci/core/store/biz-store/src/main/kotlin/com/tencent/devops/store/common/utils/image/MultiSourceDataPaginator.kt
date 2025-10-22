/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
package com.tencent.devops.store.common.utils.image

/**
 * @Description 多数据源分页加载器
 * 仅适用于在数据无变化的一段时间内多次调用，非线程安全
 * @Date 2019/11/16
 * @Version 1.0
 */
class MultiSourceDataPaginator<T>(private vararg val dataSources: PagableDataSource<T>) {

    private var totalCount = -1
    // 逐个数据源统计总量，当前累计数据量
    var maxCount = 0
    // 当前统计数据源位置
    var maxDatasourceIndex = 0

    /**
     * 获取所有数据源数据总量
     */
    fun getTotalCount(): Int {
        if (totalCount != -1) return totalCount
        totalCount = maxCount
        for (i in maxDatasourceIndex until dataSources.size) {
            totalCount += dataSources[i].getDataSize()
        }
        return totalCount
    }

    /**
     * 获取全局分页数据
     */
    @Suppress("ALL")
    fun getPagedData(page: Int, pageSize: Int?): List<T> {
        var currentCount = 0
        var currentDatasourceIndex = 0
        val validPage = if (page > 0) page else 1
        val validPageSize = if (pageSize == null || pageSize <= 0) -1 else pageSize
        val resultList = mutableListOf<T>()
        if (validPageSize == -1) {
            // 不分页，依次加载所有数据源的数据
            dataSources.forEach {
                val data = it.getData(0, -1)
                currentDatasourceIndex += 1
                currentCount += data.size
                if (currentDatasourceIndex > maxDatasourceIndex) {
                    maxDatasourceIndex = currentDatasourceIndex
                    maxCount = currentCount
                }
                resultList.addAll(data)
            }
            return resultList
        } else {
            // 计算全局偏移与数量
            val globalOffset = (validPage - 1) * validPageSize
            val globalLimit = validPageSize
            // 当前数据源偏移
            var currentOffset = globalOffset
            // 已获取的数据量
            var currentDataSize = resultList.size
            dataSources.forEach { dataSource ->
                if (currentDataSize < globalLimit) {
                    // 尝试从当前数据源加载需要的剩余数据
                    val data = dataSource.getData(currentOffset, globalLimit - currentDataSize)
                    if (data.isEmpty()) {
                        // 从当前数据源一条数据也没有加载到
                        // 更新offset，跳过当前数据源，但须知道当前数据源有多少数据以便计算后续起点
                        currentCount += dataSource.getDataSize()
                        currentDatasourceIndex += 1
                        if (currentDatasourceIndex > maxDatasourceIndex) {
                            // 记录最靠后的已加载单数据源位置与对应统计数据
                            maxDatasourceIndex = currentDatasourceIndex
                            maxCount = currentCount
                        }
                        currentOffset = globalOffset - currentCount
                    } else {
                        // 从当前数据源加载到了数据
                        resultList.addAll(data)
                        currentDataSize = resultList.size
                        if (currentDataSize < globalLimit) {
                            // 加载完当前数据源的数据后依旧不够
                            // 重置offset继续加载下一个数据源
                            if (currentOffset == 0) {
                                // 全量加载单个数据源的数据总量可用作后续全局总量统计，节省一次count操作
                                currentCount += data.size
                            } else {
                                currentCount += dataSource.getDataSize()
                                currentOffset = 0
                            }
                            currentDatasourceIndex += 1
                            if (currentDatasourceIndex > maxDatasourceIndex) {
                                maxDatasourceIndex = currentDatasourceIndex
                                maxCount = currentCount
                            }
                        }
                    }
                } else {
                    return resultList
                }
            }
            return resultList
        }
    }
}
