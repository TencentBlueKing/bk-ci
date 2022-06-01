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

package com.tencent.bk.codecc.apiquery.service.impl;

import com.google.common.base.CaseFormat;

import com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate.CodeRepoStatDailyDao;
import com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate.CodeRepoStatisticDao;
import com.tencent.bk.codecc.apiquery.defect.model.CodeRepoStatDailyModel;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate.CodeRepoInfoDao;
import com.tencent.bk.codecc.apiquery.defect.model.CodeRepoInfoModel;
import com.tencent.bk.codecc.apiquery.defect.model.CodeRepoModel;
import com.tencent.bk.codecc.apiquery.defect.model.CodeRepoStatisticModel;
import com.tencent.bk.codecc.apiquery.service.CodeRepoService;
import com.tencent.bk.codecc.apiquery.utils.PageUtils;
import com.tencent.bk.codecc.apiquery.vo.CodeRepoStatReqVO;
import com.tencent.bk.codecc.apiquery.vo.CodeRepoStatisticVO;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.util.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.tmatesoft.svn.core.internal.io.fs.IFSRepresentationCacheManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tencent.devops.common.constant.ComConstants.DAY_THIRTY;
import static com.tencent.devops.common.constant.ComConstants.DAY_THIRTYONE;

/**
 * 代码库总表数据接口实现
 *
 * @version V4.0
 * @date 2020/6/15
 */
@Slf4j
@Service
public class CodeRepoServiceImpl implements CodeRepoService {

    @Autowired
    private CodeRepoStatisticDao codeRepoStatisticDao;
    
    @Autowired
    private CodeRepoInfoDao codeRepoInfoDao;

    @Autowired
    private CodeRepoStatDailyDao codeRepoStatDailyDao;

    /**
     * 获取代码库总表数据
     *
     * @param reqVO 代码库总表请求体
     * @return page
     */
    @Override
    public Page<CodeRepoStatisticVO> queryCodeRepoList(CodeRepoStatReqVO reqVO, Integer pageNum, Integer pageSize,
            String sortField, String sortType) {
        long urlStartTime = 0;
        long urlEndTime = 0;
        long branchStartTime = 0;
        long branchEndTime = 0;
        if (StringUtils.isNotEmpty(reqVO.getUrlStartTime()) && StringUtils.isNotEmpty(reqVO.getUrlEndTime())) {
            urlStartTime = DateTimeUtils.getTimeStampStart(reqVO.getUrlStartTime());
            urlEndTime = DateTimeUtils.getTimeStampEnd(reqVO.getUrlEndTime());
        }
        if (StringUtils.isNotEmpty(reqVO.getBranchStartTime()) && StringUtils.isNotEmpty(reqVO.getBranchEndTime())) {
            branchStartTime = DateTimeUtils.getTimeStampStart(reqVO.getBranchStartTime());
            branchEndTime = DateTimeUtils.getTimeStampEnd(reqVO.getBranchEndTime());
        }
        Set<String> createFrom = reqVO.getCreateFrom();
        String searchString = reqVO.getSearchString();
        String sortFieldInDb = CaseFormat.LOWER_CAMEL
                .to(CaseFormat.LOWER_UNDERSCORE, sortField == null ? "url_first_scan" : sortField);
        Pageable pageable = PageUtils.INSTANCE
                .convertPageSizeToPageable(pageNum, pageSize, sortFieldInDb, sortType == null ? "DESC" : sortType);

        Page<CodeRepoStatisticModel> codeRepoStatisticPageable = codeRepoStatisticDao
                .queryCodeRepoList(urlStartTime, urlEndTime, branchStartTime, branchEndTime, createFrom, pageable,
                        searchString);
        List<CodeRepoStatisticModel> codeRepoStatisticModelList = codeRepoStatisticPageable.getRecords();
        List<CodeRepoStatisticVO> codeRepoStatisticList = new ArrayList<>();
        for (CodeRepoStatisticModel codeRepoStatisticModel : codeRepoStatisticModelList) {
            CodeRepoStatisticVO codeRepoStatisticVO = new CodeRepoStatisticVO();
            BeanUtils.copyProperties(codeRepoStatisticModel, codeRepoStatisticVO);
            codeRepoStatisticVO
                    .setUrlFirstScan(DateTimeUtils.timestamp2StringDate(codeRepoStatisticModel.getUrlFirstScan()));
            codeRepoStatisticVO.setBranchFirstScan(
                    DateTimeUtils.timestamp2StringDate(codeRepoStatisticModel.getBranchFirstScan()));
            codeRepoStatisticVO
                    .setBranchLastScan(DateTimeUtils.timestamp2StringDate(codeRepoStatisticModel.getBranchLastScan()));
            codeRepoStatisticList.add(codeRepoStatisticVO);
        }
        return new Page<>(codeRepoStatisticPageable.getCount(), codeRepoStatisticPageable.getPage(),
                codeRepoStatisticPageable.getPageSize(), codeRepoStatisticPageable.getTotalPages(),
                codeRepoStatisticList);
    }

