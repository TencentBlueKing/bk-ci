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

import com.tencent.bk.codecc.defect.api.BuildCheckerRestResource;
import com.tencent.bk.codecc.defect.service.CheckerImportService;
import com.tencent.bk.codecc.defect.service.ICheckerIntegratedBizService;
import com.tencent.bk.codecc.defect.vo.CheckerImportVO;
import com.tencent.devops.common.api.checkerset.CheckerPropVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

/**
 * 规则导入接口
 *
 * @version V1.0
 * @date 2020/4/10
 */
@RestResource
public class BuildCheckerRestResourceImpl implements BuildCheckerRestResource
{
    @Autowired
    private CheckerImportService checkerImportService;

    @Autowired
    private ICheckerIntegratedBizService checkerIntegratedBizService;

    @Override
    public Result<Map<String, List<CheckerPropVO>>> checkerImport(String userName,
                                                                  String projectId,
                                                                  CheckerImportVO checkerImportVO)
    {
        return new Result<>(checkerImportService.checkerImport(userName, projectId, checkerImportVO));
    }

    @Override
    public Result<List<String>> updateToolCheckerSetToStatus(String userName,
                                                             String buildId,
                                                             String toolName,
                                                             ComConstants.ToolIntegratedStatus status) {
        return new Result<>(checkerIntegratedBizService.updateToStatus(userName, buildId, toolName, status));
    }

    @Override
    public Result<String> revertToolCheckerSetStatus(String userName,
                                                     String toolName,
                                                     ComConstants.ToolIntegratedStatus status) {
        return new Result<>(checkerIntegratedBizService.revertStatus(userName, toolName, status));
    }
}
