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
                <bk-input v-model="filterData.commitMsg" class="filter-item w300" placeholder="Commit message"></bk-input>
                <bk-input v-model="filterData.triggerUser" class="filter-item w300" placeholder="Actor"></bk-input>
                <bk-select v-model="filterData.branch"
                    class="filter-item"
                    placeholder="Branch"
                    multiple
                    searchable
                    :loading="isLoadingBranch"
                    :remote-method="remoteGetBranchList"
                    @toggle="toggleFilterBranch"
                >
                    <bk-option v-for="option in branchList"
                        :key="option"
                        :id="option"
                        :name="option">
                    </bk-option>
                </bk-select>
                 <bk-select v-model="filterData.event"
                    class="filter-item"
                    placeholder="Event"
                    multiple
                    searchable
                    :loading="isLoadingEvent"
                    @toggle="toggleFilterEvent"
                >
                    <bk-option v-for="event in eventList"
                        :key="event.id"
                        :id="event.id"
                        :name="event.name">
                    </bk-option>
                </bk-select>
                <bk-select v-model="filterData[filter.id]" v-for="filter in filterList" :key="filter.id" class="filter-item" :placeholder="filter.placeholder" multiple>
                    <bk-option v-for="option in filter.data"
                        :key="option.id"
                        :id="option.id"
                        :name="option.name">
                    </bk-option>
                </bk-select>
                <bk-select
                    class="filter-item"
                    placeholder="Pipeline"
                    multiple
                    searchable
                    v-model="filterData.pipelineIds"
                    v-if="!curPipeline.pipelineId"
                    :loading="isLoadingPipeline"
                    :remote-method="remoteGetPipelineList"
                    @toggle="toggleFilterPipeline"
                >
                    <bk-option v-for="option in pipelineList"
                        :key="option.pipelineId"
                        :id="option.pipelineId"
                        :name="option.displayName">
                    </bk-option>
                </bk-select>
                <bk-button @click="resetFilter">Reset</bk-button>
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
                <bk-table-column label="Commit message">
                    <template slot-scope="props">
                        <section class="commit-message">
                            <i :class="getIconClass(props.row.buildHistory.status)"></i>
                            <p>
                                <span class="message">{{ props.row.gitRequestEvent.buildTitle }}</span>
                                <span class="info">{{ props.row.displayName }} #{{ props.row.buildHistory.buildNum }}：{{ props.row.reason }}</span>
                            </p>
                        </section>
                    </template>
                </bk-table-column>
                <bk-table-column label="Branch" width="200">
                    <template slot-scope="props">
                        <span>{{ props.row.gitRequestEvent.branch }}</span>
                    </template>
                </bk-table-column>
                <bk-table-column label="Consume" width="200">
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
                                v-if="['RUNNING', 'PREPARE_ENV', 'QUEUE', 'LOOP_WAITING', 'CALL_WAITING'].includes(props.row.buildHistory.status)"
                                v-bk-tooltips="computedOptToolTip"
                                :class="{ disabled: !curPipeline.enabled || !permission }"
                            >Cancel build</li>
                            <li @click="rebuild(props.row)" v-else :class="{ disabled: !curPipeline.enabled || !permission }" v-bk-tooltips="computedOptToolTip">Re-build</li>
                        </opt-menu>
                    </template>
                </bk-table-column>
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

        <bk-sideslider @hidden="hidden" :is-show.sync="showTriggle" :width="622" :quick-close="true" title="Trigger a custom build">
            <bk-form :model="formData" ref="triggleForm" :label-width="500" slot="content" class="triggle-form" form-type="vertical">
                <bk-form-item label="Branch" :required="true" :rules="[requireRule('Branch')]" property="branch" error-display-type="normal">
                    <bk-select v-model="formData.branch"
                        :clearable="false"
                        :loading="isLoadingBranch"
                        :remote-method="remoteGetBranchList"
                        searchable
                        @toggle="toggleFilterBranch"
                        @selected="selectBranch"
                        placeholder="Select a Branch"
                    >
                        <bk-option v-for="option in branchList"
                            :key="option"
                            :id="option"
                            :name="option">
                        </bk-option>
                    </bk-select>
                </bk-form-item>
                <bk-form-item class="mt15">
                    <bk-checkbox v-model="formData.useCommitId" @change="getPipelineBranchYaml">Use a commit history to trigger this build</bk-checkbox>
                </bk-form-item>
                <bk-form-item label="Commit" :required="true" :rules="[requireRule('Commit')]" property="commitId" error-display-type="normal" v-if="formData.useCommitId">
                    <bk-tag-input placeholder="Select a Commit"
                        v-model="formData.commitId"
                        @change="getPipelineBranchYaml"
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
                <bk-form-item label="Custom Build Message" :required="true" :rules="[requireRule('Message')]" property="customCommitMsg" desc="Custom build message will not affect your commit history." error-display-type="normal">
                    <bk-input v-model="formData.customCommitMsg" placeholder="Please enter build message"></bk-input>
                </bk-form-item>
                <bk-form-item label="Yaml" ref="codeSection" property="yaml" :required="true" :rules="[requireRule('yaml'), checkYaml]" error-display-type="normal" v-bkloading="{ isLoading: isLoadingYaml }">
                    <code-section @blur="$refs.codeSection.validate('blur')"
                        @focus="$refs.codeSection.clearValidator()"
                        :code.sync="formData.yaml"
                        :cursor-blink-rate="530"
                        :read-only="false"
                        ref="codeEditor"
                    />
                </bk-form-item>
                <bk-form-item>
                    <bk-button ext-cls="mr5" theme="primary" title="Submit" @click.stop.prevent="submitData" :loading="isTriggering">Submit</bk-button>
                    <bk-button ext-cls="mr5" title="Cancel" @click="hidden" :disabled="isTriggering">Cancel</bk-button>
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

    export default {
        components: {
            optMenu,
            codeSection
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
            return {
                buildList: [],
                compactPaging: {
                    limit: 10,
                    current: this.$route.query.page,
                    count: 0
                },
                filterData: {
                    commitMsg: '',
                    triggerUser: [],
                    branch: [],
                    event: [],
                    status: [],
                    pipelineIds: []
                },
                branchList: [],
                filterList: [
                    {
                        id: 'status',
                        placeholder: 'Status',
                        data: [
                            { name: 'Succeed', id: 'SUCCEED' },
                            { name: 'Failed', id: 'FAILED' },
                            { name: 'Canceled', id: 'CANCELED' }
                        ]
                    }
                ],
                isLoading: false,
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
                    yaml: ''
                },
                triggerCommits: [],
                checkYaml: validateRule.checkYaml,
                pipelineList: [],
                isLoadingPipeline: false,
                isLoadingEvent: false,
                eventList: []
            }
        },

        computed: {
            ...mapState(['curPipeline', 'projectId', 'projectInfo', 'permission']),

            computedOptToolTip () {
                return {
                    content: !this.curPipeline.enabled ? 'Pipeline disabled' : 'Permission denied',
                    disabled: this.curPipeline.enabled && this.permission
                }
            }
        },

        watch: {
            curPipeline: {
                handler () {
                    this.cleanFilterData()
                    this.initBuildData()
                }
            },
            filterData: {
                handler () {
                    this.initBuildData()
                },
                deep: true
            }
        },

        created () {
            this.initBuildData()
            this.loopGetList()
            this.setHtmlTitle()
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
                return [getPipelineStatusClass(status), ...getPipelineStatusCircleIconCls(status)]
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
                this.$router.push({ query: { page: 1 } })
                this.compactPaging.current = 1
                this.filterData = {
                    commitMsg: '',
                    triggerUser: [],
                    branch: [],
                    event: [],
                    status: [],
                    pipelineIds: []
                }
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
                this.compactPaging.current = page
                this.initBuildData()
            },

            loopGetList () {
                register.installWsMessage(this.getBuildData, 'IFRAMEprocess', 'history')
            },

            getBuildData () {
                const params = {
                    page: this.compactPaging.current,
                    pageSize: this.compactPaging.limit,
                    pipelineId: this.curPipeline.pipelineId,
                    ...this.filterData
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

            hidden () {
                this.showTriggle = false
                this.triggerCommits = []
                this.triggerBranches = []
                this.formData = {
                    branch: '',
                    useCommitId: false,
                    commitId: [],
                    customCommitMsg: '',
                    yaml: ''
                }
            },

            selectBranch () {
                this.getBranchCommits()
                this.getPipelineBranchYaml()
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
                this.$refs.triggleForm.validate().then(() => {
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
                    this.$bkMessage({ theme: 'error', message: err.content || err })
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
                    triggerUser: [],
                    branch: [],
                    event: [],
                    status: [],
                    pipelineIds: []
                }
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
                &.icon-exclamation, &.icon-exclamation-triangle, &.icon-clock {
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
    }
</style>
