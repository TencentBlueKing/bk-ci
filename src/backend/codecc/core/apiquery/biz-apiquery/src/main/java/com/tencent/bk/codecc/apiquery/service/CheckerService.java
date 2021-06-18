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

import com.tencent.bk.codecc.apiquery.vo.CheckerDefectStatVO;
import com.tencent.bk.codecc.apiquery.vo.CheckerSetListQueryReq;
import com.tencent.bk.codecc.apiquery.vo.CheckerSetVO;
import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO;
import com.tencent.devops.common.api.pojo.Page;

public interface CheckerService {
    /**
     * OP:多条件分页查询规则告警数
     */
    Page<CheckerDefectStatVO> getCheckerDefectStatList(TaskToolInfoReqVO reqVO, Integer pageNum, Integer pageSize,
            String sortField, String sortType);

    Long getCheckerDefectStatUpdateTime();

    /**
     * 获取规则集管理列表
     *
     * @param checkerSetListQueryReq 规则集管理列表请求体
     * @param pageNum                页数
     * @param pageSize               每页多少条
     * @param sortField              排序字段
     * @param sortType               排序类型
     * @return page
     */
    Page<CheckerSetVO> getCheckerSetList(CheckerSetListQueryReq checkerSetListQueryReq, Integer pageNum,
            Integer pageSize, String sortField, String sortType);
}
