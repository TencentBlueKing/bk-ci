package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.vo.coderepository.UploadRepositoriesVO;
import com.tencent.devops.common.api.pojo.Result;

/**
 * 上报仓库信息服务
 *
 * @version V1.0
 * @date 2019/11/15
 */
public interface UploadRepositoriesService
{
    /**
     * 上报仓库信息
     *
     * @param uploadRepositoriesVO
     * @return
     */
    Result uploadRepositories(UploadRepositoriesVO uploadRepositoriesVO);
}
