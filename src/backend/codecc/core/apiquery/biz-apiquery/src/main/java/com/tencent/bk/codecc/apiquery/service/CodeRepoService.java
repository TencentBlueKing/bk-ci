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


import com.tencent.bk.codecc.apiquery.defect.model.CodeRepoModel;
import com.tencent.bk.codecc.apiquery.vo.CodeRepoStatReqVO;
import com.tencent.bk.codecc.apiquery.vo.CodeRepoStatisticVO;
import com.tencent.devops.common.api.pojo.Page;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 代码库总表接口
 *
 * @version V1.0
 * @date 2021/2/24
 */
public interface CodeRepoService {

    /**
     * 获取代码库总表数据
     *
     * @param reqVO 代码库总表请求体
     * @return page
     */
    Page<CodeRepoStatisticVO> queryCodeRepoList(CodeRepoStatReqVO reqVO, Integer pageNum, Integer pageSize,
            String sortField, String sortType);

    /**
     * 获取新增代码库/代码分支数折线图数据
     *
     * @param reqVO 代码库总表请求体
     * @return list
     */
    List<CodeRepoStatisticVO> queryCodeRepoStatTrend(CodeRepoStatReqVO reqVO);

    Map<Long, Set<CodeRepoModel>> queryCodeRepoInfo(Collection<Long> taskIds, Collection<String> buildIds);

}
