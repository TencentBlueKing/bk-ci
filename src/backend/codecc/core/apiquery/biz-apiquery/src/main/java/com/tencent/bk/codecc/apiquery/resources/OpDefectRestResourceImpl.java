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

import com.tencent.bk.codecc.apiquery.api.OpDefectRestResource;
import com.tencent.bk.codecc.apiquery.service.IDefectQueryWarningService;
import com.tencent.bk.codecc.apiquery.task.dao.ToolMetaCacheServiceImpl;
import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO;
import com.tencent.bk.codecc.apiquery.vo.op.TaskDefectVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.auth.api.external.AuthExPermissionApi;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.utils.SpringContextUtil;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * op告警查询接口实现
 *
 * @version V3.0
 * @date 2020/9/2
 */

@RestResource
public class OpDefectRestResourceImpl implements OpDefectRestResource {

    @Autowired
    private AuthExPermissionApi authExPermissionApi;

    @Autowired
    private ToolMetaCacheServiceImpl toolMetaCache;





    @Override
    public CodeCCResult<Page<TaskDefectVO>> queryDeptTaskDefect(String userName, TaskToolInfoReqVO reqVO, Integer pageNum,
                                                                Integer pageSize, String sortField, String sortType) {

        // 判断是否为管理员
        if (!authExPermissionApi.isAdminMember(userName)) {
            throw new CodeCCException(CommonMessageCode.IS_NOT_ADMIN_MEMBER, new String[]{userName});
        }

        // 组装工具类型开头的处理类
        String toolPattern = toolMetaCache.getToolPattern(reqVO.getToolName());
        String suffix = ComConstants.BusinessType.QUERY_WARNING.value() + ComConstants.BIZ_SERVICE_POSTFIX;
        String processorBeanName = String.format("%s%s", toolPattern, suffix);

        IDefectQueryWarningService<?, ?> bizService;
        try {
            bizService = SpringContextUtil.Companion.getBean(IDefectQueryWarningService.class, processorBeanName);
        } catch (Exception e) {
            processorBeanName = String.format("%s%s", ComConstants.COMMON_BIZ_SERVICE_PREFIX, suffix);
            bizService = SpringContextUtil.Companion.getBean(IDefectQueryWarningService.class, processorBeanName);
        }

        return new CodeCCResult<>(bizService.queryDeptTaskDefect(reqVO, pageNum, pageSize, sortField, sortType));
    }


}
