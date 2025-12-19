<template>
    <bk-dialog
        class="organization"
        v-model="showDialog"
        :width="width"
        :padding="0"
        :close-icon="organizationConf.closeIcon"
        :quick-close="organizationConf.quickClose"
        :loading="isLoading"
        :auto-close="false"
        @confirm="toConfirmLogo"
        @cancel="handleCancel"
    >
        <main class="organization-select-content">
            <div class="organization-content">
                <div class="organization-card organization-tree">
                    <div class="info-header">{{ $t('store.添加可见对象') }}</div>
                    <bk-input
                        v-model="searchValue"
                        :clearable="true"
                        :right-icon="'bk-icon icon-search'"
                        class="search"
                        @right-icon-click="handlerSearchClick"
                        @enter="handlerSearchClick"
                        @change="handlerSearchClick"
                    ></bk-input>
                    <div class="tree-content">
                        <bk-big-tree
                            show-checkbox
                            :expand-on-click="false"
                            ref="organizationTree"
                            :data="treeList"
                            :disable-strictly="false"
                            :check-strictly="false"
                            :lazy-method="loadNodes"
                            :default-expanded-nodes="defaultExpandedNodes"
                            @check-change="handleChange"
                        >
                            <div
                                slot-scope="{ node, data }"
                                class="tree-item"
                            >
                                <span>{{ data.name }}</span>
                                <span
                                    v-if="node.checked && selectIds.includes(data.id || '0') && selectData.length"
                                    class="added"
                                >{{
                                    $t('store.已添加')
                                }}</span>
                            </div>
                        </bk-big-tree>
                    </div>
                </div>
                <div class="organization-card organization-selected">
                    <div class="info-header preview">{{ $t('store.结果预览') }}</div>
                    <div
                        class="preview-total"
                        v-if="selectedList.length"
                    >
                        <i18n
                            tag="span"
                            path="store.将添加X个可见范围"
                        >
                            <span class="text-blue">{{ selectedList.length }}</span>
                        </i18n>
                        <span
                            class="text-blue ml10"
                            @click="handleDeleteAll"
                        >{{ $t('store.清空') }}</span>
                    </div>
                    <div class="selected-content">
                        <div
                            class="selected-item"
                            v-for="(row, index) in selectedList"
                            :key="index"
                        >
                            {{ row.displayName }}
                            <img
                                :src="closeSvg"
                                @click="handleDelete(row)"
                            />
                        </div>
                    </div>
                </div>
            </div>
        </main>
    </bk-dialog>
</template>

