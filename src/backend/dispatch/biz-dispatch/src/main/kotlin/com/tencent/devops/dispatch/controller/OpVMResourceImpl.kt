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

package com.tencent.devops.dispatch.controller

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.api.OpVMResource
import com.tencent.devops.dispatch.pojo.VM
import com.tencent.devops.dispatch.pojo.VMCreate
import com.tencent.devops.dispatch.service.VMService
import com.tencent.devops.dispatch.utils.ShutdownVMAfterBuildUtils
import org.springframework.beans.factory.annotation.Autowired

/**
 * Created by rdeng on 2017/9/4.
 */
@RestResource
class OpVMResourceImpl @Autowired constructor(
    private val vmService: VMService,
    private val shutdownVMAfterBuildUtils: ShutdownVMAfterBuildUtils
) : OpVMResource {

    override fun list(ip: String?, name: String?, typeId: Int?, os: String?, osVersion: String?, offset: Int?, limit: Int?) = Result(vmService.queryVMs(ip, name, typeId, os, osVersion, offset, limit))

    override fun get(vmId: Int): Result<VM> {
        if (vmId < 0) {
            throw InvalidParamException("无效的虚拟机ID($vmId)")
        }
        return Result(vmService.queryVMById(vmId))
    }

    override fun add(vm: VMCreate): Result<Boolean> {
        return vmService.addVM(vm)
    }

    override fun getByIp(ip: String): Result<VM> {
        return Result(vmService.queryVMByIp(ip))
    }

    override fun delete(id: Int): Result<Boolean> {
        vmService.deleteVM(id)
        return Result(true)
    }

    override fun update(vm: VMCreate): Result<Boolean> {
        return vmService.updateVM(vm)
    }

    override fun queryStatus(vmName: String): Result<String> {
        return Result(vmService.queryVMStatus(vmName))
    }

    override fun maintain(vmId: Int, enable: Boolean): Result<Boolean> {
        vmService.maintainVM(vmId, enable)
        return Result(true)
    }

    override fun maintain(vmId: Int): Result<Boolean> {
        return Result(vmService.queryVMMaintainStatus(vmId))
    }

    override fun shutdownAfterBuild(shutdown: Boolean, pipelineId: String): Result<Boolean> {
        shutdownVMAfterBuildUtils.shutdown(shutdown, pipelineId)
        return Result(true)
    }

    override fun isShutdownAfterBuild() =
            Result(shutdownVMAfterBuildUtils.getShutdownVM())
}