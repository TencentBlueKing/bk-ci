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

package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.ServicePkgDefectRestResource;
import com.tencent.bk.codecc.defect.service.ICLOCQueryCodeLineService;
import com.tencent.bk.codecc.defect.service.IQueryWarningBizService;
import com.tencent.bk.codecc.defect.vo.ToolClocRspVO;
import com.tencent.bk.codecc.defect.vo.ToolDefectRspVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.web.RestResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

/**
 * 规则相关接口实现类
 *
 * @version V1.0
 * @date 2019/11/15
 */
@Slf4j
@RestResource
public class ServicePkgDefectRestResourceImpl implements ServicePkgDefectRestResource {

    @Autowired
    private BizServiceFactory<IQueryWarningBizService> fileAndDefectQueryFactory;

    @Autowired
    private ICLOCQueryCodeLineService iclocQueryCodeLineService;

    @Override
    public CodeCCResult<ToolDefectRspVO> queryToolDefectList(Long taskId, DefectQueryReqVO defectQueryReqVO,
                                                             Integer pageNum, Integer pageSize, String sortField, Sort.Direction sortType) {
        IQueryWarningBizService bizService = fileAndDefectQueryFactory
                .createBizService(defectQueryReqVO.getToolName(), ComConstants.BusinessType.QUERY_WARNING.value(), IQueryWarningBizService.class);
        return new CodeCCResult<>(bizService.processToolWarningRequest(taskId, defectQueryReqVO, pageNum, pageSize, sortField, sortType));
    }

    @Override
    public CodeCCResult<ToolClocRspVO> queryCodeLine(Long taskId)
    {
        return new CodeCCResult<>(iclocQueryCodeLineService.getCodeLineInfo(taskId));
    }
}
