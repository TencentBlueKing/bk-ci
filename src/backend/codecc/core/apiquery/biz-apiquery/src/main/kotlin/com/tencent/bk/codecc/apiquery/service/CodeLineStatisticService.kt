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

package com.tencent.bk.codecc.apiquery.service

import com.tencent.bk.codecc.apiquery.vo.CodeLineStatisticVO
import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO

interface CodeLineStatisticService {

    /**
     * 获取代码总量和每日分析代码总量折线图数据
     * @param reqVo 代码总量信息请求体
     * @return list
     */
    fun getCodeLineTotalAndCodeLineDailyStatData(
        reqVo: TaskToolInfoReqVO
    ): List<CodeLineStatisticVO>
}