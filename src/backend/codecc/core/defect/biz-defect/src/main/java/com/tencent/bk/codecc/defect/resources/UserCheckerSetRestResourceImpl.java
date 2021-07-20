package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.UserCheckerSetRestResource;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerSetRepository;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetEntity;
import com.tencent.bk.codecc.defect.service.IV3CheckerSetBizService;
import com.tencent.bk.codecc.defect.vo.CheckerCommonCountVO;
import com.tencent.bk.codecc.defect.vo.CheckerSetListQueryReq;
import com.tencent.bk.codecc.defect.vo.OtherCheckerSetListQueryReq;
import com.tencent.bk.codecc.defect.vo.UpdateAllCheckerReq;
import com.tencent.bk.codecc.defect.vo.enums.CheckerSetPermissionType;
import com.tencent.devops.common.api.checkerset.*;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.auth.api.external.AuthExPermissionApi;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.web.RestResource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 规则包接口实现类
 *
 * @version V1.0
 * @date 2020/1/2
 */
@Slf4j
@RestResource
public class UserCheckerSetRestResourceImpl implements UserCheckerSetRestResource
{
    @Autowired
    private IV3CheckerSetBizService checkerSetBizService;

    @Autowired
    private AuthExPermissionApi authExPermissionApi;

    @Autowired
    private CheckerSetRepository checkerSetRepository;

    @Override
    public Result<CheckerSetParamsVO> getParams(String projectId)
    {
        return new Result<>(checkerSetBizService.getParams(projectId));
    }

    @Override
    public Result<Boolean> createCheckerSet(String user, String projectId, CreateCheckerSetReqVO createCheckerSetReqVO)
    {
        checkerSetBizService.createCheckerSet(user, projectId, createCheckerSetReqVO);
        return new Result<>(true);
    }

    @Override
    public Result<Boolean> updateCheckersOfSet(String checkerSetId, String projectId, String user, UpdateCheckersOfSetReqVO updateCheckersOfSetReq)
    {
        List<CheckerSetEntity> checkerSetEntities = checkerSetRepository.findByCheckerSetId(checkerSetId);
        if (CollectionUtils.isNotEmpty(checkerSetEntities))
        {
            if (!checkerSetEntities.get(0).getProjectId().equals(projectId))
            {
                String errMsg = "只可以更新本项目内的规则集！";
                log.error(errMsg);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
            }
        }
        checkerSetBizService.updateCheckersOfSet(checkerSetId, user, updateCheckersOfSetReq.getCheckerProps(), null);
        return new Result<>(true);
    }

    @Override
    public Result<Boolean> updateCheckersOfSetForAll(String user, UpdateAllCheckerReq updateAllCheckerReq){
        return new Result<>(checkerSetBizService.updateCheckersOfSetForAll(user, updateAllCheckerReq));
    }

    @Override
    public Result<List<CheckerSetVO>> getCheckerSets(CheckerSetListQueryReq queryCheckerSetReq)
    {
        if (queryCheckerSetReq.getTaskId() != null)
        {
            return new Result<>(checkerSetBizService.getCheckerSetsOfTask(queryCheckerSetReq));
        }
        else
        {
            return new Result<>(checkerSetBizService.getCheckerSetsOfProject(queryCheckerSetReq));
        }
    }

    @Override
    public Result<List<CheckerSetVO>> getTaskCheckerSets(String projectId, long taskId, String toolName, String dimension) {
        return new Result<>(checkerSetBizService.getTaskCheckerSets(projectId,
                taskId,
                toolName,
                dimension,
                false));
    }

    @Override
    public Result<Page<CheckerSetVO>> getCheckerSetsPageable(CheckerSetListQueryReq queryCheckerSetReq) {
        if (queryCheckerSetReq.getTaskId() != null) {
            return new Result<>(checkerSetBizService.getCheckerSetsOfTaskPage(queryCheckerSetReq));
        } else {
            return new Result<>(checkerSetBizService.getCheckerSetsOfProjectPage(queryCheckerSetReq));
        }
    }

    @Override
    public Result<Page<CheckerSetVO>> getOtherCheckerSets(String projectId, OtherCheckerSetListQueryReq queryCheckerSetReq)
    {
        return new Result<>(checkerSetBizService.getOtherCheckerSets(projectId, queryCheckerSetReq));
    }

    @Override
    public Result<List<CheckerCommonCountVO>> queryCheckerSetCountList(CheckerSetListQueryReq checkerSetListQueryReq)
    {
        return new Result<>(checkerSetBizService.queryCheckerSetCountList(checkerSetListQueryReq));
    }

    @Override
    public Result<CheckerSetVO> getCheckerSetDetail(String checkerSetId, Integer version)
    {
        return new Result<>(checkerSetBizService.getCheckerSetDetail(checkerSetId, version));
    }

    @Override
    public Result<Boolean> updateCheckerSetBaseInfo(String checkerSetId, String projectId, V3UpdateCheckerSetReqVO updateCheckerSetReq)
    {
        checkerSetBizService.updateCheckerSetBaseInfo(checkerSetId, projectId, updateCheckerSetReq);
        return new Result<>(true);
    }

    @Override
    public Result<Boolean> setRelationships(String checkerSetId, String user, CheckerSetRelationshipVO checkerSetRelationshipVO)
    {
        checkerSetBizService.setRelationships(checkerSetId, user, checkerSetRelationshipVO);
        return new Result<>(true);
    }

    @Override
    public Result<Boolean> setRelationshipsOnce(String user, String projectId, long taskId, String toolName) {
        return new Result<>(checkerSetBizService.setRelationshipsOnce(user, projectId, taskId, toolName));
    }

    @Override
    public Result<Boolean> management(String user, String checkerSetId, CheckerSetManagementReqVO checkerSetManagementReqVO)
    {
        checkerSetBizService.management(user, checkerSetId, checkerSetManagementReqVO);
        return new Result<>(true);
    }

    @Override
    public Result<Map<String, List<CheckerSetVO>>> getCheckerSetListByCategory(String projectId)
    {
        return new Result<>(checkerSetBizService.getAvailableCheckerSetsOfProject(projectId));
    }

    @Override
    public Result<List<CheckerSetPermissionType>> getUserManagementPermission(AuthManagementPermissionReqVO authManagementPermissionReqVO) {
        try {
            List<CheckerSetPermissionType> checkerSetPermissionTypes = new ArrayList<>();
            if (authManagementPermissionReqVO.getCheckerSetId() != null) {
                List<CheckerSetEntity> checkerSetEntities = checkerSetRepository.findByCheckerSetId(authManagementPermissionReqVO.getCheckerSetId());
                if (CollectionUtils.isNotEmpty(checkerSetEntities)) {
                    if (checkerSetEntities.get(0).getCreator().equals(authManagementPermissionReqVO.getUser())) {
                        checkerSetPermissionTypes.add(CheckerSetPermissionType.CREATOR);
                    }
                }
            }
            if (authExPermissionApi.authProjectManager(authManagementPermissionReqVO.getProjectId(), authManagementPermissionReqVO.getUser())) {
                checkerSetPermissionTypes.add(CheckerSetPermissionType.MANAGER);
            }
            return new Result<>(checkerSetPermissionTypes);
        } catch (Exception e) {
            log.info("get checker set auth fail! ");
            return new Result<>(new ArrayList<>());
        }
    }
}
