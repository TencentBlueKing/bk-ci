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

package com.tencent.bk.codecc.codeccjob.dao.mongotemplate;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetEntity;
import com.tencent.bk.codecc.defect.vo.enums.CheckerSetCategory;
import com.tencent.bk.codecc.defect.vo.enums.CheckerSetSource;
import com.tencent.devops.common.constant.CheckerConstants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SkipOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 规则集数据DAO
 *
 * @version V4.0
 * @date 2019/11/2
 */
@Repository
public class CheckerSetDao {

    @Autowired
    private MongoTemplate mongoTemplate;


    /**
     * 查询带有指定规则的规则集
     *
     * @param checkers 规则集
     * @return list
     */
    public List<CheckerSetEntity> findByCheckerNameList(Collection<String> checkers) {
        Query query = new Query();

        if (CollectionUtils.isNotEmpty(checkers)) {
            query.addCriteria(Criteria.where("checker_props").elemMatch(Criteria.where("checker_key").in(checkers)));
        }
        query.addCriteria(Criteria.where("task_usage").gt(0).and("enable").is(1));

        return mongoTemplate.find(query, CheckerSetEntity.class);
    }

}
