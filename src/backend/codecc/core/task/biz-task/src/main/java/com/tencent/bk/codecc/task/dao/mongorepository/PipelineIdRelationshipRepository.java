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

package com.tencent.bk.codecc.task.dao.mongorepository;

import com.tencent.bk.codecc.task.model.PipelineIdRelationshipEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 流水线维度流水表持久接口
 * 
 * @date 2020/10/27
 * @version V1.0
 */
@Repository
public interface PipelineIdRelationshipRepository extends MongoRepository<PipelineIdRelationshipEntity, String>
{

    /**
     * 通过流水线id和触发日期查询
     * @param pipelineId
     * @param triggerDate
     * @return
     */
    PipelineIdRelationshipEntity findFirstByPipelineIdAndTriggerDate(String pipelineId, LocalDate triggerDate);


    /**
     * 通过触发日期和状态查找
     * @param triggerDate
     * @param status
     * @return
     */
    List<PipelineIdRelationshipEntity> findAllByTriggerDateAndStatusIsNot(LocalDate triggerDate, Integer status);
}