<script>
    import closeSvg from '@/images/close-small.svg'
    export default {
        props: {
            selectData: {
                type: Array,
                default: () => []
            },
            showDialog: Boolean,
            isLoading: Boolean,
        },
        data () {
            return {
                width: 760,
                treeList: [{ id: 0, name: this.$t('store.腾讯公司') }],
                selectedList: [],
                organizationConf: {
                    hasHeader: false,
                    hasFooter: false,
                    closeIcon: false,
                    quickClose: false,
                },
                searchValue: '',
                closeSvg,
                defaultExpandedNodes: ['0'],
                timerIds: [],
                currentSelectNode: []
            }
        },
        computed: {
            selectIds () {
                return this.currentSelectNode.map((item) => String(item.deptId))
            },
        },
        watch: {
            showDialog (val) {
                if (val) {
                    this.currentSelectNode = this.selectData?.map((i) => ({
                        ...i,
                        id: i.deptId,
                        displayName: i.deptName,
                    }))

                    this.selectedList = [...this.currentSelectNode]

                    this.selectIds.includes('0') && this.$refs.organizationTree?.setChecked(0)
                    this.clearChecked(this.selectIds, true)
                }
                if (!val) {
                    this.selectedList = []
                    this.treeList = [{ id: 0, name: this.$t('store.腾讯公司') }]
                    this.clearTimers()
                }
            },
        },

        beforeDestroy () {
            this.clearTimers()
        },

        methods: {
            clearTimers () {
                this.timerIds.forEach(timerId => {
                    clearTimeout(timerId)
                })
                this.timerIds = []
            },
        
            clearChecked (ids, status) {
              this.$refs.organizationTree?.setChecked(ids, { checked: status })
              this.$refs.organizationTree?.setDisabled(ids, { disabled: status })
            },
            handleChange (ids) {
                if (this.currentSelectNode.length) {
                    this.selectedList = [...this.currentSelectNode]
                } else {
                    this.selectedList = []
                }
                ids.forEach((id) => {
                    const node = this.$refs.organizationTree.getNodeById(id)
                    node.data.displayName = node.name
                    let parentNode = node.parent
                    while (parentNode) {
                        node.data.displayName = `${parentNode.name}/${node.data.displayName}`
                        parentNode = parentNode.parent
                    }
                    const nodeId = String(node.id)
                    if (!this.selectIds.includes(nodeId) && !this.selectedList.some(i => i.id === nodeId)) {
                        this.selectedList.push(node.data)
                    }
                })
            },

            handlerSearchClick () {
                this.$refs.organizationTree.filter(this.searchValue)
            },

            handleDelete (row) {
                this.selectedList = this.selectedList.filter((item) => item.id !== row.id)
                this.currentSelectNode = this.currentSelectNode.filter((item) => item.id !== row.id)
                this.clearChecked(row.id, false)
            },

            handleDeleteAll () {
                const clearIds = this.selectedList.map(i => i.id)
                this.clearChecked(clearIds, false)
                this.$refs.organizationTree.removeChecked()
                this.selectedList = []
                this.currentSelectNode = []
            },

            async loadNodes (node) {
                let curType = ''
                switch (node.level) {
                    case 0:
                        curType = 'bg'
                        break
                    case 1:
                        curType = 'dept'
                        break
                    default:
                        curType = 'center'
                        break
                }
                try {
                    const res = await this.$store.dispatch('store/requestOrganizations', {
                        type: curType,
                        id: node.id,
                    })
                    const data = []
                    const leaf = []
                    res.forEach((x) => {
                        x.type = curType
                        if (this.selectIds.includes(x.id)) {
                            const timerId = setTimeout(() => {
                                this.clearChecked(x.id, true)
                            }, 100)
                            this.timerIds.push(timerId)
                        }
                        data.push(x)
                        if (node.level === 2) leaf.push(x.id)
                    })
                    return { data, leaf }
                } catch (err) {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }
            },
            toConfirmLogo () {
                if (this.isLoading) return
                if (!this.selectedList.length) {
                    this.$bkMessage({
                        message: this.$t('store.请选择部门'),
                        theme: 'error',
                    })
                } else {
                    const deptInfos = []
                    this.selectedList.forEach((item) => {
                        deptInfos.push({
                            deptId: item.id,
                            deptName: item.displayName,
                        })
                    })

                    const params = {
                        deptInfos,
                    }

                    this.$emit('saveHandle', params)
                }
            },
            handleCancel () {
                this.selectedList = []
                this.currentSelectNode = []
                this.$emit('cancelHandle')
            }
        },
    }
</script>

<style lang="scss">
@import '../assets/scss/conf';
.organization {
  ::v-deep .bk-dialog-body {
    padding: 0;
  }
  .organization-content {
    display: flex;
    justify-content: space-between;
  }
  .organization-card {
    height: 440px;
    .info-header {
      padding: 20px 20px;
      color: #313238;
      font-size: 16px;
    }
    .search {
      width: 379px;
      margin: 0 20px 10px 20px;
    }
    .tree-content,
    .selected-content {
      height: 100%;
      padding: 0 20px;
      overflow: auto;
    }
    .tree-content {
      height: calc(100% - 104px);
      width: 425px;
      .tree-item {
        display: flex;
        justify-content: space-between;
        .added {
          padding-right: 10px;
          color: #d4d5d8;
        }
      }
    }
    .selected-content {
      width: 335px;
      height: calc(100% - 93px);
    }
    .selected-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 5px 10px;
      background-color: #fff;
      border-bottom: 1px solid #e3e4ea;
      img {
        width: 14px;
        height: 14px;
      }
    }
    .tree-drag-node {
      white-space: nowrap;
    }
    .preview-total {
      padding-left: 20px;
      margin-bottom: 10px;
    }
  }
  .organization-tree {
    flex: 1;
  }
  .organization-selected {
    width: 335px;
    background-color: #f5f7fa;
    border-left: 1px solid #e1e3e9;
  }
  .text-blue {
    color: #4289ff;
    cursor: pointer;
  }
  .bk-dialog-wrapper .bk-dialog-body {
    padding: 0;
    height: 440px;
  }
  .bk-dialog-wrapper .bk-dialog-tool {
    min-height: auto;
  }
  .bk-dialog-content {
    height: 446px;
  }
}
</style>
