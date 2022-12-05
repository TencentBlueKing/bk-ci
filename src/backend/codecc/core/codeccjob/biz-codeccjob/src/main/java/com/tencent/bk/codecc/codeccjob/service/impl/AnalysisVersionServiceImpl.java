/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.codeccjob.service.impl;

import com.tencent.bk.codecc.defect.dto.AnalysisVersionDTO;
import com.tencent.bk.codecc.defect.model.AnalysisVersionEntity;
import com.tencent.bk.codecc.codeccjob.dao.mongorepository.AnalysisVersionRepository;
import com.tencent.bk.codecc.codeccjob.service.AnalysisVersionService;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 分析版本服务实现类
 *
 * @version V1.0
 * @date 2019/7/16
 */
@Service
public class AnalysisVersionServiceImpl implements AnalysisVersionService
{
    @Autowired
    private AnalysisVersionRepository analysisVersionRepository;

    @Override
    public void saveAnalysisVersion(AnalysisVersionDTO analysisVersionDTO)
    {
        AnalysisVersionEntity analysisVersionEntity = new AnalysisVersionEntity();
        BeanUtils.copyProperties(analysisVersionDTO, analysisVersionEntity);
        long currentTime = System.currentTimeMillis();
        analysisVersionEntity.setCreatedBy(ComConstants.SYSTEM_USER);
        analysisVersionEntity.setCreatedDate(currentTime);
        analysisVersionEntity.setUpdatedBy(ComConstants.SYSTEM_USER);
        analysisVersionEntity.setUpdatedDate(currentTime);
        analysisVersionRepository.save(analysisVersionEntity);
    }

}
