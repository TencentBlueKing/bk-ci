package com.tencent.bk.codecc.defect.common;

import com.tencent.bk.codecc.defect.model.CLOCDefectEntity;
import com.tencent.bk.codecc.defect.vo.CLOCTreeNodeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * CLOC文件树
 * @author liwei
 * */
@Slf4j
@Component
public class CLOCTree {
    private Boolean expand;
    private Boolean eliminate = true;

    /**
     * 递归添加子节点，刷新代码行数
     * @param index 文件结构层数
     * @param parent 当前生成节点的父节点
     * @param paths 当前告警文件的路径结构数组
     * @param step 当前遍历的告警文件
     * */
    private void addChild(int index, String[] paths, CLOCTreeNodeVO parent, CLOCDefectEntity step) {
        boolean isNewNode = true;
        if (parent.getClocChildren() != null) {
            for (CLOCTreeNodeVO child : parent.getClocChildren()) {
                if (child.getName().equals(paths[index])) {
                    isNewNode = false;
                    // 刷新当前路径下的代码行数
                    refreshLines(child, step);
                    addChild(++index, paths, child, step);
                    break;
                }
            }
        }

        if (isNewNode) {
            CLOCTreeNodeVO child;
            if (index < paths.length) {
                child = new CLOCTreeNodeVO(getRandom(), paths[index], expand);
                // 刷新当前路径下的代码行数
                refreshLines(child, step);
            } else {
                child = new CLOCTreeNodeVO(getRandom(), "", expand);
            }

            if (parent.getClocChildren() == null) {
                parent.setClocChildren(new LinkedList<>());
            }

            parent.getClocChildren().add(child);
            if (++index < paths.length) {
                addChild(index, paths, child, step);
            }
        }
    }

    /**
     * 生成CLOC树，默认树不展开
     * 默认消除子节点为1的父节点
     *
     * @param clocDefectEntityList 告警文件实体
     * @param eliminate 是否消除子节点为1的父节点，默认删除
     * @param expand 是否展开树，默认不展开
     * @param streamName 流名称，用户命名根结点
     * */
    public CLOCTreeNodeVO buildTree(List<CLOCDefectEntity> clocDefectEntityList,
            String streamName,
            boolean expand,
            boolean eliminate) {
        this.expand = expand;
        this.eliminate = eliminate;
        return buildTree(clocDefectEntityList, streamName);
    }

    /**
     * 生成CLOC树，默认树不展开
     * 默认消除子节点为1的父节点
     *
     * @param clocDefectEntityList 告警文件实体
     * @param streamName 流名称，用户命名根结点
     * */
    public CLOCTreeNodeVO buildTree(List<CLOCDefectEntity> clocDefectEntityList, String streamName) {
        CLOCTreeNodeVO root = new CLOCTreeNodeVO(getRandom(), streamName, expand);
        if (CollectionUtils.isEmpty(clocDefectEntityList)) {
            log.warn("build null CLOC tree, streamName: {}", streamName);
            return root;
        }

        int index = 0;
        for (CLOCDefectEntity clocDefectEntity : clocDefectEntityList) {
            String[] paths = clocDefectEntity.getFileName().split("/");

            if (paths[0].length() == 0) {
                if (paths.length <= 1) {
                    continue;
                }
                index = 1;
            }

            // 刷新当前路径下的代码行数
            refreshLines(root, clocDefectEntity);
            addChild(index, paths, root, clocDefectEntity);
        }

        for (int i = 0; i < root.getClocChildren().size(); i++) {
            CLOCTreeNodeVO treeNode = root.getClocChildren().get(i);
            if (eliminate) {
                treeNode = eliminateDepth(treeNode);
            }
            root.getClocChildren().set(i, treeNode);
        }

        // 对树节点按字母排序
        sortTree(root);
        return root;
    }

    /**
     * 生成随机树节点ID
     * */
    private String getRandom() {
        return String.format("%s%s", System.currentTimeMillis(), new Random().nextInt());
    }

    /**
     * 刷新树节点代码行数
     *
     * @param step 当前遍历的告警文件实体类
     * @param child 当前刷新节点
     * */
    private void refreshLines(CLOCTreeNodeVO child, CLOCDefectEntity step) {
        long blank = step.getBlank();
        long code = step.getCode();
        long comment = step.getComment();
        long total = blank + code + comment;
        child.addBlank(blank);
        child.addCode(code);
        child.addComment(comment);
        child.addTotal(total);
    }

    /**
     * 当eliminate为true时消除子节点数为1的父节点
     *
     * @param root 根结点
     * */
    private CLOCTreeNodeVO eliminateDepth(CLOCTreeNodeVO root) {
        while (root.getClocChildren() != null && root.getClocChildren().size() == 1) {
            root = root.getClocChildren().get(0);
        }
        return root;
    }

    /**
     * 根据文件名对同一层节点排序
     *
     * @param root 根结点
     * */
    public void sortTree(CLOCTreeNodeVO root) {
        List<CLOCTreeNodeVO> childs = root.getClocChildren();
        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(childs)) {
            for (CLOCTreeNodeVO node : childs) {
                sortTree(node);
            }
            childs.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
            childs.sort((o1, o2) -> {
                if (!org.apache.commons.collections.CollectionUtils.isEmpty(o1.getClocChildren())
                        && org.apache.commons.collections.CollectionUtils.isEmpty(o2.getClocChildren())) {
                    return -1;
                } else if (org.apache.commons.collections.CollectionUtils.isEmpty(o1.getClocChildren())
                        && !org.apache.commons.collections.CollectionUtils.isEmpty(o2.getClocChildren())) {
                    return 1;
                } else {
                    return 0;
                }
            });
        }
    }
}
