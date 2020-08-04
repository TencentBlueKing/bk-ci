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
 
package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.service.IQueryStatisticBizService;
import com.tencent.devops.common.api.analysisresult.BaseLastAnalysisResultVO;
import com.tencent.devops.common.api.analysisresult.CommonLastAnalysisResultVO;
import com.tencent.devops.common.api.analysisresult.ToolLastAnalysisResultVO;
import org.springframework.stereotype.Service;

/**
 * cloc查询服务类
 * 
 * @date 2019/11/1
 * @version V1.0
 */
@Service("CLOCQueryStatisticBizService")
public class CLOCQueryStatisticBizServiceImpl implements IQueryStatisticBizService
{
    @Override
    public BaseLastAnalysisResultVO processBiz(ToolLastAnalysisResultVO arg) {
        return new CommonLastAnalysisResultVO();
    }
}
