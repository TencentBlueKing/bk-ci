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

package com.tencent.bk.codecc.apiquery.resources;

import com.tencent.bk.codecc.apiquery.api.OpTaskRestResource;
import com.tencent.bk.codecc.apiquery.service.TaskService;
import com.tencent.bk.codecc.apiquery.service.ToolService;
import com.tencent.bk.codecc.apiquery.vo.DeptInfoVO;
import com.tencent.bk.codecc.apiquery.vo.TaskInfoExtVO;
import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO;
import com.tencent.bk.codecc.apiquery.vo.ToolConfigPlatformVO;
import com.tencent.bk.codecc.apiquery.vo.op.ActiveTaskStatisticsVO;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * op任务接口实现
 *
 * @version V1.0
 * @date 2020/4/24
 */
@RestResource
public class OpTaskRestResourceImpl implements OpTaskRestResource {

    @Autowired
    private ToolService toolService;

    @Autowired
    private TaskService taskService;


    @Override
    public CodeCCResult<Page<ToolConfigPlatformVO>> getPlatformInfo(Long taskId, String toolName, String platformIp,
                                                                    Integer pageNum, Integer pageSize, String sortType) {
        return new CodeCCResult<>(toolService.getPlatformInfoList(taskId, toolName, platformIp, pageNum, pageSize, sortType));
    }

    @Override
    public CodeCCResult<ToolConfigPlatformVO> getPlatformDetail(Long taskId, String toolName) {
        return new CodeCCResult<>(toolService.getTaskPlatformDetail(taskId, toolName));
    }

    @Override
    public CodeCCResult<Page<TaskInfoExtVO>> getOverAllTaskList(TaskToolInfoReqVO reqVO, Integer pageNum, Integer pageSize,
                                                                String sortField, String sortType) {
        return new CodeCCResult<>(taskService.getOverAllTaskList(reqVO, pageNum, pageSize, sortField, sortType));
    }

    @Override
    public CodeCCResult<List<DeptInfoVO>> getDeptList(String parentId) {
        return new CodeCCResult<>(taskService.getChildDeptList(parentId));
    }

    @Override
    public CodeCCResult<Page<ActiveTaskStatisticsVO>> queryActiveTaskListByLog(String userName,
                                                                               TaskToolInfoReqVO taskToolInfoReqVO, Integer pageNum, Integer pageSize, String sortField, String sortType) {
        return new CodeCCResult<>(taskService.getActiveTaskList(taskToolInfoReqVO, pageNum, pageSize, sortField, sortType));
    }


}
