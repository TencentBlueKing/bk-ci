/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.task.dao.mongorepository;

import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Set;

/**
 * 代码检查任务持久层代码
 *
 * @version V1.0
 * @date 2019/4/24
 */
@Repository
public interface TaskRepository extends MongoRepository<TaskInfoEntity, String>
{
    /**
     * 根据任务负责人，平台项目id和是否有效条件查询对应代码扫描任务清单
     *
     * @param taskOwner
     * @param projectId
     * @return
     */
    @Query("{'task_owner': ?0, 'task_member': ?0, 'project_id': ?1, 'status': ?2}")
    Set<TaskInfoEntity> findTaskList(String taskOwner, String projectId, int status);

    /**
     * 根据业务id查询相应代码扫描任务
     *
     * @param taskId
     * @return
     */
    TaskInfoEntity findByTaskId(long taskId);

    /**
     * 根据taskId查询任务的所有工具
     *
     * @param taskId
     * @return
     */
    @Query(fields = "{'task_id':1, 'tool_config_info_list':1}", value = "{'task_id': ?0}")
    TaskInfoEntity findToolListByTaskId(long taskId);

    /**
     * 根据任务英文名查询相应代码扫描任务
     *
     * @param nameEn
     * @return
     */
    TaskInfoEntity findByNameEn(String nameEn);

    /**
     * 根据业务id清单查询对应代码扫描任务清单
     *
     * @param taskIds
     * @return
     */
    Set<TaskInfoEntity> findByTaskIdIn(Set<Long> taskIds);

    /**
     * 通过taskid查询项目语言
     *
     * @param taskId
     * @return
     */
    @Query(fields = "{'code_lang':1}", value = "{'task_id': ?0}")
    TaskInfoEntity findCodeLangByTaskId(long taskId);

    /**
     * 通过项目英文名查询
     *
     * @param nameEn
     * @return
     */
    Boolean existsByNameEn(String nameEn);


    /**
     * 通过流水线ID获取任务信息
     *
     * @param pipelineId
     * @return
     */
    TaskInfoEntity findByPipelineId(String pipelineId);

    /**
     * 获取最大task_id
     * @return
     */
    TaskInfoEntity findFirstByTaskIdExistsOrderByTaskIdDesc(Boolean taskExists);

}
