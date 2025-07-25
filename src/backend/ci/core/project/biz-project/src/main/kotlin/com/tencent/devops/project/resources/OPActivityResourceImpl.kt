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
package com.tencent.devops.project.resources

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.op.OPActivityResource
import com.tencent.devops.project.pojo.ActivityInfo
import com.tencent.devops.project.pojo.OPActivityUpdate
import com.tencent.devops.project.pojo.OPActivityVO
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.enums.ActivityType
import com.tencent.devops.project.service.ActivityService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OPActivityResourceImpl @Autowired constructor(private val activityService: ActivityService) : OPActivityResource {
    override fun getField(userId: String, fieldName: String): Result<List<String>> {
        return Result(activityService.getField(fieldName))
    }

    override fun upDateActivity(userId: String, activityId: Long, opActivityUpdate: OPActivityUpdate): Result<Boolean> {
        return Result(activityService.upDateActivity(activityId, opActivityUpdate))
    }

    override fun listActivity(userId: String): Result<List<OPActivityVO>> {
        return Result(activityService.listOPActivity(userId))
    }

    override fun getActivity(userId: String, activityId: Long): Result<OPActivityVO> {
        return activityService.get(userId, activityId)
    }

    override fun deleteActivity(userId: String, activityId: Long): Result<Boolean> {
        activityService.delete(userId, activityId)
        return Result(true)
    }

    override fun addActivity(userId: String, type: ActivityType, info: ActivityInfo): Result<Boolean> {
        activityService.create(userId, info, type)
        return Result(true)
    }

//    override fun getFieldV2(userId: String, fieldName: String): Result<List<String>> {
//        return Result(activityService.getField(fieldName))
//    }
}
