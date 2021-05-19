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

package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.model.CLOCDefectEntity;
import com.tencent.bk.codecc.defect.vo.UploadCLOCStatisticVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.constant.CommonMessageCode;

import java.util.List;
import java.util.Map;

/**
 * cloc工具上传统计数据接口
 *
 * @version V1.0
 * @date 2019/10/7
 */
@Deprecated
public interface CLOCUploadStatisticService
{
    Result uploadStatistic(UploadCLOCStatisticVO uploadCLOCStatisticVO);

    Result<CommonMessageCode> uploadNewStatistic(UploadCLOCStatisticVO uploadCLOCStatisticVO,
            Map<String, List<CLOCDefectEntity>> clocLanguageMap,
            String buildId,
            String streamName);
}
