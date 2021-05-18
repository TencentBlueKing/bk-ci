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
import com.tencent.bk.codecc.defect.service.impl.LintQueryWarningBizServiceImpl;
import com.tencent.bk.codecc.defect.service.openapi.ApiBizService;
import com.tencent.bk.codecc.defect.vo.CLOCDefectQueryRspInfoVO;
import com.tencent.bk.codecc.defect.vo.ToolClocRspVO;
import com.tencent.bk.codecc.defect.vo.ToolDefectRspVO;
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectExtReqVO;
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectReqVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.bk.codecc.defect.vo.openapi.CheckerPkgDefectRespVO;
import com.tencent.bk.codecc.defect.vo.openapi.CheckerPkgDefectVO;
import com.tencent.bk.codecc.defect.vo.openapi.TaskOverviewDetailRspVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.web.RestResource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
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
    private ApiBizService apiBizService;

    @Autowired
    private ICLOCQueryCodeLineService iclocQueryCodeLineService;

    @Override
    public Result<ToolDefectRspVO> queryToolDefectList(Long taskId, DefectQueryReqVO defectQueryReqVO,
                                                       Integer pageNum, Integer pageSize, String sortField, Sort.Direction sortType) {
        IQueryWarningBizService bizService = fileAndDefectQueryFactory
                .createBizService(defectQueryReqVO.getToolName(), ComConstants.BusinessType.QUERY_WARNING.value(), IQueryWarningBizService.class);
        return new Result<>(bizService.processToolWarningRequest(taskId, defectQueryReqVO, pageNum, pageSize, sortField, sortType));
    }

    @Override
    public Result<ToolClocRspVO> queryCodeLine(Long taskId, String toolName)
    {
        return new Result<>(iclocQueryCodeLineService.getCodeLineInfo(taskId, toolName));
    }

    @Override
    public Result<CLOCDefectQueryRspInfoVO> queryCodeLineByTaskIdAndLanguge(
            Long taskId,
            String toolName,
            String language) {
        return new Result<>(iclocQueryCodeLineService.generateSpecificLanguage(taskId, toolName, language));
    }

    @Override
    public Result<TaskOverviewDetailRspVO> queryTaskOverview(DeptTaskDefectReqVO reqVO, Integer pageNum,
            Integer pageSize, Sort.Direction sortType)
    {
        // TODO 临时限定接口只允许查询pcg的任务
        if (reqVO.getBgId() != 29292 && CollectionUtils.isEmpty(reqVO.getDeptIds()))
        {
            log.error("queryTaskOverview req can not query: {}", reqVO);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"bgId"}, null);
        }
        return new Result<>(apiBizService.statisticsTaskOverview(reqVO, pageNum, pageSize, sortType));
    }

    @Override
    public Result<TaskOverviewDetailRspVO> queryCustomTaskOverview(String customProjSource, Integer pageNum,
            Integer pageSize, Sort.Direction sortType)
    {
        return new Result<>(apiBizService.statCustomTaskOverview(customProjSource, pageNum, pageSize, sortType));
    }
}
