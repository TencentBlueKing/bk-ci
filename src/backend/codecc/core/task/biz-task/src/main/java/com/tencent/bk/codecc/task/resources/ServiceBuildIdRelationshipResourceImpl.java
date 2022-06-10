package com.tencent.bk.codecc.task.resources;

import com.tencent.bk.codecc.task.api.ServiceBuildIdRelationshipResource;
import com.tencent.bk.codecc.task.dao.mongorepository.BuildIdRelationshipRepository;
import com.tencent.bk.codecc.task.model.BuildIdRelationshipEntity;
import com.tencent.bk.codecc.task.vo.BuildIdRelationShipVO;
import com.tencent.devops.common.api.constant.CommonMessageCode;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.web.RestResource;
import org.apache.commons.lang.StringUtils;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

@RestResource
public class ServiceBuildIdRelationshipResourceImpl implements ServiceBuildIdRelationshipResource {

    @Autowired
    private BuildIdRelationshipRepository relationshipRepository;

    @Override
    public Result<BuildIdRelationShipVO> getRelationShip(String buildId) {
        if (StringUtils.isBlank(buildId)) {
            throw new CodeCCException(CommonMessageCode.ERROR_INVALID_PARAM_, new String[]{"commitId 不能为空"});
        }

        BuildIdRelationshipEntity relationShipEntity =  relationshipRepository.findFirstByBuildId(buildId);
        if (relationShipEntity == null) {
            return new Result<>(null);
        }

        BuildIdRelationShipVO buildIdRelationShipVO = new BuildIdRelationShipVO();
        BeanUtils.copyProperties(relationShipEntity, buildIdRelationShipVO, "taskFailRecordEntity");
        if (relationShipEntity.getTaskFailRecordEntity() != null) {
            buildIdRelationShipVO.setErrMsg(relationShipEntity.getTaskFailRecordEntity().getErrMsg());
        }
        return new Result<>(buildIdRelationShipVO);
    }
}
