package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.ServiceCheckerSetRestResource;
import com.tencent.bk.codecc.defect.service.IV3CheckerSetBizService;
import com.tencent.bk.codecc.task.vo.TaskBaseVO;
import com.tencent.devops.common.api.checkerset.CheckerSetRelationshipVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 规则包接口实现类
 *
 * @version V1.0
 * @date 2020/1/2
 */
@RestResource
public class ServiceCheckerSetRestResourceImpl implements ServiceCheckerSetRestResource {
    @Autowired
    private IV3CheckerSetBizService checkerSetBizService;

    @Override
    public CodeCCResult<Boolean> batchRelateTaskAndCheckerSet(String user, String projectId, Long taskId, List<CheckerSetVO> checkerSetList, Boolean isOpenSource) {
        return new CodeCCResult<>(checkerSetBizService.batchRelateTaskAndCheckerSet(projectId, taskId, checkerSetList, user, isOpenSource));
    }

    @Override
    public CodeCCResult<List<CheckerSetVO>> queryCheckerSets(Set<String> checkerSetList, String projectId) {
        return new CodeCCResult<>(checkerSetBizService.queryCheckerSets(checkerSetList, projectId));
    }

    @Override
    public CodeCCResult<List<CheckerSetVO>> getCheckerSets(Long taskId) {
        return new CodeCCResult<>(checkerSetBizService.getCheckerSetsByTaskId(taskId));
    }

    @Override
    public CodeCCResult<Map<String, List<CheckerSetVO>>> getCheckerSetListByCategory(String projectId) {
        return new CodeCCResult<>(checkerSetBizService.getAvailableCheckerSetsOfProject(projectId));
    }

    @Override
    public CodeCCResult<Boolean> updateCheckerSetAndTaskRelation(Long taskId, Long codeLang, String user) {
        return new CodeCCResult<>(checkerSetBizService.updateCheckerSetAndTaskRelation(taskId, codeLang, user));
    }

    @Override
    public CodeCCResult<TaskBaseVO> getCheckerAndCheckerSetCount(Long taskId, String projectId) {
        return new CodeCCResult<>(checkerSetBizService.getCheckerAndCheckerSetCount(taskId, projectId));
    }

    @Override
    public CodeCCResult<Boolean> setRelationships(String checkerSetId, String user, CheckerSetRelationshipVO checkerSetRelationshipVO)
    {
        checkerSetBizService.setRelationships(checkerSetId, user, checkerSetRelationshipVO);
        return new CodeCCResult<>(true);
    }

    @Override
    public CodeCCResult<List<CheckerSetVO>> queryCheckerSetsForOpenScan(Set<CheckerSetVO> checkerSetList, String projectId) {
        return new CodeCCResult<>(checkerSetBizService.queryCheckerSetsForOpenScan(checkerSetList, projectId));
    }
}
