<template>
    <bk-dialog
        :value="isShow"
        width="960"
        :draggable="false"
        @confirm="handleSubmit"
        @cancel="handleClose"
    >
        <section v-bkloading="{ isLoading }" v-if="group" class="pipeline-group-edit-dialog">
            <header class="pipeline-group-edit-header">{{ title }}</header>
            <div class="group-form-item">
                <label class="group-form-label">{{$t('groupStrategy')}}</label>
                <bk-radio-group class="group-form-radio-group" v-model="model.viewType">
                    <bk-radio :value="2">{{$t('staticGroup')}}</bk-radio>
                    <bk-radio :value="1">{{$t('dynamicGroup')}}</bk-radio>
                </bk-radio-group>
            </div>
            <main class="pipeline-group-edit-panel">
                <aside class="pipeline-group-edit-source">
                    <header>{{$t( isDynamicGroup ? 'dynamicStrategy' : 'addablePipelin')}}</header>
                    <article>
                        <template v-if="isDynamicGroup">
                            <div class="group-form-item">
                                <label class="group-form-label">
                                    {{$t('groupLogicLabel')}}
                                </label>
                                <bk-radio-group class="group-form-radio-group" v-model="model.logic">
                                    <bk-radio value="AND">{{$t('view.and')}}</bk-radio>
                                    <bk-radio value="OR">{{$t('view.or')}}</bk-radio>
                                </bk-radio-group>
                            </div>
                            <bk-table class="group-filters-table" :data="formatFilters">
                                <bk-table-column width="152" :label="$t('view.key')">
                                    <template slot-scope="props">
                                        <bk-select v-model="props.row.id" @change="handleFilterTypeChange(props.row)">
                                            <bk-option
                                                v-for="item in filterTypes"
                                                :id="item.id"
                                                :key="item.id"
                                                :name="item.name"
                                            />
                                        </bk-select>
                                    </template>
                                </bk-table-column>
                                <bk-table-column :label="$t('view.value')" prop="value">
                                    <div class="group-filter-value-cell" slot-scope="props">
                                        <section class="group-filter-value-input">
                                            <bk-input
                                                v-if="props.row.id === NAME_FILTER_TYPE"
                                                :placeholder="$t('view.nameTips')"
                                                maxlength="40"
                                                id="pipelineName"
                                                v-model="props.row.pipelineName"
                                            />
                                            <bk-tag-input
                                                v-else-if="props.row.id === CREATOR_FILTER_TYPE"
                                                allow-create
                                                v-model="props.row.userIds"
                                            >
                                            </bk-tag-input>
                                            <bk-select
                                                v-else
                                                v-model="props.row.labelIds"
                                                :multiple="true"
                                            >
                                                <bk-option
                                                    v-for="item in filterLabelMap[props.row.id]"
                                                    :key="item.id"
                                                    :name="item.name"
                                                    :id="item.id"
                                                />
                                            </bk-select>
                                        </section>
                                        <bk-button theme="normal" text @click="removeFilter(props)">
                                            <i class="devops-icon icon-minus-circle" />
                                        </bk-button>
                                    </div>
                                </bk-table-column>
                            </bk-table>
                            <bk-button @click="addFilters" icon="plus">{{$t('add')}}</bk-button>
                        </template>
                        <template v-else>
                            <bk-input
                                class="pipeline-group-tree-search"
                                v-model="searchKeyWord"
                                right-icon="bk-icon icon-search"
                                @enter="handleSearch"
                            />
                            <div class="pipeline-group-tree">
                                <bk-big-tree
                                    ref="pipelineGroupTree"
                                    :data="pipleinGroupTree"
                                    node-key="id"
                                    :show-icon="false"
                                >
                                    <div class="pipeline-group-tree-node" slot-scope="{ node, data }">
                                        <span class="pipeline-group-tree-node-name">{{data.name}}</span>
                                        <span class="pipeline-group-tree-node-added" v-if="model.pipelineIds.has(data.id)">
                                            {{ $t('added') }}
                                        </span>
                                        <bk-button v-else-if="!data.children" text theme="normal" class="add-pipeline-btn" @click="toggleSelect(data.id)">
                                            <i class="devops-icon icon-plus-circle"></i>
                                        </bk-button>
                                    </div>
                                </bk-big-tree>
                            </div>
                        </template>
                    </article>
                </aside>
                <aside class="pipeline-group-edit-preview">
                    <header>
                        {{$t('resultPreview')}}
                        <span v-if="isDynamicGroup" class="show-removed-pipeline-switcher">
                            <bk-switcher v-model="showRemovedPipeline" theme="primary"></bk-switcher>
                            {{ $t('showRemovedPipeline') }}
                        </span>
                    </header>
                    <article v-bkloading="{ isLoading: loading }">
                        <header>
                            <span>{{group ? group.name : '--'}}</span>
                        </header>
                        <ul v-if="preAddedPipelineList.length > 0" class="preview-pipeline-ul">
                            <li
                                v-for="pipeline in preAddedPipelineList"
                                :key="pipeline.pipelineId"

                            >
                                <main>
                                    <p>
                                        <span
                                            :class="{
                                                'is-new-add-pipeline': pipeline.isNew,
                                                'is-removed-pipeline': pipeline.isRemoved
                                            }"
                                            v-if="pipeline.tag"
                                        >
                                            [{{ $t(pipeline.tag) }}]
                                        </span>
                                        {{ pipeline.pipelineName }}
                                    </p>
                                    <footer v-bk-tooltips="{ content: pipeline.groups, extCls: 'pipeline-group-tooltips' }">
                                        {{pipeline.groups}}
                                    </footer>
                                </main>
                                <bk-button
                                    text
                                    v-if="!isDynamicGroup"
                                    theme="primary"
                                    @click="toggleSelect(pipeline.pipelineId)"
                                >
                                    <span v-if="pipeline.isRemoved">
                                        {{$t('restore.restore')}}
                                    </span>
                                    <i v-else class="devops-icon icon-minus-circle" />
                                </bk-button>
                            </li>
                        </ul>
                        <div v-if="isDynamicGroup" v-show="isFilterChange" class="dynamic-group-preview-mask">
                            <div class="dynamic-group-preview-refresh">
                                {{$t('filterChanged')}}
                                <bk-button text size="small" @click="updatePreview">
                                    {{$t('refreshPreview')}}
                                </bk-button>
                            </div>
                        </div>
                    </article>
                </aside>
            </main>
        </section>
    </bk-dialog>
