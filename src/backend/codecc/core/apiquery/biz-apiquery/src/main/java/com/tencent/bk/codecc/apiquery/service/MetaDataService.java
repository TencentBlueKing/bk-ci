package com.tencent.bk.codecc.apiquery.service;

import com.tencent.bk.codecc.task.vo.MetadataVO;

import java.util.List;
import java.util.Map;

public interface MetaDataService
{
    /**
     * 获取代码语言源数据
     *
     * @return metaVO
     */
    List<MetadataVO> getCodeLangMetadataList();


    /**
     * 获取屏蔽用户名单
     *
     * @return list
     */
    List<String> queryExcludeUserList();


    /**
     * 获取风险系数基本数据
     *
     * @return map
     */
    public Map<String, String> getRiskFactorConfig(String toolName);

}
