/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.apiquery.service.impl

import com.tencent.bk.codecc.apiquery.service.CodeLineStatisticService
import com.tencent.bk.codecc.apiquery.task.dao.CodeLineStatisticDao
import com.tencent.bk.codecc.apiquery.vo.CodeLineStatisticVO
import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.RedisKeyConstants
import com.tencent.devops.common.util.DateTimeUtils
import org.apache.commons.lang.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service

@Service
class CodeLineStatisticServiceImpl @Autowired constructor(
    private val codeLineStatisticDao: CodeLineStatisticDao,
    private val redisTemplate: RedisTemplate<String, String>
) : CodeLineStatisticService {

    override fun getCodeLineTotalAndCodeLineDailyStatData(reqVo: TaskToolInfoReqVO): List<CodeLineStatisticVO> {
        val createFromReq = reqVo.createFrom
        val endTime = reqVo.endTime
        // 获取日期 默认显示30天
        val dates = DateTimeUtils.getDatesByStartTimeAndEndTime(reqVo.startTime, endTime, 30)
        // 根据时间、来源 获取代码总量、每日分析代码总量趋势图数据
        val codeLineStatisticList = codeLineStatisticDao.findByDateAndCreateFrom(dates, createFromReq)
        val statisticModelMap = codeLineStatisticList.associateBy { it.date }

        val codeLineStatisticVOList = dates.map { date ->
            val codeLineStatisticVO = CodeLineStatisticVO()
            codeLineStatisticVO.date = date
            val model = statisticModelMap[date]
            codeLineStatisticVO.dailyCode = model?.dailyTotal ?: 0L
            codeLineStatisticVO.sumCode = model?.sumCode ?: 0L
            codeLineStatisticVO
        }

        // 获取当天数据
        val currentDate = DateTimeUtils.getDateByDiff(0)
        if (StringUtils.isEmpty(endTime) || currentDate == endTime) {
            // 获取当天代码分析总量
            val createFromSet = reqVo.createFrom
            var dailyCode: Long = 0
            createFromSet.forEach {
                val key = "${RedisKeyConstants.CODE_LINE_STAT}$currentDate:$it"
                val entries = redisTemplate.opsForHash<String, String>().entries(key)
                dailyCode += entries.getOrDefault(ComConstants.TOTAL_BLANK, "0").toLong()
                dailyCode += entries.getOrDefault(ComConstants.TOTAL_COMMENT, "0").toLong()
                dailyCode += entries.getOrDefault(ComConstants.TOTAL_CODE, "0").toLong()
            }
            // 获取截止到当天代码总量
            val codeLineStatisticVO = codeLineStatisticVOList[codeLineStatisticVOList.size - 1]
            codeLineStatisticVO.date = currentDate
            codeLineStatisticVO.dailyCode = dailyCode
            // 取消实时计算当天代码总量 改为沿用昨天的数值
            val lastCodeLineStatisticVO = codeLineStatisticVOList[codeLineStatisticVOList.size - 2]
            codeLineStatisticVO.sumCode = lastCodeLineStatisticVO.sumCode
        }
        return codeLineStatisticVOList
    }
}
