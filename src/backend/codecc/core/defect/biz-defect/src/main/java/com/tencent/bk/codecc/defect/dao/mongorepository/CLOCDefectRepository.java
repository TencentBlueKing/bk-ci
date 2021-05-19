/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.defect.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.CLOCDefectEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * cloc工具持久层接口
 *
 * @version V1.0
 * @date 2019/10/7
 */
@Repository
public interface CLOCDefectRepository extends MongoRepository<CLOCDefectEntity, String>
{
    /**
     * 查询该任务的CLOC信息
     *
     * @param taskId
     * @return
     */
    @Deprecated
    List<CLOCDefectEntity> findByTaskId(long taskId);

    /**
     * 查询该任务的CLOC信息
     *
     * @param taskId
     * @return
     */
    List<CLOCDefectEntity> findByTaskIdAndToolNameIn(long taskId, List<String> toolName);

    /**
     * 批量查询任务的CLOC信息
     *
     * @param taskIdSet 任务ID集合
     * @return list
     */
    @Deprecated
    List<CLOCDefectEntity> findByTaskIdIn(Collection<Long> taskIdSet);

    /**
     * 获取当前任务下所有生效告警记录
     *
     *
     */
    List<CLOCDefectEntity> findByTaskIdAndToolNameInAndStatusIsNot(Long taskId, List<String> toolName, String status);

}
