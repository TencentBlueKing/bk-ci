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

import com.tencent.bk.codecc.task.model.GrayTaskCategoryEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 工具项目分类持久
 * 
 * @date 2021/1/4
 * @version V1.0
 */
@Repository
public interface GrayTaskCategoryRepository extends MongoRepository<GrayTaskCategoryEntity, String>
{
    /**
     * 根据项目id和状态查询
     * @param projectId
     * @param status
     * @return
     */
    List<GrayTaskCategoryEntity> findByProjectIdAndStatus(String projectId, Integer status);

    /**
     * 根据项目id和流水线id查找
     * @param projectId
     * @param pipelineId
     * @return
     */
    GrayTaskCategoryEntity findFirstByProjectIdAndPipelineId(String projectId, String pipelineId);

    /**
     * 通过工蜂项目id查找
     * @param gongfengProjectId
     * @return
     */
    GrayTaskCategoryEntity findFirstByProjectIdAndGongfengProjectId(String projectId, Integer gongfengProjectId);
}
