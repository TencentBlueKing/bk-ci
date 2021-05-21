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

package com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.apiquery.defect.model.CodeRepoStatDailyModel;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 代码库分支每日统计表持久类
 *
 * @version V2.0
 * @date 2021/3/25
 */
@Repository
public class CodeRepoStatDailyDao {
    @Autowired
    @Qualifier("defectMongoTemplate")
    private MongoTemplate mongoTemplate;

    public List<CodeRepoStatDailyModel> getUrlCountAndBranchCountByEndTime(String startTime, String endTime,
            String createFrom) {
        Criteria criteria = new Criteria();
        List<Criteria> criteriaList = Lists.newArrayList();
        // 来源
        if (StringUtils.isNotEmpty(createFrom)) {
            criteriaList.add(Criteria.where("data_from").is(createFrom));
        }
        // 时间
        if (StringUtils.isNotEmpty(startTime) && StringUtils.isNotEmpty(endTime)) {
            criteriaList.add(Criteria.where("date").gte(startTime).lte(endTime));
        }
        if (CollectionUtils.isNotEmpty(criteriaList)) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }

        return mongoTemplate.find(new Query(criteria), CodeRepoStatDailyModel.class, "t_code_repo_stat_daily");
    }
}
