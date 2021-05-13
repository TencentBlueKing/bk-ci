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

package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.vo.CodeRepoVO;
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectReqVO;
import com.tencent.devops.common.api.QueryTaskListReqVO;

import java.util.Map;
import java.util.Set;

/**
 * 代码库服务接口
 *
 * @date 2019/12/3
 * @version V1.0
 */
public interface CodeRepoService {
    /**
     * 根据任务id和构建id查询代码库信息
     * @param taskId
     * @param buildId
     * @return
     */
    Set<CodeRepoVO> getCodeRepoInfoByTaskId(Long taskId, String buildId);

    /**
     * 获取任务id和代码仓库的映射信息
     * @param taskIds
     * @return
     */
    Map<Long, Set<CodeRepoVO>> getCodeRepoListByTaskIds(Set<Long> taskIds);

    /**
     * 初始化代码仓库及分支名数据
     *
     * @param reqVO     请求体
     * @param pageNum   页码
     * @param pageSize  每页数量
     * @param sortField 排序字段
     * @param sortType  排序类型
     * @return
     */
    Boolean initCodeRepoStatistic(DeptTaskDefectReqVO reqVO, Integer pageNum, Integer pageSize, String sortField,
            String sortType);

    /**
     * 初始化新增代码库/代码分支数数据
     *
     * @param reqVO 请求体
     * @return
     */
    Boolean initCodeRepoStatTrend(QueryTaskListReqVO reqVO);
}
