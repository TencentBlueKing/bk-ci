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

package com.tencent.devops.store.common.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.label.Label
import com.tencent.devops.store.pojo.common.label.LabelRequest
import org.jooq.Record

/**
 * 标签业务逻辑类
 *
 * since: 2019-03-22
 */
interface LabelService {

    /**
     * 获取所有标签信息
     * @param type 0:插件 1：模板
     */
    fun getAllLabel(type: Byte): Result<List<Label>?>

    /**
     * 根据id获取标签信息
     */
    fun getLabel(id: String): Result<Label?>

    /**
     * 保存标签信息
     */
    fun saveLabel(labelRequest: LabelRequest, type: Byte): Result<Boolean>

    /**
     * 更新标签信息
     */
    fun updateLabel(id: String, labelRequest: LabelRequest, type: Byte): Result<Boolean>

    /**
     * 根据id删除标签信息
     */
    fun deleteLabel(id: String): Result<Boolean>

    /**
     * 为标签集合添加标签
     */
    fun addLabelToLabelList(it: Record, labelList: MutableList<Label>)
}
