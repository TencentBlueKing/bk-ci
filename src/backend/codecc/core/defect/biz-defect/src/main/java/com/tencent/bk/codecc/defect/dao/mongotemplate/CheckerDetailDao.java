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
import com.tencent.bk.codecc.defect.dao.AddFieldOperation;
import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.bk.codecc.defect.vo.enums.CheckerCategory;
import com.tencent.bk.codecc.defect.vo.enums.CheckerListSortType;
import com.tencent.bk.codecc.defect.vo.enums.CheckerRecommendType;
import com.tencent.devops.common.api.checkerset.CheckerPropVO;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.ToolIntegratedStatus;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SkipOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 规则详情持久类
 *
 * @version V1.0
 * @date 2019/12/26
 */
@Repository
public class CheckerDetailDao {
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 按照复合条件查询规则
     * @param keyWord
     * @param codeLang
     * @param checkerCategory
     * @param toolName
     * @param tag
     * @param severity
     * @param editable
     * @param checkerRecommend
     * @param selectedCheckerKey
     * @param checkerSetSelected
     * @param pageNum
     * @param pageSize
     * @param sortType
     * @param sortField
     * @param toolIntegratedStatus
     * @return
     */
    public List<CheckerDetailEntity> findByComplexCheckerCondition(String keyWord, Set<String> codeLang,
                                                                   Set<CheckerCategory> checkerCategory,
                                                                   Set<String> toolName, Set<String> tag,
                                                                   Set<String> severity, Set<Boolean> editable,
                                                                   Set<CheckerRecommendType> checkerRecommend,
                                                                   Set<String> selectedCheckerKey,
                                                                   Set<Boolean> checkerSetSelected, Integer pageNum,
                                                                   Integer pageSize, Sort.Direction sortType,
                                                                   CheckerListSortType sortField,
                                                                   ToolIntegratedStatus toolIntegratedStatus) {

        //由于需要根据是否选中排序，所以采用聚合查询
        Criteria criteria = new Criteria();
        List<Criteria> andCriteria = new ArrayList<>();
        //去除多余的严重等级数据
        if (StringUtils.isNotBlank(keyWord)) {
            List<Criteria> keywordCriteria = new ArrayList<>();
            keywordCriteria.add(Criteria.where("checker_desc").regex(keyWord));
            keywordCriteria.add(Criteria.where("checker_key").regex(keyWord));
            andCriteria.add(new Criteria().orOperator(keywordCriteria.toArray(new Criteria[0])));
        }
        if (CollectionUtils.isNotEmpty(codeLang)) {
            andCriteria.add(Criteria.where("checker_language").elemMatch(new Criteria().in(codeLang)));
        }
        if (CollectionUtils.isNotEmpty(checkerCategory)) {
            andCriteria.add(Criteria.where("checker_category").in(checkerCategory.stream()
                    .map(Enum::name).collect(Collectors.toSet())));
        }
        if (CollectionUtils.isNotEmpty(toolName)) {
            andCriteria.add(Criteria.where("tool_name").in(toolName));
        }
        if (CollectionUtils.isNotEmpty(tag)) {
            andCriteria.add(Criteria.where("checker_tag").elemMatch(new Criteria().in(tag)));
        }
        if (CollectionUtils.isNotEmpty(severity)) {
            andCriteria.add(Criteria.where("severity").in(severity.stream().map(sev -> {
                if (Integer.valueOf(sev) == ComConstants.PROMPT) {
                    return ComConstants.PROMPT_IN_DB;
                } else {
                    return Integer.valueOf(sev);
                }
            }).collect(Collectors.toList())));
        } else {
            andCriteria.add(Criteria.where("severity").in(Arrays.asList(ComConstants.SERIOUS, ComConstants.NORMAL,
                    ComConstants.PROMPT, ComConstants.PROMPT_IN_DB)));
        }
        if (CollectionUtils.isNotEmpty(editable)) {
            andCriteria.add(Criteria.where("editable").in(editable));
        }
        if (CollectionUtils.isNotEmpty(checkerRecommend)) {
            andCriteria.add(Criteria.where("checker_recommend").in(checkerRecommend.stream()
                    .map(Enum::name).collect(Collectors.toSet())));
        }

        if (CollectionUtils.isNotEmpty(checkerSetSelected) && CollectionUtils.isNotEmpty(selectedCheckerKey)) {
            List<Criteria> criteriaList = new ArrayList<>();
            if (checkerSetSelected.contains(true)) {
                criteriaList.add(Criteria.where("checker_key").in(selectedCheckerKey));
            }
            if (checkerSetSelected.contains(false)) {
                criteriaList.add(Criteria.where("checker_key").nin(selectedCheckerKey));
            }
            andCriteria.add(new Criteria().orOperator(criteriaList.toArray(new Criteria[0])));
        }

        if (toolIntegratedStatus != ToolIntegratedStatus.P) {
            andCriteria.add(Criteria.where("checker_version").in(Arrays.asList(toolIntegratedStatus.value(),
                    ToolIntegratedStatus.P.value(),
                    null)));
        } else {
            andCriteria.add(Criteria.where("checker_version").nin(Arrays.asList(
                    ToolIntegratedStatus.G.value(),
                    ToolIntegratedStatus.T.value())));
        }

        if (CollectionUtils.isNotEmpty(andCriteria)) {
            criteria.andOperator(andCriteria.toArray(new Criteria[0]));
        }

        MatchOperation match = Aggregation.match(criteria);

        //查询
        Aggregation agg;

        if (CollectionUtils.isNotEmpty(selectedCheckerKey)) {
            //添加字段
            AddFieldOperation addField = new AddFieldOperation(new Document("checkerSetSelected",
                    new Document("$in", new Object[]{"$checker_key", selectedCheckerKey})));

            if (null != pageNum || null != pageSize || null != sortType || null != sortField) {
                Integer queryPageNum = pageNum == null || pageNum - 1 < 0 ? 0 : pageNum - 1;
                Integer queryPageSize = pageSize == null || pageSize <= 0 ? 10 : pageSize;
                Sort.Direction querySortType = null == sortType ? Sort.Direction.ASC : sortType;
                String querySortField = null == sortField ? CheckerListSortType.checkerKey.getName() :
                        sortField.getName();
                //根据是否选中排序
                SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "checkerSetSelected").and(querySortType,
                        querySortField);
                SkipOperation skip = Aggregation.skip(Long.valueOf(queryPageNum * queryPageSize));
                LimitOperation limit = Aggregation.limit(queryPageSize);
                agg = Aggregation.newAggregation(match, addField, sort, skip, limit);
            } else {
                SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "checkerSetSelected");
                agg = Aggregation.newAggregation(match, addField, sort);
            }
        } else {
            if (null != pageNum || null != pageSize || null != sortType || null != sortField) {
                Integer queryPageNum = pageNum == null || pageNum - 1 < 0 ? 0 : pageNum - 1;
                Integer queryPageSize = pageSize == null || pageSize <= 0 ? 10 : pageSize;
                Sort.Direction querySortType = null == sortType ? Sort.Direction.ASC : sortType;
                String querySortField = null == sortField ? CheckerListSortType.checkerKey.getName() :
                        sortField.getName();
                SortOperation sort = Aggregation.sort(querySortType, querySortField);
                SkipOperation skip = Aggregation.skip(Long.valueOf(queryPageNum * queryPageSize));
                LimitOperation limit = Aggregation.limit(queryPageSize);
                agg = Aggregation.newAggregation(match, sort, skip, limit);
            } else {
                agg = Aggregation.newAggregation(match);
            }
        }

        AggregationResults<CheckerDetailEntity> queryResult = mongoTemplate.aggregate(agg, "t_checker_detail",
                CheckerDetailEntity.class);
        return queryResult.getMappedResults();
    }

    /**
     * 根据工具名查找CheckerDetail
     *
     * @param toolName
     * @param pageable
     * @return
     */
    public Page<CheckerDetailEntity> findCheckerDetailByToolName(String toolName, @NotNull Pageable pageable) {
        Criteria criteria = Criteria.where("tool_name").is(toolName);
        //总条数
        long totalCount = mongoTemplate.count(new Query(criteria), "t_checker_detail");
        // 以tool_name进行过滤
        MatchOperation match = Aggregation.match(criteria);
        // 分页排序
        int pageSize = pageable.getPageSize();
        int pageNumber = pageable.getPageNumber();
        SortOperation sort = Aggregation.sort(pageable.getSort());
        SkipOperation skip = Aggregation.skip((long) (pageNumber * pageSize));
        LimitOperation limit = Aggregation.limit(pageSize);
        Aggregation agg = Aggregation.newAggregation(match, sort, skip, limit);
        AggregationResults<CheckerDetailEntity> queryResults =
                mongoTemplate.aggregate(agg, "t_checker_detail", CheckerDetailEntity.class);
        // 计算总页数
        int totalPageNum = 0;
        if (totalCount > 0) {
            totalPageNum = ((int) totalCount + pageSize - 1) / pageSize;
        }
        return new Page<>(totalCount, pageNumber + 1, pageSize, totalPageNum, queryResults.getMappedResults());
    }

    /**
     * 根据工具和规则key查询规则
     * @param toolCheckerMap
     * @return
     */
    public List<CheckerDetailEntity> findByToolNameAndCheckers(Map<String, List<CheckerPropVO>> toolCheckerMap) {
        Criteria criteria = new Criteria();
        List<Criteria> orCriteriaList = Lists.newArrayList();
        toolCheckerMap.forEach((toolName, checkerList) -> {
            Set<String> checkers = checkerList.stream().map(CheckerPropVO::getCheckerKey).collect(Collectors.toSet());
            orCriteriaList.add(Criteria.where("tool_name").is(toolName).and("checker_key").in(checkers));
        });

        if (CollectionUtils.isNotEmpty(orCriteriaList)) {
            criteria.orOperator(orCriteriaList.toArray(new Criteria[0]));
        }

        Document fieldsObj = new Document();
        fieldsObj.put("checker_version", true);
        Query query = new BasicQuery(new Document(), fieldsObj);

        query.addCriteria(criteria);
        List<CheckerDetailEntity> checkerList = mongoTemplate.find(query, CheckerDetailEntity.class);
        return checkerList;
    }
}
