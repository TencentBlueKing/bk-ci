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

import com.tencent.bk.codecc.defect.model.LintFileEntity;
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
public interface LintDefectRepository extends MongoRepository<LintFileEntity, String>
{
    /**
     * 通过任务id，工具名和相对路径查询lint类告警文件信息
     *
     * @param taskId
     * @param toolName
     * @param filePath
     * @return
     */
    LintFileEntity findFirstByTaskIdAndToolNameAndRelPath(long taskId, String toolName, String filePath);

    /**
     * 通过任务id和工具名查询告警文件清单
     *
     * @param taskId
     * @param toolName
     * @return
     */
    @Query(fields = "{'analysis_version':1, 'status':1, 'defect_count':1, 'new_count':1, 'history_count':1}", value = "{'task_id': ?0, 'tool_name': ?1}")
    List<LintFileEntity> findFileByTaskIdAndToolName(long taskId, String toolName);

    /**
     * 通过任务id和工具名查询告警文件清单
     *
     * @param taskId
     * @param toolName
     * @return
     */
    List<LintFileEntity> findByTaskIdAndToolName(long taskId, String toolName);


    /**
     * 通过任务Id、工具名称、状态查询Lint文件信息
     *
     * @param taskId
     * @param toolName
     * @param status
     * @return
     */
    @Query(fields = "{'defect_list': 1, 'task_id': 1}", value = "{'task_id': ?0, 'tool_name': ?1, 'status': ?2}")
    List<LintFileEntity> findByTaskIdAndToolNameAndStatus(long taskId, String toolName, int status);


    /**
     * 批量获取指定任务的工具告警
     *
     * @param taskIds  任务ID集合
     * @param toolName 工具名称
     * @return file defect list
     */
    @Query(fields = "{'task_id': 1, 'file_path': 1, 'defect_count': 1, 'status': 1, 'defect_list': 1, 'checker_list': 1, 'file_update_time': 1, 'createTime': 1}",
            value = "{'task_id': {'$in': ?0}, 'tool_name': ?1}")
    List<LintFileEntity> findByTaskIdInAndToolNameIs(Collection<Long> taskIds, String toolName);

    /**
     * 批量获取指定任务的工具告警
     *
     * @param taskIds  任务ID集合
     * @param toolName 工具名称
     * @param pageable 分页
     * @return file defect list
     */
    @Query(fields = "{'task_id': 1, 'file_path': 1, 'defect_count': 1, 'status': 1, 'defect_list': 1, 'checker_list': 1, 'file_update_time': 1, 'createTime': 1}",
            value = "{'task_id': {'$in': ?0}, 'tool_name': ?1}")
    Page<LintFileEntity> findByTaskIdInAndToolNameIs(Collection<Long> taskIds, String toolName, Pageable pageable);

    /**
     * 通过任务id，工具名，状态，相对路径查询告警文件清单
     *
     * @param taskId
     * @param toolName
     * @param status
     * @param relPaths
     * @return
     */
    @Query(value = "{'task_id': ?0, 'tool_name': ?1, 'status': ?2, 'rel_path': {'$in': ?3}}")
    List<LintFileEntity> findByTaskIdAndToolNameAndStatusAndRelPathIn(long taskId, String toolName, int status, Set<String> relPaths);

    /**
     * 通过文件id和和告警id查询告警
     *
     * @param entityId
     * @param defectId
     * @return
     */
    @Query(value = "{'_id': ?0, 'defect_list': {'$elemMatch': {'defect_id': ?1}}}")
    LintFileEntity findByEntityIdAndDefectId(String entityId, String defectId);

    /**
     * 通过文件ID列表查询
     *
     * @param fileEntityIds
     * @return
     */
    List<LintFileEntity> findByEntityIdIn(Set<String> fileEntityIds);

    /**
     * 通过任务id、工具名、文件路径查询告警文件清单
     *
     * @param taskId
     * @param toolName
     * @param filePathSet
     * @return
     */
    List<LintFileEntity> findByTaskIdAndToolNameAndFilePathIn(long taskId, String toolName, Set<String> filePathSet);

    /**
     * 通过任务id、工具名、文件相对路径查询告警文件清单
     *
     * @param taskId
     * @param toolName
     * @param relPathSet
     * @return
     */
    List<LintFileEntity> findByTaskIdAndToolNameAndRelPathIn(long taskId, String toolName, Set<String> relPathSet);

    /**
     * 通过任务Id、工具名称、状态查询告警
     *
     * @param taskId
     * @param toolNameList
     * @param status
     * @return
     */
    List<LintFileEntity> findByTaskIdAndToolNameInAndStatus(long taskId, List<String> toolNameList, int status);
}
