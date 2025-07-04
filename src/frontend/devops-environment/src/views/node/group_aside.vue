<template>
    <div class="node-group-tree">
        <div
            v-for="group in nodeGroup"
            :key="group.type"
        >
            <div
                class="group-title"
                v-if="group.type === 'tag_type'"
            >
                <span>{{ group.tltle }}</span>
                <p
                    class="title-right"
                    @click="handleChangeTag(true)"
                >
                    <i class="devops-icon icon-plus"></i>
                    <span>{{ $t('environment.新增标签') }}</span>
                </p>
            </div>

            <ul
                class="node-list"
                v-if="group.type === 'node_type'"
            >
                <li
                    v-for="item in group.groups"
                    :key="item.id"
                    class="node-item"
                    :class="{ 'active': $route.params.nodeType === item.id }"
                    @click="handleNodeClick(item.id)"
                >
                    <span class="node-name">{{ item.name }}</span>
                    <span
                        class="count-tag"
                        :class="{ 'active': $route.params.nodeType === item.id }"
                    >{{ item.nodeCount }}</span>
                </li>
            </ul>

            <ul
                class="label-list"
                v-if="group.type === 'tag_type'"
            >
                <li
                    v-for="groupItem in group.groups"
                    :key="groupItem.id"
                    class="label-group"
                >
                    <div
                        class="group-header"
                    >
                        <p @click="toggleGroupExpand(groupItem.id)">
                            <i :class="['devops-icon', 'icon-expanded', isGroupExpanded(groupItem.id) ? 'icon-down-shape' : 'icon-right-shape']"></i>
                            <span class="group-name">{{ groupItem.name }}</span>
                        </p>
                        
                        <bk-dropdown-menu
                            ref="dropdown"
                            trigger="click"
                            align="center"
                            ext-cls="drop-menu"
                            :position-fixed="true"
                        >
                            <div
                                class="dropdown-trigger-btn"
                                slot="dropdown-trigger"
                            >
                                <i class="manage-icon manage-icon-more-fill"></i>
                            </div>
                            <ul
                                class="bk-dropdown-list"
                                slot="dropdown-content"
                            >
                                <li
                                    @click="handleChangeTag(false, groupItem)"
                                >
                                    <a href="javascript:;">{{ $t('environment.修改标签') }}</a>
                                </li>
                                <li @click="handleDeleteLabel(groupItem)">
                                    <a href="javascript:;">{{ $t('environment.delete') }}</a>
                                </li>
                            </ul>
                        </bk-dropdown-menu>
                    </div>

                    <ul
                        class="sub-node-list"
                        v-if="groupItem.children"
                        v-show="isGroupExpanded(groupItem.id)"
                    >
                        <li
                            v-for="child in groupItem.children"
                            :key="child.id"
                            class="node-item sub-node-item"
                            :class="{ 'active': $route.params.nodeType === String(child.id) }"
                            @click="handleNodeClick(child.id)"
                        >
                            <span class="node-name">{{ child.name }}</span>
                            <span
                                class="count-tag"
                                :class="{ 'active': $route.params.nodeType === String(child.id) }"
                            >{{ child.nodeCount }}</span>
                        </li>
                    </ul>
                </li>
            </ul>
        </div>

        <bk-dialog
            v-model="isShowTagChange"
            :render-directive="'if'"
            :mask-close="false"
            :width="480"
            header-position="left"
            :title="tagTitle"
            ext-cls="tag-change"
            :style="{ '--dialog-top-translateY': `translateY(${dialogTopOffset}px)` }"
        >
            <div class="tag-dialog-content">
                <bk-form
                    :label-width="100"
                    :model="formData"
                    :rules="rules"
                    ref="tagForm"
                >
                    <bk-form-item
                        label="标签label"
                        :required="true"
                        property="label"
                    >
                        <bk-input
                            v-model="formData.tagKeyName"
                            :clearable="true"
                        ></bk-input>
                    </bk-form-item>

                    <bk-form-item
                        label="标签value"
                        style="margin-top: 24px;"
                    >
                        <div
                            class="value-content"
                            :style="{ 'max-height': `${ulMaxHeight}px` }"
                        >
                            <div
                                v-for="(item, index) in formData.tagValues"
                                :key="index"
                                class="value-row"
                            >
                                <bk-input
                                    v-model="formData.tagValues[index]"
                                    :clearable="true"
                                ></bk-input>
                                <i
                                    class="devops-icon icon-plus-circle"
                                    @click="addTagValueRow"
                                ></i>
                                <i
                                    class="devops-icon icon-minus-circle"
                                    v-if="formData.tagValues.length > 1"
                                    @click="deleteTagValueRow(index)"
                                ></i>
                            </div>
                        </div>
                    </bk-form-item>
                </bk-form>
            </div>
            <div
                slot="footer"
                class="dialog-footer"
            >
                <bk-button
                    theme="primary"
                    :disabled="!formData.tagKeyName"
                    @click="handleConfirm"
                >
                    {{ $t('environment.save') }}
                </bk-button>
                <bk-button @click="handleCancel">{{ $t('environment.cancel') }}</bk-button>
            </div>
        </bk-dialog>
    </div>
