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
import com.tencent.bk.codecc.apiquery.defect.model.CheckerSetModel;
import com.tencent.bk.codecc.apiquery.defect.model.CheckerSetTaskRelationshipModel;
import com.tencent.bk.codecc.apiquery.vo.CheckerSetListQueryReq;
import com.tencent.bk.codecc.defect.vo.enums.CheckerSetSource;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.util.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.CountOperation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SkipOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 规则集数据DAO
 *
 * @version V2.0
 * @date 2020/5/12
 */
@Repository
@Slf4j
public class CheckerSetDao {
    @Autowired
    @Qualifier("defectMongoTemplate")
    private MongoTemplate mongoTemplate;


    /**
     * 根据任务ID集合获取关联的规则集
     *
     * @param taskIdSet 任务ID集合
     * @return list
     */
    public List<CheckerSetTaskRelationshipModel> findByTaskId(Collection<Long> taskIdSet) {
        Criteria criteria = new Criteria();
        if (CollectionUtils.isNotEmpty(taskIdSet)) {
            criteria.andOperator(Criteria.where("task_id").in(taskIdSet));
        }

        AggregationOptions options = new AggregationOptions.Builder().allowDiskUse(true).build();
        Aggregation agg = Aggregation.newAggregation(Aggregation.match(criteria)).withOptions(options);

        AggregationResults<CheckerSetTaskRelationshipModel> queryResults =
                mongoTemplate.aggregate(agg, "t_checker_set_task_relationship", CheckerSetTaskRelationshipModel.class);
        return queryResults.getMappedResults();
    }

    /**
     * 按规则集ID集合获取规则集详情列表
     *
     * @param checkerSetIdList 规则集ID集合
     * @param withProps        是否查询checkerProps字段
     * @return list
     */
    public List<CheckerSetModel> findByCheckerSetIdList(Collection<String> checkerSetIdList, Boolean withProps) {
        List<CheckerSetModel> checkerSetModelList = Lists.newArrayList();
        if (CollectionUtils.isEmpty(checkerSetIdList)) {
            return checkerSetModelList;
        }

        Criteria criteria = new Criteria();
        criteria.andOperator(Criteria.where("checker_set_id").in(checkerSetIdList));
        // 简化查询字段
        List<String> queryFieldList = Lists.newArrayList("checker_set_id", "checker_set_name", "code_lang",
                "checker_set_lang", "scope", "version", "creator", "create_time", "last_update_time", "checker_count",
                "task_usage", "enable", "sort_weight", "project_id", "description", "catagories", "legacy", "official");
        if (withProps) {
            queryFieldList.add("checker_props");
        }
        ProjectionOperation project = Aggregation.project(queryFieldList.toArray(new String[0]));

        AggregationOptions options = new AggregationOptions.Builder().allowDiskUse(true).build();
        Aggregation agg = Aggregation.newAggregation(Aggregation.match(criteria), project).withOptions(options);

        AggregationResults<CheckerSetModel> queryResults =
                mongoTemplate.aggregate(agg, "t_checker_set", CheckerSetModel.class);
        return queryResults.getMappedResults();

    }


    /**
     * 根据规则集id和版本查询规则集
     */
    public CheckerSetModel findByCheckerSetIdAndVersion(String checkerSetId, int version) {
        Query query = new Query();
        query.addCriteria(
                Criteria.where("checker_set_id").is(checkerSetId)
                        .and("version").is(version)
        );
        return mongoTemplate.findOne(query, CheckerSetModel.class, "t_checker_set");
    }

    public CheckerSetModel findLatestVersionByCheckerSetId(String checkerSetId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("checker_set_id").is(checkerSetId))
                .with(Sort.by(Sort.Direction.DESC,"version"));

