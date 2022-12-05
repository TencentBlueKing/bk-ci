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

import com.tencent.bk.codecc.defect.model.CCNDefectEntity;
import com.tencent.bk.codecc.defect.model.LintFileEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * 圈复杂度文件查询持久代码
 *
 * @version V1.0
 * @date 2019/5/28
 */
@Repository
public interface CCNDefectRepository extends MongoRepository<CCNDefectEntity, String>
{

    /**
     * 通过主键id寻找圈复杂度信息
     *
     * @param entityId
     * @return
     */
    CCNDefectEntity findFirstByEntityId(String entityId);


    /**
     * 通过任务id，工具名和状态查询告警信息
     *
     * @param taskId
     * @param status
     * @return
     */
    List<CCNDefectEntity> findByTaskIdAndStatus(long taskId, int status);

    /**
     * 通过任务id，工具、文件路径
     *
     * @param taskId
     * @param filePath
     * @return
     */
    List<CCNDefectEntity> findByTaskIdAndRelPath(long taskId, String filePath);


    /**
     * 查询该任务的CCN告警
     *
     * @param taskId
     * @return
     */
    List<CCNDefectEntity> findByTaskId(long taskId);

    /**
     * 根据告警ID列表查询
     *
     * @param entityIds
     * @return
     */
    List<CCNDefectEntity> findByEntityIdIn(Set<String> entityIds);


    /**
     * 删除原数据
     * @param taskId
     */
    void deleteByTaskIdIsAndPinpointHashIsNull(Long taskId);

    /**
     * 通过任务id、文件路径查询告警文件清单
     *
     * @param taskId
     * @param filePathSet
     * @return
     */
    List<CCNDefectEntity> findByTaskIdAndFilePathIn(long taskId, Set<String> filePathSet);

    /**
     * 通过任务id、文件相对路径查询告警文件清单
     *
     * @param taskId
     * @param relPathSet
     * @return
     */
    List<CCNDefectEntity> findByTaskIdAndRelPathIn(long taskId, Set<String> relPathSet);

    @Query(fields = "{'ccn':1, 'status':1}", value = "{'task_id': ?0, 'status': {'$gt':1}}")
    List<CCNDefectEntity> findCloseDefectByTaskId(Long taskId);
}