</template>
  
<script>
    import { NODE_RESOURCE_ACTION, NODE_RESOURCE_TYPE } from '@/utils/permission'

    export default {
        name: 'NodeGroupTree',
        data () {
            return {
                NODE_RESOURCE_TYPE,
                NODE_RESOURCE_ACTION,
                expandedGroupIds: [],
                isAdd: false,
                isShowTagChange: false,
                dialogTopOffset: null,
                rules: {
                    label: [{ required: true, trigger: 'blur', message: this.$t('environment.requiredField') }]
                },
                formData: {
                    tagKeyName: '',
                    tagValues: ['']
                },
                tagList: [],
                nodesCounts: {}
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            ulMaxHeight () {
                return window.innerHeight * 0.8 - 184
            },
            nodeGroup () {
                const { CMDB = 0, THIRDPARTY = 0 } = this.nodesCounts || {}
                const totalCount = CMDB + THIRDPARTY
                console.log(this.tagList.map(item => ({
                    id: item.tagKeyId,
                    name: item.tagKeyName,
                    children: item.tagValues.map(tag => ({
                        ...tag,
                        id: tag.tagValueId,
                        name: tag.tagValueName
                    }))
                })))
                return [
                    {
                        type: 'node_type',
                        groups: [
                            { id: 'allNode', name: this.$t('environment.全部节点'), nodeCount: totalCount },
                            { id: 'THIRDPARTY', name: this.$t('environment.私有构建节点'), nodeCount: THIRDPARTY },
                            { id: 'CMDB', name: this.$t('environment.部署节点'), nodeCount: CMDB }
                        ]
                    },
                    {
                        type: 'tag_type',
                        tltle: this.$t('environment.按节点标签'),
                        groups: this.tagList.map(item => ({
                            id: item.tagKeyId,
                            name: item.tagKeyName,
                            children: item.tagValues.map(tag => ({
                                nodeCount: tag.nodeCount,
                                id: tag.tagValueId,
                                name: tag.tagValueName
                            }))
                        }))
                    }
                ]
            },
            allGroupIds () {
                const labelGroup = this.nodeGroup.find(g => g.type === 'tag_type')
                return labelGroup ? labelGroup.groups.map(g => g.id) : []
            },
            tagTitle () {
                return this.isAdd ? this.$t('environment.新增标签') : this.$t('environment.修改标签')
            }
        },
        watch: {
            nodeGroup () {
                this.expandedGroupIds = this.allGroupIds
            },
            'formData.tagValues': {
                handler (newVal) {
                    if (newVal) {
                        const ITEM_HEIGHT = 48
                        const DIALOG_EXTRA_HEIGHT = 184
                        const totalListHeight = newVal.length * ITEM_HEIGHT
                        const listHeight = Math.min(totalListHeight, this.ulMaxHeight)
                        this.dialogTopOffset = -Math.round((listHeight + DIALOG_EXTRA_HEIGHT) / 2)
                    }
                },
                immediate: true
            }
        },
        created () {
            this.expandedGroupIds = this.allGroupIds
        },
        mounted () {
            this.getNodeTypeCount()
            this.getTagTypeList()
        },
        methods: {
            async getNodeTypeCount () {
                try {
                    const res = await this.$store.dispatch('environment/requestGetCounts', {
                        projectId: this.projectId
                    })
                    this.nodesCounts = res
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                }
            },
            async getTagTypeList () {
                try {
                    const res = await this.$store.dispatch('environment/requestNodeTagList', {
                        projectId: this.projectId
                    })
                    this.tagList = res
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                }
            },
            isGroupExpanded (groupId) {
                return this.expandedGroupIds.includes(groupId)
            },
            toggleGroupExpand (groupId) {
                if (this.isGroupExpanded(groupId)) {
                    this.expandedGroupIds = this.expandedGroupIds.filter(id => id !== groupId)
                } else {
                    this.expandedGroupIds.push(groupId)
                }
            },
            handleChangeTag (type, groupItem) {
                this.isShowTagChange = true
                this.isAdd = type
                if (groupItem) {
                    this.formData = {
                        tagKeyName: groupItem.name,
                        tagValues: groupItem.children.map(child => (child.name))
                    }
                }
            },
            handleNodeClick (nodeType) {
                this.$router.push({ name: 'nodeList', params: { nodeType } })
            },
            handleDeleteLabel (groupItem) {
                this.$bkInfo({
                    theme: 'warning',
                    type: 'warning',
                    title: `${this.$t('environment.确认删除标签', [groupItem.name])}`,
                    confirmFn: async () => {
                        try {
                            const res = await this.$store.dispatch('environment/deleteNodeTag', {
                                projectId: this.projectId,
                                tagKeyId: groupItem.id
                            })
                            if (res) {
                                this.getTagTypeList()
                                this.$bkMessage({
                                    message: this.$t('environment.successfullyDeleted'),
                                    theme: 'success'
                                })
                            }
                        } catch (e) {
                            this.handleError(
                                e,
                                {
                                    projectId: this.projectId,
                                    resourceType: NODE_RESOURCE_TYPE,
                                    resourceCode: this.projectId,
                                    action: NODE_RESOURCE_ACTION.DELETE
                                }
                            )
                        }
                    }
                })
            },
            addTagValueRow () {
                this.formData.tagValues.push('')
            },
            deleteTagValueRow (index) {
                if (this.formData.tagValues.length > 1) {
                    this.formData.tagValues.splice(index, 1)
                }
            },
            async handleConfirm () {
                const valid = await this.$refs.tagForm.validate()
                if (valid) {
                    const filteredValues = this.formData.tagValues.filter(value => value.trim() !== '')
                    const params = {
                        ...this.formData,
                        tagValues: filteredValues
                    }
                    if (this.isAdd) {
                        const res = await this.$store.dispatch('environment/createdNodeTag', {
                            projectId: this.projectId,
                            params
                        })
                        if (res) {
                            this.getTagTypeList()
                            this.$bkMessage({
                                message: this.$t('environment.新增成功'),
                                theme: 'success'
                            })
                        }
                    } else {
                        console.log({
                            projectId: this.projectId,
                            tagKeyId: this.formData.id,
                            params
                        })
                        const res = await this.$store.dispatch('environment/editNodeTag', {
                            projectId: this.projectId,
                            params
                        })
                        if (res) {
                            this.getTagTypeList()
                            this.$bkMessage({
                                message: this.$t('environment.编辑成功'),
                                theme: 'success'
                            })
                        }
                    }
                    this.isShowTagChange = false
                }
            },
            handleCancel () {
                this.isShowTagChange = false
                this.formData = {
                    tagKeyName: '',
                    tagValues: ['']
                }
            }
        }
    }
</script>

<style lang="scss">
.tag-change .bk-dialog {
    top: 50% !important;
    transform: var(--dialog-top-translateY) !important;
}
</style>
  
<style scoped lang="scss">
  .node-group-tree {
    padding-top: 4px;
    background-color: #fff;
  }
  
  .group-title {
    display: flex;
    justify-content: space-between;
    position: relative;
    padding: 10px 16px;
    font-size: 12px;
    color: #979BA5;

    .title-right {
        color: #3A84FF;
        cursor: pointer;

        span {
            margin-left: 4px;
        }
    }
  }

  .group-title::before {
    content: '';
    position: absolute;
    top: 0;
    left: 16px;
    right: 16px;
    height: 1px;
    border-top: 1px solid #DCDEE5;
  }

  .label-list {
    margin-bottom: 4px;
    color: #63656E;
  }

  .node-list {
    margin-bottom: 28px;
    color: #313238;
  }

  .node-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 4px;
    padding: 0 28px 0 16px;
    height: 32px;
    line-height: 32px;
    cursor: pointer;
  }
  
  .node-item:hover, .sub-node-item:hover {
    background-color: #E1ECFF;
  }
  
  .node-item.active, .sub-node-item.active {
    background-color: #E1ECFF;
    border-right: 2px solid #3A84FF;
    color: #3A84FF;
  }
  
  .node-name {
    flex: 1;
  }
  
  .count-tag {
    display: inline-block;
    width: 30px;
    height: 16px;
    line-height: 16px;
    background: #F0F1F5;
    border-radius: 8px;
    font-size: 12px;
    text-align: center;
    color: #979BA5;
  }

  .count-tag.active {
    color: #3A84FF;
    background: #FFFFFF;
  }

  .label-group {
    margin-bottom: 8px;
  }
  
  .group-header {
    padding: 8px 12px;
    cursor: pointer;
    border-radius: 4px;
    display: flex;
    justify-content: space-between;
    color: #63656E;
    align-items: center;

    &:hover {
        background-color: #F0F1F5;
    }

    &:hover .dropdown-trigger-btn {
        display: block;
    }

    p {
        flex: 1;
    }

    .dropdown-trigger-btn {
        display: none;
    }
  }

  .icon-expanded {
    color: #979BA5;
    vertical-align: middle;
  }
  
  .group-name {
    flex: 1;
    margin-left: 4px;
  }

  .sub-node-item {
    margin-top: 0;
    padding-left: 40px;
  }

  .tag-dialog-content {
    padding: 0 16px;

    .bk-form-control {
        width: 250px;
    }

    .icon-plus-circle {
        margin: 0 8px;
    }

    i {
        color: #979BA5;
    }

    .value-content {
        overflow: auto;

        .value-row {
          display: flex;
          align-items: center;
    
          &:not(:first-child) {
            margin-top: 16px;
          }
        }
    }
  }

  .dialog-footer {
    text-align: right;
  }
</style>
