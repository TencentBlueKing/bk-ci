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

package com.tencent.bk.codecc.apiquery.resources;

import com.tencent.bk.codecc.apiquery.api.OpCheckerRestResource;
import com.tencent.bk.codecc.apiquery.service.CheckerService;
import com.tencent.bk.codecc.apiquery.vo.CheckerDefectStatVO;
import com.tencent.bk.codecc.apiquery.vo.CheckerSetListQueryReq;
import com.tencent.bk.codecc.apiquery.vo.CheckerSetVO;
import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 规则告警数查询接口实现
 */
@RestResource
public class OpCheckerRestResourceImpl implements OpCheckerRestResource {

    @Autowired
    private CheckerService checkerService;

    @Override
    public Result<Page<CheckerDefectStatVO>> getCheckerDefectStatList(TaskToolInfoReqVO reqVO, Integer pageNum,
            Integer pageSize, String sortField, String sortType) {
        return new Result<>(checkerService.getCheckerDefectStatList(reqVO, pageNum, pageSize, sortField, sortType));
    }

    @Override
    public Result<Long> getCheckerDefectStatUpdateTime() {
        return new Result<>(checkerService.getCheckerDefectStatUpdateTime());
    }

    @Override
    public Result<Page<CheckerSetVO>> getCheckerSetList(CheckerSetListQueryReq checkerSetListQueryReq, Integer pageNum,
            Integer pageSize, String sortField, String sortType) {
        return new Result<>(
                checkerService.getCheckerSetList(checkerSetListQueryReq, pageNum, pageSize, sortField, sortType));
    }
}
