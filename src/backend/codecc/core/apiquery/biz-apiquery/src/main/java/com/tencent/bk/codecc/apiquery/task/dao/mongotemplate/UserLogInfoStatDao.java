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

import com.mongodb.BasicDBObject;
import com.tencent.bk.codecc.apiquery.task.model.UserLogInfoModel;
import com.tencent.bk.codecc.apiquery.task.model.UserLogInfoStatModel;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.util.DateTimeUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SkipOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 用户登录信息持久层
 *
 * @version V1.0
 * @date 2020/10/19
 */
@Repository
public class UserLogInfoStatDao {
    @Autowired
    @Qualifier("taskMongoTemplate")
    private MongoTemplate mongoTemplate;


    /**
     * 获取总用户登录列表
     *
     * @param pageable
     * @return
     */
    public Page<UserLogInfoStatModel> findAllUserLogInfoStatPage(Pageable pageable) {

        //总条数
        long totalCount = mongoTemplate.count(new Query(), "t_user_log_info_stat");

        // 分页排序
        int pageSize = pageable.getPageSize();
        int pageNumber = pageable.getPageNumber();
        SortOperation sort = Aggregation.sort(pageable.getSort());
        SkipOperation skip = Aggregation.skip((long) (pageNumber * pageSize));
        LimitOperation limit = Aggregation.limit(pageSize);

        Aggregation aggregation = Aggregation.newAggregation(sort, skip, limit);

        AggregationResults<UserLogInfoStatModel> queryResults =
                mongoTemplate.aggregate(aggregation, "t_user_log_info_stat", UserLogInfoStatModel.class);

        // 计算总页数
        int totalPageNum = 0;
        if (totalCount > 0) {
            totalPageNum = ((int) totalCount + pageSize - 1) / pageSize;
        }
        return new Page<>(totalCount, pageNumber + 1, pageSize, totalPageNum, queryResults.getMappedResults());
    }

    /**
     * 根据用户名获取组织架构
     *
     * @param userNameList
     * @return
     */
    public List<UserLogInfoStatModel> findOrgByUserNameList(Collection<String> userNameList) {
        Query query = new Query();
        query.addCriteria(Criteria.where("user_name").in(userNameList));

        return mongoTemplate.find(query, UserLogInfoStatModel.class, "t_user_log_info_stat");
    }


    /**
     * 获取每日新增用户数量
     *
     * @param date
     * @return
     */
    public Integer findUserAddCountByDaily(String date) {
        long[] startTimeAndEndTime = DateTimeUtils.getStartTimeAndEndTime(date, date);
        //条件 首次登录时间在今天,说明是今天新增的用户
        Criteria criteria = Criteria.where("first_login").gte(startTimeAndEndTime[0]).lte(startTimeAndEndTime[1]);
        //总条数
        Integer userCount = Math.toIntExact(mongoTemplate.count(new Query(criteria), "t_user_log_info_stat"));
        return userCount;
    }

    /**
     * 获取全部用户的数量
     *
     * @param date
     * @return
     */
    public Integer findUserLogInCount(String date) {
        long[] startTimeAndEndTime = DateTimeUtils.getStartTimeAndEndTime(date, date);
        //条件 首次登录时间 < 当天时间 就可以获得当天,用户的总数量
        Criteria criteria = Criteria.where("first_login").lte(startTimeAndEndTime[1]);
        //总条数
        Integer userCount = Math.toIntExact(mongoTemplate.count(new Query(criteria), "t_user_log_info_stat"));

        return userCount;
    }
}