</template>

<script>
    import { mapState, mapGetters, mapActions } from 'vuex'
    import {
        NAME_FILTER_TYPE,
        CREATOR_FILTER_TYPE,
        FILTER_BY_LABEL
    } from '@/utils/pipelineConst'

    export default {
        props: {
            group: {
                type: Object,
                required: true
            }
        },
        data () {
            return {
                NAME_FILTER_TYPE,
                CREATOR_FILTER_TYPE,
                loading: false,
                pipleinGroupTree: [],
                searchKeyWord: '',
                isSubmiting: false,
                isLoading: false,
                pipelineGroupMap: {},
                showRemovedPipeline: false,
                isFilterChange: false,
                preview: {
                    addedPipelineInfos: [],
                    removedPipelineInfos: [],
                    reservePipelineInfos: []
                },
                inited: false,
                model: {
                    viewType: this.group?.viewType ?? 2,
                    pipelineIds: new Set(this.group?.pipelineIds),
                    filters: [],
                    logic: 'AND'
                }
            }
        },
        computed: {

            ...mapState('pipelines', [
                'tagGroupList'
            ]),
            ...mapGetters('pipelines', [
                'groupMap'
            ]),

            conditionConst () {
                return {
                    LIKE: 'LIKE',
                    INCLUDE: 'INCLUDE'
                }
            },
            title () {
                return `${this.group?.name} - ${this.$t('pipelineCountEdit')}`
            },
            isShow () {
                return this.group !== null
            },
            isDynamicGroup () {
                return this.model.viewType === 1
            },
            filterTypes () {
                console.log(this.tagGroupList)
                return [
                    {
                        id: NAME_FILTER_TYPE,
                        name: this.$t('pipelineName'),
                        '@type': NAME_FILTER_TYPE
                    },
                    {
                        id: CREATOR_FILTER_TYPE,
                        name: this.$t('creator'),
                        '@type': CREATOR_FILTER_TYPE
                    },
                    ...this.tagGroupList.map(item => ({
                        '@type': FILTER_BY_LABEL,
                        ...item
                    }))
                ]
            },
            formatFilters () { // TODO: ugly
                return this.model.filters.map(item => {
                    let id
                    switch (item['@type']) {
                        case NAME_FILTER_TYPE:
                        case CREATOR_FILTER_TYPE:
                            id = item['@type']

                            break
                        default:
                            id = item.groupId
                    }
                    item.id = id

                    return item
                })
            },
            filterLabelMap () {
                return this.tagGroupList.reduce((acc, item) => {
                    acc[item.id] = item.labels
                    return acc
                }, {})
            },
            preAddedPipelineList () {
                console.log(this.groupMap)
                return [
                    ...(this.showRemovedPipeline
                        ? this.preview.removedPipelineInfos.map(pipeline => ({
                            ...pipeline,
                            tag: 'removeFrom',
                            isRemoved: true,
                            groups: (this.pipelineGroupMap[pipeline.pipelineId]?.groupIds.map(groupId => this.groupMap[groupId]?.name) ?? []).join(';')
                        }))
                        : []),
                    ...this.preview.addedPipelineInfos.map(pipeline => ({
                        ...pipeline,
                        tag: 'new',
                        isNew: true,
                        groups: (this.pipelineGroupMap[pipeline.pipelineId]?.groupIds.map(groupId => this.groupMap[groupId]?.name) ?? []).join(';')
                    })),
                    ...this.preview.reservePipelineInfos.map(pipeline => ({
                        ...pipeline,
                        groups: (this.pipelineGroupMap[pipeline.pipelineId]?.groupIds.map(groupId => this.groupMap[groupId]?.name) ?? []).join(';')
                    }))
                ]
            }
        },
        watch: {
            group: function (group) {
                this.init(group)
            },
            'model.filters': {
                handler () {
                    if (this.inited) {
                        this.isFilterChange = true
                    }
                    this.inited = true
                },
                deep: true
            }
        },
        created () {
            if (this.isShow) {
                this.init(this.group)
            }
        },
        methods: {
            ...mapActions('pipelines', [
                'requestPipelineGroup',
                'requestGroupListsDict',
                'requestLabelLists',
                'updatePipelineGroup',
                'previewGroupResult'
            ]),
            async init (group) {
                if (this.isLoading || !group?.id) return
                this.isLoading = true
                const params = {
                    projectId: this.$route.params.projectId,
                    id: group.id
                }
                const [groupDetail, { dict, pipelineGroupMap }] = await Promise.all([
                    this.requestPipelineGroup(params),
                    this.requestGroupListsDict(params),
                    this.requestLabelLists(params)
                ])
                const pipelineIdsSet = new Set(groupDetail.pipelineIds)
                this.model = {
                    viewType: groupDetail.viewType ?? group.viewType,
                    pipelineIds: pipelineIdsSet,
                    filters: groupDetail.filters ?? [],
                    logic: groupDetail.logic
                }
                this.pipleinGroupTree = [
                    ...dict.personalViewList,
                    ...dict.projectViewList
                ].map(groupItem => {
                    return {
                        id: groupItem.viewId,
                        name: groupItem.viewName,
                        children: groupItem.pipelineList.map(pipeline => ({
                            id: pipeline.pipelineId,
                            checked: pipelineIdsSet.has(pipeline.pipelineId),
                            name: pipeline.pipelineName
                        }))
                    }
                }, [])
                this.pipelineGroupMap = pipelineGroupMap
                this.preview.reservePipelineInfos = groupDetail.pipelineIds.map(id => ({
                    pipelineId: id,
                    pipelineName: this.pipelineGroupMap[id].pipelineName,
                    groups: (this.pipelineGroupMap[id]?.groupIds.map(groupId => this.groupMap[groupId]?.name) ?? []).join(';')
                }))
                this.isLoading = false
            },
            handleSearch () {
                this.$refs.pipelineGroupTree.searchNode(this.searchKeyWord)
                // const searchResult = this.$refs.tree5.getSearchResult()
                // this.isEmpty = searchResult.isEmpty
            },
            async toggleSelect (id) {
                if (!this.model.pipelineIds.has(id)) {
                    this.model.pipelineIds.add(id)
                } else {
                    this.model.pipelineIds.delete(id)
                }
                this.model.pipelineIds = new Set(this.model.pipelineIds)
                this.updatePreview()
            },
            async updatePreview () {
                try {
                    this.loading = true
                    const { data } = await this.previewGroupResult({
                        projectId: this.$route.params.projectId,
                        name: this.group.name,
                        projected: this.group.projected,
                        ...this.group,
                        ...this.model,
                        pipelineIds: Array.from(this.model.pipelineIds)
                    })
                    this.preview = data
                    this.isFilterChange = false
                } catch (error) {
                    console.log(error)
                } finally {
                    this.loading = false
                }
            },
            handleFilterTypeChange (filter) {
                switch (filter.id) {
                    case NAME_FILTER_TYPE:
                        filter.condition = this.conditionConst.LIKE
                        filter['@type'] = NAME_FILTER_TYPE
                        break
                    case CREATOR_FILTER_TYPE:
                        filter.condition = this.conditionConst.INCLUDE
                        filter['@type'] = CREATOR_FILTER_TYPE
                        break
                    default:
                        filter['@type'] = FILTER_BY_LABEL
                        filter.groupId = filter.id
                }
            },
            addFilters () {
                this.model.filters.push({
                    '@type': NAME_FILTER_TYPE,
                    id: NAME_FILTER_TYPE,
                    condition: this.conditionConst.LIKE,
                    pipelineName: ''
                })
            },
            removeFilter ({ $index }) {
                this.model.filters.splice($index, 1)
            },
            async handleSubmit () {
                if (this.isSubmiting) return
                let message = this.$t('pipelineCountEditSuccess', [this.group.name])
                let theme = 'success'
                try {
                    this.isSubmiting = true

                    await this.updatePipelineGroup({
                        projectId: this.$route.params.projectId,
                        ...this.group,
                        ...this.model,
                        pipelineIds: Array.from(this.model.pipelineIds)
                    })
                    this.handleClose()
                } catch (error) {
                    message = error.message || error
                    theme = 'danger'
                } finally {
                    this.isSubmiting = false
                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },

            handleClose () {
                this.isLoading = false
                this.model = {
                    viewType: 2,
                    pipelineIds: new Set(),
                    filters: [],
                    logic: 'AND'
                }
                this.preview = {
                    addedPipelineInfos: [],
                    removedPipelineInfos: [],
                    reservePipelineInfos: []
                }
                this.$emit('close')
            }
        }
    }
</script>

<style lang="scss">
    @import '@/scss/conf';
    @import '@/scss/mixins/ellipsis';
    .pipeline-group-edit-dialog {
        display: flex;
        flex-direction: column;
        overflow: hidden;
        height: 666px;
        .pipeline-group-edit-header {
            color: #313238;
            font-size: 14px;
            margin-bottom: 16px;
        }
        .group-form-item {
            font-size: 12px;
            margin-bottom: 24px;
            .group-form-label {
                display: flex;
                padding-bottom: 10px;
            }
            .group-form-radio-group {
                > :first-child {
                    margin-right: 60px;
                }
            }
        }
        .pipeline-group-edit-panel {
            display: flex;
            flex: 1;
            border: 1px solid #DCDEE5;
            overflow: hidden;
            .pipeline-group-edit-source {
                border-right: 1px solid #DCDEE5;
            }
            .pipeline-group-edit-source,
            .pipeline-group-edit-preview {
                display: flex;
                flex-direction: column;
                flex: 1;
                overflow: hidden;
                > header {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    background: #FAFBFD;
                    font-weight: bold;
                    padding: 12px 16px;
                    border-bottom: 1px solid #DCDEE5;
                    .show-removed-pipeline-switcher {
                        display: flex;
                        align-items: center;
                        font-weight: normal;
                        font-size: 12px;
                        > :first-child {
                            margin-right: 4px;
                        }
                    }
                }
                > article {
                    position: relative;
                    padding: 16px;
                    flex: 1;
                    display: flex;
                    flex-direction: column;
                    overflow: hidden;
                    .pipeline-group-tree {
                        overflow: auto;
                        flex: 1;
                        .pipeline-group-tree-node {
                            display: flex;
                            align-items: center;
                            justify-content: space-between;
                            font-size: 12px;
                            .pipeline-group-tree-node-name {
                                flex: 1;
                                @include ellipsis();
                            }
                            .pipeline-group-tree-node-added {
                                color: #C4C6CC;
                            }
                            .add-pipeline-btn {
                                display: flex;
                                align-self: flex-start;
                                margin-right: 16px;
                            }
                        }
                    }
                    .group-filters-table {
                        margin-bottom: 8px;
                        .group-filter-value-cell {
                            display: flex;
                            align-items: center;
                            .group-filter-value-input {
                                flex: 1;
                                margin-right: 8px;
                            }
                        }
                    }
                }
                .preview-pipeline-ul {
                    border: 1px solid #DCDEE5;
                    margin: 16px 0;
                    > li {
                        height: 48px;
                        display: flex;
                        align-items: center;
                        border-bottom: 1px solid #DCDEE5;
                        padding: 0 16px;
                        > main {
                            display: flex;
                            flex-direction: column;
                            flex: 1;
                            overflow: hidden;
                            > p,
                            > footer {
                                font-size: 12px;
                                line-height: 20px;
                                @include ellipsis();
                            }
                            .is-new-add-pipeline {
                                color: $successColor;
                                padding: 0 4px;
                            }
                            .is-removed-pipeline {
                                color: $warningColor;
                                padding: 0 4px;
                            }

                            > footer {
                                color: #979BA5;
                            }
                        }
                        &:last-child {
                            border: 0;
                        }
                        &:hover {
                            background-color: #E1ECFF;
                        }
                    }

                }
                &.pipeline-group-edit-preview {
                    > article > header {
                        display: flex;
                        justify-content: space-between;
                    }

                }

                .dynamic-group-preview-mask {
                    width: 100%;
                    height: 100%;
                    position: absolute;
                    display: flex;
                    padding-top: 80px;
                    justify-content: center;
                    background: rgba(255, 255, 255, 0.7);
                    .dynamic-group-preview-refresh {
                        font-size: 12px;
                        display: grid;
                        grid-row-gap: 8px;
                        grid-auto-rows: 16px;
                        width: 170px;
                        height: 84px;
                        border: 1px solid #DCDEE5;
                        background: white;
                        border-radius: 2px;
                        justify-content: center;
                        align-content: center;
                    }
                }
            }
        }
    }
    .pipeline-group-tooltips {
        max-width: 420px;
    }
</style>
