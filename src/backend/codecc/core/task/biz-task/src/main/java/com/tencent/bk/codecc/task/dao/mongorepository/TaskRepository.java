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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
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
     * 根据业务id查询相应代码扫描任务
     *
     * @param taskId
     * @return
     */
    TaskInfoEntity findFirstByTaskId(long taskId);

    /**
     * 根据taskId查询任务的所有工具
     *
     * @param taskId
     * @return
     */
    @Query(fields = "{'task_id':1, 'tool_config_info_list':1}", value = "{'task_id': ?0}")
    TaskInfoEntity findToolListFirstByTaskId(long taskId);

    /**
     * 通过taskid查询任务信息，不包含工具信息
     *
     * @param taskId
     * @return
     */
    @Query(fields = "{'tool_config_info_list':0}", value = "{'task_id': ?0}")
    TaskInfoEntity findTaskInfoWithoutToolsFirstByTaskId(long taskId);

    /**
     * 根据任务英文名查询相应代码扫描任务
     *
     * @param nameEn
     * @return
     */
    TaskInfoEntity findFirstByNameEn(String nameEn);

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
    TaskInfoEntity findCodeLangFirstByTaskId(long taskId);

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
    TaskInfoEntity findFirstByPipelineId(String pipelineId);

    /**
     * 通过流水线ID清单获取任务信息清单
     *
     * @param pipelineIds
     * @return
     */
    Set<TaskInfoEntity> findByPipelineIdIn(Set<String> pipelineIds);

    /**
     * 获取最大task_id
     *
     * @return
     */
    TaskInfoEntity findFirstByTaskIdExistsOrderByTaskIdDesc(Boolean taskExists);

    /**
     * 分页查询所有项目
     *
     * @return
     */
    @Query("{}")
    Page<TaskInfoEntity> findTasksByPage(Pageable pageable);

    /**
     * 根据bg id查询
     * @param bgId
     * @return
     */
    List<TaskInfoEntity> findByBgId(Integer bgId);

    /**
     * 通过task id清单
     * @param taskIds
     * @return
     */
    List<TaskInfoEntity> findByTaskIdIn(List<Long> taskIds);


    /**
     * 根据用户名查询名下有权限的任务列表
     *
     * @param taskOwner 用户名
     * @param status    任务状态
     * @return set
     */
    @Query("{'task_owner': ?0, 'task_member': ?0, 'status': ?1}")
    List<TaskInfoEntity> findTaskList(String taskOwner, int status);

    /**
     * 根据创建来源查询
     * @param createFrom
     * @return
     */
    List<TaskInfoEntity> findByCreateFrom(String createFrom);

    /**
     * 根据创建来源查询
     * @param createFrom
     * @return
     */
    Page<TaskInfoEntity> findByCreateFrom(String createFrom, Pageable pageable);

    /**
     * 根据项目id查询
     * @param projectId
     * @return
     */
    List<TaskInfoEntity> findByProjectId(String projectId);

    List<TaskInfoEntity> findByProjectId(String projectId, Pageable pageable);

    /**
     * 根据gongfengid查询
     * @param gongfengProjectId
     * @return
     */
    TaskInfoEntity findFirstByGongfengProjectId(Integer gongfengProjectId);

    /**
     *
     * @param createFrom
     * @param gongfengProjectId
     * @return
     */
    List<TaskInfoEntity> findByGongfengProjectIdIsAndCreateFromIs(Integer gongfengProjectId, String createFrom);


    /**
     * 获取特定创建的最大任务id值
     * @param createFrom
     * @return
     */
    TaskInfoEntity findFirstByCreateFromOrderByTaskIdDesc(String createFrom);


    /**
     * 分页查询任务列表
     *
     * @param status     任务状态
     * @param bgId       事业群ID集合
     * @param deptIds    部门ID集合
     * @param createFrom 创建来源
     * @param pageable   分页器
     * @return page
     */
    @Query(fields = "{'execute_time':0, 'execute_date':0, 'timer_expression':0, 'last_disable_task_info':0, 'default_filter_path':0, 'tool_config_info_list':0, 'custom_proj_info':0}")
    Page<TaskInfoEntity> findByStatusAndBgIdAndDeptIdInAndCreateFromIn(Integer status, Integer bgId,
            Collection<Integer> deptIds, List<String> createFrom, Pageable pageable);


    /**
     * 按创建来源 任务状态查询
     * @param status     任务状态
     * @param createFrom 创建来源
     * @return list
     */
    @Query(fields = "{'task_id': 1}")
    List<TaskInfoEntity> findByStatusAndCreateFromIn(Integer status, Collection<String> createFrom);

    /**
     * 根据业务id查询相应代码扫描任务
     *
     * @param taskId
     * @return
     */
    TaskInfoEntity findFirstByTaskIdAndStatus(long taskId, int status);

    /**
     * 根据gongfengid查询
     * @param gongfengProjectId
     * @return
     */
    TaskInfoEntity findFirstByGongfengProjectIdAndStatusAndProjectIdRegex(Integer gongfengProjectId,
                                                                          Integer status,
                                                                          String projectId);

    /**
     * 通过项目ID和createFrom判断是否存在开源治理项目
     *
     * @param projectId
     * @param createFrom
     * @return
     */
    Boolean existsByProjectIdAndCreateFrom(String projectId, String createFrom);

    /**
     * 通过项目ID和createFrom查询任务
     *
     * @param projectId
     * @param createFroms
     * @return
     */
    Set<TaskInfoEntity>  findByProjectIdAndCreateFromIn(String projectId, Set<String> createFroms);
}
