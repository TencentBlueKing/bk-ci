package com.tencent.devops.common.service;

import com.tencent.devops.common.api.BaseDataVO;
import java.util.List;

public interface BaseDataCacheService {
    List<BaseDataVO> getLanguageBaseDataFromCache(List<String> languages);

    List<BaseDataVO> getLanguageBaseDataFromCache(Long codeLang);

    BaseDataVO getToolOrder();
}
