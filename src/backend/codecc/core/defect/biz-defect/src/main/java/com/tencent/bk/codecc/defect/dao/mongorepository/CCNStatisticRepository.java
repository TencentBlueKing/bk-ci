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

package com.tencent.bk.codecc.defect.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.CCNStatisticEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Lint每次分析的统计信息持久化
 *
 * @version V1.0
 * @date 2019/5/5
 */
@Repository
public interface CCNStatisticRepository extends MongoRepository<CCNStatisticEntity, String>
{
    /**
     * 根据任务id和工具名称查询统计信息
     *
     * @param taskId
     * @param toolName
     * @return
     */
    CCNStatisticEntity findFirstByTaskIdAndToolNameOrderByTimeDesc(long taskId, String toolName);

    /**
     * 通过任务id，工具名和时间查询统计信息
     *
     * @param taskId
     * @param toolName
     * @return
     */
    @Query(fields = "{'time':1, 'average_ccn':1}", value = "{'task_id':?0, 'tool_name': ?1}")
    List<CCNStatisticEntity> findByTaskIdAndToolNameOrderByTimeDesc(long taskId, String toolName);

    /**
     * 根据任务ID、构建ID查询
     *
     * @param taskId
     * @param buildId
     * @return
     */
    CCNStatisticEntity findFirstByTaskIdAndBuildId(long taskId, String buildId);

    /**
     * 根据任务ID、工具名称、构建ID查询
     *
     * @param taskId
     * @param toolName
     * @param buildId
     * @return
     */
    CCNStatisticEntity findFirstByTaskIdAndToolNameAndBuildId(long taskId, String toolName, String buildId);


    /**
     * 通过任务id，工具名和时间查询统计信息
     *
     * @param taskId   任务ID
     * @param toolName 工具名称
     * @return entity list
     */
    @Query(fields = "{'time':1, 'ccn_beyond_threshold_sum':1}", value = "{'task_id':?0, 'tool_name': ?1}")
    List<CCNStatisticEntity> findByTaskIdAndToolName(long taskId, String toolName);
}
