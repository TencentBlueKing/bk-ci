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
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.apiquery.defect.dao.CheckerDefectStatDao;
import com.tencent.bk.codecc.apiquery.defect.model.CheckerDefectStatModel;
import com.tencent.bk.codecc.apiquery.defect.model.CheckerPropsModel;
import com.tencent.bk.codecc.apiquery.defect.model.CheckerSetCategoryModel;
import com.tencent.bk.codecc.apiquery.defect.model.CheckerSetModel;
import com.tencent.bk.codecc.apiquery.service.CheckerService;
import com.tencent.bk.codecc.apiquery.service.ICheckerSetBizService;
import com.tencent.bk.codecc.apiquery.utils.PageUtils;
import com.tencent.bk.codecc.apiquery.vo.CheckerDefectStatVO;
import com.tencent.bk.codecc.apiquery.vo.CheckerSetListQueryReq;
import com.tencent.bk.codecc.apiquery.vo.CheckerSetVO;
import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.util.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 规则告警数管理服务接口
 *
 * @version V2.0
 * @date 2020/5/12
 */
@Slf4j
@Service
public class OpCheckServiceImpl implements CheckerService {
    @Autowired
    private CheckerDefectStatDao checkerDefectStatDao;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private ICheckerSetBizService iCheckerSetBizService;


    @Override
    public Page<CheckerDefectStatVO> getCheckerDefectStatList(TaskToolInfoReqVO reqVO, Integer pageNum,
            Integer pageSize, String sortField, String sortType) {
        log.info("op getCheckerDefectStatList req: pageNum{} pageSize{} content:{}", pageNum, pageSize, reqVO);

        if (reqVO.getEndTime() == null) {
            reqVO.setEndTime(DateTimeUtils.getDateByDiff(0));
        }
        if (CollectionUtils.isEmpty(reqVO.getCreateFrom())) {
            reqVO.setCreateFrom(Sets.newHashSet(ComConstants.DefectStatType.ALL.value()));
        }

        String sortFieldInDb = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, sortField);
        Pageable pageable =
                PageUtils.INSTANCE.convertPageSizeToPageable(pageNum, pageSize, sortFieldInDb, sortType);
        Page<CheckerDefectStatModel> modelPage = checkerDefectStatDao.findCheckerDefectStatPage(reqVO, pageable);
        List<CheckerDefectStatModel> defectStatModelList = modelPage.getRecords();
        if (CollectionUtils.isEmpty(defectStatModelList)) {
            return new Page<>(0, 0, 0, Lists.newArrayList());
        }

        List<CheckerDefectStatVO> defectStatVOS = defectStatModelList.stream().map(item -> {
            CheckerDefectStatVO checkerDefectStatVO = new CheckerDefectStatVO();
            BeanUtils.copyProperties(item, checkerDefectStatVO);
            return checkerDefectStatVO;
        }).collect(Collectors.toList());

        return new Page<>(modelPage.getCount(), modelPage.getPage(), modelPage.getPageSize(), modelPage.getTotalPages(),
                defectStatVOS);
    }

    /**
     * 获取规则告警数统计时间
     * @return long
     */
    @Override
    public Long getCheckerDefectStatUpdateTime() {
        String updateTime = redisTemplate.opsForValue().get(RedisKeyConstants.CHECKER_DEFECT_STAT_TIME);
        return StringUtils.isNotEmpty(updateTime) ? Long.parseLong(updateTime) : 0;
    }


    /**
     * 获取规则集管理列表
     *
     * @param checkerSetListQueryReq 规则集管理列表请求体
     * @param pageNum                页数
     * @param pageSize               每页多少条
     * @param sortField              排序字段
     * @param sortType               排序类型
     * @return page
     */
    @Override
    public Page<CheckerSetVO> getCheckerSetList(CheckerSetListQueryReq checkerSetListQueryReq, Integer pageNum,
            Integer pageSize, String sortField, String sortType) {
        log.info("op getCheckerSetList req: {}", checkerSetListQueryReq);
        String sortFieldInDb =
                CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, sortField == null ? "version" : sortField);
        Pageable pageable = PageUtils.INSTANCE
                .convertPageSizeToPageable(pageNum, pageSize, sortFieldInDb, sortType == null ? "DESC" : sortType);

        long checkerSetModelListStartTime = System.currentTimeMillis();
        // 分页查询规则管理列表
        Page<CheckerSetModel> checkerSetModelList =
                iCheckerSetBizService.getCheckerSetList(checkerSetListQueryReq, pageable);
        log.info("getCheckerSetList: iCheckerSetBizService.getCheckerSetList(), time consuming: [{}]",
                System.currentTimeMillis() - checkerSetModelListStartTime);

        List<CheckerSetVO> checkerSetList = new ArrayList<>();
        List<CheckerSetModel> records = checkerSetModelList.getRecords();
        for (CheckerSetModel record : records) {
            CheckerSetVO checkerSetVO = new CheckerSetVO();
            BeanUtils.copyProperties(record, checkerSetVO);
            checkerSetVO.setCheckerProps(null);
            if (record.getCreateTime() != null) {
                // 格式化创建时间
                checkerSetVO.setCreateStrTime(DateTimeUtils.timestamp2StringDate(record.getCreateTime()));
            }
            // 设置工具数
            ArrayList<CheckerPropsModel> checkerPropsList = null;
            if (CollectionUtils.isNotEmpty(record.getCheckerProps())) {
                checkerPropsList = record.getCheckerProps().stream().collect(
                        Collectors.collectingAndThen(Collectors.toCollection(() ->
                                        new TreeSet<>(Comparator.comparing(CheckerPropsModel::getToolName))),
                                ArrayList::new));
            }
            String toolNameStr = "";
            int toolCount = 0;
            if (CollectionUtils.isNotEmpty(checkerPropsList)) {
                toolNameStr =
                        StringUtils.join(checkerPropsList.stream().map(CheckerPropsModel::getToolName).toArray(), ",");
                toolCount = checkerPropsList.size();
            }
            checkerSetVO.setToolName(toolNameStr);
            checkerSetVO.setToolCount(toolCount);
            List<CheckerSetCategoryModel> catagories = record.getCatagories();
            checkerSetVO.setTaskUsage(record.getTaskUsage() == null ? 0 : record.getTaskUsage());
            // 设置类别
            if (CollectionUtils.isNotEmpty(catagories)) {
                checkerSetVO.setCatagories(catagories.get(0).getEnName());
            }
            checkerSetList.add(checkerSetVO);
        }
        return new Page<>(checkerSetModelList.getCount(), checkerSetModelList.getPage(),
                checkerSetModelList.getPageSize(), checkerSetModelList.getTotalPages(), checkerSetList);
    }

}
