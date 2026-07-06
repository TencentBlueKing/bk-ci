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

import com.tencent.devops.store.pojo.common.InstalledComponentInfo
import com.tencent.devops.store.pojo.common.InstalledComponentQueryReq
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum

/**
 * 已安装组件查询扩展点(SPI)。
 *
 * 通用安装记录查询([StoreInstalledComponentService])只负责组装与storeType无关的公共字段；
 * 各组件类型(插件/模板/镜像等)若需要补充个性化信息(如插件关联流水线数量、卸载权限、镜像规格等)，
 * 实现本接口并声明为Spring Bean即可被自动织入，无需改动通用查询主流程。
 *
 * 这样设计使后续 UserAtomResource#getInstalledAtoms 等历史逻辑能够平滑收敛到统一接口，
 * 同时不影响其它已有组件类型的既有行为。
 */
interface StoreInstalledComponentExtHandler {

    /**
     * 该扩展点负责处理的组件类型
     */
    fun getStoreType(): StoreTypeEnum

    /**
     * 在公共字段组装完成后，对结果做storeType个性化富化。
     * 默认实现为不做任何处理，便于实现方按需覆盖。
     *
     * @param userId 当前操作人
     * @param queryReq 查询请求
     * @param components 已组装公共字段的安装记录(顺序与最终返回一致，实现方应原地补充extData或返回新列表)
     */
    fun enrich(
        userId: String,
        queryReq: InstalledComponentQueryReq,
        components: List<InstalledComponentInfo>
    ): List<InstalledComponentInfo> = components
}
