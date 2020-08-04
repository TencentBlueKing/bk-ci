/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.defect.common;

import com.tencent.bk.codecc.defect.vo.TreeNodeVO;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;


/**
 * @date 2016/9/20
 */
@Component
public class Tree
{
    private Boolean expand;
    private Boolean eliminate = true;

    private void addChild(int index, String[] path, TreeNodeVO parent)
    {
        boolean isNewNode = true;
        if (parent.getChildren() != null)
        {
            for (TreeNodeVO child : parent.getChildren())
            {
                if (child.getName().equals(path[index]))
                {
                    isNewNode = false;
                    addChild(++index, path, child);
                    break;
                }
            }
        }
        if (isNewNode)
        {
            TreeNodeVO child;
            // 避免ArrayIndexOutOfBoundsException
            if (path.length > index) {
                child = new TreeNodeVO(getRandom(), path[index], expand);
            } else {
                child = new TreeNodeVO(getRandom(), "", expand);
            }
            if (parent.getChildren() == null)
            {
                parent.setChildren(new ArrayList<>());
            }

            parent.getChildren().add(child);
            if (++index < path.length)
            {
                addChild(index, path, child);
            }
        }
    }

    public TreeNodeVO buildTree(Set<String> paths, String streamName)
    {
        TreeNodeVO root = new TreeNodeVO(getRandom(), streamName, expand);
        if (paths == null || paths.size() == 0)
        {
            return root;
        }

        int index = 0;
        for (String path : paths)
        {
            String[] dirs = path.split("/");
            if (dirs[0].length() == 0)
            {
                index = 1;
            }

            addChild(index, dirs, root);
        }

        for (int i = 0; i < root.getChildren().size(); i++)
        {
            TreeNodeVO treeNode = root.getChildren().get(i);
            if ("usr".equals(treeNode.getName()) || treeNode.getName().contains("Program Files"))
            {
                continue;
            }
            if (eliminate)
            {
                // 把单节点的树去掉
                treeNode = eliminateDepth(treeNode);
            }
            root.getChildren().set(i, treeNode);
        }

        // 对树节点按字母排序
        sortTree(root);
        return root;
    }

    public TreeNodeVO buildTree(Set<String> paths, String streamName, boolean expand, boolean eliminate)
    {
        this.expand = expand;
        this.eliminate = eliminate;
        return buildTree(paths, streamName);
    }

    private TreeNodeVO eliminateDepth(TreeNodeVO root)
    {
        while (root.getChildren() != null && root.getChildren().size() == 1)
        {
            root = root.getChildren().get(0);
        }
        return root;
    }

    public void sortTree(TreeNodeVO root)
    {
        List<TreeNodeVO> childs = root.getChildren();
        if (CollectionUtils.isNotEmpty(childs))
        {
            for (TreeNodeVO node : childs)
            {
                sortTree(node);
            }
            childs.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
        }
    }

    private String getRandom(){
        return String.format("%s%s", System.currentTimeMillis(), new Random().nextInt());
    }



}
