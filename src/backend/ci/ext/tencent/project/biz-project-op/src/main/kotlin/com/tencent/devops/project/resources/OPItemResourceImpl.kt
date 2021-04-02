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

package com.tencent.devops.project.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.op.OPItemResource
import com.tencent.devops.project.api.pojo.ItemInfoResponse
import com.tencent.devops.project.api.pojo.ServiceItem
import com.tencent.devops.project.api.pojo.ItemListVO
import com.tencent.devops.project.service.ServiceItemService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OPItemResourceImpl @Autowired constructor(
    private val itemService: ServiceItemService
) : OPItemResource {

    override fun list(itemName: String?, pid: String?, page: Int, pageSize: Int): Result<ItemListVO> {
        return itemService.queryItem(itemName, pid, page, pageSize)
    }

    override fun getItemList(userId: String): Result<List<ServiceItem>?> {
        return Result(itemService.getItemListForOp())
    }

    override fun create(userId: String, createInfo: ItemInfoResponse): Result<Boolean> {
        return itemService.createItem(userId, createInfo)
    }

    override fun update(userId: String, itemId: String, updateInfo: ItemInfoResponse): Result<Boolean> {
        return itemService.updateItem(userId, itemId, updateInfo)
    }

    override fun get(itemId: String): Result<ServiceItem?> {
        return itemService.getItem(itemId)
    }

    override fun delete(userId: String, itemId: String): Result<Boolean> {
        return itemService.delete(userId, itemId)
    }

    override fun disable(userId: String, itemId: String): Result<Boolean> {
        return itemService.disable(userId, itemId)
    }

    override fun enable(userId: String, itemId: String): Result<Boolean> {
        return itemService.enable(userId, itemId)
    }
}
