package com.tencent.bk.codecc.defect.resoureces;

import com.tencent.bk.codecc.defect.api.ServiceOpenSourcePkgDefectRestResource;
import com.tencent.bk.codecc.defect.service.ApiBizService;
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectReqVO;
import com.tencent.bk.codecc.defect.vo.openapi.TaskOverviewDetailRspVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.constant.CommonMessageCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ServiceOpenSourcePkgDefectRestResourceImpl implements ServiceOpenSourcePkgDefectRestResource {

    @Autowired
    private ApiBizService apiBizService;

    @Override
    public CodeCCResult<TaskOverviewDetailRspVO> queryTaskOverview(DeptTaskDefectReqVO reqVO, Integer pageNum,
                                                                   Integer pageSize, Sort.Direction sortType)
    {
        // TODO 临时限定接口只允许查询pcg的任务
        if (reqVO.getBgId() != 29292 && CollectionUtils.isEmpty(reqVO.getDeptIds()))
        {
            log.error("queryTaskOverview req can not query: {}", reqVO);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"bgId"}, null);
        }
        return new CodeCCResult<>(apiBizService.statisticsTaskOverview(reqVO, pageNum, pageSize, sortType));
    }

    @Override
    public CodeCCResult<TaskOverviewDetailRspVO> queryCustomTaskOverview(String customProjSource, Integer pageNum,
                                                                         Integer pageSize, Sort.Direction sortType)
    {
        return new CodeCCResult<>(apiBizService.statCustomTaskOverview(customProjSource, pageNum, pageSize, sortType));
    }
}
