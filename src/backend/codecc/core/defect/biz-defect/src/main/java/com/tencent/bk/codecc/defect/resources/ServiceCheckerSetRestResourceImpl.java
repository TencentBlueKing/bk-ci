package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.ServiceCheckerSetRestResource;
import com.tencent.bk.codecc.defect.service.IV3CheckerSetBizService;
import com.tencent.bk.codecc.defect.vo.CheckerSetListQueryReq;
import com.tencent.bk.codecc.task.vo.TaskBaseVO;
import com.tencent.devops.common.api.checkerset.CheckerSetRelationshipVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.pojo.Result;
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
    public Result<Boolean> batchRelateTaskAndCheckerSet(String user, String projectId, Long taskId, List<CheckerSetVO> checkerSetList, Boolean isOpenSource) {
        return new Result<>(checkerSetBizService.batchRelateTaskAndCheckerSet(projectId, taskId, checkerSetList, user, isOpenSource));
    }

    @Override
    public Result<List<CheckerSetVO>> queryCheckerSets(Set<String> checkerSetList, String projectId) {
        return new Result<>(checkerSetBizService.queryCheckerSets(checkerSetList, projectId));
    }

    @Override
    public Result<List<CheckerSetVO>> getCheckerSets(Long taskId) {
        return new Result<>(checkerSetBizService.getCheckerSetsByTaskId(taskId));
    }

    @Override
    public Result<Map<String, List<CheckerSetVO>>> getCheckerSetListByCategory(String projectId) {
        return new Result<>(checkerSetBizService.getAvailableCheckerSetsOfProject(projectId));
    }

    @Override
    public Result<Boolean> updateCheckerSetAndTaskRelation(Long taskId, Long codeLang, String user) {
        return new Result<>(checkerSetBizService.updateCheckerSetAndTaskRelation(taskId, codeLang, user));
    }

    @Override
    public Result<TaskBaseVO> getCheckerAndCheckerSetCount(Long taskId, String projectId) {
        return new Result<>(checkerSetBizService.getCheckerAndCheckerSetCount(taskId, projectId));
    }

    @Override
    public Result<Boolean> setRelationships(String checkerSetId, String user, CheckerSetRelationshipVO checkerSetRelationshipVO)
    {
        checkerSetBizService.setRelationships(checkerSetId, user, checkerSetRelationshipVO);
        return new Result<>(true);
    }

    @Override
    public Result<List<CheckerSetVO>> queryCheckerSetsForOpenScan(Set<CheckerSetVO> checkerSetList, String projectId) {
        return new Result<>(checkerSetBizService.queryCheckerSetsForOpenScan(checkerSetList, projectId));
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
}
