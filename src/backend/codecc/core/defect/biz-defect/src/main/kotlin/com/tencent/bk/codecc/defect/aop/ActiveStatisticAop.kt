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

package com.tencent.bk.codecc.defect.aop

import com.tencent.bk.codecc.defect.model.CLOCStatisticEntity
import com.tencent.bk.codecc.defect.service.statistic.ActiveStatisticService
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.util.ThreadPoolUtil
import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.annotation.Pointcut
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Aspect
@Component
class ActiveStatisticAop @Autowired constructor(
        private val activeStatisticService: ActiveStatisticService
) {

    @Pointcut("@annotation(com.tencent.devops.common.web.aop.annotation.ActiveStatistic)")
    fun doPointcut() {
    }

    @Before("doPointcut()&&args(uploadTaskLogStepVO)")
    fun beforeMethod(uploadTaskLogStepVO: UploadTaskLogStepVO) {
        ThreadPoolUtil.addRunnableTask{
            activeStatisticService.statTaskAndTool(uploadTaskLogStepVO)
        }
    }

    @After("doPointcut()&&args(clocStatisticEntity)")
    fun afterMethod(clocStatisticEntity: Collection<CLOCStatisticEntity>) {
        if (!clocStatisticEntity.isNullOrEmpty()) {
            ThreadPoolUtil.addRunnableTask {
                activeStatisticService.statCodeLineByCloc(clocStatisticEntity)
            }
        }
    }

}