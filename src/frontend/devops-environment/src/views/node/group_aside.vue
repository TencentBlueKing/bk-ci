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
                    <span>{{ $t('environment.addTag') }}</span>
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
                    :class="{ 'active': activeNodeType === item.id }"
                    @click="handleNodeClick(item.id)"
                >
                    <span class="node-name">{{ item.name }}</span>
                    <span
                        class="count-tag"
                        :class="{ 'active': activeNodeType === item.id }"
                    >{{ item.nodeCount }}</span>
                </li>
            </ul>

            <ul
                class="label-list"
                v-if="group.type === 'tag_type'"
            >
                <li
                    v-for="groupItem in group.groups"
                    :key="groupItem.tagKeyId"
                    :class="['label-group', isGroupExpanded(groupItem.tagKeyId) ? 'active-list' : '']"
                >
                    <div
                        class="group-header"
                    >
                        <p
                            @click="toggleGroupExpand(groupItem.tagKeyId)"
                            class="group-label"
                        >
                            <i :class="['devops-icon', 'icon-expanded', isGroupExpanded(groupItem.tagKeyId) ? 'icon-down-shape' : 'icon-right-shape']"></i>
                            <span
                                class="group-name"
                                v-bk-overflow-tips
                            >{{ groupItem.tagKeyName }}</span>
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
                                    v-bk-tooltips="{
                                        content: $t('environment.builtInLabelCannotBeModified'),
                                        disabled: groupItem.canUpdate !== 'INTERNAL'
                                    }"
                                >
                                    <a
                                        href="javascript:;"
                                        v-if="groupItem.canUpdate !== 'INTERNAL'"
                                    >{{ $t('environment.updateTag') }}</a>
                                    <bk-button
                                        v-else
                                        text
                                        class="no-can-delete"
                                        :disabled="groupItem.canUpdate === 'INTERNAL'"
                                    >
                                        {{ $t('environment.updateTag') }}
                                    </bk-button>
                                </li>
                                <li
                                    @click="handleDeleteLabel(groupItem)"
                                    v-bk-tooltips="{
                                        content: groupItem.canUpdate === 'INTERNAL' ? $t('environment.builtInLabelCannotBeDeleted') : $t('environment.removeNodesBeforeDeletingLabel'),
                                        disabled: groupItem.canUpdate === 'TRUE'
                                    }"
                                >
                                    <a
                                        href="javascript:;"
                                        v-if="groupItem.canUpdate === 'TRUE'"
                                    >{{ $t('environment.delete') }}</a>
                                    <bk-button
                                        v-else
                                        text
                                        class="no-can-delete"
                                        :disabled="groupItem.canUpdate !== 'TRUE'"
                                    >
                                        {{ $t('environment.delete') }}
                                    </bk-button>
                                </li>
                            </ul>
                        </bk-dropdown-menu>
                    </div>

                    <ul
                        class="sub-node-list"
                        v-if="groupItem.tagValues"
                        v-show="isGroupExpanded(groupItem.tagKeyId)"
                    >
                        <li
                            v-for="child in groupItem.tagValues"
                            :key="child.tagValueId"
                            class="node-item sub-node-item"
                            :class="{ 'active': activeNodeType === String(child.tagValueId) }"
                            @click="handleSubNodeClick(child)"
                        >
                            <span
                                class="node-name"
                                v-bk-overflow-tips
                            >{{ child.tagValueName }}</span>
                            <span
                                class="count-tag"
                                :class="{ 'active': activeNodeType === String(child.tagValueId) }"
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
            @cancel="handleCancel"
        >
            <div class="tag-dialog-content">
                <bk-alert
                    type="info"
                    v-if="formData.canUpdate !== 'TRUE'"
                    class="tag-info"
                    :title="formData.canUpdate === 'INTERNAL' ? $t('environment.builtInLabelCannotBeModified') : $t('environment.labelInUseRemoveNodesBeforeEditOrDelete')"
                ></bk-alert>
                <bk-form
                    :label-width="100"
                    :model="formData"
                >
                    <bk-form-item
                        :label="$t('environment.tagLabel')"
                        :required="true"
                        property="tagLabel"
                    >
                        <input
                            v-model="formData.tagKeyName"
                            :clearable="true"
                            :disabled="formData.canUpdate !== 'TRUE'"
                            :maxlength="64"
                            :name="`tagLabel_${index}`"
                            v-validate="'required'"
                            class="value-input"
                            :class="{ 'is-danger': errors.has(`tagLabel_${index}`) }"
                        />
                    </bk-form-item>

                    <bk-form-item
                        :label="$t('environment.tagValue')"
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
                                <input
                                    v-model="formData.tagValues[index].tagValueName"
                                    :clearable="true"
                                    :disabled="item.canUpdate !== 'TRUE'"
                                    :maxlength="64"
                                    :name="`tagValueName_${index}`"
                                    v-validate="'required'"
                                    class="value-input"
                                    :class="{ 'is-danger': errors.has(`tagValueName_${index}`) || getDuplicateFlags()[index] }"
                                />
                                <i
                                    class="devops-icon icon-plus-circle"
                                    @click="addTagValueRow"
                                ></i>
                                <i
                                    class="devops-icon icon-minus-circle"
                                    v-if="formData.tagValues.length > 1"
                                    @click="deleteTagValueRow(index, item.nodeCount)"
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
                    :disabled="!formData.tagKeyName.trim()"
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
    import { mapActions, mapState } from 'vuex'
    import { ENV_ACTIVE_NODE_TYPE, ALLNODE } from '@/store/constants'

    export default {
        name: 'NodeGroupTree',
        data () {
            return {
                NODE_RESOURCE_TYPE,
                NODE_RESOURCE_ACTION,
                ENV_ACTIVE_NODE_TYPE,
                ALLNODE,
                expandedGroupIds: [],
                isAdd: false,
                isShowTagChange: false,
                dialogTopOffset: null,
                storedActiveNodeType: localStorage.getItem(ENV_ACTIVE_NODE_TYPE) || ALLNODE,
                formData: {
                    tagKeyName: '',
                    canUpdate: 'TRUE',
                    tagValues: [{
                        canUpdate: 'TRUE',
                        tagValueName: ''
                    }]
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            ...mapState('environment', ['nodeTagList', 'nodeCount']),
            ulMaxHeight () {
                return window.innerHeight * 0.8 - 184
            },
            nodeGroup () {
                const { CMDB = 0, THIRDPARTY = 0 } = this.nodeCount || {}
                const totalCount = CMDB + THIRDPARTY
                return [
                    {
                        type: 'node_type',
                        groups: [
                            { id: 'allNode', name: this.$t('environment.allNodes'), nodeCount: totalCount },
                            { id: 'THIRDPARTY', name: this.$t('environment.selfHostedNodes'), nodeCount: THIRDPARTY },
                            { id: 'CMDB', name: this.$t('environment.deploymentNode'), nodeCount: CMDB }
                        ]
                    },
                    {
                        type: 'tag_type',
                        tltle: this.$t('environment.byNodeLabel'),
                        groups: this.nodeTagList
                    }
                ]
            },
            tagTitle () {
                return this.isAdd ? this.$t('environment.addTag') : this.$t('environment.updateTag')
            },
            activeNodeType () {
                const stored = localStorage.getItem(ENV_ACTIVE_NODE_TYPE) || ALLNODE

                if (this.$route.name === 'nodeList' && this.$route.params.nodeType) {
                    return this.$route.params.nodeType
                } else if (this.isTagValueId(stored)) {
                    return stored
                }
                return stored
            }
        },
        watch: {
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
            },
            '$route': {
                immediate: true,
                deep: true,
                handler (to, from) {
                    if (from?.name === 'setNodeTag' && to.name === 'nodeList') {
                        this.refreshSidebarData()
                    }
                }
            }
        },
        async mounted () {
            this.getNodeTypeCount()
            await this.getTagTypeList()
            this.setDefaultExpandedGroup()
        },
        methods: {
            ...mapActions('environment', ['requestNodeTagList', 'requestGetCounts']),
            refreshSidebarData () {
                this.getTagTypeList()
            },
            isTagValueId (value) {
                if (!value) return false
                const labelGroup = this.nodeGroup.find(g => g.type === 'tag_type')
                if (!labelGroup) return false
        
                return labelGroup.groups.some(group =>
                    group.tagValues?.some(v => String(v.tagValueId) === String(value))
                )
            },
            findGroupByNodeType () {
                const nodeType = this.$route.params.nodeType
                if (!nodeType) return null
                
                const labelGroup = this.nodeGroup.find(g => g.type === 'tag_type')
                if (!labelGroup || !labelGroup.groups.length) return null
                
                for (const group of labelGroup.groups) {
                    if (group.tagValues && group.tagValues.some(v => String(v.tagValueId) === String(nodeType))) {
                        return group.tagKeyId
                    }
                }
                
                return null
            },
            setDefaultExpandedGroup () {
                const groupId = this.findGroupByNodeType()
                if (groupId) {
                    this.expandedGroupIds = [groupId]
                } else {
                    const labelGroup = this.nodeGroup.find(g => g.type === 'tag_type')
                    if (labelGroup && labelGroup.groups.length > 0) {
                        this.expandedGroupIds = [labelGroup.groups[0].tagKeyId]
                    }
                }
            },
            async getNodeTypeCount () {
                await this.requestGetCounts(this.projectId)
            },
            getTagValues () {
                return this.nodeTagList?.flatMap(group =>
                    group.tagValues?.map(v => String(v.tagValueId)) || []
                ) || []
            },
            async getTagTypeList () {
                await this.requestNodeTagList(this.projectId)
                const nodeType = this.$route.params.nodeType
                const validNodeTypes = ['allNode', 'THIRDPARTY', 'CMDB']
                const isValidNodeType = validNodeTypes.includes(nodeType) || this.getTagValues().includes(nodeType)
                if (this.$route.name === 'nodeList' && !isValidNodeType) {
                    this.handleNodeClick(ALLNODE)
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
                    this.formData = JSON.parse(JSON.stringify(groupItem))
                }
            },
            handleNodeClick (nodeType) {
                this.storedActiveNodeType = String(nodeType)
                localStorage.setItem(ENV_ACTIVE_NODE_TYPE, nodeType)
            
                this.$router.push({
                    name: 'nodeList',
                    params: {
                        ...this.$route.params,
                        nodeType
                    },
                    query: { ...this.$route.query }
                })
            },
            handleSubNodeClick (child) {
                this.storedActiveNodeType = String(child.tagValueId)
                localStorage.setItem(ENV_ACTIVE_NODE_TYPE, this.storedActiveNodeType)
        
                this.$router.push({
                    name: 'nodeList',
                    params: {
                        ...this.$route.params,
                        nodeType: this.storedActiveNodeType
                    },
                    query: { ...this.$route.query }
                })
            },
            handleDeleteLabel (groupItem) {
                if (groupItem.canUpdate !== 'TRUE') {
                    return
                }
                this.$bkInfo({
                    theme: 'warning',
                    type: 'warning',
                    extCls: 'info-content',
                    title: `${this.$t('environment.confirmDeleteTag', [groupItem.tagKeyName])}`,
                    confirmFn: async () => {
                        try {
                            const res = await this.$store.dispatch('environment/deleteNodeTag', {
                                projectId: this.projectId,
                                tagKeyId: groupItem.tagKeyId
                            })
                            if (res) {
                                const currentNodeType = this.$route.params.nodeType
                                const isDeleteCurNodeType = groupItem.tagValues.map(i => String(i.tagValueId)).includes(currentNodeType)
                                isDeleteCurNodeType && this.handleNodeClick(ALLNODE)

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
                this.formData.tagValues.push({
                    canUpdate: 'TRUE',
                    tagValueName: ''
                })
            },
            deleteTagValueRow (index, nodeCount) {
                if (nodeCount) {
                    return
                }
                if (this.formData.tagValues.length > 1) {
                    this.formData.tagValues.splice(index, 1)
                }
            },
            async createNodeTag () {
                const filteredValues = this.formData.tagValues.map(i => i.tagValueName.trim()).filter(i => i !== '')
                const params = {
                    ...this.formData,
                    tagValues: filteredValues
                }
                const res = await this.$store.dispatch('environment/createdNodeTag', {
                    projectId: this.projectId,
                    params: params
                })
                if (res) {
                    this.$bkMessage({
                        message: this.$t('environment.successfullyAdded'),
                        theme: 'success'
                    })
                }
            },
            async editNodeTag () {
                const params = {
                    tagKeyId: this.formData.tagKeyId,
                    tagKeyName: this.formData.tagKeyName,
                    tagValues: this.formData.tagValues.map(({ nodeCount, canUpdate, ...rest }) => rest)
                }

                const res = await this.$store.dispatch('environment/editNodeTag', {
                    projectId: this.projectId,
                    params
                })
                if (res) {
                    this.$bkMessage({
                        message: this.$t('environment.successfullyModified'),
                        theme: 'success'
                    })
                }
            },
            getDuplicateFlags () {
                const values = this.formData.tagValues.map(item => item.tagValueName.trim())
                const flags = []

                for (let i = 0; i < values.length; i++) {
                    if (values[i] === "") {
                        flags.push(false)
                    } else {
                        flags.push(values.indexOf(values[i]) !== i)
                    }
                }

                return flags
            },
            hasDuplicateValues () {
                const nonEmptyValues = this.formData.tagValues
                    .map(item => item.tagValueName.trim())
                    .filter(value => value !== "")

                return new Set(nonEmptyValues).size !== nonEmptyValues.length
            },
            async handleConfirm () {
                if (this.hasDuplicateValues()) {
                    this.$bkMessage({
                        message: this.$t('environment.tagValueDuplicate'),
                        theme: 'error'
                    })
                    return
                }
                const isValid = await this.$validator.validateAll()
                if (isValid) {
                    try {
                        if (this.isAdd) {
                            await this.createNodeTag()
                        } else {
                            await this.editNodeTag()
                        }
                    } catch (err) {
                        this.$bkMessage({
                            message: err.message ? err.message : err,
                            theme: 'error'
                        })
                    } finally {
                        this.getTagTypeList()
                        this.handleCancel()
                    }
                }
            },
            handleCancel () {
                this.isShowTagChange = false
                this.formData = {
                    tagKeyName: '',
                    canUpdate: 'TRUE',
                    tagValues: [{
                        canUpdate: 'TRUE',
                        tagValueName: ''
                    }]
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
.info-content {
    .bk-dialog-type-header .header {
        word-break: break-all;
    }
}
</style>
  
<style scoped lang="scss">
  .node-group-tree {
    padding-top: 4px;
    height: 100%;
    background-color: #fff;
    box-shadow: 1px 0 0 0 #EAEBF0;
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
    height: calc(100vh - 274px);
    overflow-y: auto;
    margin-bottom: 4px;
    color: #63656E;

    &::-webkit-scrollbar-thumb {
        background-color: #dcdee5 !important;
        border-radius: 20px !important;
        &:hover {
            background-color: #979ba5 !important;
        }
    }
    &::-webkit-scrollbar {
        width: 8px !important;
        height: 8px !important;
    }
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
    background-color: #f5f7fa;
  }

  .active-list {
    background-color: #FAFBFD;
  }
  
  .node-item.active, .sub-node-item.active {
    background-color: #E1ECFF;
    border-right: 2px solid #3A84FF;
    color: #3A84FF;
  }
  
  .node-name {
    flex: 1;
    width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
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
    height: 32px;
    line-height: 32px;
    padding: 0 12px;
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

    .manage-icon-more-fill:hover {
        color: #3A84FF;
        border-radius: 50%;
        background-color: rgba(99, 101, 110, 0.1);
    }

    .no-can-delete {
        width: 100%;
        text-align: left;
        height: 32px;
        padding: 0 16px;
        &:hover {
            background-color: #F5F7FA;
        }
    }

    .group-label {
        display: flex;
        flex: 1;
        width: calc(100% - 12px);
        align-items: center;

        span{
            display: inline-block;
            flex: 1;
            width: 100%;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
    }

    .dropdown-trigger-btn {
        display: none;
        color: #979BA5;
        font-size: 16px;
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

    .tag-info {
        margin-bottom: 16px;
    }

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

  .value-input {
    padding: 0 10px 0 8px;
    border: 1px solid #c4c6cc;
    border-radius: 2px;
    color: #63656e;
    height: 32px;
    width: 250px;
    outline: none;
    font-size: 12px;
    &:focus {
        border-color: #3a84ff;
    }
    &.is-danger {
        border-color: #ff5656;
        background-color: #fff4f4
    }
  }

  .dialog-footer {
    text-align: right;
  }
</style>
