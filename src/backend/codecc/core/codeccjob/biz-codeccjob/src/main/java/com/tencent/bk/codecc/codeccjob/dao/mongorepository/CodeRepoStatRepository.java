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

import com.tencent.bk.codecc.defect.model.CodeRepoStatisticEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * 代码仓库分支统计持久层
 *
 * @version V1.0
 * @date 2021/2/23
 */

@Repository
public interface CodeRepoStatRepository extends MongoRepository<CodeRepoStatisticEntity, String> {

    /**
     * 获取代码库第一条数据
     * @param dataFrom 开源/非开源
     * @param url      代码库地址
     * @return entity
     */
    CodeRepoStatisticEntity findOneByDataFromAndUrl(String dataFrom, String url);

    /**
     * 获取指定唯一的代码仓库统计信息
     * @param dataFrom 开源/非开源
     * @param url      代码库地址
     * @param branch   分支
     * @return entity
     */
    CodeRepoStatisticEntity findFirstByDataFromAndUrlAndBranch(String dataFrom, String url, String branch);

}
