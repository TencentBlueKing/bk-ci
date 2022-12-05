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

package com.tencent.bk.codecc.defect.dao.mongotemplate;

import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 代码仓库总表持久类
 *
 * @version V2.0
 * @date 2021/3/24
 */
@Repository
public class CodeRepoStatisticDao {
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 根据时间获取总代码仓库数量
     *
     * @param endTime    结束时间
     * @param createFrom 数据来源
     * @return int
     */
    public int getUrlCountByEndTimeAndCreateFrom(long endTime, String createFrom) {
        Criteria criteria = getCodeRepoStatTrendCriteria(endTime, createFrom, "url_first_scan");
        return mongoTemplate.findDistinct(new Query(criteria),"url","t_code_repo_statistic",String.class)
                .size();
    }

    /**
     * 根据时间获取总代码分支数量
     *
     * @param endTime    结束时间
     * @param createFrom 数据来源
     * @return int
     */
    public int getBranchCountByEndTimeAndCreateFrom(long endTime, String createFrom) {
        Criteria criteria = getCodeRepoStatTrendCriteria(endTime, createFrom, "branch_first_scan");
        return (int) mongoTemplate.count(new Query(criteria), "t_code_repo_statistic");
    }

    /**
     * 获取总代码库/分支数量公共条件
     *
     * @param endTime    时间
     * @param createFrom 来源
     * @param firstScan  时间字段
     * @return criteria
     */
    @NotNull
    private Criteria getCodeRepoStatTrendCriteria(long endTime, String createFrom, String firstScan) {
        Criteria criteria = new Criteria();
        List<Criteria> criteriaList = Lists.newArrayList();
        // 时间
        if (endTime != 0) {
            criteriaList.add(Criteria.where(firstScan).lte(endTime));
        }
        // 来源
        if (StringUtils.isNotEmpty(createFrom)) {
            criteriaList.add(Criteria.where("data_from").is(createFrom));
        }
        if (CollectionUtils.isNotEmpty(criteriaList)) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }
        return criteria;
    }
}