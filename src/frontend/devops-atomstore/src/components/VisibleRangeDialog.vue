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
        @confirm="handleConfirm"
        @cancel="handleCancel"
    >
        <main class="organization-select-content">
            <div class="organization-content">
                <div class="organization-card organization-tree">
                    <div class="info-header">{{ routeType !== 'devx' ? $t('store.添加可见对象') : $t('store.添加范围') }}</div>

                    <div
                        v-if="routeType === 'devx'"
                        class="custom-tabs"
                    >
                        <div
                            v-for="panel in panels"
                            :key="panel.name"
                            :class="['tab-item', { 'active': activePanel === panel.name }]"
                            @click="activePanel = panel.name"
                        >
                            {{ panel.label }}
                        </div>
                    </div>
                    <bk-input
                        v-model="searchValue"
                        :clearable="true"
                        :right-icon="'bk-icon icon-search'"
                        class="search"
                        @right-icon-click="handlerSearchClick"
                        @enter="handlerSearchClick"
                        @change="handlerSearchClick"
                    ></bk-input>
                    <div
                        v-show="routeType !== 'devx' || (routeType === 'devx' && activePanel === 'dept')"
                        class="tree-content"
                        :style="{ height: routeType === 'devx' ? 'calc(100% - 142px)' : 'calc(100% - 104px)' }"
                    >
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
                                    v-if="node.checked && selectIds.includes(data.id || '0') && selectData?.deptInfos?.length"
                                    class="added"
                                >{{
                                    $t('store.已添加')
                                }}</span>
                            </div>
                        </bk-big-tree>
                    </div>
                    <div
                        v-show="routeType === 'devx' && activePanel === 'project'"
                        class="tree-content project-list"
                        v-bkloading="{ isLoading: isLoadingProject }"
                    >
                        <div
                            v-for="project in filteredProjectList"
                            :key="project.projectCode"
                            class="project-item"
                        >
                            <bk-checkbox
                                :value="isProjectSelected(project)"
                                :disabled="selectIds.includes(project.projectCode) && selectData?.projectInfos?.length"
                                @change="handleProjectSelect(project, $event)"
                            >
                                {{ project.projectName }}
                                <span class="project-code">{{ project.projectCode }}</span>
                            </bk-checkbox>
                        </div>
                        <div
                            v-if="!isLoadingProject && !filteredProjectList.length"
                            class="empty-tip"
                        >
                            {{ $t('store.暂无数据') }}
                        </div>
                    </div>
                </div>
                <div class="organization-card organization-selected">
                    <div class="info-header preview">{{ $t('store.结果预览') }}</div>
                    <!-- 云研发模式：分组展示 -->
                    <div
                        v-if="routeType === 'devx'"
                        class="grouped"
                    >
                        <bk-collapse v-model="activeCollapseNames">
                            <!-- 组织架构组 -->
                            <bk-collapse-item
                                v-if="deptSelectedList.length"
                                hide-arrow
                                name="dept"
                            >
                                <div class="item-header">
                                    <p class="group-header">
                                        <bk-icon
                                            :type="activeCollapseNames.includes('dept') ? 'angle-down' : 'angle-right'"
                                            style="font-size: 21px;"
                                        />
                                        <span>【{{ $t('store.组织') }}】</span>
                                        <i18n
                                            tag="span"
                                            path="store.共X个"
                                        >
                                            <span class="text-blue">{{ deptSelectedList.length }}</span>
                                        </i18n>
                                    </p>
                                    <img
                                        :src="besom"
                                        class="group-delete"
                                        @click.stop="handleDeleteGroup('dept')"
                                    />
                                </div>
                                <div
                                    slot="content"
                                    class="selected-content"
                                >
                                    <div
                                        v-for="(row, index) in deptSelectedList"
                                        :key="'dept-' + index"
                                        class="selected-item"
                                    >
                                        <div>
                                            <p class="item-name">{{ row.displayName.split('/').pop() }}</p>
                                            <p class="item-sub">{{ row.displayName.split('/').slice(0, -1).join('/') }}</p>
                                        </div>
                                        <img
                                            :src="closeSvg"
                                            @click="handleDelete(row)"
                                        />
                                    </div>
                                </div>
                            </bk-collapse-item>

                            <!-- 项目组 -->
                            <bk-collapse-item
                                v-if="projectSelectedList.length"
                                hide-arrow
                                name="project"
                            >
                                <div class="item-header">
                                    <p class="group-header">
                                        <bk-icon
                                            :type="activeCollapseNames.includes('project') ? 'angle-down' : 'angle-right'"
                                            style="font-size: 21px;"
                                        />
                                        <span>【{{ $t('store.项目') }}】</span>
                                        <i18n
                                            tag="span"
                                            path="store.共X个"
                                        >
                                            <span class="text-blue">{{ projectSelectedList.length }}</span>
                                        </i18n>
                                    </p>
                                    <img
                                        :src="besom"
                                        class="group-delete"
                                        @click.stop="handleDeleteGroup('project')"
                                    />
                                </div>
                                <div
                                    slot="content"
                                    class="selected-content"
                                >
                                    <div
                                        v-for="(row, index) in projectSelectedList"
                                        :key="'project-' + index"
                                        class="selected-item"
                                    >
                                        <div>
                                            <p class="item-name">{{ row.projectName }}</p>
                                            <p class="item-sub">{{ row.projectCode }}</p>
                                        </div>
                                        <img
                                            :src="closeSvg"
                                            @click="handleDelete(row)"
                                        />
                                    </div>
                                </div>
                            </bk-collapse-item>
                        </bk-collapse>
                    </div>
                    <!-- 非devx模式：保持原有展示 -->
                    <template v-else>
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
                    </template>
                </div>
            </div>
        </main>
    </bk-dialog>
