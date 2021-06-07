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

package com.tencent.bk.codecc.apiquery.service.impl;

import com.tencent.bk.codecc.apiquery.service.MetaDataService;
import com.tencent.bk.codecc.apiquery.task.dao.BaseDataDao;
import com.tencent.bk.codecc.apiquery.task.model.BaseDataModel;
import com.tencent.bk.codecc.task.api.ServiceBaseDataResource;
import com.tencent.bk.codecc.task.api.UserMetaRestResource;
import com.tencent.bk.codecc.task.vo.MetadataVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工具源数据接口实现
 *
 * @version V4.0
 * @date 2020/6/15
 */
@Slf4j
@Service
public class MetaDataServiceImpl implements MetaDataService {
    @Autowired
    private Client client;

    @Autowired
    private BaseDataDao baseDataDao;


    @Override
    public List<MetadataVO> getCodeLangMetadataList() {
        Result<Map<String, List<MetadataVO>>> metaDataResult =
                client.get(UserMetaRestResource.class).metadatas(ComConstants.KEY_CODE_LANG);
        if (metaDataResult.isNotOk() || metaDataResult.getData() == null) {
            log.error("meta data result is empty! meta data type {}", ComConstants.KEY_CODE_LANG);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        return metaDataResult.getData().get(ComConstants.KEY_CODE_LANG);
    }


    /**
     * 获取屏蔽用户名单
     *
     * @return list
     */
    @Override
    public List<String> queryExcludeUserList() {
        Result<List<String>> metaDataResult =
                client.get(ServiceBaseDataResource.class).queryExcludeUserMember();
        if (metaDataResult.isNotOk() || metaDataResult.getData() == null) {
            log.error("meta data result is empty! meta data type {}", ComConstants.KEY_EXCLUDE_USER_LIST);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL, new String[]{"queryExcludeUserList"});
        }
       return metaDataResult.getData();
    }


    /**
     * 获取风险系数基本数据
     *
     * @return map
     */
    public Map<String, String> getRiskFactorConfig(String toolName) {
        // 获取风险系数值
        List<BaseDataModel> baseDataModelList =
                baseDataDao.findAllByParamTypeAndParamCode(ComConstants.PREFIX_RISK_FACTOR_CONFIG, toolName);

        return baseDataModelList.stream()
                .collect(Collectors.toMap(BaseDataModel::getParamName, BaseDataModel::getParamValue, (k, v) -> v));
    }


}
