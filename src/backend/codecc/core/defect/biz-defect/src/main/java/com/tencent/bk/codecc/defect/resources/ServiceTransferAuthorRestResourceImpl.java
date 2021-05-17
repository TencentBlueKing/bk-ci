package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.ServiceTransferAuthorRestResource;
import com.tencent.bk.codecc.defect.service.IQueryTransferAuthorBizService;
import com.tencent.bk.codecc.defect.vo.common.AuthorTransferVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.web.RestResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 工具构建信息实现类
 *
 * @version V1.0
 * @date 2019/11/17
 */
@Slf4j
@RestResource
public class ServiceTransferAuthorRestResourceImpl implements ServiceTransferAuthorRestResource
{
    @Autowired
    private IQueryTransferAuthorBizService queryTransferAuthorBizService;

    @Override
    public Result<AuthorTransferVO> getAuthorTransfer(long taskId)
    {
        return new Result<>(queryTransferAuthorBizService.getAuthorTransfer(taskId));
    }
}
