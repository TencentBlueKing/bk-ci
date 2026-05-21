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

package com.tencent.devops.store.common.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.AtomWhitelist

/**
 * 插件功能白名单业务逻辑接口
 * 表设计：一行一个 whitelistType + JSON 数组 atom_codes
 */
interface AtomWhitelistService {

    /**
     * 查询某个插件是否在指定类型的白名单中
     * @param atomCode 插件代码
     * @param whitelistType 白名单类型
     * @return true-在白名单中且启用, false-不在白名单中或未启用
     */
    fun isAtomInWhitelist(atomCode: String, whitelistType: String): Boolean

    /**
     * 获取某类白名单下所有启用的插件代码
     * @param whitelistType 白名单类型
     * @return 插件代码列表
     */
    fun getAtomCodesByType(whitelistType: String): List<String>

    /**
     * 新增或更新白名单记录（按 whitelistType upsert）
     * @param whitelistType 白名单类型
     * @param atomCodes 插件代码列表
     * @param description 描述
     * @param operator 操作人
     * @return Result<Boolean>
     */
    fun addOrUpdate(
        whitelistType: String,
        atomCodes: List<String>,
        description: String?,
        operator: String
    ): Result<Boolean>

    /**
     * 根据 whitelistType 删除白名单记录
     * @param whitelistType 白名单类型
     * @param operator 操作人
     * @return Result<Boolean>
     */
    fun delete(whitelistType: String, operator: String): Result<Boolean>

    /**
     * 启用或禁用白名单记录
     * @param whitelistType 白名单类型
     * @param enabled 是否启用
     * @param operator 操作人
     * @return Result<Boolean>
     */
    fun enableOrDisable(whitelistType: String, enabled: Boolean, operator: String): Result<Boolean>

    /**
     * 分页查询白名单记录
     * @param whitelistType 白名单类型（可选）
     * @param enabled 是否启用（可选）
     * @param page 页码
     * @param pageSize 每页大小
     * @return Result<Page<AtomWhitelist>>
     */
    fun list(
        whitelistType: String?,
        enabled: Boolean?,
        page: Int,
        pageSize: Int
    ): Result<Page<AtomWhitelist>>
}