    /**
     * 获取新增代码库/代码分支数折线图数据
     *
     * @param reqVO 代码库总表请求体
     * @return list
     */
    @Override
    public List<CodeRepoStatisticVO> queryCodeRepoStatTrend(CodeRepoStatReqVO reqVO) {
        // 数据来源(默认user)
        String createFrom = "user";
        if (StringUtils.isNotEmpty(reqVO.getCreateFromRadio())) {
            createFrom = reqVO.getCreateFromRadio();
        }
        // 获取仓库创建日期 默认显示30天
        List<String> dates = DateTimeUtils
                .getDatesByStartTimeAndEndTime(reqVO.getUrlStartTime(), reqVO.getUrlEndTime(), DAY_THIRTYONE);
        if (StringUtils.isEmpty(reqVO.getUrlStartTime()) && StringUtils.isEmpty(reqVO.getUrlEndTime())) {
            dates.remove(dates.size() - 1);
        }
        List<CodeRepoStatisticVO> codeRepoStatTrendList = new ArrayList<>();
        String startTime = "";
        String endTime = "";
        if (CollectionUtils.isNotEmpty(dates)) {
            startTime = dates.get(0);
            endTime = dates.get(dates.size() - 1);
        }

        long startTimeMillis = System.currentTimeMillis();

        List<CodeRepoStatDailyModel> codeRepoStatDailyModelList =
                codeRepoStatDailyDao.getUrlCountAndBranchCountByEndTime(startTime, endTime, createFrom);

        log.info("queryCodeRepoStatTrend: get codeRepoStatDailyModelList, time consuming: [{}ms]",
                System.currentTimeMillis() - startTimeMillis);

        Map<String, CodeRepoStatDailyModel> codeRepoStatDailyModelMap = codeRepoStatDailyModelList.stream()
                .collect(Collectors.toMap(CodeRepoStatDailyModel::getDate, Function.identity(), (k, v) -> v));

        for (String date : dates) {
            // 获取代码仓库/分支数量
            CodeRepoStatisticVO codeRepoStatisticVO = new CodeRepoStatisticVO();
            codeRepoStatisticVO.setDate(date);
            if (codeRepoStatDailyModelMap.get(date) != null) {
                codeRepoStatisticVO.setUrlCount(codeRepoStatDailyModelMap.get(date).getCodeRepoCount());
            }
            if (codeRepoStatDailyModelMap.get(date) != null) {
                codeRepoStatisticVO.setBranchCount(codeRepoStatDailyModelMap.get(date).getBranchCount());
            }
            if (codeRepoStatDailyModelMap.get(date) != null) {
                codeRepoStatisticVO.setNewCodeRepoCount(codeRepoStatDailyModelMap.get(date).getNewCodeRepoCount());
            }
            if (codeRepoStatDailyModelMap.get(date) != null) {
                codeRepoStatisticVO.setNewBranchCount(codeRepoStatDailyModelMap.get(date).getNewBranchCount());
            }
            codeRepoStatTrendList.add(codeRepoStatisticVO);
        }

        return codeRepoStatTrendList;
    }

    /**
     * 查询任务指定构建id的代码仓库信息
     *
     * @param taskIds  任务id集合
     * @param buildIds 构建id集合
     * @return map
     */
    @Override
    public Map<Long, Set<CodeRepoModel>> queryCodeRepoInfo(Collection<Long> taskIds, Collection<String> buildIds) {
        Map<Long, Set<CodeRepoModel>> taskCodeRepoMap = Maps.newHashMap();
        if (CollectionUtils.isEmpty(taskIds) || CollectionUtils.isEmpty(buildIds)) {
            return taskCodeRepoMap;
        }
        List<CodeRepoInfoModel> repoInfoModelList = codeRepoInfoDao.findByTaskIdAndBuildId(taskIds, buildIds);
        for (CodeRepoInfoModel codeRepoInfoModel : repoInfoModelList) {
            Set<CodeRepoModel> codeRepoModels =
                    taskCodeRepoMap.computeIfAbsent(codeRepoInfoModel.getTaskId(), k -> Sets.newHashSet());
            List<CodeRepoModel> repoList = codeRepoInfoModel.getRepoList();
            if (CollectionUtils.isNotEmpty(repoList)) {
                for (CodeRepoModel codeRepoModel : repoList) {
                    if (StringUtils.isNotEmpty(codeRepoModel.getUrl())
                            && StringUtils.isNotEmpty(codeRepoModel.getBranch())) {
                        codeRepoModels.add(codeRepoModel);
                    }
                }
            }
        }
        return taskCodeRepoMap;
    }
}
