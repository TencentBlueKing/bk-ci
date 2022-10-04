<template>
    <bk-dialog
        ext-cls="add-group-dialog"
        width="800"
        :value="addToDialogShow"
        :close-icon="false"
        :draggable="false"
        @confirm="handleSubmit"
        @cancel="handleClose"
    >
        <main v-if="addToDialogShow" class="add-group-main" v-bkloadin="{ isLoading }">
            <aside class="add-group-left">
                <header>
                    {{ title }}
                </header>
                <p>
                    {{ groupTitle }}
                </p>
                <bk-input
                    :placeholder="$t('searchPipelineGroup')"
                    v-model="filterKeyword"
                    @enter="handleSearch"
                    right-icon="bk-icon icon-search"
                />
                <bk-big-tree
                    ref="pipelineGroupTree"
                    :data="pipelineGroupsTree"
                    node-key="id"
                    :show-icon="false"
                    default-expand-all
                    class="add-to-pipeline-group-list"
                >
                    <div class="add-to-pipeline-group-tree-node" slot-scope="{ node, data }">
                        <span @click.stop>
                            <bk-checkbox
                                :disabled="savedPipelineGroupMap[data.id]"
                                :value="isChecked(data)"
                                :indeterminate="isIndeterminate(data)"
                                @change="(checked) => handleChecked(checked, data)"
                            />
                        </span>
                        <span class="add-to-pipeline-group-item-name">{{data.name}}</span>
                        <span class="added-pipeline-group-desc" v-if="savedPipelineGroupMap[data.id]">{{$t('added')}}</span>
                    </div>
                </bk-big-tree>

            </aside>
            <aside class="add-group-right">
                <header>
                    {{ $t('resultPreview') }}
                </header>
                <p>
                    <span>{{$t('selectedGroup', [selectedGroups.length])}}</span>
                    <bk-button size="small" v-if="selectedGroups.length > 0" text @click="emptySelectedGroups">{{$t('newlist.reset')}}</bk-button>
                </p>
                <ul class="add-group-result-preview-list">
                    <li v-for="(group, index) in selectedGroups" :key="group.id">
                        <span class="add-selected-group-name">{{ group.name }}</span>
                        <span @click="remove(index)" class="bk-icon icon-close">
                        </span>
                    </li>
                </ul>
            </aside>
        </main>
    </bk-dialog>
</template>

