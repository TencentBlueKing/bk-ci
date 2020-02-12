package com.tencent.devops.store.service.common

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.StoreMediaInfo
import com.tencent.devops.store.pojo.common.StoreMediaInfoRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum

interface StoreMediaService {

    /**
     * 新增媒体文件
     */
    fun add(
        userId: String,
        type: StoreTypeEnum,
        storeMediaInfo: StoreMediaInfoRequest
    ): Result<Boolean>

    /**
     * 更新媒体文件
     */
    fun update(
        userId: String,
        id: String,
        storeMediaInfo: StoreMediaInfoRequest
    ): Result<Boolean>

    /**
     * 获取单条媒体信息
     */
    fun get(
        userId: String,
        id: String
    ): Result<StoreMediaInfo?>

    /**
     * 获取媒体信息
     */
    fun getByCode(
        storeCode: String,
        storeType: StoreTypeEnum
    ): Result<List<StoreMediaInfo>?>
}