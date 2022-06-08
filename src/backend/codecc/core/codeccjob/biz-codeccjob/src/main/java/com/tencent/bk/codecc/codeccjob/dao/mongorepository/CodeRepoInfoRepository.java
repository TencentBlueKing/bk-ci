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

package com.tencent.bk.codecc.codeccjob.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.incremental.CodeRepoInfoEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * 代码仓库信息持久层
 *
 * @version V1.0
 * @date 2021/2/23
 */

@Repository
public interface CodeRepoInfoRepository extends MongoRepository<CodeRepoInfoEntity, String> {

    /**
     * 获取指定构建id的代码仓库信息
     * @param taskId  任务id
     * @param buildId 构建id
     * @return entity
     */
    CodeRepoInfoEntity findFirstByTaskIdAndBuildId(long taskId, String buildId);

}
