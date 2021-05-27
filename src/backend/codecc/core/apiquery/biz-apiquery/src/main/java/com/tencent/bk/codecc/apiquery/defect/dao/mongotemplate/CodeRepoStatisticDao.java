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
import com.tencent.bk.codecc.apiquery.defect.model.CodeRepoStatisticModel;
import com.tencent.devops.common.api.pojo.Page;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.SkipOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 代码仓库总表持久类
 *
 * @version V2.0
 * @date 2020/6/15
 */
@Repository
public class CodeRepoStatisticDao {
    @Autowired
    @Qualifier("defectMongoTemplate")
    private MongoTemplate mongoTemplate;

    /**
     * 获取代码库总表数据
     *
     * @param urlStartTime    仓库扫描开始时间
     * @param urlEndTime      仓库扫描结束时间
     * @param branchStartTime 分支扫描开始时间
     * @param branchEndTime   分支扫描结束时间
     * @param createFrom      数据来源
     * @param pageable        分页数据
     * @param searchString    搜索框内容
     * @return page
     */
    public Page<CodeRepoStatisticModel> queryCodeRepoList(long urlStartTime, long urlEndTime, long branchStartTime,
            long branchEndTime, Set<String> createFrom, Pageable pageable, String searchString) {

        Criteria criteria = new Criteria();
        List<Criteria> criteriaList = Lists.newArrayList();

        // 第一次仓库扫描时间
        if (urlStartTime != 0 && urlEndTime != 0) {
            criteriaList.add(Criteria.where("url_first_scan").gte(urlStartTime).lte(urlEndTime));
        }
        // 第一次分支扫描时间
        if (branchStartTime != 0 && branchEndTime != 0) {
            criteriaList.add(Criteria.where("branch_first_scan").gte(branchStartTime).lte(branchEndTime));
        }
        // 来源
        if (CollectionUtils.isNotEmpty(createFrom)) {
            criteriaList.add(Criteria.where("data_from").in(createFrom));
        }
        // 搜索条件 url/branch
        if (StringUtils.isNotEmpty(searchString)) {
            List<Criteria> searchCriteria = new ArrayList<>();
            searchCriteria.add(Criteria.where("url").regex(searchString));
            searchCriteria.add(Criteria.where("branch").regex(searchString));
            criteriaList.add(new Criteria().orOperator(searchCriteria.toArray(new Criteria[0])));
        }

        if (CollectionUtils.isNotEmpty(criteriaList)) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }

        // 获取满足条件的总数
        long totalCount = mongoTemplate.count(new Query(criteria), "t_code_repo_statistic");

        // 分页排序
        int pageSize = pageable.getPageSize();
        int pageNumber = pageable.getPageNumber();
        SortOperation sort = Aggregation.sort(pageable.getSort());
        SkipOperation skip = Aggregation.skip(Long.valueOf(pageNumber * pageSize));
        LimitOperation limit = Aggregation.limit(pageSize);

        Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(criteria), sort, skip, limit);
        AggregationResults<CodeRepoStatisticModel> queryResults =
                mongoTemplate.aggregate(aggregation, "t_code_repo_statistic", CodeRepoStatisticModel.class);

        // 计算总页数
        int totalPageNum = 0;
        if (totalCount > 0) {
            totalPageNum = (Integer.parseInt(String.valueOf(totalCount)) + pageSize - 1) / pageSize;
        }

        // 页码加1返回
        return new Page<>(totalCount, pageNumber + 1, pageSize, totalPageNum, queryResults.getMappedResults());
    }
}
