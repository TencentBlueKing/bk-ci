package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.common.CLOCTree;
import com.tencent.bk.codecc.defect.dao.mongorepository.CLOCDefectRepository;
import com.tencent.bk.codecc.defect.model.CLOCDefectEntity;
import com.tencent.bk.codecc.defect.service.TreeService;
import com.tencent.bk.codecc.defect.vo.CLOCTreeNodeVO;
import com.tencent.bk.codecc.defect.vo.TreeNodeVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants.Tool;
import com.tencent.devops.common.constant.CommonMessageCode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Slf4j
@Component("CLOCTreeBizService")
public class CLOCTreeServiceImpl implements TreeService {
    @Autowired
    private Client client;

    @Autowired
    private CLOCDefectRepository clocDefectRepository;

    @Autowired
    CLOCTree clocTree;

    /**
     * 获取CLOC告警文件树
     *
     * @param taskId 任务ID
     * @param toolNames 工具名称集合
     * */
    @Override
    public TreeNodeVO getTreeNode(Long taskId, List<String> toolNames) {
        if (taskId == null) {
            return new CLOCTreeNodeVO();
        }
        List<CLOCDefectEntity> defectEntityList = clocDefectRepository.findByTaskIdAndToolNameInAndStatusIsNot(taskId, toolNames, "DISABLED");

        if (CollectionUtils.isEmpty(defectEntityList)) {
            return new CLOCTreeNodeVO();
        }

        Result<TaskDetailVO> taskBaseResult;
        try {
            taskBaseResult = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
        } catch (Exception e) {
            log.error("get task info fail!, task id: {}", taskId);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }

        if (taskBaseResult.isNotOk() || Objects.isNull(taskBaseResult.getData())) {
            log.error("mongorepository task info fail! taskId is: {}, msg: {}",
                    taskId, taskBaseResult.getMessage());
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }

        log.info("get CLOC tree node, taskId: {}", taskId);

        TaskDetailVO taskBase = taskBaseResult.getData();
        CLOCTreeNodeVO root = clocTree.buildTree(defectEntityList, taskBase.getNameCn(),
                false, true);
        // 防止只扫到一个文件的情况下文件节点被前端刷掉不显示
        if (defectEntityList.size() == 1) {
            CLOCTreeNodeVO newRoot = new CLOCTreeNodeVO(String.format("%s%s",
                    System.currentTimeMillis(),
                    new Random().nextInt()), root.getName(), false);
            newRoot.setClocChildren(Collections.singletonList(root));
            return newRoot;
        }

        return root;
    }

    @Override
    public TreeNodeVO getTreeNode(Long taskId, Set<String> filePaths) {
        return new CLOCTreeNodeVO();
    }

    @Override
    public Boolean support(String type) {
        return false;
    }

    /**
     * 获取告警文件路径集合
     * */
    @Override
    public Set<String> getDefectPaths(Long taskId, String toolName) {
        Set<String> defectPaths = new TreeSet<>();
        List<String> toolList;
        if (Tool.SCC.name().equals(toolName)) {
            toolList = Collections.singletonList(toolName);
        } else {
            toolList = Arrays.asList(toolName, null);
        }
        List<CLOCDefectEntity> clocDefectEntityList =
                clocDefectRepository.findByTaskIdAndToolNameInAndStatusIsNot(taskId, toolList, "DISABLED");
        if (CollectionUtils.isEmpty(clocDefectEntityList)) {
            return defectPaths;
        }

        clocDefectEntityList.forEach(clocDefectEntity -> {
            if (StringUtils.isNotBlank(clocDefectEntity.getFileName())) {
                defectPaths.add(clocDefectEntity.getFileName());
            }
        });
        return defectPaths;
    }

    @Override
    public Map<String, String> getRelatePathMap(long taskId) {
        return Collections.singletonMap("", "");
    }
}
