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

package com.tencent.bk.codecc.apiquery.task.dao.mongotemplate;

import com.tencent.bk.codecc.apiquery.task.model.UserLogInfoModel;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * 用户统计持久层代码
 *
 * @version V1.0
 * @date 2020/10/19
 */

@Repository
public class UserLogInfoDao {

    @Autowired
    @Qualifier("taskMongoTemplate")
    private MongoTemplate mongoTemplate;


    /**
     * 查询每日用户登录列表
     *
     * @param startDate 开始日期
     * @param endDate   截止日期
     * @param pageable  排序
     * @return list
     */
    public List<UserLogInfoModel> findDailyUserLogInfoList(Date startDate, Date endDate, @NotNull Pageable pageable) {
        // 日期条件
        Criteria criteria = Criteria.where("login_date").gte(startDate).lte(endDate);

        MatchOperation match = Aggregation.match(criteria);

        // 以日期分组统计每日人数
        GroupOperation group = Aggregation.group("login_date")
                .first("login_date").as("login_date")
                .addToSet("user_name").as("user_name_list");

        // 排序
        SortOperation sort = Aggregation.sort(pageable.getSort());

        Aggregation aggregation = Aggregation.newAggregation(match, group, sort);
        AggregationResults<UserLogInfoModel> queryResults =
                mongoTemplate.aggregate(aggregation, "t_user_log_info", UserLogInfoModel.class);
        return queryResults.getMappedResults();
    }


    /**
     * 获取指定时间段用户登录的数量
     *
     * @param startTimeAndEndTime 日期
     * @return int
     */
    public Integer findUserLogInCountByDaily(long[] startTimeAndEndTime) {
        Document document = new Document();
        document.put("login_date", new Document("$gte", new Date(startTimeAndEndTime[0]))
                .append("$lte", new Date(startTimeAndEndTime[1])));
        List distinct = mongoTemplate.findDistinct(new BasicQuery(document),
                "user_name", "t_user_log_info", Object.class);
        return distinct.size();
    }
}
