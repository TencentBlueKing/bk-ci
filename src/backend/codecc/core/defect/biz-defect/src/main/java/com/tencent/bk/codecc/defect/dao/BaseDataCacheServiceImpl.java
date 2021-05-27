package com.tencent.bk.codecc.defect.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Maps;
import com.tencent.bk.codecc.task.api.ServiceBaseDataResource;
import com.tencent.devops.common.api.BaseDataVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.BaseDataCacheService;
import com.tencent.devops.common.util.JsonUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BaseDataCacheServiceImpl implements BaseDataCacheService {
    @Autowired
    private Client client;

    /**
     * 工具基础信息缓存
     */
    private final Map<String, BaseDataVO> baseDataVOMap = Maps.newConcurrentMap();

    @Override
    public List<BaseDataVO> getLanguageBaseDataFromCache(List<String> languages) {
        if (baseDataVOMap.isEmpty()) {
            initMap();
        }

        List<BaseDataVO> baseDataVOList = new ArrayList<>();
        languages.forEach(lang -> {
            baseDataVOMap.values().forEach(baseDataVO -> {
                List<String> langArray = JsonUtil.INSTANCE.to(baseDataVO.getParamExtend2(),
                        new TypeReference<List<String>>() {});
                if (langArray.contains(lang)) {
                    baseDataVOList.add(baseDataVO);
                }
            });
        });

        return baseDataVOList;
    }

    @Override
    public List<BaseDataVO> getLanguageBaseDataFromCache(Long codeLang) {
        if (baseDataVOMap.isEmpty()) {
            initMap();
        }

        return baseDataVOMap.values().stream()
                .filter(baseDataVO -> {
                    if (StringUtils.isNumeric(baseDataVO.getParamCode())) {
                        long paramCode = Long.parseLong(baseDataVO.getParamCode());
                        return (paramCode & codeLang) != 0;
                    }
                    return false;
                })
        .collect(Collectors.toList());
    }

    @Override
    public BaseDataVO getToolOrder() {
        if (baseDataVOMap.isEmpty()) {
            initMap();
        }

        return baseDataVOMap.get(ComConstants.KEY_TOOL_ORDER);
    }

    private void initMap() {
        Result<List<BaseDataVO>> result = client.get(ServiceBaseDataResource.class).findBaseData();
        if (result.isNotOk() || result.getData() == null || result.getData().isEmpty()) {
            log.error("all tool base data is null");
            throw new CodeCCException("all tool base data is null");
        }
        baseDataVOMap.clear();
        result.getData().forEach(baseDataVO -> {
            if (StringUtils.isNotBlank(baseDataVO.getLangFullKey())) {
                baseDataVOMap.put(baseDataVO.getLangFullKey(), baseDataVO);
            } else {
                baseDataVOMap.put(baseDataVO.getParamCode(), baseDataVO);
            }
        });
    }
}
