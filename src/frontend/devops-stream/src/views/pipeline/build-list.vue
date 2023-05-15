<template>
    <article class="pipelines-main">
        <header class="main-head section-box">
            <span class="head-text">
                <span class="pipeline-name text-ellipsis" v-bk-overflow-tips>{{ curPipeline.displayName }}</span>
                <span class="yml-name text-ellipsis" @click="goToGit" v-if="curPipeline.filePath">
                    <span v-bk-overflow-tips>{{ curPipeline.filePath }}</span>
                    <icon name="cc-jump-link" size="16"></icon>
                </span>
                <span class="pipeline-status" v-if="!curPipeline.enabled">{{$t('pipeline.disabled')}}</span>
            </span>

            <section v-if="curPipeline.pipelineId" class="head-options">
                <div v-bk-tooltips="computedOptToolTip" class="nav-button">
                    <bk-button @click="showTriggleBuild"
                        :disabled="!curPipeline.enabled || !permission"
                        size="small"
                        class="options-btn"
                    >{{$t('pipeline.triggerBuild')}}</bk-button>
                </div>
                <opt-menu>
                    <li @click="togglePipelineEnable"
                        :class="{ disabled: !permission }"
                        v-bk-tooltips="{ content: $t('exception.permissionDeny'), disabled: permission }"
                    >{{ curPipeline.enabled ? $t('pipeline.disablePipeline') : $t('pipeline.enablePipeline') }}</li>
                </opt-menu>
            </section>
        </header>

        <section class="main-body section-box">
            <section class="build-filter">
                <bk-input
                    v-model="filterData.commitMsg"
                    class="filter-item w300"
                    :placeholder="$t('pipeline.commitMsgWithEnter')"
                    @enter="handleFilterChange"
                ></bk-input>
                <bk-input
                    v-model="filterData.triggerUser"
                    name="triggerUser"
                    class="filter-item w300"
                    :placeholder="$t('pipeline.actor')"
                    @change="handleFilterChange"
                ></bk-input>
                <bk-select
                    v-model="filterData.branch"
                    class="filter-item"
                    :placeholder="$t('pipeline.branch')"
                    multiple
                    searchable
                    :loading="isLoadingBuildBranch"
                    :remote-method="remoteGetBuildBranchList"
                    @toggle="toggleFilterBuildBranch"
                    @change="handleFilterChange"
                >
                    <bk-option v-for="option in buildBranchList"
                        :key="option"
                        :id="option"
                        :name="option">
                    </bk-option>
                </bk-select>
                 <bk-select
                    v-model="filterData.event"
                    class="filter-item"
                    :placeholder="$t('pipeline.event')"
                    :loading="isLoadingEvent"
                    multiple
                    searchable
                    @toggle="toggleFilterEvent"
                    @change="handleFilterChange"
                >
                    <bk-option
                        v-for="event in eventList"
                        :key="event.id"
                        :id="event.id"
                        :name="event.name">
                    </bk-option>
                </bk-select>
                <bk-select
                    v-for="filter in filterList"
                    :key="filter.key"
                    :placeholder="filter.placeholder"
                    class="filter-item"
                    multiple
                    @change="(val) => handleStatusChange(val, filter.id)"
                >
                    <bk-option
                        v-for="option in filter.data"
                        :key="option.id"
                        :id="option.id"
                        :name="option.name">
                    </bk-option>
                </bk-select>
                <bk-select
                    class="filter-item"
                    :placeholder="$t('pipeline.pipeline')"
                    multiple
                    searchable
                    v-model="filterData.pipelineIds"
                    v-if="!curPipeline.pipelineId"
                    :loading="isLoadingPipeline"
                    :remote-method="remoteGetPipelineList"
                    @toggle="toggleFilterPipeline"
                    @change="handleFilterChange"
                >
                    <bk-option v-for="option in pipelineList"
                        :key="option.pipelineId"
                        :id="option.pipelineId"
                        :name="option.displayName">
                    </bk-option>
                </bk-select>
                <bk-button @click="resetFilter">{{$t('reset')}}</bk-button>
            </section>

            <bk-table :data="buildList"
                :header-cell-style="{ background: '#fafbfd' }"
                :outer-border="false"
                :header-border="false"
                v-bkloading="{ isLoading }"
                @row-click="goToBuildDetail"
                class="build-table"
                size="large"
            >
                <bk-table-column :label="$t('pipeline.commitMsg')">
                    <template slot-scope="props">
                        <section class="commit-message">
                            <i :class="getIconClass(props.row.buildHistory.status)"></i>
                            <p class="content">
                                <span class="message">{{ props.row.gitRequestEvent.buildTitle }}</span>
                                <span class="info">{{ props.row.displayName }} #{{ props.row.buildHistory.buildNum }}：{{ props.row.reason }}</span>
                            </p>
                        </section>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('pipeline.branch')" width="200" show-overflow-tooltip>
                    <template slot-scope="props">
                        <span>{{ props.row.gitRequestEvent.branch }}</span>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('pipeline.consume')" width="200">
                    <template slot-scope="props">
                        <p class="consume">
                            <span class="consume-item"><i class="bk-icon icon-clock"></i>{{ props.row.buildHistory.executeTime | totalFliter }}</span>
                            <span class="consume-item"><i class="bk-icon icon-calendar"></i>{{ props.row.buildHistory.startTime | timeFilter }}</span>
                        </p>
                    </template>
                </bk-table-column>
                <bk-table-column width="150" class-name="handler-btn">
                    <template slot-scope="props">
                        <opt-menu>
                            <li @click="cancelBuild(props.row)"
                                v-if="['RUNNING', 'PREPARE_ENV', 'QUEUE', 'LOOP_WAITING', 'CALL_WAITING', 'REVIEWING', 'TRIGGER_REVIEWING'].includes(props.row.buildHistory.status)"
                                v-bk-tooltips="computedOptToolTip"
                                :class="{ disabled: !curPipeline.enabled || !permission }"
                            >{{$t('pipeline.cancelBuild')}}</li>
                            <li @click="rebuild(props.row)" v-else :class="{ disabled: !curPipeline.enabled || !permission }" v-bk-tooltips="computedOptToolTip">{{$t('pipeline.rebuild')}}</li>
                        </opt-menu>
                    </template>
                </bk-table-column>
                <template #empty>
                    <EmptyTableStatus :type="emptyType" @clear="resetFilter" />
                </template>
            </bk-table>
            <bk-pagination small
                :current.sync="compactPaging.current"
                :count.sync="compactPaging.count"
                :limit="compactPaging.limit"
                :show-limit="false"
                @change="pageChange"
                class="build-paging"
            />
        </section>

        <bk-sideslider @hidden="hidden" :is-show.sync="showTriggle" :width="triggleWidth" :quick-close="true" :title="$t('pipeline.triggerTitle')">
            <bk-form :model="formData" ref="triggleForm" :label-width="500" slot="content" class="triggle-form" form-type="vertical">
                <bk-form-item :label="$t('pipeline.branch')" :required="true" :rules="[requireRule($t('pipeline.branch'))]" property="branch" error-display-type="normal">
                    <bk-select v-model="formData.branch"
                        :clearable="false"
                        :loading="isLoadingBranch"
                        :remote-method="remoteGetBranchList"
                        searchable
                        @toggle="toggleFilterBranch"
                        @selected="selectBranch"
                        :placeholder="$t('pipeline.branchSelectPlaceholder')"
                    >
                        <bk-option v-for="option in branchList"
                            :key="option"
                            :id="option"
                            :name="option">
                        </bk-option>
                    </bk-select>
                </bk-form-item>
                <template v-if="disableManual">
                    <bk-form-item v-if="yamlErrorMessage">
                        <bk-alert
                            type="error"
                            :title="yamlErrorMessage"
                        ></bk-alert>
                    </bk-form-item>
                    <bk-form-item v-else>
                        <bk-alert
                            type="warning"
                            :title="$t('pipeline.noYamlOrDisable')"
                        ></bk-alert>
                    </bk-form-item>
                </template>
                <template v-else>
                    <bk-form-item class="mt15">
                        <bk-checkbox v-model="formData.useCommitId" @change="getPipelineParams">{{$t('pipeline.commitTriggerTips')}}</bk-checkbox>
                    </bk-form-item>
                    <bk-form-item :label="$t('pipeline.commit')" :required="true" :rules="[requireRule($t('pipeline.commit'))]" property="commitId" error-display-type="normal" v-if="formData.useCommitId">
                        <bk-tag-input :placeholder="$t('pipeline.commitPlaceholder')"
                            v-model="formData.commitId"
                            @change="getPipelineParams"
                            :max-data="1"
                            :loading="isLoadingCommit"
                            :list="triggerCommits"
                            :tpl="renderCommitList"
                            tooltip-key="message"
                            allow-create
                            save-key="id"
                            display-key="message"
                            search-key="message"
                            trigger="focus"
                        >
                        </bk-tag-input>
                    </bk-form-item>
                    <bk-form-item :label="$t('pipeline.buildMsg')" :required="true" :rules="[requireRule($t('pipeline.buildMsg'))]" property="customCommitMsg" :desc="$t('pipeline.buildMsgTips')" error-display-type="normal">
                        <bk-input v-model="formData.customCommitMsg" :placeholder="$t('pipeline.buildMsgPlaceholder')"></bk-input>
                    </bk-form-item>
                    <!-- <bk-form-item
                        label="Yaml"
                        ref="codeSection"
                        property="yaml"
                        error-display-type="normal"
                        class="mb8"
                        :required="true"
                        :rules="[requireRule('yaml'), checkYaml]"
                        v-bkloading="{ isLoading: isLoadingYaml }"
                    >
                        <code-section @blur="$refs.codeSection.validate('blur')"
                            @focus="$refs.codeSection.clearValidator()"
                            :code.sync="formData.yaml"
                            :cursor-blink-rate="530"
                            :read-only="false"
                            ref="codeEditor"
                        />
                    </bk-form-item> -->
                    <bk-form-item
                        v-if="uiFormSchema && Object.keys(uiFormSchema).length && uiFormSchema.properties && Object.keys(uiFormSchema.properties).length"
                        :label="$t('pipeline.variable')"
                    >
                        <bk-ui-form
                            class="ui-form"
                            v-bkloading="{ isLoading: isLoadingSchema }"
                            v-model="formData.inputs"
                            ref="bkUiForm"
                            :schema="uiFormSchema"
                            :layout="uiFormLayout"
                            :rules="uiFormRules"
                        />
                    </bk-form-item>
                </template>
                <bk-form-item>
                    <bk-button
                        ext-cls="mr5"
                        theme="primary"
                        :disabled="disableManual"
                        @click.stop.prevent="submitData"
                        :loading="isTriggering"
                    >{{$t('submit')}}</bk-button>
                    <bk-button
                        ext-cls="mr5"
                        :disabled="isTriggering || disableManual"
                        @click="hidden"
                    >{{$t('cancel')}}</bk-button>
                </bk-form-item>
            </bk-form>
        </bk-sideslider>
    </article>
