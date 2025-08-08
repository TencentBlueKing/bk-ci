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

package com.tencent.devops.store.pojo.common

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.pojo.common.enums.RdTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreSortTypeEnum
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "研发商店-查询组件条件")
data class StoreInfoQuery(
    @get:Schema(title = "组件类型", required = true)
    val storeType: String,
    @get:Schema(title = "普通组件代码列表", required = false)
    var normalStoreCodes: Set<String>? = null,
    @get:Schema(title = "调试组件代码列表", required = false)
    var testStoreCodes: Set<String>? = null,
    @get:Schema(title = "项目代码", required = false)
    val projectCode: String? = null,
    @get:Schema(title = "搜索关键字", required = false)
    val keyword: String? = null,
    @get:Schema(title = "分类ID", required = false)
    val classifyId: String? = null,
    @get:Schema(title = "标签ID", required = false)
    val labelId: String? = null,
    @get:Schema(title = "范畴ID", required = false)
    val categoryId: String? = null,
    @get:Schema(title = "评分", required = false)
    val score: Int? = null,
    @get:Schema(title = "研发来源类型", required = false)
    val rdType: RdTypeEnum? = null,
    @get:Schema(title = "是否推荐标识 true：推荐，false：不推荐", required = false)
    val recommendFlag: Boolean? = null,
    @get:Schema(title = "是否查询项目下组件标识", required = true)
    val queryProjectComponentFlag: Boolean,
    @get:Schema(title = "是否已安装", required = false)
    val installed: Boolean? = null,
    @get:Schema(title = "是否需要更新标识 true：需要，false：不需要", required = false)
    val updateFlag: Boolean? = null,
    @get:Schema(title = "排序", required = false)
    val sortType: StoreSortTypeEnum? = null,
    @get:Schema(title = "实例ID", required = false)
    val instanceId: String? = null,
    @get:Schema(title = "是否查测试中版本 true：是，false：否", required = false)
    val queryTestFlag: Boolean? = null,
    @get:Schema(title = "页码", required = true)
    val page: Int,
    @get:Schema(title = "每页数量", required = true)
    val pageSize: Int
) {
    fun validate() {
        // 检查 projectCode 是否为空
        if (getSpecQueryFlag() && projectCode.isNullOrEmpty()) {
            throw ErrorCodeException(errorCode = StoreMessageCode.STORE_QUERY_PARAM_CHECK_FAIL)
        }
    }

    fun getSpecQueryFlag(): Boolean {
        return queryProjectComponentFlag || installed != null || updateFlag != null || queryTestFlag != null
    }
}
