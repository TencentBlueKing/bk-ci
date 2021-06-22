package com.tencent.bk.codecc.task.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Data;

@Data
@ApiModel("规则参数操作选项")
public class CreateTaskConfigVO {

    @ApiModelProperty("语言集合")
    List<String> langs;
}
