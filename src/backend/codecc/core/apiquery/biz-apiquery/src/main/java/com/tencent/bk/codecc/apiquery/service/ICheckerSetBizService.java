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


import com.tencent.bk.codecc.apiquery.defect.model.CheckerSetModel;
import com.tencent.bk.codecc.apiquery.vo.CheckerSetListQueryReq;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.pojo.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ICheckerSetBizService {
    /**
     * 根据任务ID集合获取关联的规则集
     *
     * @param taskIds 任务ID集合
     * @return map
     */
    Map<Long, List<CheckerSetVO>> getCheckerSetByTaskIdSet(Collection<Long> taskIds);


    /**
     * 根据任务Id查询任务已经关联的规则集列表
     *
     * @param taskId 任务ID
     * @return list
     */
    List<CheckerSetVO> getCheckerSetsByTaskId(Long taskId);

    /**
     * 分页查询规则管理列表
     *
     * @param checkerSetListQueryReq 规则集管理列表请求体
     * @param pageable               分页数据
     * @return page
     */
    Page<CheckerSetModel> getCheckerSetList(CheckerSetListQueryReq checkerSetListQueryReq, Pageable pageable);
}
