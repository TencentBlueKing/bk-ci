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

package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.ServiceDefectTreeResource;
import com.tencent.bk.codecc.defect.service.IDataReportBizService;
import com.tencent.bk.codecc.defect.service.TreeService;
import com.tencent.bk.codecc.defect.vo.TreeNodeVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDataReportRspVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.util.JsonUtil;
import com.tencent.devops.common.web.RestResource;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONArray;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 告警模块树服务实现
 *
 * @version V1.0
 * @date 2019/5/20
 */
@Slf4j
@RestResource
public class ServiceDefectTreeResourceImpl implements ServiceDefectTreeResource
{
    @Autowired
    @Qualifier("CommonTreeBizService")
    private TreeService treeService;

    @Autowired
    private BizServiceFactory<IDataReportBizService> dataReportBizServiceBizServiceFactory;

    @Override
    public Result<TreeNodeVO> getTreeNode(Long taskId, List<String> toolNames)
    {
        return new Result<>(treeService.getTreeNode(taskId, toolNames));
    }

    @Override
    public Result<JSONArray> getBatchDataReports(Long taskId, Set<String> toolNames)
    {
        if(CollectionUtils.isEmpty(toolNames))
        {
            return new Result<>(null);
        }
        List<CommonDataReportRspVO> dataReportList = new ArrayList<>();
        for(String toolName : toolNames)
        {
            IDataReportBizService dataReportBizService = dataReportBizServiceBizServiceFactory.createBizService(toolName,
                    ComConstants.BusinessType.DATA_REPORT.value(), IDataReportBizService.class);
            dataReportList.add(dataReportBizService.getDataReport(taskId, toolName, 14, null, null));
        }
        JSONArray jsonArray = JSONArray.fromObject(JsonUtil.INSTANCE.toJson(dataReportList.stream().filter(Objects::nonNull).collect(Collectors.toList())));
        return new Result<>(jsonArray);
    }


}
