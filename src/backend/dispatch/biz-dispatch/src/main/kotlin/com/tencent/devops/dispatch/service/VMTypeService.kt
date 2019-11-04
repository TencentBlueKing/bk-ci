/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

package com.tencent.devops.dispatch.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.dispatch.dao.VMTypeDao
import com.tencent.devops.dispatch.pojo.VMType
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class VMTypeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val vmTypeDao: VMTypeDao
) {

    fun queryAllVMType(): List<VMType> {
        val types = ArrayList<VMType>()
        val allTypes = vmTypeDao.findAllVMType(dslContext) ?: return types
        allTypes.forEach {
            val type = vmTypeDao.parseVMType(it)
            if (type != null)
                types.add(type)
        }
        return types
    }

    fun queryTypeById(typeId: Int): VMType? {
        return vmTypeDao.parseVMType(vmTypeDao.findVMTypeById(dslContext, typeId))
    }

    fun createType(typeName: String): Result<Boolean> {
        // 判断名字是否重复
        val nameCount = vmTypeDao.countByName(dslContext, typeName)
        if (nameCount > 0) {
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_EXIST,
                arrayOf(typeName),
                false
            )
        }
        vmTypeDao.createVMType(dslContext, typeName)
        return Result(true)
    }

    fun updateType(vmType: VMType): Result<Boolean> {
        val nameCount = vmTypeDao.countByName(dslContext, vmType.typeName)
        if (nameCount > 0) {
            val vmTypeObj = vmTypeDao.findVMTypeById(dslContext, vmType.id)
            if (null != vmTypeObj && vmTypeObj.typeName != vmType.typeName) {
                return MessageCodeUtil.generateResponseDataObject(
                    CommonMessageCode.PARAMETER_IS_EXIST,
                    arrayOf(vmType.typeName),
                    false
                )
            }
        }
        vmTypeDao.updateVMType(dslContext, vmType.id, vmType.typeName)
        return Result(true)
    }

    fun deleteType(vmTypeId: Int) {
        vmTypeDao.deleteVMType(dslContext, vmTypeId)
    }
}