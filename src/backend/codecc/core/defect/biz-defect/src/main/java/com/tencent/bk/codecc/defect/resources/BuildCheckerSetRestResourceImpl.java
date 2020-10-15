package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.BuildCheckerSetRestResource;
import com.tencent.bk.codecc.defect.service.IV3CheckerSetBizService;
import com.tencent.devops.common.api.checkerset.CheckerSetRelationshipVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@RestResource
public class BuildCheckerSetRestResourceImpl implements BuildCheckerSetRestResource {

    @Autowired
    private IV3CheckerSetBizService checkerSetBizService;

    @Override
    public CodeCCResult<Boolean> setRelationships(String user, String type, String projectId, Long taskId,
                                                  List<CheckerSetVO> checkerSetVOList) {
        CheckerSetRelationshipVO checkerSetRelationshipVO = new CheckerSetRelationshipVO();
        checkerSetRelationshipVO.setType(type);
        checkerSetRelationshipVO.setTaskId(taskId);
        checkerSetRelationshipVO.setProjectId(projectId);
        checkerSetVOList.forEach(checkerSet -> {
            checkerSetRelationshipVO.setVersion(checkerSet.getVersion());
            checkerSetBizService.setRelationships(checkerSet.getCheckerSetId(), user, checkerSetRelationshipVO);
        });
        return new CodeCCResult<>(true);
    }
}