</template>

<script>
    import { mapState, mapActions } from 'vuex'
    import { pipelines } from '@/http'
    import {
        goYaml,
        preciseDiff,
        timeFormatter,
        modifyHtmlTitle,
        debounce
    } from '@/utils'
    import optMenu from '@/components/opt-menu'
    import codeSection from '@/components/code-section'
    import { getPipelineStatusClass, getPipelineStatusCircleIconCls } from '@/components/status'
    import register from '@/utils/websocket-register'
    import validateRule from '@/utils/validate-rule'
    import createForm from '@blueking/bkui-form'
    import '@blueking/bkui-form/dist/bkui-form.css'
    import UiTips from '@/components/ui-form/tips.vue'
    import UiSelector from '@/components/ui-form/selector.vue'
    import EmptyTableStatus from '@/components/empty-table-status'
    const BkUiForm = createForm({
        components: {
            tips: UiTips,
            selector: UiSelector
        }
    })

    export default {
        components: {
            optMenu,
            codeSection,
            BkUiForm,
            EmptyTableStatus
        },

        filters: {
            timeFilter (val) {
                return timeFormatter(val)
            },

            totalFliter (val) {
                return preciseDiff(val)
            }
        },

        data () {
            const { commitMsg, triggerUser, branch, event, status, pipelineIds } = this.$route.query
            const getFilterData = () => {
                return {
                    commitMsg: commitMsg || '',
                    triggerUser: triggerUser || '',
                    branch: (branch && branch.split(',')) || [],
                    event: (event && event.split(',')) || [],
                    status: (status && status.split(',')) || [],
                    pipelineIds: (pipelineIds && pipelineIds.split(',')) || []
                }
            }
            return {
                buildList: [],
                compactPaging: {
                    limit: 10,
                    current: +this.$route.query.page || 1,
                    count: 0
                },
                filterData: getFilterData(),
                buildBranchList: [],
                branchList: [],
                filterList: [
                    {
                        id: 'status',
                        key: new Date().getSeconds(),
                        placeholder: this.$t('status'),
                        data: [
                            { name: this.$t('pipeline.succeed'), val: ['SUCCEED'], id: 'succeed' },
                            { name: this.$t('pipeline.failed'), val: ['FAILED'], id: 'failed' },
                            { name: this.$t('pipeline.canceled'), val: ['CANCELED'], id: 'canceled' },
                            { name: this.$t('pipeline.queue'), val: ['QUEUE', 'QUEUE_CACHE'], id: 'queue' },
                            { name: this.$t('pipeline.queueTimeout'), val: ['QUEUE_TIMEOUT'], id: 'queueTimeout' },
                            { name: this.$t('pipeline.running'), val: ['RUNNING'], id: 'running' },
                            { name: this.$t('pipeline.reviewing'), val: ['REVIEWING', 'TRIGGER_REVIEWING'], id: 'reviewing' },
                            { name: this.$t('pipeline.stageSuccess'), val: ['STAGE_SUCCESS'], id: 'stageSuccess' },
                        ]
                    }
                ],
                isLoading: false,
                isLoadingBuildBranch: false,
                isLoadingBranch: false,
                isLoadingCommit: false,
                isLoadingYaml: false,
                isTriggering: false,
                showTriggle: false,
                formData: {
                    branch: '',
                    useCommitId: false,
                    commitId: [],
                    customCommitMsg: '',
                    yaml: '',
                    inputs: {}
                },
                triggerCommits: [],
                checkYaml: validateRule.checkYaml,
                pipelineList: [],
                isLoadingPipeline: false,
                isLoadingEvent: false,
                eventList: [],
                isLoadingSchema: false,
                disableManual: false,
                uiFormSchema: {},
                uiFormLayout: {
                    container: {
                        gap: '8px'
                    }
                },
                uiFormRules: {
                    required: {
                        validator: "{{ $self.value !== undefined && !!String($self.value).length }}",
                        message: this.$t('pipeline.required')
                    }
                },
                emptyYaml: false,
                yamlErrorMessage: ''
            }
        },

        beforeRouteEnter (to, from, next) {
            next((vm) => {
                vm.initBuildData()
            })
        },

        computed: {
            ...mapState(['curPipeline', 'projectId', 'projectInfo', 'permission']),

            computedOptToolTip () {
                return {
                    content: !this.curPipeline.enabled ? this.$t('pipeline.pipelineDisabled') : this.$t('exception.permissionDeny'),
                    disabled: this.curPipeline.enabled && this.permission
                }
            },

            triggleWidth () {
                return window.innerWidth * 0.8
            },

            defaultBranch () {
                return this.projectInfo.default_branch || ''
            },

            emptyType () {
                return (
                    this.filterData.commitMsg
                    || this.filterData.triggerUser
                    || this.filterData.branch.length
                    || this.filterData.event.length
                    || this.filterData.status.length
                    || this.filterData.pipelineIds.length
                )
                ? 'search-empty'
                : 'empty'
            }
        },

        watch: {
            curPipeline: {
                handler (newVal, oldVal) {
                    if (Object.keys(oldVal).length) this.cleanFilterData()
                    this.initBuildData()
                }
            }
        },

        created () {
            this.loopGetList()
            this.setHtmlTitle()
            this.toggleFilterEvent(true)
            this.toggleFilterBuildBranch(true)
            this.toggleFilterPipeline(true)
        },

        beforeDestroy () {
            register.unInstallWsMessage('history')
        },

        methods: {
            ...mapActions(['setCurPipeline']),

            setHtmlTitle () {
                modifyHtmlTitle(this.curPipeline?.displayName)
            },

            getIconClass (status) {
                return [getPipelineStatusClass(status), ...getPipelineStatusCircleIconCls(status), 'statuc-icon']
            },

            handleStatusChange (val, id) {
                const filter = this.filterList.find(filter => filter.id === id)
                const options = filter.data.filter(data => val.includes(data.id))
                this.filterData[id] = options.map(opstion => opstion.val).flat()
                this.handleFilterChange()
            },

            handleFilterChange () {
                this.initBuildData()
                const query = { page: 1 }
                Object.keys(this.filterData).forEach(key => {
                    if (this.filterData[key].length && typeof this.filterData[key] === 'string') {
                        query[key] = this.filterData[key]
                    } else if (this.filterData[key].length && Array.isArray(this.filterData[key])) {
                        query[key] = this.filterData[key].join(',')
                    }
                })
                this.$router.replace({ query })
            },

            toggleFilterBranch (isOpen) {
                if (isOpen) {
                    this.isLoadingBranch = true
                    this.getPipelineBranchApi().then((branchList) => {
                        this.branchList = branchList
                        this.isLoadingBranch = false
                    })
                }
            },

            toggleFilterBuildBranch (isOpen) {
                if (isOpen) {
                    this.isLoadingBuildBranch = true
                    this.getPipelineBuildBranchApi().then((branchList) => {
                        this.buildBranchList = branchList
                        this.isLoadingBuildBranch = false
                    })
                }
            },

            remoteGetBuildBranchList (search) {
                return new Promise((resolve) => {
                    debounce(() => {
                        this.getPipelineBuildBranchApi({ search }).then((branchList) => {
                            this.buildBranchList = branchList
                            resolve()
                        })
                    })
                })
            },

            remoteGetBranchList (search) {
                return new Promise((resolve) => {
                    debounce(() => {
                        this.getPipelineBranchApi({ search }).then((branchList) => {
                            this.branchList = branchList
                            resolve()
                        })
                    })
                })
            },

            toggleFilterEvent (isOpen) {
                if (isOpen) {
                    this.isLoadingEvent = true
                    pipelines.getEventList().then((res) => {
                        this.eventList = res || []
                    }).catch((err) => {
                        this.messageError(err.message || err)
                    }).finally(() => {
                        this.isLoadingEvent = false
                    })
                }
            },

            toggleFilterPipeline (isOpen) {
                if (isOpen) {
                    this.isLoadingPipeline = true
                    this.getPipelineList().then((pipelineList) => {
                        this.pipelineList = pipelineList
                        this.isLoadingPipeline = false
                    })
                }
            },

            remoteGetPipelineList (keyword) {
                return new Promise((resolve) => {
                    debounce(() => {
                        this.getPipelineList({ keyword }).then((pipelineList) => {
                            this.pipelineList = pipelineList
                            resolve()
                        })
                    })
                })
            },

            getPipelineList (query) {
                const params = {
                    page: 1,
                    pageSize: 100,
                    projectId: this.projectId,
                    ...query
                }
                return new Promise((resolve, reject) => {
                    pipelines.getPipelineInfoList(params).then((res) => {
                        resolve(res || [])
                    }).catch((err) => {
                        resolve()
                        this.$bkMessage({ theme: 'error', message: err.message || err })
                    })
                })
            },

            cleanFilterData () {
                this.compactPaging.current = 1
                this.filterData = {
                    commitMsg: '',
                    triggerUser: '',
                    branch: [],
                    event: [],
                    status: [],
                    pipelineIds: []
                }
                this.handleFilterChange()
            },

            initBuildData () {
                this.isLoading = true
                return this.getBuildData().catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isLoading = false
                })
            },

            pageChange (page) {
                this.$router.push({
                    query: {
                        page
                    }
                })
                this.compactPaging.current = +page
                this.initBuildData()
            },

            loopGetList () {
                register.installWsMessage(this.getBuildData, 'IFRAMEprocess', 'history')
            },

            getBuildData () {
                let { triggerUser } = this.filterData
                triggerUser = triggerUser ? triggerUser.split(',') : []
                const params = {
                    page: this.compactPaging.current,
                    pageSize: this.compactPaging.limit,
                    pipelineId: this.curPipeline.pipelineId,
                    ...this.filterData,
                    triggerUser
                }
                return pipelines.getPipelineBuildList(this.projectId, params).then((res = {}) => {
                    this.buildList = (res.records || []).map((build) => {
                        return {
                            ...build,
                            buildHistory: build.buildHistory || {},
                            gitRequestEvent: build.gitRequestEvent || {}
                        }
                    })
                    this.compactPaging.count = res.count
                })
            },

            togglePipelineEnable () {
                if (!this.permission) return
                this.clickEmpty()
                pipelines.toggleEnablePipeline(this.projectId, this.curPipeline.pipelineId, !this.curPipeline.enabled).then(() => {
                    const pipeline = {
                        ...this.curPipeline,
                        enabled: !this.curPipeline.enabled
                    }
                    this.setCurPipeline(pipeline)
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                })
            },

            showTriggleBuild () {
                if (!this.curPipeline.enabled || !this.permission) return
                if (this.defaultBranch) {
                    this.formData.branch = this.defaultBranch
                    this.branchList = [this.defaultBranch]
                    this.getBranchCommits()
                    this.getPipelineParams()
                }
                this.showTriggle = true
            },

            getPipelineBranchApi (query = {}) {
                const params = {
                    page: 1,
                    perPage: 100,
                    projectId: this.projectId,
                    ...query
                }
                return new Promise((resolve, reject) => {
                    pipelines.getPipelineBranches(params).then((res) => {
                        resolve(res || [])
                    }).catch((err) => {
                        resolve()
                        this.$bkMessage({ theme: 'error', message: err.message || err })
                    })
                })
            },

            getPipelineBuildBranchApi (query = {}) {
                const params = {
                    page: 1,
                    perPage: 100,
                    projectId: this.projectId,
                    ...query
                }
                return new Promise((resolve, reject) => {
                    pipelines.getPipelineBuildBranches(params).then((res) => {
                        resolve(res || [])
                    }).catch((err) => {
                        resolve()
                        this.$bkMessage({ theme: 'error', message: err.message || err })
                    })
                })
            },

            hidden () {
                this.showTriggle = false
                this.triggerCommits = []
                this.triggerBranches = []
                this.uiFormSchema = {}
                this.formData = {
                    branch: '',
                    useCommitId: false,
                    commitId: [],
                    customCommitMsg: '',
                    yaml: ''
                }
                this.yamlErrorMessage = ''
                this.emptyYaml = false
                this.disableManual = false
            },

            selectBranch () {
                this.getBranchCommits()
                // this.getPipelineBranchYaml()
                this.getPipelineParams()
            },

            getBranchCommits (value, options, query = {}) {
                const params = {
                    page: 1,
                    perPage: 100,
                    projectId: this.projectId,
                    branch: this.formData.branch,
                    ...query
                }
                this.formData.commitId = []
                this.isLoadingCommit = true
                return pipelines.getPipelineCommits(params).then((res) => {
                    this.triggerCommits = res || []
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isLoadingCommit = false
                })
            },

            getPipelineBranchYaml () {
                const branchName = this.formData.branch
                const commitId = this.formData.useCommitId ? this.formData.commitId[0] : undefined
                if (!branchName && !commitId) return

                this.isLoadingYaml = true
                return pipelines.getPipelineBranchYaml(this.projectId, this.curPipeline.pipelineId, { branchName, commitId }).then((res) => {
                    this.formData.yaml = res || ''
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isLoadingYaml = false
                })
            },

            getPipelineParams () {
                const branchName = this.formData.branch
                const commitId = this.formData.useCommitId ? this.formData.commitId[0] : undefined
                if (!branchName && !commitId) return

                this.isLoadingSchema = true
                this.yamlErrorMessage = ''
                this.emptyYaml = false
                this.disableManual = false
                return pipelines.getPipelineParamJson(this.projectId, this.curPipeline.pipelineId, { branchName, commitId }).then((res) => {
                    this.uiFormSchema = res.schema || {}
                    this.formData.yaml = res.yaml || ''
                    this.formData.inputs = {}
                    this.disableManual = res.enable === false
                }).catch((err) => {
                    if (err.code === 2129028) {
                        this.emptyYaml = true
                        this.disableManual = true
                    } else if (err.code === 2129029) {
                        this.yamlErrorMessage = err.message
                        this.disableManual = true
                    } else {
                        this.$bkMessage({ theme: 'error', message: err.message || err })
                    }
                }).finally(() => {
                    this.isLoadingSchema = false
                })
            },

            goToGit () {
                goYaml(this.projectInfo.web_url, this.curPipeline.branch, this.curPipeline.filePath)
            },

            cancelBuild (row) {
                if (!this.curPipeline.enabled || !this.permission) return

                this.clickEmpty()
                pipelines.cancelBuildPipeline(this.projectId, row.pipelineId, row.buildHistory.id).then(() => {
                    this.initBuildData()
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                })
            },

            clickEmpty () {
                const button = document.createElement('input')
                button.type = 'button'
                document.body.appendChild(button)
                button.click()
                document.body.removeChild(button)
            },

            rebuild (row) {
                if (!this.curPipeline.enabled || !this.permission) return

                this.clickEmpty()
                pipelines.rebuildPipeline(this.projectId, row.pipelineId, row.buildHistory.id).then(() => {
                    this.initBuildData()
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                })
            },

            submitData () {
                Promise.all([
                    this.$refs.triggleForm.validate(),
                    this.$refs.bkUiForm?.validate()
                ]).then(() => {
                    const postData = {
                        ...this.formData,
                        projectId: this.projectId,
                        commitId: this.formData.useCommitId ? this.formData.commitId[0] : undefined
                    }
                    this.isTriggering = true
                    pipelines.trigglePipeline(this.curPipeline.pipelineId, postData).then(() => {
                        this.$bkMessage({ theme: 'success', message: 'Submitted successfully' })
                        this.hidden()
                        this.initBuildData()
                    }).catch((err) => {
                        this.$bkMessage({ theme: 'error', message: err.message || err })
                    }).finally(() => {
                        this.isTriggering = false
                    })
                }, (err) => {
                    const message = Array.isArray(err) ? `${err[0].path}是${err[0].message}` : (err.content || err.message || err)
                    this.$bkMessage({ theme: 'error', message })
                })
            },

            goToBuildDetail (row) {
                this.$router.push({
                    name: 'buildDetail',
                    params: {
                        buildId: row.buildHistory.id,
                        pipelineId: row.pipelineId
                    },
                    query: this.$route.query
                })
            },

            resetFilter () {
                this.filterData = {
                    commitMsg: '',
                    triggerUser: '',
                    branch: [],
                    event: [],
                    status: [],
                    pipelineIds: []
                }
                this.filterList[0].key = new Date().getSeconds()
                this.handleFilterChange()
            },

            requireRule (name) {
                return {
                    required: true,
                    message: name + this.$t('isRequired'),
                    trigger: 'blur'
                }
            },

            renderCommitList (node) {
                const parentClass = 'bk-selector-node bk-selector-member'
                const textClass = 'text'
                const innerHtml = `[${node.id.slice(0, 9)}]: ${node.message}`
                return (
                    <div class={parentClass}>
                        <span class={textClass} domPropsInnerHTML={innerHtml}></span>
                    </div>
                )
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .pipelines-main {
        padding-left: 16px;
        .main-head {
            height: 50px;
            background: #fff;
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 0 27px;
            .head-text {
                display: flex;
                align-items: center;
            }
            .head-options {
                display: flex;
                align-items: center;
                .options-btn {
                    margin-right: 10px;
                }
            }
            .pipeline-name {
                color: #313328;
                max-width: 300px;
                display: inline-block;
            }
            .yml-name {
                display: inline-block;
                margin: 0 3px 0 16px;
                max-width: 300px;
                cursor: pointer;
            }
            svg {
                vertical-align: sub;
            }
            .pipeline-status {
                background: #fafbfd;
                border: 1px solid rgba(151,155,165,0.30);
                border-radius: 11px;
                display: inline-block;
                margin-left: 16px;
                padding: 0 11px;
                line-height: 20px;
                font-size: 12px;
            }
        }

        .main-body {
            margin-top: 16px;
            height: calc(100% - 66px);
            overflow: auto;
            background: #fff;
            padding: 16px 24px 0;
            .build-filter {
                display: flex;
                align-items: center;
                margin-bottom: 17px;
                .filter-item {
                    width: 200px;
                    margin-right: 8px;
                }
                .w300 {
                    width: 300px;
                }
                /deep/ .user-selector-container {
                    z-index: 5;
                }
            }
            .build-paging {
                margin: 6px 0 0;
                display: flex;
                align-items: center;
                justify-content: center;
                /deep/ span {
                    outline: none;
                    margin-left: 0;
                }
            }
        }
    }
    .build-table {
        .commit-message {
            display: flex;
            align-items: top;
            font-size: 12px;
            .bk-icon, .stream-icon {
                width: 32px;
                height: 32px;
                font-size: 32px;
                margin-right: 8px;
                line-height: 32px;
                &.executing {
                    font-size: 14px;
                }
                &.icon-exclamation, &.icon-exclamation-triangle, &.icon-clock, &.stream-reviewing-2 {
                    font-size: 24px;
                }
                &.running {
                    color: #459fff;
                }
                &.canceled {
                    color: #f6b026;
                }
                &.danger {
                    color: #ff5656;
                }
                &.success {
                    color: #34d97b;
                }
                &.pause {
                    color: #ff9801;
                }
            }
            .content {
                flex: 1;
            }
            .message {
                display: block;
                color: #313328;
                line-height: 24px;
                margin-bottom: 4px;
            }
            .info {
                color: #979ba5;
                line-height: 16px;
            }
        }
        .consume-item {
            display: flex;
            align-items: center;
            font-size: 12px;
            &:first-child {
                margin-bottom: 7px;
            }
            .bk-icon {
                font-size: 14px;
                margin-right: 5px;
            }
            .icon-clock {
                font-size: 15px;
            }
        }
        /deep/ .bk-table-row {
            cursor: pointer;
        }
        /deep/ .handler-btn {
            .cell {
                overflow: visible;
            }
        }
    }
    .triggle-form {
        padding: 20px 30px;
        /deep/ button {
            margin: 8px 10px 0 0;
        }
        /deep/ .bk-tag-selector .bk-tag-input .tag {
            max-width: 500px;
        }
        .ui-form {
            padding: 10px;
            border: 1px solid #c4c6cc;
        }
    }
    .mb8 {
        margin-bottom: 8px;
    }
</style>
