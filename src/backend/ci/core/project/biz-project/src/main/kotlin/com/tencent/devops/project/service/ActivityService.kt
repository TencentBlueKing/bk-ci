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

package com.tencent.devops.project.service

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.dao.ActivityDao
import com.tencent.devops.project.pojo.ActivityInfo
import com.tencent.devops.project.pojo.ActivityStatus
import com.tencent.devops.project.pojo.OPActivityUpdate
import com.tencent.devops.project.pojo.OPActivityVO
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.enums.ActivityType
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ActivityService @Autowired constructor(
    private val dslContext: DSLContext,
    private val activityDao: ActivityDao
) {

    fun list(type: ActivityType): List<ActivityInfo> {
        return activityDao.list(dslContext, type, ActivityStatus.ACTIVITY).map {
            ActivityInfo(
                name = it.name,
                link = it.link,
                createTime = DateTimeUtil.toDateTime(it.createTime)
            )
        }
    }

    fun create(userId: String, activityInfo: ActivityInfo, type: ActivityType) {
        activityDao.create(dslContext, userId, activityInfo, type)
    }

    fun delete(userId: String, activityId: Long) {
        activityDao.delete(dslContext, activityId)
    }

    fun get(userId: String, activityId: Long): Result<OPActivityVO> {
        val tActivityRecord = activityDao.get(dslContext, activityId)

        if (tActivityRecord != null) {
            return Result(tActivityRecord.let {
                OPActivityVO(
                    id = it.id,
                    name = it.name,
                    englishName = it.englishName,
                    link = it.link,
                    type = it.type,
                    status = it.status,
                    creator = it.creator, createTime = DateTimeUtil.toDateTime(it.createTime)
                )
            })
        }
        return Result(
            405,
            I18nUtil.getCodeLanMessage(ProjectMessageCode.ID_INVALID, language = I18nUtil.getLanguage(userId))
        )
    }

    fun listOPActivity(userId: String): List<OPActivityVO> {
        return activityDao.listOPActivity(dslContext)
    }

    fun upDateActivity(activityId: Long, opActivityUpdate: OPActivityUpdate): Boolean {
        return activityDao.upDate(dslContext, activityId, opActivityUpdate)
    }

    fun getField(fieldName: String): List<String> {
        val fieldDate = ArrayList<String>()
        if (fieldName == "TYPE") {
            ActivityType.values().forEach {
                fieldDate.add(it.name)
            }
        }
        if (fieldName == "STATUS") {
            ActivityStatus.values().forEach {
                fieldDate.add(it.name)
            }
        }
        return fieldDate
    }
}
