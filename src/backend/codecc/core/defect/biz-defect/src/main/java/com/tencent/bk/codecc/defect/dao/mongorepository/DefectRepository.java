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

import com.tencent.bk.codecc.defect.model.DefectEntity;
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
 * 告警查询持久代码
 *
 * @version V1.0
 * @date 2019/10/20
 */
@Repository
public interface DefectRepository extends MongoRepository<DefectEntity, String>
{
    /**
     * 通过实体id查询告警信息
     *
     * @param entityId
     * @return
     */
    DefectEntity findFirstByEntityId(String entityId);

    /**
     * 通过任务id，工具名查询告警信息
     *
     * @param taskId
     * @param toolName
     * @return
     */
    List<DefectEntity> findByTaskIdAndToolName(long taskId, String toolName);

    /**
     * 通过任务id，工具名查询告警信息
     *
     * @param taskId
     * @param toolNameSet
     * @return
     */
    List<DefectEntity> findByTaskIdAndToolNameIn(long taskId, List<String> toolNameSet);

    /**
     * 通过任务id，工具名和状态查询告警信息
     *
     * @param taskId
     * @param toolName
     * @param status
     * @return
     */
    List<DefectEntity> findByTaskIdAndToolNameAndStatus(long taskId, String toolName, int status);

    /**
     * 通过任务id，工具名和状态查询告警信息
     *
     * @param taskId
     * @param toolNameSet
     * @param status
     * @return
     */
    List<DefectEntity> findByTaskIdAndToolNameInAndStatus(long taskId, List<String> toolNameSet, int status);

    /**
     * 根据taskId，工具名查询所有的告警ID
     *
     * @param taskId
     * @param toolName
     * @return
     */
    @Query(fields = "{'id':1}", value = "{'task_id': ?0, 'tool_name': ?1}")
    List<DefectEntity> findIdsByTaskIdAndToolName(long taskId, String toolName);

    /**
     * 通过任务id，工具名查询告警信息
     *
     * @param taskId
     * @param toolName
     * @return
     */
    @Query(fields = "{'id':1, 'status':1, 'author_list':1, 'severity':1, 'file_path_name':1, 'exclude_time':1, 'checker_name':1, 'create_time':1}", value = "{'task_id': ?0, 'tool_name': ?1, 'id': {'$in': ?2}}")
    List<DefectEntity> findStatusAndAuthorAndSeverityByTaskIdAndToolNameAndIdIn(long taskId, String toolName, Set<String> idSet);

    /**
     * 通过任务id，工具名查询告警信息
     *
     * @param taskId
     * @param toolName
     * @return
     */
    @Query(value = "{'task_id': ?0, 'tool_name': ?1, 'id': {'$in': ?2}}")
    List<DefectEntity> findByTaskIdAndToolNameAndIdIn(long taskId, String toolName, Set<String> idSet);

    /**
     * 根据entityIdSet查询告警信息
     *
     * @param entityIdSet
     * @return
     */
    @Query(fields = "{'id':1, 'status':1}", value = "{'_id': {'$in': ?0}}")
    List<DefectEntity> findStatusByEntityIdIn(Set<String> entityIdSet);

    /**
     * 根据规则名和任务id查询
     * @param checkerName
     * @param taskId
     * @return
     */
    Page<DefectEntity> findByCheckerNameInAndTaskIdIn(List<String> checkerName, List<Long> taskId, Pageable pageable);

    /**
     * 根据规则名查询
     * @param checkerName
     * @return
     */
    Page<DefectEntity> findByCheckerNameIn(List<String> checkerName, Pageable pageable);


    /**
     * 获取批量任务、规则名范围的告警数据
     *
     * @param toolName       工具名称
     * @param taskIdSet      任务ID集合
     * @param checkerNameSet 规则名集合
     * @return entity list
     */
    @Query(fields = "{'stream_name':0, 'display_category':0, 'display_type':0}",
            value = "{'tool_name': ?0, 'task_id': {'$in': ?1}, 'checker_name': {'$in': ?2}}")
    List<DefectEntity> findByToolNameAndTaskIdInAndCheckerNameIn(String toolName, Collection<Long> taskIdSet,
            Set<String> checkerNameSet);


    /**
     * 通过任务Id、工具名称、已关闭的告警
     *
     * @param taskId
     * @param toolName
     * @return
     */
    @Query(fields = "{'severity':1, 'status':1}", value = "{'task_id': ?0, 'tool_name': ?1, 'status': {'$gt':1}}")
    List<DefectEntity> findCloseDefectByTaskIdAndToolName(long taskId, String toolName);

    /**
     * 通过任务id，工具名和状态查询告警信息
     *
     * @param taskId
     * @param toolNameList
     * @param status
     * @return
     */
    Integer countByTaskIdAndToolNameInAndStatusAndSeverity(long taskId,
                                                           List<String> toolNameList,
                                                           int status,
                                                           int severity);

    /**
     * 通过任务id，工具名和状态查询告警信息
     *
     * @param taskId
     * @param toolName
     * @param status
     * @return
     */
    Integer countByTaskIdAndToolNameAndStatusAndSeverity(long taskId, String toolName, int status, int severity);
}
