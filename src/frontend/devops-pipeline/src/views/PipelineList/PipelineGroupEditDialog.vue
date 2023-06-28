<template>
    <bk-dialog
        :value="isShow"
        width="960"
        ext-cls="pipeline-group-edit-dialog auto-height-dialog"
        scrollable
        :quick-close="false"
        :close-icon="false"
        :draggable="false"
    >
        <section v-if="group" class="pipeline-group-edit-dialog-main">
            <aside class="pipeline-group-edit-source">
                <header class="pipeline-group-edit-header">{{ title }}</header>
                <div class="group-form-item">
                    <label class="group-form-label">{{$t('groupStrategy')}}</label>
                    <bk-radio-group class="group-form-radio-group" v-model="model.viewType" @change="handleViewTypeChange">
                        <bk-radio v-for="strategy in groupStrategy" :value="strategy.value" :key="strategy.value">
                            <span v-bk-tooltips="strategy.tooltips" class="group-strategy-radio">
                                {{strategy.label}}
                            </span>
                        </bk-radio>
                    </bk-radio-group>
                </div>
                <article>
                    <template v-if="isDynamicGroup">
                        <div class="group-form-item">
                            <label class="group-form-label">
                                {{$t('groupLogicLabel')}}
                            </label>
                            <bk-radio-group class="group-form-radio-group" v-model="model.logic" @change="changeFilterChangeFlag">
                                <bk-radio value="AND">{{$t('view.and')}}</bk-radio>
                                <bk-radio value="OR">{{$t('view.or')}}</bk-radio>
                            </bk-radio-group>
                        </div>
                        <bk-table class="group-filters-table" height="100%" :data="formatFilters">
                            <bk-table-column width="152" :label="$t('view.key')">
                                <template slot-scope="props">
                                    <bk-select
                                        :key="props.key"
                                        :clearable="false"
                                        v-model="props.row.id"
                                        @change="handleFilterTypeChange(props.row)"
                                    >
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
                                <div :title="props.row.key" :key="props.row.key" class="group-filter-value-cell" slot-scope="props">
                                    <bk-form
                                        :label-width="0"
                                        :ref="`dynamicForms_${props.row.key}`"
                                        :model="props.row"
                                        class="group-filter-value-input"
                                    >
                                        <bk-form-item v-if="props.row.id === NAME_FILTER_TYPE" v-bind="getDynamicFilterConf(props.row.id, props.row)">
                                            <bk-input
                                                :placeholder="$t('view.nameTips')"
                                                maxlength="40"
                                                id="pipelineName"
                                                v-model="props.row.pipelineName"
                                            />
                                        </bk-form-item>
                                        <bk-form-item v-else-if="props.row.id === CREATOR_FILTER_TYPE" v-bind="getDynamicFilterConf(props.row.id)">
                                            <bk-tag-input
                                                allow-create
                                                allow-auto-match
                                                v-model="props.row.userIds"
                                            />
                                        </bk-form-item>
                                        <bk-form-item v-else v-bind="getDynamicFilterConf(props.row.id)">
                                            <bk-select
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
                                        </bk-form-item>
                                    </bk-form>
                                    <span class="filter-operations-span">
                                        <bk-button theme="normal" text @click="removeFilter(props)">
                                            <i class="devops-icon icon-minus-circle" />
                                        </bk-button>
                                        <bk-button theme="normal" text @click="addFilters(props.$index)">
                                            <i class="devops-icon icon-plus-circle" />
                                        </bk-button>
                                    </span>
                                </div>
                            </bk-table-column>
                        </bk-table>
                        <bk-button outline theme="primary" @click="updatePreview">{{$t('pipelinesPreview')}}</bk-button>
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
                                :default-expanded-nodes="defaultExpandedNodes"
                                :show-icon="false"
                            >
                                <div @click.stop :class="['pipeline-group-tree-node', {
                                    'is-delete': data.deleted
                                }]" slot-scope="{ node, data }">
                                    <bk-checkbox
                                        v-bind="isChecked(data.id)"
                                        :disabled="!data.checkable"
                                        :class="{
                                            'pipeline-group-tree-node-checkbox': true,
                                            'last-checked': savedPipelineInfos.has(data.id)
                                        }"
                                        @change="(checked) => data.hasChild ? handleRootCheck(checked, data) : handleCheck(checked, data)"
                                    >
                                        {{data.name}}
                                    </bk-checkbox>
                                    <span class="pipeline-group-tree-node-desc" v-if="data.deleted">{{ $t('deleted') }}</span>
                                    <span v-if="data.hasChild">（{{ data.children.length }}）</span>
                                </div>
                            </bk-big-tree>
                        </div>
                    </template>
                </article>
            </aside>
            <aside class="pipeline-group-edit-preview">
                <header>
                    {{$t('resultPreview')}}
                    <span class="pipeline-preview-time" v-if="previewTime">{{$t('previewTime', [previewTime])}}</span>
                </header>
                <article v-bkloading="{ isLoading: loading }">
                    <header class="preview-pipeline-title">
                        {{$t('total')}}
                        <span class="pipeline-total-count">
                            {{ totalPreviewCount }}
                        </span>
                        {{$t('strip')}}，
                        <p>
                            {{$t('new')}}
                            <span class="pipeline-add-count">
                                {{ preview.addedPipelineInfos.length }}
                            </span>
                            {{$t('strip')}}，
                        </p>
                        <p>
                            {{$t('removeFrom')}}
                            <span class="pipeline-removed-count">
                                {{ preview.removedPipelineInfos.length }}
                            </span>
                            {{$t('strip')}}
                        </p>
                    </header>
                    <ul v-if="preAddedPipelineList.length > 0" class="preview-pipeline-ul">
                        <li
                            v-for="(pipeline, index) in preAddedPipelineList"
                            :key="pipeline.pipelineId"
                        >
                            <bk-popover class="pipeline-group-tooltips">
                                <main class="pipeline-group-left-side">
                                    <p>
                                        <bk-tag
                                            ext-cls="pipeline-classify-tag"
                                            v-if="pipeline.tag"
                                            :theme="pipeline.theme"
                                        >
                                            {{ $t(pipeline.tag) }}
                                        </bk-tag>
                                        {{ pipeline.pipelineName }}
                                    </p>
                                    <footer>
                                        {{pipeline.groups}}
                                    </footer>
                                </main>
                                <div slot="content">
                                    <p>{{pipeline.pipelineName}}</p>
                                    <p>
                                        {{pipeline.groups}}
                                    </p>
                                </div>
                            </bk-popover>
                            <span
                                v-if="!isDynamicGroup && (pipeline.isRemoved || pipeline.isAdded)"
                                v-bk-tooltips="pipeline.tooltips"
                                class="pipeline-operate-btn"
                                @click="togglePipeline(pipeline, index)"
                            >
                                <logo
                                    :name="pipeline.isAdded ? 'close' : 'undo'"
                                    size="18"
                                />
                            </span>
                        </li>
                    </ul>
                </article>
            </aside>
        </section>
        <footer slot="footer">
            <bk-popover v-bind="saveDisableTips">
                <bk-button
                    theme="primary"
                    v-bk-tooltips="saveDisableTips"
                    @click="handleSubmit"
                    :disabled="isFilterChange"
                >
                    {{$t('save')}}
                </bk-button>
            </bk-popover>
            <bk-button @click="handleClose">
                {{$t('cancel')}}
            </bk-button>
        </footer>
    </bk-dialog>
