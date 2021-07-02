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

import com.tencent.bk.codecc.apiquery.api.OpCodeRepoRestResource;
import com.tencent.bk.codecc.apiquery.service.CodeRepoService;
import com.tencent.bk.codecc.apiquery.vo.CodeRepoStatReqVO;
import com.tencent.bk.codecc.apiquery.vo.CodeRepoStatisticVO;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.web.RestResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * op代码仓库查询接口实现
 *
 * @version V3.0
 * @date 2021/2/24
 */
@Slf4j
@RestResource
public class OpCodeRepoRestResourceImpl implements OpCodeRepoRestResource {

    @Autowired
    private CodeRepoService codeRepoService;

    @Override
    public Result<Page<CodeRepoStatisticVO>> queryCodeRepoList(CodeRepoStatReqVO reqVO, Integer pageNum,
            Integer pageSize, String sortField, String sortType) {
        return new Result<>(codeRepoService.queryCodeRepoList(reqVO, pageNum, pageSize, sortField, sortType));
    }

    @Override
    public Result<List<CodeRepoStatisticVO>> queryCodeRepoStatTrend(CodeRepoStatReqVO reqVO) {
        return new Result<>(codeRepoService.queryCodeRepoStatTrend(reqVO));
    }
}
