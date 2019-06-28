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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.store.service.common

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.Category
import com.tencent.devops.store.pojo.common.CategoryRequest

/**
 * 范畴业务逻辑类
 *
 * since: 2019-03-22
 */
interface CategoryService {

    /**
     * 获取所有范畴信息
     * @param type 0:插件 1：模板
     */
    fun getAllCategory(type: Byte): Result<List<Category>?>

    /**
     * 根据id获取范畴信息
     */
    fun getCategory(id: String): Result<Category?>

    /**
     * 保存范畴信息
     */
    fun saveCategory(categoryRequest: CategoryRequest, type: Byte): Result<Boolean>

    /**
     * 更新范畴信息
     */
    fun updateCategory(id: String, categoryRequest: CategoryRequest, type: Byte): Result<Boolean>

    /**
     * 根据id删除范畴信息
     */
    fun deleteCategory(id: String): Result<Boolean>
}
