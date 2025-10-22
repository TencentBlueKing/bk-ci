/*
* Tencent is pleased to support the open source community by making
* 蓝鲸智云PaaS平台社区版 (BlueKing PaaS Community Edition) available.
*
* Copyright (C) 2021 Tencent.  All rights reserved.
*
* 蓝鲸智云PaaS平台社区版 (BlueKing PaaS Community Edition) is licensed under the MIT License.
*
* License for 蓝鲸智云PaaS平台社区版 (BlueKing PaaS Community Edition):
*
* ---------------------------------------------------
* Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
* documentation files (the "Software"), to deal in the Software without restriction, including without limitation
* the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
* to permit persons to whom the Software is furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in all copies or substantial portions of
* the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
* THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
* CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
* IN THE SOFTWARE.
*/
import { defineComponent, PropType, ref } from 'vue';

import { useStore } from '@/store';
import Icon from './Icon';
import { useFolderOperation, useRepoTree, useRouteParams } from '@/hooks';
import { Loading, Tree } from 'bkui-vue';
import { Artifact, Operation } from '@/utils/vue-ts';
import { UPDATE_REPO_TREE_ITEM } from '@/store/constants';
import OperationMenu from './OperationMenu';


type NodeClickHandler = (...args: any[]) => any;

export default defineComponent({
  props: {
    tree: {
      type: Array,
      required: true,
    },
    activeNode: {
      type: String,
    },
    showOperationEntry: Boolean,
    handleNodeClick: {
      type: Function as PropType<NodeClickHandler>,
      required: true,
    },
  },
  emits: ['operate'],
  setup(props, ctx) {
    const store = useStore();
    const treeRef = ref();
    const repoParams = useRouteParams();
    const { expandTreeNode, fetchTreeChildren } = useRepoTree(repoParams.value);

    ctx.expose({
      expandTreeNode,
      setOpen: (...args: any[]) => {
        treeRef.value?.setOpen(...args);
      },
      setChecked: (...args: any[]) => {
        treeRef.value?.setChecked(...args);
      },
      setSelect: (...args: any[]) => {
        treeRef.value?.setSelect(...args);
      },
    });
    function handleOperation(e: MouseEvent, operation: Operation, repo: Artifact) {
      e.stopImmediatePropagation();

      ctx.emit('operate', operation, repo);
    }

    function handleNodeClick(...args: any[]) {
      const [repo] = args;
      props.handleNodeClick(repo.fullPath);
    }

    function toggleNodeCollapse(repo: Artifact, params: Partial<Artifact>) {
      console.log(repo);
      store.commit(UPDATE_REPO_TREE_ITEM, {
        key: repo.fullPath,
        ...params,
      });
    }
    function handleNodeCollapse(repo: any) {
      toggleNodeCollapse(repo, {
        isOpen: false,
      });
    }

    async function handleNodeExpand(repo: any) {
      if (!repo.fetched) {
        toggleNodeCollapse(repo, {
          key: repo.fullPath,
          isLoading: true,
        });
        await fetchTreeChildren(repo.fullPath);
      }
      toggleNodeCollapse(repo, {
        key: repo.fullPath,
        isLoading: false,
        isOpen: true,
        fetched: true,
      });
    }

    return () => (
      <Tree
        class="repo-tree"
        ref={treeRef}
        data={props.tree}
        label="displayName"
        lineHeight={40}
        nodeKey="fullPath"
        children="children"
        autoOpenParentNode={false}
        onNodeClick={handleNodeClick}
        onNodeExpand={handleNodeExpand}
        onNodeCollapse={handleNodeCollapse}
        selected={props.activeNode}
      >
        {{
          nodeAction: ({ isOpen, isLoading }: Artifact) => (
            <span class="repo-tree-expand-icon">
              {isLoading
                ? <Loading indicator={() => <Icon size="16" name="circle-2-1" class="spin-icon" />} />
                : <Icon name={isOpen ? 'angle-down' : 'angle-right'} />}
            </span>
          ),
          nodeType: ({ isOpen }: Artifact) => (
            <span class="repo-tree-expand-icon">
              <Icon class="repo-tree-icon" size={14} name={isOpen ? 'folder-open' : 'folder'} />
            </span>
          ),
          nodeAppend: (repo: Artifact) => {
            if (!props.showOperationEntry) return null;
            const operationList = useFolderOperation(repo, repoParams.value);
            return (
              props.activeNode === repo.fullPath && operationList.length > 0 && (
                <OperationMenu
                  class="repo-tree-item-ext"
                  operationList={operationList}
                  handleOperation={(e: MouseEvent, operation: Operation) => handleOperation(e, operation, repo)}
                >
                </OperationMenu>
              )
            );
          },
        }}
      </Tree>
    );
  },
});
