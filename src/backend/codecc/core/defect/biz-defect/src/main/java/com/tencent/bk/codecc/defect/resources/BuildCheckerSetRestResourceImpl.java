package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.BuildCheckerSetRestResource;
import com.tencent.bk.codecc.defect.service.ICheckerSetIntegratedBizService;
import com.tencent.bk.codecc.defect.service.IV3CheckerSetBizService;
import com.tencent.bk.codecc.defect.vo.integrated.ToolCheckerSetToStatusVo;
import com.tencent.devops.common.api.checkerset.CheckerSetRelationshipVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;

@RestResource
public class BuildCheckerSetRestResourceImpl implements BuildCheckerSetRestResource {

    @Autowired
    private IV3CheckerSetBizService checkerSetBizService;

    @Autowired
    private ICheckerSetIntegratedBizService checkerSetIntegratedBizService;

    @Override
    public Result<Boolean> setRelationships(String user, String type, String projectId, Long taskId,
                                            List<CheckerSetVO> checkerSetVOList) {
        CheckerSetRelationshipVO checkerSetRelationshipVO = new CheckerSetRelationshipVO();
        checkerSetRelationshipVO.setType(type);
        checkerSetRelationshipVO.setTaskId(taskId);
        checkerSetRelationshipVO.setProjectId(projectId);
        checkerSetVOList.forEach(checkerSet -> {
            checkerSetRelationshipVO.setVersion(checkerSet.getVersion());
            checkerSetBizService.setRelationships(checkerSet.getCheckerSetId(), user, checkerSetRelationshipVO);
        });
        return new Result<>(true);
    }

    @Override
    public Result<String> updateToolCheckerSetToStatus(String user,
                                                       String buildId,
                                                       String toolName,
                                                       ComConstants.ToolIntegratedStatus status,
                                                       ToolCheckerSetToStatusVo toolCheckerSetToStatusVo) {
        return new Result<>(checkerSetIntegratedBizService.updateToStatus(
            toolName,
            buildId,
            status,
            user,
            toolCheckerSetToStatusVo.getCheckerSetIds(),
            toolCheckerSetToStatusVo.getCheckerIds()));
    }

    @Override
    public Result<String> revertToolCheckerSetStatus(String user,
                                                     String toolName,
                                                     ComConstants.ToolIntegratedStatus status,
                                                     Set<String> checkerSetIds) {
        return new Result<>(checkerSetIntegratedBizService.revertStatus(toolName, status, user, checkerSetIds));
    }
}
