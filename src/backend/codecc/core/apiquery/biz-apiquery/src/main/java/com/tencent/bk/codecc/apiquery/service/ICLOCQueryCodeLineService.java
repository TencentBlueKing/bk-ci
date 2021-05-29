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

package com.tencent.bk.codecc.apiquery.service;

import com.tencent.bk.codecc.apiquery.defect.model.CLOCStatisticModel;

import java.util.Collection;
import java.util.List;

/**
 * cloc查询代码行数接口
 *
 * @version V1.0
 * @date 2020/3/31
 */
public interface ICLOCQueryCodeLineService {

    /**
     * 按任务ID获取代码行数
     *
     * @param taskIds 任务ID集合
     * @return int
     */
    Long queryCodeLineByTaskIds(Collection<Long> taskIds);

    List<CLOCStatisticModel> queryTaskLastCodeLine(Collection<Long> taskIds);

}
