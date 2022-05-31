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

import com.tencent.bk.codecc.defect.model.DUPCDefectEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 重复率类工具查询持久类
 *
 * @version V1.0
 * @date 2019/5/14
 */
@Repository
public interface DUPCDefectRepository extends MongoRepository<DUPCDefectEntity, String>
{
    /**
     * 根据任务id和工具名称寻找DUPC类重复文件
     *
     * @param taskId
     * @return
     */
    @Query(fields = "{'block_list':0}", value = "{'task_id': ?0}")
    List<DUPCDefectEntity> findByTaskIdWithoutBlockList(long taskId);

    /**
     * 根据主键查询告警信息
     *
     * @param entityId
     * @return
     */
    DUPCDefectEntity findFirstByEntityId(String entityId);

    /**
     * 根据任务id和相对路径查询告警信息
     *
     * @param taskId
     * @param relPath
     * @return
     */
    DUPCDefectEntity findByTaskIdAndRelPath(long taskId, String relPath);


    /**
     * 根据任务id和状态查询告警信息
     *
     * @param taskId
     * @param status
     * @return
     */
    @Query(fields = "{'rel_path':1, 'dup_rate':1}", value = "{'task_id': ?0, 'status':?1}")
    List<DUPCDefectEntity> getByTaskIdAndStatus(long taskId, int status);

    /**
     * 通过任务id和状态查询全量数据
     *
     * @param taskId
     * @param status
     * @return
     */
    @Query(fields = "{'block_list':0}", value = "{'task_id': ?0, 'status':?1}")
    List<DUPCDefectEntity> findByTaskIdAndStatus(long taskId, int status);

    /**
     * 根据任务id和文件路径查询告警信息
     *
     * @param taskId
     * @param status
     * @return
     */
    @Query(fields = "{'block_list':0}", value = "{'task_id': ?0, 'file_path':?1}")
    DUPCDefectEntity findFirstByTaskIdAndFilePath(long taskId, String status);

}
