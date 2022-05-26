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

package com.tencent.bk.codecc.defect.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.CodeRepoFromAnalyzeLogEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * 工具侧上报的分析记录中代码仓库url
 *
 * @date 2019/10/29
 * @version V1.0
 */
@Repository
public interface CodeRepoFromAnalyzeLogRepository extends MongoRepository<CodeRepoFromAnalyzeLogEntity, String>
{
    /**
     * 通过任务id寻找代码仓库
     * @param taskId
     * @return
     */
    CodeRepoFromAnalyzeLogEntity findCodeRepoFromAnalyzeLogEntityFirstByTaskId(long taskId);

    /**
     * 通过任务id集寻找仓库信息
     * @param taskIds
     * @return
     */
    Set<CodeRepoFromAnalyzeLogEntity> findByTaskIdIn(Set<Long> taskIds);

    /**
     * 根据任务id集合查询仓库信息
     *
     * @return
     */
    @Query(value = "{'task_id': {'$in': ?0}}")
    List<CodeRepoFromAnalyzeLogEntity> findCodeRepoFromAnalyzeLogEntityByTaskIdIn(List<Long> taskIdList,
            Pageable pageable);
}
