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

import com.tencent.bk.codecc.defect.model.IgnoreCheckerEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Collection;
import java.util.List;

/**
 * 配置忽略规则包持久层
 *
 * @version V1.0
 * @date 2019/6/6
 */
public interface IgnoreCheckerRepository extends MongoRepository<IgnoreCheckerEntity, String>
{

    /**
     * 查询工具的忽略规则
     *
     * @param taskId
     * @param toolName
     * @return
     */
    IgnoreCheckerEntity findFirstByTaskIdAndToolName(Long taskId, String toolName);


    /**
     * 批量获取任务工具规则忽略情况
     *
     * @param toolName    工具名称
     * @param checkerList 任务ID列表
     * @return ignore list
     */

    List<IgnoreCheckerEntity> findByToolNameAndOpenNonDefaultCheckersIn(String toolName,
            Collection<String> checkerList);


    /**
     * 批量获取任务的单个工具规则忽略情况
     *
     * @param taskIdSet 任务ID列表
     * @param toolName  工具名称
     * @return ignore list
     */
    @Query(fields = "{'task_id':1, 'close_default_checkers':1}", value = "{'task_id': {'$in': ?0}, 'tool_name': ?1}")
    List<IgnoreCheckerEntity> findByTaskIdInAndToolNameIs(Collection<Long> taskIdSet, String toolName);
}
