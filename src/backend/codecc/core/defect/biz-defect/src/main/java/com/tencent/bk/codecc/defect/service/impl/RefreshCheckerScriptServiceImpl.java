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

package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.CheckerDetailDao;
import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.bk.codecc.defect.service.RefreshCheckerScriptService;
import com.tencent.bk.codecc.defect.utils.PageUtils;
import com.tencent.devops.common.api.pojo.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 刷新 规则描述、详细说明 数据
 *
 * @version V2.0
 * @date 2020/11/18
 */
@Service
@Slf4j
public class RefreshCheckerScriptServiceImpl implements RefreshCheckerScriptService {
    @Autowired
    private CheckerDetailDao checkerDetailDao;

    @Autowired
    private CheckerRepository checkerRepository;

    @Override
    public Boolean initCheckerDetailScript(String toolName, Integer pageNum, Integer pageSize, String sortField,
            String sortType) {
        if (sortField == null) {
            // 排序字段为login_date
            sortField = "checker_key";
        }
        if (sortType == null) {
            sortType = "ASC";
        }
        int size;
        Pageable pageable = PageUtils.INSTANCE.generaPageableUnlimitedPageSize(pageNum, pageSize, sortField, sortType);
        do {
            Page<CheckerDetailEntity> checkerDetailPage =
                    checkerDetailDao.findCheckerDetailByToolName(toolName, pageable);
            List<CheckerDetailEntity> checkerDetailModels = checkerDetailPage.getRecords();
            size = checkerDetailModels.size();
            // 修改后的存进来,批量编辑
            List<CheckerDetailEntity> modifyCheckerDetailModels = new ArrayList<>();

            if (CollectionUtils.isNotEmpty(checkerDetailModels)) {
                // 正则表达式
                Pattern compile = Pattern.compile("<br> ?\n?");
                checkerDetailModels.forEach(checkerDetailModel -> {
                    // 详细说明
                    String checkerDescModel = checkerDetailModel.getCheckerDescModel();
                    if (StringUtils.isNotEmpty(checkerDescModel)) {
                        if (checkerDescModel.contains("<br>")) {
                            // 将checkerDescModel中的"<br> "替换成"\n"
                            checkerDetailModel.setCheckerDescModel(compile.matcher(checkerDescModel).replaceAll("\n"));
                        }
                    }
                    // 描述
                    String checkerDesc = checkerDetailModel.getCheckerDesc();
                    if (StringUtils.isNotEmpty(checkerDesc)) {
                        if (checkerDesc.contains("<br>")) {
                            // 将checkerDesc中的"<br> "替换成"\n"
                            checkerDetailModel.setCheckerDesc(compile.matcher(checkerDesc).replaceAll("\n"));
                        }
                    }
                    modifyCheckerDetailModels.add(checkerDetailModel);
                });
                checkerRepository.saveAll(modifyCheckerDetailModels);
            }
            pageable = PageUtils.INSTANCE
                    .generaPageableUnlimitedPageSize(checkerDetailPage.getPage() + 1, pageSize, sortField, sortType);
        } while (size >= pageSize);
        return true;
    }
}