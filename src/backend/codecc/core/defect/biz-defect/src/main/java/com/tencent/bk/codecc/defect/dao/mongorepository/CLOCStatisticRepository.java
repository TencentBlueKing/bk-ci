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

import com.tencent.bk.codecc.defect.model.CLOCStatisticEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * cloc统计持久
 * 
 * @date 2020/4/9
 * @version V1.0
 */
@Repository
public interface CLOCStatisticRepository extends MongoRepository<CLOCStatisticEntity, String>
{
    /**
     * 通过任务id查找cloc统计清单
     * @param taskId
     * @return
     */
    List<CLOCStatisticEntity> findByTaskId(Long taskId, String toolName);

    /**
     * 通过任务id查找cloc统计清单
     * @param taskId
     * @return
     */
    List<CLOCStatisticEntity> findByTaskIdAndToolName(Long taskId, String toolName);

    /**
     * 根据 task_id 查询当前任务下最近一次
     * 构建的记录
     * @param taskId 任务ID
     */
    CLOCStatisticEntity findFirstByTaskIdOrderByUpdatedDateDesc(Long taskId, String toolName);

    /**
     * 根据 task_id 查询当前任务下最近一次
     * 构建的记录
     * @param taskId 任务ID
     */
    CLOCStatisticEntity findFirstByTaskIdAndToolNameOrderByUpdatedDateDesc(Long taskId, String toolName);

    /**
     * 根据task_id和language查询当前任务下最近一次
     * @param taskId
     * @param language
     * @return
     */
    CLOCStatisticEntity findFirstByTaskIdAndLanguageOrderByUpdatedDateDesc(Long taskId, String language);

    /**
     * 根据task_id和language查询当前任务下最近一次
     * @param taskId
     * @param language
     * @return
     */
    CLOCStatisticEntity findFirstByTaskIdAndToolNameAndLanguageOrderByUpdatedDateDesc(
            Long taskId, String toolName, String language);

    /**
     * 根据 task_id 和 build_id 查询单个记录
     * @param taskId 任务ID
     * @param buildId 构建ID
     */
    List<CLOCStatisticEntity> findByTaskIdAndToolNameAndBuildId(Long taskId, String toolName, String buildId);

}