</template>

<script>
    import closeSvg from '@/images/close-small.svg'
    import besom from '@/images/besom.svg'
    export default {
        props: {
            selectData: {
                type: Array,
                default: () => []
            },
            showDialog: Boolean,
            isLoading: Boolean,
            routeType: String
        },
        data () {
            return {
                width: 905,
                treeList: [{ id: 0, name: this.$t('store.腾讯公司') }],
                selectedList: [],
                organizationConf: {
                    hasHeader: false,
                    hasFooter: false,
                    closeIcon: false,
                    quickClose: false,
                },
                searchValue: '',
                besom,
                closeSvg,
                defaultExpandedNodes: ['0'],
                timerIds: [],
                currentSelectNode: [],
                activePanel: 'dept',
                projectList: [],
                isLoadingProject: true,
                activeCollapseNames: ['dept', 'project'],
            }
        },
        computed: {
            selectIds () {
                return this.currentSelectNode
                    .map((item) => String(item.id))
            },
            panels () {
                return [
                    { name: 'dept', label: this.$t('store.按组织架构') },
                    { name: 'project', label: this.$t('store.按项目') },
                ]
            },
            deptSelectedList () {
                return this.selectedList.filter(item => item.rangeType !== 'project')
            },
            projectSelectedList () {
                return this.selectedList.filter(item => item.rangeType === 'project')
            },
            filteredProjectList () {
                if (!this.searchValue || this.routeType !== 'devx' || this.activePanel !== 'project') {
                    return this.projectList
                }
                const keyword = this.searchValue.toLowerCase()
                return this.projectList.filter(project =>
                    project.projectName.toLowerCase().includes(keyword)
                    || project.projectCode.toLowerCase().includes(keyword)
                )
            }
        },
        watch: {
            showDialog (val) {
                if (val) {
                    // 处理数据回显
                    const deptList = this.selectData?.deptInfos || this.selectData || []
                    const deptSelectList = deptList.map((i) => ({
                        ...i,
                        id: i.deptId,
                        displayName: i.deptName,
                        rangeType: 'dept'
                    }))
                    const projectList = this.selectData?.projectInfos || []
                    const projectSelectList = projectList.map((i) => ({
                        ...i,
                        id: i.projectCode,
                        displayName: i.projectName,
                        rangeType: 'project'
                    }))

                    this.currentSelectNode = [...deptSelectList, ...projectSelectList]
                    this.selectedList = [...this.currentSelectNode]

                    // 重置折叠状态，默认全部展开
                    this.activeCollapseNames = ['dept', 'project']

                    this.selectIds.includes('0') && this.$refs.organizationTree?.setChecked(0)
                    this.clearChecked(this.selectIds, true)

                    if (this.routeType === 'devx' && !this.projectList.length) {
                        this.fetchProjectList()
                    }
                }
                if (!val) {
                    this.selectedList = []
                    this.currentSelectNode = []
                    this.treeList = [{ id: 0, name: this.$t('store.腾讯公司') }]
                    this.clearTimers()
                    this.activePanel = 'dept'
                    this.searchValue = ''
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
            fetchProjectList () {
                this.isLoadingProject = true
                this.$store.dispatch('store/requestProjectList', { enabled: true }).then((res) => {
                    this.projectList = res || []
                }).catch(err => this.$bkMessage({ message: err.message || err, theme: 'error' })).finally(() => {
                    this.isLoadingProject = false
                })
            },
            isProjectSelected (project) {
                return this.selectedList.some(item => item.id === project.projectCode)
            },
            handleProjectSelect (project, checked) {
                if (checked) {
                    const projectData = {
                        ...project,
                        id: project.projectCode,
                        displayName: project.projectName || project.projectCode,
                        rangeType: 'project',
                    }
                    this.selectedList.push(projectData)
                } else {
                    this.selectedList = this.selectedList.filter(item => item.id !== project.projectCode)
                }
            },
        
            clearChecked (ids, status) {
              this.$refs.organizationTree?.setChecked(ids, { checked: status })
              this.$refs.organizationTree?.setDisabled(ids, { disabled: status })
            },
            handleChange (ids) {
                const idSet = new Set(ids.map(id => String(id)))

                this.selectedList = this.selectedList.filter(item => {
                    if (item.rangeType === 'project') return true
                    return idSet.has(String(item.id))
                })

                idSet.forEach(id => {
                    const alreadyInList = this.selectedList.some(item => String(item.id) === String(id))
                    if (!alreadyInList) {
                        const node = this.$refs.organizationTree.getNodeById(id)
                        if (!node) return
                        const data = { ...node.data }
                        data.displayName = node.name
                        let parentNode = node.parent
                        while (parentNode) {
                            data.displayName = `${parentNode.name}/${data.displayName}`
                            parentNode = parentNode.parent
                        }
                        this.selectedList.push(data)
                    }
                })
            },

            handlerSearchClick () {
                this.$refs.organizationTree.filter(this.searchValue)
            },
            handleDeleteGroup (type) {
                if (type === 'project') {
                    const projectIds = this.projectSelectedList.map(item => item.id)
                    this.selectedList = this.selectedList.filter(item => !projectIds.includes(item.id))
                    this.currentSelectNode = this.currentSelectNode.filter(item => !projectIds.includes(item.id))
                } else {
                    const deptIds = this.deptSelectedList.map(item => item.id)
                    this.selectedList = this.selectedList.filter(item => !deptIds.includes(item.id))
                    this.currentSelectNode = this.currentSelectNode.filter(item => !deptIds.includes(item.id))
                    this.clearChecked(deptIds, false)
                }
            },

            handleDelete (row) {
                this.selectedList = this.selectedList.filter((item) => item.id !== row.id)
                this.currentSelectNode = this.currentSelectNode.filter((item) => item.id !== row.id)
                if (row.rangeType === 'project') {
                    this.handleProjectSelect(row, false)
                } else {
                    this.clearChecked(row.id, false)
                }
            },

            handleDeleteAll () {
                const clearIds = this.selectedList
                    .filter(i => i.rangeType !== 'project')
                    .map(i => i.id)
                this.clearChecked(clearIds, false)
                this.$refs.organizationTree?.removeChecked()
                this.selectedList = []
                this.currentSelectNode = []
            },

            getDeptInfos () {
                const orgList = []
                const projectList = []
                this.selectedList.forEach((item) => {
                    if (item.rangeType === 'project') {
                        projectList.push({
                            projectCode: item.id,
                            projectName: item.displayName,
                        })
                    } else {
                        orgList.push({
                            deptId: item.id,
                            deptName: item.displayName,
                        })
                    }
                })
                if (this.routeType === 'devx') {
                    return { deptInfos: orgList, projectInfos: projectList }
                } else {
                    return orgList
                }
            },

            emitUpdate () {
                const deptInfos = this.getDeptInfos()
                this.$emit('update', {
                    deptInfos: deptInfos.deptInfos || [],
                    projectInfos: deptInfos.projectInfos || []
                })
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
            handleConfirm () {
                if (this.isLoading) return
                if (!this.selectedList.length) {
                    this.$bkMessage({
                        message: this.routeType === 'devx' ? this.$t('store.请选择可见范围') : this.$t('store.请选择部门'),
                        theme: 'error',
                    })
                    return
                }
                const deptInfos = this.getDeptInfos()
                if (this.routeType === 'devx') {
                    this.$emit('saveHandle', { ...deptInfos })
                } else {
                    this.$emit('saveHandle', { deptInfos })
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
    height: 688px;
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
      width: 480px;
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
  .custom-tabs {
    display: flex;
    margin: 0 20px 12px 20px;
    gap: 32px;
    height: 24px;
    border-bottom: 1px solid #DCDEE5;
    .tab-item {
      padding-bottom: 6px;
      cursor: pointer;
      color: #4D4F56;
      font-size: 14px;
      border-bottom: 2px solid transparent;
      &:hover {
        color: #3a84ff;
      }
      &.active {
        color: #3a84ff;
        border-bottom-color: #3a84ff;
      }
    }
  }
  .project-list {
    overflow: auto;
    font-size: 12px;
    color: #4D4F56;
    height: calc(100% - 142px) !important;
    .project-item {
        padding: 6px 8px;
        .project-code {
            margin-left: 16px;
            color: #979BA5;
        }
    }
    .empty-tip {
      padding: 20px 0;
      text-align: center;
      color: #979ba5;
    }
  }
  .organization-selected {
    width: 480px;
    background-color: #f5f7fa;
    border-left: 1px solid #e1e3e9;
  }
  .text-blue {
    color: #4289ff;
    cursor: pointer;
  }
  .grouped {
    padding: 0 20px;
    height: calc(100% - 64px);
    overflow: auto;
    .selected-content {
        width: 419px;
        padding: 0;
    }
    .item-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        .group-header {
            display: flex;
            align-items: center;
            font-size: 12px;
        }
    }
    .group-delete {
        width: 14px;
        height: 14px;
    }
    .selected-item {
        padding: 5px 16px;
        margin-bottom: 4px;
        div {
            display: flex;
            flex-direction: column;
            align-items: start;
        }
        .item-name {
            font-size: 13px;
            color: #313238;
            line-height: 1.4;
        }
        .item-sub {
            font-size: 12px;
            color: #979ba5;
            margin-top: 4px;
            text-align: left;
            line-height: 1.4;
        }
    }
  }
  .bk-dialog-wrapper .bk-dialog-body {
    padding: 0;
    height: 688px;
  }
  .bk-dialog-wrapper .bk-dialog-tool {
    min-height: auto;
  }
  .bk-dialog-content {
    height: 694px;
  }
}
</style>