</template>

<script>
    import { mapState, mapGetters, mapActions } from 'vuex'
    import moment from 'moment'
    import Logo from '@/components/Logo'
    import {
        NAME_FILTER_TYPE,
        CREATOR_FILTER_TYPE,
        FILTER_BY_LABEL,
        VIEW_CONDITION
    } from '@/utils/pipelineConst'
    import {
        hashID
    } from '@/utils/util'

    const defaultFilter = {
        '@type': NAME_FILTER_TYPE,
        id: NAME_FILTER_TYPE,
        condition: VIEW_CONDITION.LIKE,
        pipelineName: ''
    }
    export default {
        components: {
            Logo
        },
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
                savedPipelineInfos: new Set(),
                preTypePreview: {},
                inited: false,
                previewTime: null,
                model: {
                    viewType: this.group?.viewType ?? 2,
                    pipelineIds: new Set(this.group?.pipelineIds ?? []),
                    filters: [{
                        ...defaultFilter
                    }],
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
            defaultExpandedNodes () {
                return this.group?.id ? [this.group?.id] : []
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
                return [
                    ...this.preview.addedPipelineInfos.map(pipeline => this.generatePreviewPipeline(pipeline, false, true)),
                    ...this.preview.removedPipelineInfos.map(pipeline => this.generatePreviewPipeline(pipeline, true, false)),
                    ...this.preview.reservePipelineInfos.map(this.generatePreviewPipeline)
                ]
            },
            parentCheckStatusMap () {
                const res = this.pipleinGroupTree.reduce((acc, root) => {
                    let checkedNum = 0
                    root.children.forEach(pipeline => {
                        if (this.model.pipelineIds.has(pipeline.id)) {
                            checkedNum++
                        }
                    })
                    acc[root.id] = {
                        checked: checkedNum > 0 && checkedNum === root.children.length,
                        indeterminate: checkedNum > 0 && checkedNum < root.children.length
                    }
                    return acc
                }, {})
                return res
            },
            saveDisableTips () {
                return {
                    content: this.$t('saveBeforePreviewTips'),
                    disabled: !this.isFilterChange
                }
            },
            totalPreviewCount () {
                return this.preAddedPipelineList.length - this.preview.removedPipelineInfos.length
            },
            groupStrategy () {
                return [
                    {
                        value: 1,
                        label: this.$t('dynamicGroup'),
                        tooltips: this.$t('dynamicGroupTips')
                    }, {
                        value: 2,
                        label: this.$t('staticGroup'),
                        tooltips: this.$t('staticGroupTips')
                    }
                ]
            }
        },
        watch: {
            group: function (group) {
                this.init(group)
            },
            'model.filters': {
                handler () {
                    this.changeFilterChangeFlag()
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
                'requestTagList',
                'updatePipelineGroup',
                'previewGroupResult',
                'requestGetGroupLists'
            ]),
            isChecked (id) {
                return this.parentCheckStatusMap[id] ?? {
                    checked: this.model.pipelineIds.has(id)
                }
            },
            getDynamicFilterConf (id, row) {
                let property = 'labelIds'
                let message = 'view.labelTips'

                switch (id) {
                    case NAME_FILTER_TYPE:
                        property = 'pipelineName'
                        message = 'subpage.nameNullTips'
                        break
                    case CREATOR_FILTER_TYPE:
                        property = 'userIds'
                        message = 'view.creatorTips'
                        break
                }

                return {
                    property,
                    rules: [
                        {
                            required: true,
                            message: this.$t(message),
                            trigger: 'blur'
                        }
                    ]
                }
            },
            changeFilterChangeFlag () {
                if (this.model.viewType === 1) {
                    if (this.inited) {
                        this.isFilterChange = true
                    }
                    this.inited = true
                }
            },
            handleViewTypeChange (viewType) {
                this.isFilterChange = viewType === 1
                if (viewType === 1) {
                    this.preTypePreview = {
                        ...this.preview
                    }
                    this.preview = {
                        addedPipelineInfos: [],
                        removedPipelineInfos: [],
                        reservePipelineInfos: []
                    }
                } else if (viewType === 2) {
                    this.previewTime = null
                    if (Object.keys(this.preTypePreview).length > 0) {
                        this.preview = {
                            ...this.preTypePreview
                        }
                        this.preTypePreview = {}
                    }
                }
            },
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
                    this.requestTagList(params)
                ])
                this.savedPipelineInfos = new Set(groupDetail.pipelineIds)
                this.model = {
                    viewType: groupDetail.viewType ?? group.viewType,
                    pipelineIds: new Set(groupDetail.pipelineIds),
                    filters: (groupDetail.filters.length > 0 ? groupDetail.filters : this.model.filters).map(filter => ({
                        ...filter,
                        key: hashID()
                    })),
                    logic: groupDetail.logic
                }
                this.pipleinGroupTree = [
                    ...dict.personalViewList,
                    ...dict.projectViewList
                ].map(groupItem => {
                    const children = groupItem.pipelineList
                        .filter(pipeline => !pipeline.delete || pipeline.viewId === group.id)
                        .map(pipeline => ({
                            id: pipeline.pipelineId,
                            name: pipeline.pipelineName,
                            deleted: pipeline.delete,
                            checkable: true
                        }))
                    return {
                        id: groupItem.viewId,
                        name: groupItem.viewName,
                        hasChild: true,
                        checkable: children.length > 0,
                        children
                    }
                }, [])
                this.pipelineGroupMap = pipelineGroupMap
                this.preview.reservePipelineInfos = groupDetail.pipelineIds.map(pipelineId => this.generatePreviewPipeline({
                    pipelineId
                }))
                this.isLoading = false
            },
            handleSearch () {
                this.$refs.pipelineGroupTree.filter(this.searchKeyWord)
                // const searchResult = this.$refs.tree5.getSearchResult()
                // this.isEmpty = searchResult.isEmpty
            },
            generatePreviewPipeline (originPipeline, isRemoved = false, isAdded = false) {
                const { pipelineId, pipelineName } = originPipeline
                const pipelingGroup = this.pipelineGroupMap[originPipeline.pipelineId]
                let metaData = {}
                switch (true) {
                    case isRemoved:
                        metaData = {
                            tag: 'removeFrom',
                            isRemoved,
                            theme: 'danger'
                        }
                        break
                    case isAdded:
                        metaData = {
                            tag: 'new',
                            isAdded,
                            theme: 'success'
                        }
                        break
                }
                return {
                    pipelineId,
                    pipelineName: pipelineName ?? pipelingGroup?.pipelineName,
                    ...metaData,
                    groups: (pipelingGroup?.groupIds ?? []).map(groupId => this.groupMap[groupId]?.name).join(';'),
                    tooltips: {
                        content: this.$t(isAdded ? 'cancelAdd' : 'restore.restore'),
                        delay: [500, 0],
                        disabled: !(isAdded || isRemoved)
                    }
                }
            },
            handleRootCheck (checked, root) {
                root.children.forEach(child => this.handleCheck(checked, child))
            },
            async handleCheck (checked, data) {
                const previewPipeline = this.generatePreviewPipeline({
                    pipelineId: data.id,
                    pipelineName: data.name
                })
                if (checked) {
                    if (this.savedPipelineInfos.has(data.id) && !this.model.pipelineIds.has(data.id)) {
                        this.preview.reservePipelineInfos.push(previewPipeline)
                        this.preview.removedPipelineInfos = this.preview.removedPipelineInfos.filter(pip => pip.pipelineId !== data.id)
                    } else if (!this.model.pipelineIds.has(data.id)) {
                        this.preview.addedPipelineInfos.push(previewPipeline)
                    }
                } else {
                    if (this.savedPipelineInfos.has(data.id)) {
                        this.preview.removedPipelineInfos.push(previewPipeline)
                        this.preview.reservePipelineInfos = this.preview.reservePipelineInfos.filter(pip => pip.pipelineId !== data.id)
                    } else {
                        this.preview.addedPipelineInfos = this.preview.addedPipelineInfos.filter(pip => pip.pipelineId !== data.id)
                    }
                }
                this.updatePipelineIds(data.id, checked)
            },
            togglePipeline (pipeline, index) {
                const { isRemoved, isAdded, pipelineId } = pipeline
                let checked = true
                if (isRemoved) {
                    const originPipeline = this.generatePreviewPipeline(pipeline)
                    this.preview.reservePipelineInfos.push(originPipeline)
                    this.preview.removedPipelineInfos.splice(index, 1)
                } else if (isAdded) {
                    this.preview.addedPipelineInfos.splice(index, 1)
                    checked = false
                }
                this.updatePipelineIds(pipelineId, checked)
            },
            updatePipelineIds (id, checked) {
                console.log(this.model.pipelineIds.length, id, checked, this.model.pipelineIds.has(id))
                if (this.model.pipelineIds.has(id) && !checked) {
                    this.model.pipelineIds.delete(id)
                } else if (!this.model.pipelineIds.has(id)) {
                    this.model.pipelineIds.add(id)
                }
                this.model.pipelineIds = new Set(this.model.pipelineIds)
            },
            async checkcDynamicFiltersValid () {
                try {
                    const res = await Promise.all(this.formatFilters?.map((_, index) => {
                        const form = this.$refs[`dynamicForms_${index}`]
                        return form?.validate?.() ?? Promise.resolve(true)
                    }))
                    return res.every(res => res)
                } catch (e) {
                    this.$showTips({
                        message: e.content ?? e,
                        theme: 'error'
                    })
                    return false
                }
            },
            async updatePreview () {
                try {
                    const valid = await this.checkcDynamicFiltersValid()
                    if (!valid) return
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
                    this.previewTime = moment().format('HH:mm:ss')
                    this.isFilterChange = false
                } catch (error) {
                    console.log(error)
                    this.$showTips({
                        message: error.message
                    })
                } finally {
                    this.loading = false
                }
            },
            handleFilterTypeChange (filter) {
                this.$refs[`dynamicForms_${filter.key}`]?.clearError?.()
                switch (filter.id) {
                    case NAME_FILTER_TYPE:
                        filter.condition = VIEW_CONDITION.LIKE
                        filter['@type'] = NAME_FILTER_TYPE
                        filter.userIds = []
                        filter.labelIds = []
                        break
                    case CREATOR_FILTER_TYPE:
                        filter.condition = VIEW_CONDITION.INCLUDE
                        filter['@type'] = CREATOR_FILTER_TYPE
                        filter.pipelineName = ''
                        filter.labelIds = []
                        break
                    default:
                        filter['@type'] = FILTER_BY_LABEL
                        filter.groupId = filter.id
                        filter.pipelineName = ''
                        filter.userIds = []
                }
            },
            addFilters (index) {
                this.model.filters.splice(index + 1, 0, {
                    key: hashID(),
                    ...defaultFilter
                })
            },
            removeFilter ({ $index }) {
                if (this.model.filters.length === 1) {
                    this.model.filters.splice($index, 1, {
                        ...defaultFilter,
                        key: hashID()
                    })
                } else {
                    this.model.filters.splice($index, 1)
                }
            },
            async handleSubmit () {
                if (this.isSubmiting) return
                let message = this.$t('pipelineCountEditSuccess', [this.group.name])
                let theme = 'success'
                try {
                    this.isSubmiting = true
                    const pipelineIds = Array.from(this.model.pipelineIds)
                    await this.updatePipelineGroup({
                        projectId: this.$route.params.projectId,
                        ...this.group,
                        ...this.model,
                        pipelineIds: pipelineIds
                    })
                    this.requestGetGroupLists(this.$route.params)
                    this.handleClose()
                    this.$emit('done')
                } catch (error) {
                    message = error.message || error
                    theme = 'error'
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
                    filters: [{
                        ...defaultFilter
                    }],
                    logic: 'AND'
                }
                this.inited = false
                this.isFilterChange = false
                this.previewTime = null
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
        .bk-dialog-content {
          height: calc(80vh - 50px);
        }
        .bk-dialog-tool {
            display: none;
        }
        .bk-dialog-body {
            padding: 0;
            position: relative;
        }
        .pipeline-group-edit-dialog-main {
            display: flex;
            overflow: hidden;
            position: absolute !important;
            left: 0;
            top: 0;
            right: 0;
            bottom: 0;
            .pipeline-group-edit-source,
            .pipeline-group-edit-preview {
                display: flex;
                flex-direction: column;
                flex: 2;
                background: #F5F7FA;
                overflow: hidden;
                padding: 24px;
                &.pipeline-group-edit-source {
                    border-right: 1px solid #DCDEE5;
                    background: white;
                    flex: 3;
                }
                > header {
                    display: flex;
                    align-items: center;
                    color: #313238;
                    padding: 16px 0;
                    .pipeline-preview-time {
                        display: flex;
                        color: #979BA5;
                        font-size: 12px;
                        margin-left: 10px;
                    }
                }

                > article {
                    position: relative;
                    flex: 1;
                    display: flex;
                    flex-direction: column;
                    overflow: hidden;
                    .preview-pipeline-title {
                        display: flex;
                        align-items: center;
                        justify-content: flex-start;
                        font-size: 12px;
                        font-weight: bold;
                        .pipeline-total-count {
                            color: $primaryColor;
                        }
                        .pipeline-add-count {
                            color: $successColor;
                        }
                        .pipeline-removed-count {
                            color: $dangerColor;
                        }
                    }
                    .pipeline-group-tree-search {
                        margin: 8px 0;
                    }
                    .pipeline-group-tree {
                        overflow: auto;
                        flex: 1;
                        padding-right: 20px;
                        .pipeline-group-tree-node {
                            display: flex;
                            align-items: center;

                            font-size: 12px;
                            height: 32px;
                            &.is-delete {
                                justify-content: space-between;
                                .bk-checkbox-text {
                                    text-decoration: line-through;
                                }
                            }
                            // TODO: ugly overwrite
                            .last-checked.is-checked {
                                .bk-checkbox {
                                    border-color: #8F9DF6;
                                    background-color: #8F9DF6;
                                }
                            }

                            .pipeline-group-tree-node-checkbox {
                                display: inline-flex;
                                .bk-checkbox-text {
                                    @include ellipsis();
                                    flex: 1;
                                }
                            }
                            .pipeline-group-tree-node-desc {
                                color: #c4c4c4;
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
                            .filter-operations-span {
                                width: 40px;
                                display: flex;
                                align-items: center;
                                justify-content: space-between;
                            }
                        }
                    }
                }

                .preview-pipeline-ul {
                    margin: 16px 0;
                    overflow: auto;
                    > li {
                        height: 48px;
                        display: flex;
                        align-items: center;
                        border-bottom: 1px solid #DCDEE5;
                        padding: 0 16px;
                        background: white;
                        .pipeline-group-tooltips {
                            display: flex;
                            flex: 1;
                            overflow: hidden;
                            > div {
                                width: 100%;
                            }
                        }
                        .pipeline-group-left-side {
                            display: flex;
                            flex-direction: column;
                            flex: 1;
                            overflow: hidden;
                            .pipeline-classify-tag {
                                margin: 0;
                            }
                            > p,
                            > footer {
                                font-size: 12px;
                                line-height: 20px;
                                @include ellipsis();
                            }

                            > footer {
                                color: #979BA5;
                            }
                        }
                        .pipeline-operate-btn {
                            display: none;
                            cursor: pointer;
                        }
                        &:last-child {
                            border: 0;
                        }
                        &:hover {
                            background-color: #E1ECFF;
                            .pipeline-operate-btn {
                                display: flex;
                            }
                        }
                    }

                }
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
                    .group-strategy-radio {
                        border-bottom: 1px dashed;
                    }
                }
            }
        }
    }

</style>
