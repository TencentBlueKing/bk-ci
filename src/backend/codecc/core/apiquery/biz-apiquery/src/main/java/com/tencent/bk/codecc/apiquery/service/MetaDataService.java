package com.tencent.bk.codecc.apiquery.service;

import com.tencent.bk.codecc.task.vo.MetadataVO;

import java.util.List;

public interface MetaDataService
{
    /**
     * 获取代码语言源数据
     *
     * @return metaVO
     */
    List<MetadataVO> getCodeLangMetadataList();

}