<script>
    import { mapState, mapActions, mapGetters } from 'vuex'
    export default {
        props: {
            pipeline: {
                type: Object,
                default: () => ({})
            },
            pipelineList: {
                type: Object,
                default: () => []
            },
            isPatch: {
                type: Boolean,
                default: false
            },
            addToDialogShow: Boolean
        },
        data () {
            return {
                isLoading: false,
                selectedGroups: [],
                filterKeyword: ''
            }
        },
        computed: {
            ...mapState('pipelines', [
                'allPipelineGroup'
            ]),
            ...mapGetters('pipelines', [
                'groupMap'
            ]),
            title () {
                return this.$t(this.isPatch ? 'patchAddTo' : 'addTo')
            },
            groupTitle () {
                return this.isPatch ? this.$t('patchAddToGroupTitle', [this.pipelineList.length]) : this.$t('addToGroupTitle', [this.pipeline.pipelineName])
            },
            pipelineGroups () {
                return this.allPipelineGroup.filter(group => group.viewType === 2)
            },
            pipelineGroupsTree () {
                return this.pipelineGroups.reduce((acc, group) => {
                    const index = group.projected ? 1 : 0
                    acc[index].children.push(group)
                    return acc
                }, [{
                    id: 'personal',
                    name: this.$t('personalPipelineGroup'),
                    children: []
                }, {
                    id: 'projected',
                    name: this.$t('projectPipelineGroup'),
                    children: []
                }])
            },
            groupIdMap () {
                return this.pipelineGroups.reduce((acc, group) => ({
                    ...acc,
                    [group.name]: group.id
                }), {})
            },
            selectedGroupIdMap () {
                return this.selectedGroups.reduce((acc, group) => ({
                    ...acc,
                    [group.id]: true
                }), {})
            },
            savedPipelineGroups () {
                return this.pipeline?.viewNames?.map(name => ({
                    id: this.groupIdMap[name],
                    name
                })) ?? []
            },
            savedPipelineGroupMap () {
                return this.savedPipelineGroups.reduce((acc, group) => ({
                    ...acc,
                    [group.id]: true
                }), {})
            }
        },
        methods: {
            ...mapActions('pipelines', [
                'addPipelineToGroup'
            ]),
            handleClose () {
                this.selectedGroups = []
                this.filterKeyword = ''
                this.$emit('close')
            },
            handleSearch () {
                console.log(this.$refs.pipelineGroupTree)
                this.$refs.pipelineGroupTree.filter(this.filterKeyword)
            },
            isChecked ({ id, children }) {
                if (Array.isArray(children)) {
                    return children.every(this.isChecked)
                }
                return this.savedPipelineGroupMap[id] || this.selectedGroupIdMap[id]
            },
            isIndeterminate ({ children }) {
                if (Array.isArray(children)) {
                    let checkNum = 0
                    children.forEach(child => {
                        if (this.isChecked(child)) checkNum++
                    })
                    return checkNum > 0 && checkNum < children.length
                }
                return false
            },
            handleChecked (checked, { id, name, children }) {
                const isRoot = Array.isArray(children)

                if (checked) {
                    const sub = isRoot
                        ? children.filter(child => !this.isChecked(child)).map(child => ({
                            id: child.id,
                            name: child.name
                        }))
                        : [{
                            id,
                            name
                        }]
                    this.selectedGroups.push(...sub)
                } else {
                    const editableChildren = children?.filter(child => !this.savedPipelineGroupMap[id]) ?? []
                    const removeMap = isRoot
                        ? editableChildren.reduce((acc, child) => {
                            acc[child.id] = true
                            return acc
                        }, {})
                        : {
                            [id]: true
                        }

                    this.selectedGroups = this.selectedGroups.filter(group => !removeMap[group.id])
                }
            },
            emptySelectedGroups () {
                this.selectedGroups = []
            },
            remove (index) {
                this.selectedGroups.splice(index, 1)
            },
            async handleSubmit () {
                if (this.isLoading) return
                this.isLoading = true
                let message = this.$t(this.isPatch ? 'patchAddToSuc' : 'addToSuc')
                let theme = 'success'
                try {
                    await this.addPipelineToGroup({
                        projectId: this.$route.params.projectId,
                        pipelineIds: this.isPatch ? this.pipelineList.map(pipeline => pipeline.pipelineId) : [this.pipeline.pipelineId],
                        viewIds: this.selectedGroups.map(group => group.id)
                    })

                    this.selectedGroups.forEach(group => {
                        this.$store.commit('pipelines/UPDATE_PIPELINE_GROUP', {
                            id: group.id,
                            body: {
                                pipelineCount: this.groupMap[group.id].pipelineCount + 1
                            }
                        })
                    })
                    this.handleClose()
                    this.$emit('done')
                } catch (e) {
                    console.log(e)
                    message = e?.message ?? e
                    theme = 'error'
                } finally {
                    this.$showTips({
                        message,
                        theme
                    })
                    this.isLoading = false
                }
            }
        }
    }
</script>

<style lang="scss">
    @import '@/scss/mixins/ellipsis';
    .add-group-dialog {
        .bk-dialog-tool {
            display: none;
        }
        .bk-dialog-body {
            padding: 0;
        }
        .add-group-main {
            display: flex;
            height: 480px;

            .add-group-left {
                display: flex;
                flex-direction: column;
                flex: 3;
                background: white;
                padding: 24px;
                overflow: hidden;
                > p {
                    font-size: 12px;
                    margin: 16px 0;
                    color: #979BA5;
                }
                .add-to-pipeline-group-list {
                    flex: 1;
                    overflow: auto;
                    margin-top: 8px;
                    .add-to-pipeline-group-tree-node {
                        display: flex;
                        font-size: 12px;
                        .add-to-pipeline-group-item-name {
                            flex: 1;
                            @include ellipsis();
                            margin-left: 8px;
                        }
                        .added-pipeline-group-desc {
                            color: #c4c4c4;
                        }
                    }
                }
            }
            .add-group-right {
                display: flex;
                flex-direction: column;
                flex: 2;
                padding: 24px;
                background: #F5F7FA;
                box-shadow: -1px 0 0 0 #DCDEE5;
                overflow: hidden;
                > p {
                    margin: 16px 0;
                    > span {
                        padding-right: 10px;
                        font-size: 12px;
                    }
                }
                .add-group-result-preview-list {
                    flex: 1;
                    overflow: auto;
                    > li {
                        font-size: 12px;
                        height: 30px;
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        background: white;
                        padding: 0 10px;
                        .add-selected-group-name {
                            flex: 1;
                            @include ellipsis();
                        }
                        .icon-close {
                            font-size: 20px;
                            cursor: pointer;
                            padding: 5px;
                        }
                        &:hover {
                            background: #E1ECFF;
                        }
                    }
                }
            }
        }
    }
</style>
