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

import com.tencent.bk.codecc.defect.model.CodeFileUrlEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 代码仓库url的持久化
 * 
 * @date 2019/10/25
 * @version V1.0
 */
@Repository
public interface CodeFileUrlRepository extends MongoRepository<CodeFileUrlEntity, String>
{
    List<CodeFileUrlEntity> findByTaskId(long taskId);

    CodeFileUrlEntity findFirstByTaskIdAndFile(long taskId, String filePath);
}
