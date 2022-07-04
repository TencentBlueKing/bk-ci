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

import com.tencent.bk.codecc.task.model.CustomProjEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 个性化工蜂扫描持久接口
 *
 * @date 2020/3/23
 * @version V1.0
 */
@Repository
public interface CustomProjRepository extends MongoRepository<CustomProjEntity, String>
{

    /**
     * 根据url查询个性化项目信息
     * @param customProjSource
     * @param url
     * @return
     */
    List<CustomProjEntity> findByCustomProjSourceAndUrl(String customProjSource, String url);


    /**
     * 按照url和分支和来源查找
     * @param url
     * @param branch
     * @param customProjSource
     * @return
     */
    CustomProjEntity findFirstByCustomProjSourceAndUrlAndBranch(String customProjSource, String url, String branch);


    /**
     * 按来源分页获取项目信息
     *
     * @param customProjSource 项目来源列表
     * @param pageable         分页
     * @return page
     */
    Page<CustomProjEntity> findByCustomProjSource(String customProjSource, Pageable pageable);

    /**
     * 通过流水线id查找
     * @param pipelineId
     * @return
     */
    CustomProjEntity findFirstByPipelineId(String pipelineId);

    /**
     * 通过任务id查找
     * @param taskId
     * @return
     */
    CustomProjEntity findFirstByTaskId(Long taskId);

    /**
     * 通过代码库地址查找
     *
     * @param id
     * @param customProjSource
     * @return
     */
    CustomProjEntity findFirstByGongfengProjectIdAndCustomProjSource(Integer id, String customProjSource);

    /**
     * 通过工蜂ID查找
     *
     */
    CustomProjEntity findFirstByGongfengProjectId(Integer gongfengProjectId);

    /**
     * 通过工蜂id查找
     * @param customProjSource
     * @param ids
     * @return
     */
    @Query(fields = "{'gongfeng_project_id':1}")
    List<CustomProjEntity> findByCustomProjSourceAndGongfengProjectIdIn(String customProjSource, List<Integer> ids);

    /**
     * 根据流水线Id查询
     *
     * @param pipelineIds
     * @return
     */
    List<CustomProjEntity> findByPipelineIdIn(List<String> pipelineIds);

    /**
     * 根据任务Id查询
     *
     * @param taskIds
     * @return
     */
    List<CustomProjEntity> findByTaskIdIn(List<Long> taskIds);
}
