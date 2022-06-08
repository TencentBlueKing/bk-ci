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

import com.tencent.bk.codecc.task.model.ToolConfigInfoEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 工具配置持久层代码
 *
 * @version V1.0
 * @date 2019/4/25
 */
@Repository
public interface ToolRepository extends MongoRepository<ToolConfigInfoEntity, String>
{
    /**
     * 根据代码检查任务id查询工具清单
     *
     * @param taskId
     * @return
     */
    List<ToolConfigInfoEntity> findByTaskId(long taskId);

    /**
     * 根据代码检查任务id和工具名查询对应工具信息
     *
     * @param taskId
     * @param toolName
     * @return
     */
    ToolConfigInfoEntity findFirstByTaskIdAndToolName(long taskId, String toolName);

    /**
     * 根据代码检查任务id和工具名查询对应工具信息
     *
     * @param taskId
     * @param toolName
     * @return
     */
    @Query(fields = "{'platform_ip':1}", value = "{'task_id': ?0, 'tool_name': ?1}")
    ToolConfigInfoEntity findPlatformIpFirstByTaskIdAndToolName(long taskId, String toolName);


    /**
     * 根据工具名称及状态获取工具信息
     *
     * @param toolName     工具名称
     * @param followStatus 工具跟进状态
     * @return entity task id list
     */
    @Query(fields = "{'task_id':1}")
    List<ToolConfigInfoEntity> findByToolNameAndFollowStatusIs(String toolName, Integer followStatus);

    /**
     * 反向工具跟进状态查询工具信息
     *
     * @param toolName     工具名称
     * @param followStatus 跟进状态
     * @return task id list
     */
    @Query(fields = "{'task_id':1}")
    List<ToolConfigInfoEntity> findByToolNameAndFollowStatusNot(String toolName, Integer followStatus);

    /**
     * 批量查询指定状态的工具配置信息
     *
     * @param taskIdSet 任务ID集合
     * @return list
     */
    @Query(fields = "{'checker_set':0}")
    List<ToolConfigInfoEntity> findByTaskIdIn(Collection<Long> taskIdSet);
}
