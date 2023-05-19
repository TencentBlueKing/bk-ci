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

package com.tencent.devops.store.service.common.impl

import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.type.StoreDispatchType
import com.tencent.devops.store.dao.common.StoreDeptRelDao
import com.tencent.devops.store.pojo.common.DeptInfo
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreDeptService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StoreDeptServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeDeptRelDao: StoreDeptRelDao
) : StoreDeptService {

    override fun getTemplateImageDeptMap(stageList: List<Stage>): Map<String, List<DeptInfo>?> {
        val templateImageCodeSet = mutableSetOf<String>()
        stageList.forEach { stage ->
            val containerList = stage.containers
            containerList.forEach { container ->
                handleTemplateImageCodeSet(container, templateImageCodeSet)
            }
        }
        return getStoreDeptRelMap(templateImageCodeSet, StoreTypeEnum.IMAGE.type.toByte())
    }

    private fun handleTemplateImageCodeSet(
        container: Container,
        templateImageCodeSet: MutableSet<String>
    ) {
        if (container is VMBuildContainer && container.dispatchType is StoreDispatchType) {
            val imageCode = (container.dispatchType as StoreDispatchType).imageCode
            if (!imageCode.isNullOrBlank()) templateImageCodeSet.add(imageCode)
        }
    }

    override fun getTemplateAtomDeptMap(stageList: List<Stage>): Map<String, List<DeptInfo>?> {
        val templateAtomCodeSet = mutableSetOf<String>()
        stageList.forEach { stage ->
            val containerList = stage.containers
            containerList.forEach { container ->
                val elementList = container.elements
                elementList.forEach { element ->
                    templateAtomCodeSet.add(element.getAtomCode())
                }
            }
        }
        return getStoreDeptRelMap(templateAtomCodeSet, StoreTypeEnum.ATOM.type.toByte())
    }

    private fun getStoreDeptRelMap(
        storeCodeList: MutableCollection<String>,
        storeType: Byte
    ): MutableMap<String, MutableList<DeptInfo>?> {
        val storeDeptRelRecords = storeDeptRelDao.batchList(
            dslContext = dslContext,
            storeCodeList = storeCodeList,
            storeType = storeType
        )
        val storeDeptRelMap = mutableMapOf<String, MutableList<DeptInfo>?>()
        storeDeptRelRecords?.forEach { storeDeptRelRecord ->
            val deptId = storeDeptRelRecord.deptId
            val deptName = storeDeptRelRecord.deptName
            val storeCode = storeDeptRelRecord.storeCode
            val storeDeptList = storeDeptRelMap[storeCode]
            if (storeDeptList != null) {
                storeDeptList.add(DeptInfo(deptId, deptName))
            } else {
                storeDeptRelMap[storeCode] = mutableListOf(DeptInfo(deptId, deptName))
            }
        }
        storeCodeList.removeAll(storeDeptRelMap.keys)
        storeCodeList.forEach { storeCode ->
            storeDeptRelMap[storeCode] = null
        }
        return storeDeptRelMap
    }
}
