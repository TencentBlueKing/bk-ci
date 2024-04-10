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

package com.tencent.devops.stream.resources.op

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.stream.api.op.OpGitCIServicesResource
import com.tencent.devops.stream.pojo.GitCIServicesConf
import com.tencent.devops.stream.service.GitCIServicesConfService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpGitCIServicesResourceImpl @Autowired constructor(
    private val gitCIServicesConfService: GitCIServicesConfService
) : OpGitCIServicesResource {
    override fun create(userId: String, gitCIServicesConf: GitCIServicesConf): Result<Boolean> {
        return try {
            Result(gitCIServicesConfService.create(userId, gitCIServicesConf))
        } catch (e: CustomException) {
            Result(e.status.statusCode, e.message ?: "")
        } catch (e: Exception) {
            Result(1, "op git ci create service failed.")
        }
    }

    override fun update(userId: String, id: Long, enable: Boolean?): Result<Boolean> {
        return try {
            Result(gitCIServicesConfService.update(userId, id, enable))
        } catch (e: CustomException) {
            Result(e.status.statusCode, e.message ?: "")
        } catch (e: Exception) {
            Result(1, "op git ci update service failed.")
        }
    }

    override fun delete(userId: String, id: Long): Result<Boolean> {
        return try {
            Result(gitCIServicesConfService.delete(userId, id))
        } catch (e: CustomException) {
            Result(e.status.statusCode, e.message ?: "")
        } catch (e: Exception) {
            Result(1, "op git ci delete service failed.")
        }
    }

    override fun list(userId: String): Result<List<GitCIServicesConf>> {
        return try {
            Result(gitCIServicesConfService.list(userId))
        } catch (e: CustomException) {
            Result(e.status.statusCode, e.message ?: "")
        } catch (e: Exception) {
            Result(1, "op git ci list service failed.")
        }
    }
}