        return mongoTemplate.findOne(query, CheckerSetModel.class, "t_checker_set");
    }

    /**
     * 分页查询规则管理列表O
     *
     * @param checkerSetListQueryReq 规则集管理列表请求体
     * @param pageable               分页数据
     * @return
     */
    public Page<CheckerSetModel> getCheckerSetList(CheckerSetListQueryReq checkerSetListQueryReq, Pageable pageable) {
        Criteria criteria = new Criteria();
        List<Criteria> criteriaList = Lists.newArrayList();

        // 类别
        Set<String> catagories = checkerSetListQueryReq.getCatagories();
        if (CollectionUtils.isNotEmpty(catagories)) {
            criteriaList.add(Criteria.where("catagories").elemMatch(Criteria.where("en_name").in(catagories)));
        }
        // 工具名
        Set<String> toolName = checkerSetListQueryReq.getToolName();
        if (CollectionUtils.isNotEmpty(toolName)) {
            criteriaList.add(Criteria.where("tool_name").in(toolName));
        }

        // 来源
        Set<String> checkerSetSource = checkerSetListQueryReq.getCheckerSetSource();
        if (CollectionUtils.isNotEmpty(checkerSetSource)) {
            List<Criteria> sourceCriteria = new ArrayList<>();
            sourceCriteria.add(Criteria.where("checker_set_source").in(checkerSetSource));
            if (checkerSetSource.contains(CheckerSetSource.SELF_DEFINED)) {
                sourceCriteria.add(Criteria.where("checker_set_source").exists(false));
                sourceCriteria.add(Criteria.where("checker_set_source").is(null));
            }
            criteriaList.add(new Criteria().orOperator(sourceCriteria.toArray(new Criteria[0])));
        }

        // 日期
        String startTime = checkerSetListQueryReq.getStartTime();
        String endTime = checkerSetListQueryReq.getEndTime();
        if (StringUtils.isNotEmpty(startTime) && StringUtils.isNotEmpty(endTime)) {
            criteriaList.add(Criteria.where("create_time").gte(DateTimeUtils.getTimeStamp(startTime))
                    .lte(DateTimeUtils.getTimeStamp(endTime)));
        }

        Set<String> codeLang = checkerSetListQueryReq.getCodeLang();
        if (CollectionUtils.isNotEmpty(codeLang)) {
            List<Criteria> langCriteria = new ArrayList<>();
            langCriteria.add(Criteria.where("checker_set_lang").in(codeLang));
            Criteria langArrayCriteria = Criteria.where("legacy").is(true);
            List<Criteria> langOrCriteria = new ArrayList<>();
            for (String lang : codeLang) {
                langOrCriteria.add(Criteria.where("checker_set_lang").regex(lang));
            }
            langCriteria.add(new Criteria().andOperator(langArrayCriteria,
                    new Criteria().orOperator(langOrCriteria.toArray(new Criteria[0]))));
            criteriaList.add(new Criteria().orOperator(langCriteria.toArray(new Criteria[0])));
        }

        // 搜索框模糊查询
        String quickSearch = checkerSetListQueryReq.getQuickSearch();
        if (StringUtils.isNotEmpty(quickSearch)) {
            List<Criteria> quickSearchCriteria = new ArrayList<>();
            quickSearchCriteria.add(Criteria.where("checker_set_id").regex(quickSearch));
            quickSearchCriteria.add(Criteria.where("checker_set_name").regex(quickSearch));
            criteriaList.add(new Criteria().orOperator(quickSearchCriteria.toArray(new Criteria[0])));
        }

        if (CollectionUtils.isNotEmpty(criteriaList)) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }

        MatchOperation match = Aggregation.match(criteria);

        // 以checker_set_id进行分组
        GroupOperation group = Aggregation.group("checker_set_id")
                .first("checker_set_id").as("checker_set_id")
                .first("checker_set_name").as("checker_set_name")
                .first("description").as("description")
                .first("catagories").as("catagories")
                .first("checker_set_lang").as("checker_set_lang")
                .first("checker_count").as("checker_count")
                .first("checker_props").as("checker_props")
                .first("creator").as("creator")
                .first("task_usage").as("task_usage")
                .first("create_time").as("create_time")
                .first("checker_set_source").as("checker_set_source")
                .first("version").as("version");

        long getTotalCountStartTime = System.currentTimeMillis();
        Aggregation aggregationDistinct = Aggregation
                .newAggregation(match, Aggregation.group("checker_set_id"), Aggregation.count().as("checker_count"));

        AggregationResults<CheckerSetModel> queryResultList =
                mongoTemplate.aggregate(aggregationDistinct, "t_checker_set", CheckerSetModel.class);

        List<CheckerSetModel> checkerSetModelList = queryResultList.getMappedResults();
        int totalCount = 0;
        if (CollectionUtils.isNotEmpty(checkerSetModelList)) {
            totalCount = checkerSetModelList.get(0).getCheckerCount();
        }
        log.info("getCheckerSetList: get totalCount, time consuming: [{}]",
                System.currentTimeMillis() - getTotalCountStartTime);
        // 分页排序
        int pageSize = pageable.getPageSize();
        int pageNumber = pageable.getPageNumber();
        SortOperation sort = Aggregation.sort(pageable.getSort());
        SkipOperation skip = Aggregation.skip(Long.valueOf(pageNumber * pageSize));
        LimitOperation limit = Aggregation.limit(pageSize);

        // 允许磁盘操作(支持较大数据集合的处理)
        AggregationOptions options = new AggregationOptions.Builder().allowDiskUse(true).build();
        Aggregation aggregation = Aggregation.newAggregation(match, sort, group, skip, limit).withOptions(options);
        AggregationResults<CheckerSetModel> queryResults =
                mongoTemplate.aggregate(aggregation, "t_checker_set", CheckerSetModel.class);

        // 计算总页数
        int totalPageNum = 0;
        if (totalCount > 0) {
            totalPageNum = (Integer.parseInt(String.valueOf(totalCount)) + pageSize - 1) / pageSize;
        }
        // 页码加1返回
        return new Page<>(totalCount, pageNumber + 1, pageSize, totalPageNum, queryResults.getMappedResults());
    }
}
