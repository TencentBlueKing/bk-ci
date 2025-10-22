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

package com.tencent.devops.project.service.impl

import com.tencent.devops.project.dao.ServiceTypeDao
import com.tencent.devops.project.pojo.service.ServiceType
import com.tencent.devops.project.pojo.service.ServiceTypeModify
import com.tencent.devops.project.service.ServiceTypeService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ServiceTypeServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val serviceTypeDao: ServiceTypeDao
) : ServiceTypeService {
    override fun get(id: Long): ServiceType {
        return serviceTypeDao.get(dslContext, id)
    }

    override fun createServiceType(userId: String, title: String, weight: Int): ServiceType {
        return serviceTypeDao.create(dslContext, userId, title, weight)
    }

    override fun updateServiceType(userId: String, serviceTypeId: Long, serviceTypeModify: ServiceTypeModify) {
        serviceTypeDao.update(dslContext, userId, serviceTypeId, serviceTypeModify)
    }

    override fun deleteServiceType(serviceTypeId: Long): Boolean {
        return serviceTypeDao.delete(dslContext, serviceTypeId)
    }

    override fun list(): List<ServiceType> {
        return serviceTypeDao.list(dslContext)
    }
}
