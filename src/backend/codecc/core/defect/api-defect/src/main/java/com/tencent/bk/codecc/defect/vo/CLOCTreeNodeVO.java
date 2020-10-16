package com.tencent.bk.codecc.defect.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("CLOC工具文件树")
public class CLOCTreeNodeVO extends TreeNodeVO {
    @ApiModelProperty("代码行数")
    private long codeLines;

    @ApiModelProperty("空白行数")
    private long blankLines;

    @ApiModelProperty("注释行数")
    private long commentLines;

    @ApiModelProperty("总行数")
    private long totalLines;

    @ApiModelProperty("树ID")
    private String treeId;

    @ApiModelProperty("树节点")
    private String name;

    @ApiModelProperty("子树集合")
    private List<CLOCTreeNodeVO> clocChildren;

    @ApiModelProperty("是否开放")
    private Boolean expanded;

    public CLOCTreeNodeVO() {
    }

    public CLOCTreeNodeVO(String treeId, String name, boolean open) {
        if (name == null) {
            this.name = "";
        } else {
            this.name = name;
        }
        this.treeId = treeId;
        this.expanded = open;
    }

    public void addBlank(long blankLines) {
        this.blankLines += blankLines;
    }

    public void addCode(long codeLines) {
        this.codeLines += codeLines;
    }

    public void addComment(long commentLines) {
        this.commentLines += commentLines;
    }

    public void addTotal(long totalLines) {
        this.totalLines += totalLines;
    }

}
