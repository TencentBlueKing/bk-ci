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

package com.tencent.devops.common.api.checkerset;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 更新规则集基础信息请求体视图
 *
 * @version V1.0
 * @date 2021/2/7
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class V3UpdateCheckerSetReqExtVO extends V3UpdateCheckerSetReqVO {

    /**
     * 规则集id
     */
    private String checkerSetId;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 来源 enum CheckerSetSource
     */
    private String checkerSetSource;

}
