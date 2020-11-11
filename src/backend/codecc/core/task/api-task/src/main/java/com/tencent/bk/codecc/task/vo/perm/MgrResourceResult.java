package com.tencent.bk.codecc.task.vo.perm;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("")
public class MgrResourceResult {
    private List<TaskPolicyVO> policy;
    private List<TaskRoleVO> role;
}
