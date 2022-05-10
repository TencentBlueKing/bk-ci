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

package com.tencent.bk.codecc.task.dao.mongotemplate;

import com.tencent.bk.codecc.task.model.UserLogInfoEntity;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

/**
 * 用户日志统计Dao
 *
 * @version V1.0
 * @date 2020/10/20
 */
@Repository
public class UserLogDao {

    @Autowired
    private MongoTemplate mongoTemplate;


    /**
     * 获取所有用户名单
     * @return list
     */
    public List<String> findDistinctUserName() {
        List<String> distinct = mongoTemplate.findDistinct(new Query(),"user_name","t_user_log_info",String.class);
        return CollectionUtils.isEmpty(distinct)? Collections.emptyList() : distinct;
    }


    /**
     * 批量获取用户的首次登录时间和最近登录日期
     *
     * @param userNameList list
     * @return entities
     */
    public List<UserLogInfoEntity> findByLoginTimeDesc(List<String> userNameList) {

        Criteria criteria = new Criteria();
        criteria.and("user_name").in(userNameList);

        MatchOperation match = Aggregation.match(criteria);
        SortOperation sort = Aggregation.sort(Sort.Direction.ASC, "login_time");

        GroupOperation group = Aggregation.group("user_name")
                .first("user_name").as("user_name")
                .first("login_time").as("login_time")
                .last("login_date").as("login_date");

        Aggregation agg = Aggregation.newAggregation(match, sort, group);
        return mongoTemplate.aggregate(agg, "t_user_log_info", UserLogInfoEntity.class).getMappedResults();
    }


}
