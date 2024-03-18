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

package com.tencent.devops.dispatch.kubernetes.startcloud.dao

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.common.service.utils.ByteUtils
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.EnvStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.EnvironmentResourceData
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.ResourceVmRespDataMachineResource
import com.tencent.devops.model.dispatch.kubernetes.tables.TDispatchWorkspace
import com.tencent.devops.model.dispatch.kubernetes.tables.TWindowsGpuPool
import com.tencent.devops.model.dispatch.kubernetes.tables.TWindowsVmResource
import com.tencent.devops.model.dispatch.kubernetes.tables.records.TDispatchWorkspaceRecord
import com.tencent.devops.model.dispatch.kubernetes.tables.records.TWindowsGpuPoolRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record2
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class WindowsGpuResourceDao {

    fun createOrUpdateResource(
        dslContext: DSLContext,
        resourceList: List<EnvironmentResourceData>
    ) {
        if (resourceList.isEmpty()) {
            return
        }
        dslContext.batch(
            resourceList.map {
            with(TWindowsGpuPool.T_WINDOWS_GPU_POOL) {
                dslContext.insertInto(
                    this,
                    CGS_ID,
                    ZONE_ID,
                    CGS_IP,
                    MACHINE_TYPE,
                    STATUS,
                    USER_INSTANCE_LIST,
                    LOCKED,
                    PROJECT_ID,
                    DISK,
                    HDISK,
                    IMAGESTANDARD,
                    NODE,
                    IMAGE,
                    CPU,
                    MEMORY,
                    REGISTER_TIME
                ).values(
                    it.cgsId,
                    it.zoneId,
                    it.cgsIp,
                    it.machineType,
                    it.status,
                    JsonUtil.getObjectMapper().writeValueAsString(it.userInstanceList),
                    ByteUtils.bool2Byte(it.locked ?: false),
                    it.projectId ?: "",
                    it.disk,
                    it.hdisk,
                    ByteUtils.bool2Byte(it.imageStandard ?: false),
                    it.node ?: "",
                    it.image ?: "",
                    it.cpu ?: "",
                    it.mem ?: "",
                    it.registerCgsTime
                ).onDuplicateKeyUpdate()
                    .set(STATUS, it.status)
            }
        }
        ).execute()
    }

    // 删除已有数据
    fun deleteAllResource(
        dslContext: DSLContext
    ) {
        return with(TWindowsGpuPool.T_WINDOWS_GPU_POOL) {
            dslContext.delete(this)
                .where(CGS_ID.isNotNull)
                .execute()
        }
    }

    fun getCgsResourceList(
        dslContext: DSLContext,
        cgsIds: List<String>?,
        ips: List<String>?
    ): Result<TWindowsGpuPoolRecord> {
        with(TWindowsGpuPool.T_WINDOWS_GPU_POOL) {
            val conditions = mutableListOf<Condition>()
            if (!cgsIds.isNullOrEmpty() && cgsIds.size == 1) {
                conditions.add(CGS_ID.eq(cgsIds.first()))
            } else if (!cgsIds.isNullOrEmpty()) {
                conditions.add(CGS_ID.`in`(cgsIds))
            }

            if (!ips.isNullOrEmpty()) {
                conditions.add(CGS_IP.`in`(ips))
            }

            return dslContext.selectFrom(this)
                .where(conditions)
                .fetch()
        }
    }

    fun getCgsWorkspace(
        dslContext: DSLContext,
        cgsId: String,
        status: EnvStatusEnum? = null
    ): TDispatchWorkspaceRecord? {
        with(TDispatchWorkspace.T_DISPATCH_WORKSPACE) {
            val condition = mutableListOf<Condition>()
            condition.add(ENVIRONMENT_UID.eq(cgsId))
            status?.let {
                condition.add(STATUS.eq(status.ordinal))
            }
            return dslContext.selectFrom(this)
                .where(condition)
                .fetchAny()
        }
    }

    /**
     * 获取cgs的机型和区域列表
     */
    fun getCgsConfig(
        dslContext: DSLContext
    ): List<Record2<String, String>> {
        with(TWindowsGpuPool.T_WINDOWS_GPU_POOL) {
            return dslContext.selectDistinct(ZONE_ID, MACHINE_TYPE).from(this)
                .orderBy(MACHINE_TYPE.asc())
                .skipCheck()
                .fetch()
        }
    }

    // 删除vm库存资源信息
    fun deleteVmResource(
        dslContext: DSLContext
    ) {
        return with(TWindowsVmResource.T_WINDOWS_VM_RESOURCE) {
            dslContext.delete(this)
                .where(MACHINE_TYPE.isNotNull)
                .execute()
        }
    }

    // 插入Vm资源数据
    fun insertVmResource(
        dslContext: DSLContext,
        resourceList: List<ResourceVmRespDataMachineResource>
    ) {
        if (resourceList.isEmpty()) {
            return
        }
        dslContext.batch(
            resourceList.map {
                with(TWindowsVmResource.T_WINDOWS_VM_RESOURCE) {
                    dslContext.insertInto(
                        this,
                        ZONE_ID,
                        MACHINE_TYPE,
                        CAP,
                        USED,
                        FREE
                    ).values(
                        it.zoneId,
                        it.machineType,
                        it.cap ?: 0,
                        it.used ?: 0,
                        it.free ?: 0
                    )
                }
            }
        ).execute()
    }
}
