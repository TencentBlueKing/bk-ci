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

package com.tencent.bk.codecc.codeccjob.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.DefectEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

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
     * 通过任务id，工具名和状态查询告警信息
     *
     * @param taskId
     * @param toolName
     * @param status
     * @return
     */
    List<DefectEntity> findByTaskIdAndToolNameAndStatus(long taskId, String toolName, int status);

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
    @Query(fields = "{'id':1, 'status':1}", value = "{'task_id': ?0, 'tool_name': ?1, 'id': {'$in': ?2}}")
    List<DefectEntity> findStatusByTaskIdAndToolNameAndIdIn(long taskId, String toolName, Set<Long> idSet);

    /**
     * 根据entityIdSet查询告警信息
     *
     * @param entityIdSet
     * @return
     */
    @Query(fields = "{'id':1, 'status':1}", value = "{'_id': {'$in': ?0}}")
    List<DefectEntity> findStatusByEntityIdIn(Set<String> entityIdSet);

    /**
     * 通过任务id，工具名查询告警作者信息
     *
     * @return
     */
    @Query(fields = "{'author_list':1}", value = "{'task_id': ?0, 'tool_name': {'$in': ?1}, 'status': ?2}")
    List<DefectEntity> findAuthorListByTaskIdAndToolNameInAndStatus(long taskId, List<String> toolNameList, int status, Pageable pageable);
}
