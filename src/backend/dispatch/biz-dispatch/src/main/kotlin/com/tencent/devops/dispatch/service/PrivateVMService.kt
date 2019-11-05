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

package com.tencent.devops.dispatch.service

import com.tencent.devops.dispatch.dao.PrivateVMDao
import com.tencent.devops.dispatch.dao.VMDao
import com.tencent.devops.dispatch.pojo.VMWithPrivateProject
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PrivateVMService @Autowired constructor(
    private val dslContext: DSLContext,
    private val privateVMDao: PrivateVMDao,
    private val vmDao: VMDao
) {

    fun list(): List<VMWithPrivateProject> {
        val records = privateVMDao.list(dslContext)
        val vmIds = records.map {
            it.vmId
        }.toSet()

        val map = records.map {
            it.vmId to it.projectId
        }.toMap()
        val vmRecords = vmDao.findVMByIds(dslContext, vmIds)
        return vmRecords.map {
            VMWithPrivateProject(
                    it.vmId,
                    it.vmMachineId,
                    it.vmTypeId,
                    it.vmIp,
                    it.vmName,
                    map[it.vmId] ?: ""
            )
        }
    }

    fun add(vmId: Int, projectId: String) {
        privateVMDao.add(dslContext, vmId, projectId)
    }

    fun delete(vmId: Int, projectId: String) {
        privateVMDao.delete(dslContext, vmId, projectId)
    }
}