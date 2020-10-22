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
 
package com.tencent.bk.codecc.klocwork.resources;

import com.tencent.bk.codecc.defect.vo.DefectDetailVO;
import com.tencent.bk.codecc.klocwork.api.ServiceKwDefectRestResource;
import com.tencent.bk.codecc.klocwork.service.KwDefectService;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.web.RestResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * klocwork项目配置接口实现
 * 
 * @date 2019/11/18
 * @version V1.0
 */
@RestResource
@Slf4j
public class ServiceKwDefectRestResourceImpl implements ServiceKwDefectRestResource
{
    @Autowired
    private KwDefectService kwDefectService;

    @Override
    public CodeCCResult<DefectDetailVO> getFilesContent(DefectDetailVO defectDetailVO)
    {
        return new CodeCCResult<>(kwDefectService.getFilesContent(defectDetailVO));
    }
}
