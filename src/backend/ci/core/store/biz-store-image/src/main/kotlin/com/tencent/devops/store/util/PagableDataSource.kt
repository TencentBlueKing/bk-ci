package com.tencent.devops.store.util

/**
 * @Description
 * @Date 2019/11/16
 * @Version 1.0
 */
interface PagableDataSource<T> {
    /**
     * 根据offset与limit从数据源获取数据
     * limit=-1表示加载所有数据
     */
    fun getData(offset: Int, limit: Int): List<T>

    /**
     * 获取数据源数据总量
     */
    fun getDataSize(): Int
}