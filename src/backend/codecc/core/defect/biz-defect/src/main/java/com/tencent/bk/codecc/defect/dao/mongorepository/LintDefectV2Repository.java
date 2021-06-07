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

import com.tencent.bk.codecc.defect.model.LintDefectV2Entity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 查询分析记录持久层代码
 *
 * @version V1.0
 * @date 2019/5/5
 */
@Repository
public interface LintDefectV2Repository extends MongoRepository<LintDefectV2Entity, String>
{
    LintDefectV2Entity findByEntityId(String entityId);

    /**
     * 通过任务Id、工具名称、状态查询Lint文件信息
     *
     * @param taskId
     * @param toolName
     * @param status
     * @return
     */
    List<LintDefectV2Entity> findByTaskIdAndToolNameAndStatus(long taskId, String toolName, int status);

    /**
     * 通过任务Id、工具名称、状态查询Lint文件信息
     *
     * @param taskId
     * @param toolNameSet
     * @param status
     * @return
     */
    List<LintDefectV2Entity> findByTaskIdAndToolNameInAndStatus(long taskId, List<String> toolNameSet, int status);

    /**
     * 通过任务id、工具名、文件路径查询告警文件清单
     *
     * @param taskId
     * @param toolName
     * @param filePathSet
     * @return
     */
    List<LintDefectV2Entity> findByTaskIdAndToolNameAndFilePathIn(long taskId, String toolName, Set<String> filePathSet);

    /**
     * 通过任务id、工具名、文件相对路径查询告警文件清单
     *
     * @param taskId
     * @param toolName
     * @param relPathSet
     * @return
     */
    List<LintDefectV2Entity> findByTaskIdAndToolNameAndRelPathIn(long taskId, String toolName, Set<String> relPathSet);

    /**
     * 根据entityIdSet查询告警信息
     *
     * @param entityIdSet
     * @return
     */
    @Query(fields = "{'status':1}", value = "{'_id': {'$in': ?0}}")
    List<LintDefectV2Entity> findStatusByEntityIdIn(Set<String> entityIdSet);

    /**
     * 通过任务Id、工具名称、状态查询Lint文件信息
     *
     * @param taskId
     * @param toolName
     * @param status
     * @return
     */
    @Query(fields = "{'severity':1, 'author':1, 'checker':1, 'line_update_time':1}", value = "{'task_id': ?0, 'tool_name': ?1, 'status': ?2}")
    List<LintDefectV2Entity> findFiledsByTaskIdAndToolNameAndStatus(long taskId, String toolName, int status);

    List<LintDefectV2Entity> findByEntityIdIn(Set<String> entityIds);

    List<LintDefectV2Entity> findByTaskIdAndStatus(long taskId, int status);

    /**
     * 通过任务Id、工具名称、已关闭的告警
     *
     * @param taskId
     * @param toolName
     * @return
     */
    @Query(fields = "{'severity':1, 'status':1}", value = "{'task_id': ?0, 'tool_name': ?1, 'status': {'$gt':1}}")
    List<LintDefectV2Entity> findCloseDefectByTaskIdAndToolName(long taskId, String toolName);

    @Query(fields = "{'severity':1, 'author':1, 'line_update_time':1}", value = "{'task_id': ?0, 'tool_name': ?1, 'status': ?2}")
    Page<LintDefectV2Entity> findByTaskIdAndToolNameAndStatus(long taskId, String toolName, int status, Pageable pageable);

    List<LintDefectV2Entity> findByTaskIdAndToolNameInAndStatus(Long taskId, List<String> toolNameList, int status);

    Integer countByTaskIdAndToolNameInAndStatusAndSeverity(Long taskId,
                                                           List<String> toolNameList,
                                                           int status,
                                                           int severity);

    Integer countByTaskIdAndToolNameAndStatusAndSeverity(Long taskId,
                                                         String toolNameList,
                                                         int status,
                                                         int severity);
}
