<template>
    <article class="pipeline-list">
        <infinite-scroll class="pipeline-list-wrapper" ref="infiniteScroll" :data-fetcher="requestPipelineList" :page-size="60" scroll-box-class-name="pipeline-list" v-slot="slotProps">
            <template v-if="!slotProps.isLoading">
                <list-empty
                    v-if="!slotProps.list.length"
                    :has-filter="hasFilter"
                    @showCreate="toggleTemplatePopup"
                    @showImport="toggleImportPipelinePopup"
                    @showSlide="showSlide"
                    :has-pipeline="hasPipeline">
                </list-empty>

                <section v-else>
                    <list-create-header
                        :layout="layout"
                        :has-filter="hasFilter"
                        :num="slotProps.totals"
                        @showSlide="showSlide"
                        @changeLayout="changeLayoutType"
                        @changeOrder="changeOrderType"
                        @showCreate="toggleTemplatePopup"
                        @showImport="toggleImportPipelinePopup"
                    >
                    </list-create-header>

                    <section class="pipeline-list-content">
                        <div class="pipeline-list-cards" v-if="layout === 'card'">
                            <task-card
                                v-for="(card, index) of slotProps.list"
                                :has-permission="card.hasPermission"
                                :config="pipelineFeConfMap[card.pipelineId]"
                                :index="index"
                                :key="`taskCard${card.pipelineId}`"
                                :can-manual-startup="card.canManualStartup">
                            </task-card>
                        </div>

                        <div class="pipeline-list-table" v-if="layout === 'table'">
                            <task-table
                                :pipeline-fe-conf-map="pipelineFeConfMap"
                                :list="slotProps.list">
                            </task-table>
                        </div>
                    </section>

                </section>
            </template>
        </infinite-scroll>

        <pipeline-template-popup :toggle-popup="toggleTemplatePopup" :is-show="templatePopupShow"></pipeline-template-popup>
        <import-pipeline-popup :is-show.sync="importPipelinePopupShow"></import-pipeline-popup>

        <pipeline-filter v-if="slideShow" :is-show="slideShow" @showSlide="showSlide" :is-disabled="isDisabled" :selected-filter="currentFilter" @filter="filterCommit" class="pipeline-filter"></pipeline-filter>

        <bk-dialog
            width="800"
            v-model="copyDialogConfig.isShow"
            :title="copyDialogConfig.title"
            :mask-close="false"
            :close-icon="false"
            :auto-close="false"
            header-position="left"
            @confirm="copyConfirmHandler"
            @cancel="copyCancelHandler"
        >
            <template>
                <section class="copy-pipeline bk-form" v-bkloading="{ isLoading: copyConfig.loading }">
                    <div class="bk-form-item">
                        <label class="bk-label">{{ $t('name') }}：</label>
                        <div class="bk-form-content">
                            <input type="text" class="bk-form-input" :placeholder="$t('pipelineNameInputTips')"
                                name="newPipelineName"
                                v-validate="'required|max:40'"
                                v-model="copyConfig.newPipelineName"
                                :class="{
                                    'is-danger': errors.has('newPipelineName')
                                }"
                            >
                            <p class="error-tips" v-if="errors.has('newPipelineName')">{{ $t('pipelineNameInputTips') }}</p>
                        </div>
                    </div>

                    <div class="bk-form-item">
                        <label class="bk-label">{{ $t('desc') }}：</label>
                        <div class="bk-form-content">
                            <input type="text" class="bk-form-input" :placeholder="$t('pipelineDescInputTips')"
                                name="newPipelineDesc"
                                v-model="copyConfig.newPipelineDesc"
                                v-validate.initial="'max:100'"
                                :class="{
                                    'is-danger': errors.has('newPipelineDesc')
                                }"
                            >
                            <p class="error-tips" v-if="errors.has('newPipelineDesc')"> {{ errors.first("newPipelineDesc") }}</p>
                        </div>
                    </div>
                </section>
            </template>
        </bk-dialog>

        <bk-dialog
            width="800"
            v-model="saveAsTemp.isShow"
            :title="saveAsTemp.title"
            :close-icon="false"
            :mask-close="false"
            :auto-close="false"
            header-position="left"
            @confirm="saveAsConfirmHandler"
            @cancel="saveAsCancelHandler">
            <section class="copy-pipeline bk-form" ref="saveAsTemp">
                <div class="bk-form-item">
                    <label class="bk-label">{{ $t('template.name') }}</label>
                    <div class="bk-form-content">
                        <input type="text"
                            class="bk-form-input"
                            :placeholder="$t('template.nameInputTips')"
                            v-model="saveAsTemp.templateName"
                            :class="{ 'is-danger': errors.has('saveTemplateName') }"
                            name="saveTemplateName"
                            v-validate="'required|max:30'"
                            maxlength="30"
                        >
                    </div>
                    <div v-if="errors.has('saveTemplateName')" class="error-tips err-name">{{ $t('template.nameInputTips') }}</div>
                </div>

                <div class="bk-form-item">
                    <label class="bk-label tip-bottom">{{ $t('template.applySetting') }}
                        <span v-bk-tooltips.bottom="$t('template.tipsSetting')" class="bottom-start">
                            <i class="bk-icon icon-info-circle"></i>
                        </span>
                    </label>
                    <div class="bk-form-content">
                        <bk-radio-group v-model="saveAsTemp.isCopySetting">
                            <bk-radio v-for="(entry, key) in copySettings" :key="key" :value="entry.value" class="auth-radio">{{ entry.label }}</bk-radio>
                        </bk-radio-group>
                    </div>
                </div>
            </section>
        </bk-dialog>
    </article>
</template>

<script>
    // import pipelineWebsocket from '@/utils/pipelineWebSocket'
    import webSocketMessage from '@/utils/webSocketMessage'
    import { mapGetters, mapState } from 'vuex'
    import PipelineTemplatePopup from '@/components/pipelineList/PipelineTemplatePopup'
    import ImportPipelinePopup from '@/components/pipelineList/ImportPipelinePopup'
    import { bus } from '@/utils/bus'
    import taskCard from '@/components/pipelineList/taskCard'
    import taskTable from '@/components/pipelineList/taskTable'
    import listEmpty from '@/components/pipelineList/listEmpty'
    import listCreateHeader from '@/components/pipelineList/listCreateHeader'
    import pipelineFilter from '@/components/pipelineList/PipelineFilter'
    import InfiniteScroll from '@/components/InfiniteScroll'
    import {
        convertMStoString,
        convertMStoStringByRule,
        navConfirm
    } from '@/utils/util'

    export default {
        components: {
            'task-card': taskCard,
            'task-table': taskTable,
            'list-create-header': listCreateHeader,
            'list-empty': listEmpty,
            PipelineTemplatePopup,
            pipelineFilter,
            InfiniteScroll,
            ImportPipelinePopup
        },

        data () {
            const layout = localStorage.getItem('pipelineLayout') || 'card'
            const sortType = localStorage.getItem('pipelineSortType') || 'CREATE_TIME'
            return {
                hasTemplatePermission: true,
                templatePopupShow: false,
                importPipelinePopupShow: false,
                responsiveConfig: {
                    wrapper: null,
                    width: 0,
                    cardRealWidth: 0,
                    cardMaxWidth: 0,
                    standardMarginRight: 25,
                    allowCalc: true
                },
                copyDialogConfig: {
                    isShow: false,
                    title: this.$t('newlist.copyPipeline'),
                    closeIcon: false,
                    quickClose: false,
                    padding: '0 20px',
                    pipelineId: ''
                },
                copyConfig: {
                    newPipelineName: '',
                    newPipelineDesc: '',
                    loading: false,
                    newPipelineGroup: '',
                    config: {
                        data: this.groupList,
                        onChange: this.pipelineGroupChange
                    }
                },
                saveAsTemp: {
                    isShow: false,
                    title: this.$t('newlist.saveAsTemp'),
                    closeIcon: false,
                    quickClose: false,
                    padding: '0 20px',
                    pipelineId: '',
                    templateName: '',
                    isCopySetting: true
                },
                copySettings: [
                    { label: this.$t('true'), value: true },
                    { label: this.$t('false'), value: false }
                ],
                tipsSetting: {
                    content: this.$t('template.tipsSetting'),
                    placements: ['right']
                },
                layout,
                sortType,
                slideShow: false,
                isDisabled: false,
                filter: {},
                currentFilter: {},
                pipelineFeConfMap: {}
            }
        },

        computed: {
            ...mapGetters({
                statusMap: 'pipelines/getStatusMap',
                tagGroupList: 'pipelines/getTagGroupList'
            }),
            ...mapState('pipelines', [
                'currentViewId',
                'pageLoading',
                'hasCreatePermission'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            pageType () {
                return this.$route.params.type
            },
            hasFilter () {
                const res = (this.filter && Object.keys(this.filter).length && (this.filter.filterByCreator !== '' || this.filter.filterByPipelineName !== '' || this.filter.filterByLabels !== ''))
                return res
            },
            pipelineList () {
                return this.$refs.infiniteScroll ? this.$refs.infiniteScroll.list : []
            }
        },

        watch: {
            pageType (val) {
                if (val) {
                    this.filter = {}
                    this.currentFilter = {}
                    // this.initPage()
                    this.$nextTick(() => {
                        if (this.$refs.infiniteScroll) {
                            this.$refs.infiniteScroll.fetchData()
                        }
                    })
                }
            }
        },

        created () {
            bus.$on('title-click', (pipelineId) => {
                this.titleClickHandler(pipelineId)
            })

            bus.$on('error-noticed', (pipelineId) => {
                this.errorNoticed(pipelineId)
            })

            bus.$on('triggers-exec', (params, pipelineId) => {
                this.triggersExec(params, pipelineId)
            })

            bus.$on('terminate-pipeline', (pipelineId) => {
                this.terminatePipeline(pipelineId)
            })

            bus.$on('resume-pipeline', (pipelineId) => {
                this.resumePipeline(pipelineId)
            })

            bus.$on('set-permission', (...args) => {
                this.setPermissionConfig(...args)
            })
        },

        mounted () {
            this.initPage()
            webSocketMessage.installWsMessage(this.updatePipelineStatus)
        },

        beforeDestroy () {
            // pipelineWebsocket.disconnect()
            bus.$off('title-click')
            bus.$off('error-noticed')
            bus.$off('triggers-exec')
            bus.$off('terminate-pipeline')
            bus.$off('resume-pipeline')
            bus.$off('set-permission')
            webSocketMessage.unInstallWsMessage()
        },

        methods: {
            async changeLayoutType (val) {
                localStorage.setItem('pipelineLayout', val)
                this.layout = val
            },
            async changeOrderType (val) {
                localStorage.setItem('pipelineSortType', val)
                this.sortType = val
                this.$nextTick(() => {
                    if (this.$refs.infiniteScroll) {
                        this.$refs.infiniteScroll.updateList()
                    }
                })
            },
            async filterCommit (data, currentFilter, needLoad = true) { // needLoad 阻止重复请求流水线列表
                this.filter = data
                this.currentFilter = currentFilter
                if (needLoad && this.$refs.infiniteScroll) {
                    this.$nextTick(() => {
                        this.$refs.infiniteScroll.updateList()
                    })
                }
                this.isDisabled = false
            },
            toggleTemplatePopup (templatePopupShow) {
                if (!this.hasCreatePermission) {
                    this.toggleCreatePermission()
                } else {
                    this.templatePopupShow = templatePopupShow
                }
            },

            toggleImportPipelinePopup (importPipelinePopupShow) {
                this.importPipelinePopupShow = importPipelinePopupShow
            },

            toggleCreatePermission () {
                this.setPermissionConfig(this.$permissionResourceMap.pipeline, this.$permissionActionMap.create)
            },
            localConvertMStoString (num) {
                return convertMStoString(num)
            },
            showSlide (val) {
                this.slideShow = val
            },
            togglePageLoading (val) {
                if (this.pageLoading !== val) {
                    this.$nextTick(() => {
                        this.$store.commit('pipelines/showPageLoading', val)
                    })
                }
            },
            calcLatestStartBuildTime (row) {
                if (row.latestBuildStartTime) {
                    try {
                        if (window.pipelineVue.$i18n && window.pipelineVue.$i18n.locale === 'en-US') {
                            return this.localConvertMStoString(row.currentTimestamp - row.latestBuildStartTime)
                        } else {
                            let result = this.localConvertMStoString(row.currentTimestamp - row.latestBuildStartTime).match(/^[0-9]{1,}([\u4e00-\u9fa5]){1,}/)[0]
                            if (result.indexOf('分') > 0) {
                                result += '钟'
                            }
                            return `${result}前`
                        }
                    } catch (err) {
                        return '---'
                    }
                } else {
                    return '--'
                }
            },
            /**
             * 计算执行中任务的百分比
             */
            calcPercentage (item) {
                const {
                    latestBuildStartTime,
                    latestBuildEstimatedExecutionSeconds
                } = item
                const time = +new Date()

                if (!latestBuildStartTime || !latestBuildEstimatedExecutionSeconds) {
                    return '0%'
                }

                const calcTime = Math.ceil((time - latestBuildStartTime) / (latestBuildEstimatedExecutionSeconds * 100 * 1000))

                if (this.statusMap[item.latestBuildStatus] === 'error' || this.statusMap[item.latestBuildStatus] === 'cancel') {
                    return '100%'
                }

                if (calcTime > 99) {
                    return '99%'
                } if (calcTime < 1) {
                    return '1%'
                } else {
                    return `${calcTime}%`
                }
            },
            /**
             *  初始化页面数据
             */
            async init () {
                this.requestTemplatePermission()
                this.requestHasCreatePermission()
                this.requestGrouptLists()
            },

            async initPage () {
                this.togglePageLoading(true)
                await this.init()
                this.togglePageLoading(false)
            },

            requestTemplatePermission () {
                this.$store.dispatch('pipelines/requestTemplatePermission', this.projectId).then((res) => {
                    this.hasTemplatePermission = res
                })
            },

            /**
             *  请求pipeline列表
             */
            async requestPipelineList (page = 1, pageSize) {
                const {
                    $store,
                    filter
                } = this
                let response
                try {
                    response = await $store.dispatch('pipelines/requestAllPipelinesListByFilter', {
                        projectId: this.projectId,
                        page,
                        pageSize,
                        sortType: this.sortType || 'CREATE_TIME',
                        filterByLabels: filter.filterByLabels,
                        filterByPipelineName: filter.filterByPipelineName,
                        filterByCreator: filter.filterByCreator,
                        viewId: this.currentViewId
                    })

                    $store.commit('pipelines/updateAllPipelineList', response.records)

                    const pipelineFeConfMap = response.records.reduce((pipelineFeConfMap, item, index) => {
                        pipelineFeConfMap[item.pipelineId] = {
                            name: item.pipelineName,
                            pipelineName: item.pipelineName,
                            desc: typeof item.pipelineDesc === 'string' && item.pipelineDesc.trim(),
                            isRunning: false,
                            status: '',
                            content: [
                                {
                                    key: this.$t('lastBuildNum'),
                                    value: item.buildNumRule ? (item.latestBuildNumAlias ? item.latestBuildNumAlias : `#${item.latestBuildNum}`) : (item.latestBuildNum ? `#${item.latestBuildNum}` : '--')
                                },
                                {
                                    key: this.$t('lastExecTime'),
                                    value: item.latestBuildStartTime ? this.calcLatestStartBuildTime(item) : this.$t('newlist.noExecution')
                                }
                            ],
                            runningInfo: {
                                time: 0,
                                percentage: this.calcPercentage(item),
                                log: '',
                                buildCount: item.runningBuildCount || 0
                            },
                            footer: [
                                {
                                    upperText: item.taskCount,
                                    lowerText: this.$t('newlist.totalAtomNums'),
                                    handler: this.goEditPipeline
                                },
                                {
                                    upperText: item.buildCount,
                                    lowerText: this.$t('newlist.execTimes'),
                                    handler: this.goHistory
                                }
                            ],
                            marginRight: 0,
                            width: '352px',
                            customBtns: [],
                            buttonAllow: {
                                confirmFailure: true,
                                terminatePipeline: true,
                                exec: true,
                                continuePipeline: true
                            },
                            pipelineId: item.pipelineId,
                            templateId: item.templateId,
                            buildId: item.latestBuildId || 0,
                            extMenu: [],
                            isInstanceTemplate: item.instanceFromTemplate
                        }
                        return pipelineFeConfMap
                    }, {})

                    this.pipelineFeConfMap = {
                        ...this.pipelineFeConfMap,
                        ...pipelineFeConfMap
                    }
                    this.updatePipelineStatus(response.records.reduce((itemMap, item) => {
                        itemMap[item.pipelineId] = item
                        return itemMap
                    }, {}), true)
                    return response
                } catch (err) {
                    $store.commit('pipelines/updateAllPipelineList', [])
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                }
            },
            /** *
             * 获取标签及其分组
             */
            async requestGrouptLists () {
                const { $store } = this
                let res
                try {
                    res = await $store.dispatch('pipelines/requestGetGroupLists', {
                        projectId: this.projectId
                    })
                    $store.commit('pipelines/updateGroupLists', res)
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                }
            },
            async requestHasCreatePermission () {
                const { $store } = this
                let res
                try {
                    res = await $store.dispatch('pipelines/requestHasCreatePermission', {
                        projectId: this.projectId
                    })
                    $store.commit('pipelines/updateCreatePermission', res)
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                }
            },
            /**
             *  处理收藏和取消收藏
             */
            async togglePipelineCollect (pipelineId) {
                const pipeline = this.pipelineList.find(item => item.pipelineId === pipelineId)
                if (!pipeline) return
                const isCollect = !pipeline.hasCollect
                try {
                    await this.$store.dispatch('pipelines/requestToggleCollect', {
                        projectId: this.projectId,
                        pipelineId,
                        isCollect
                    })
                    pipeline.hasCollect = isCollect

                    this.pipelineFeConfMap[pipelineId].extMenu[1].text = isCollect ? this.$t('uncollect') : this.$t('collect')
                    if (this.currentViewId === 'collect' && !isCollect) {
                        this.$store.commit('pipelines/removePipelineById', pipelineId)
                    }

                    this.$showTips({
                        message: isCollect ? this.$t('collectSuc') : this.$t('uncollectSuc'),
                        theme: 'success'
                    })
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                }
            },
            /**
             *  跳转编辑
             */
            goEditPipeline (pipelineId) {
                this.$router.push({
                    name: 'pipelinesEdit',
                    params: {
                        projectId: this.projectId,
                        pipelineId: pipelineId
                    }
                })
            },
            /**
             *  跳转执行历史
             */
            goHistory (pipelineId) {
                this.$router.push({
                    name: 'pipelinesHistory',
                    params: {
                        projectId: this.projectId,
                        pipelineId: pipelineId
                    }
                })
            },
            updatePipelineStatus (data, isFirst = false) {
                const {
                    statusMap
                } = this
                const knownErrorList = JSON.parse(localStorage.getItem('pipelineKnowError')) || {}
                Object.keys(data).forEach(pipelineId => {
                    const item = data[pipelineId]
                    if (item) {
                        const status = statusMap[item.latestBuildStatus]
                        let feConfig = {
                            isRunning: false,
                            status: status || 'not_built'
                        }
                        // 单独修改当前任务是否在执行的状态, 拼接右下角按钮
                        switch (feConfig.status) {
                            case 'error': {
                                const isKnowErrorPipeline = !!knownErrorList[`${this.projectId}_${pipelineId}_${item.latestBuildId}`]
                                feConfig = {
                                    ...feConfig,
                                    customBtns: isKnowErrorPipeline
                                        ? []
                                        : [{
                                            icon: 'check-1',
                                            text: this.$t('newlist.known'),
                                            handler: 'error-noticed'
                                        }],
                                    isRunning: !isKnowErrorPipeline,
                                    status: isKnowErrorPipeline ? 'known_error' : 'error'
                                }
                                break
                            }
                            case 'cancel': {
                                const isKnowCancelPipeline = !!knownErrorList[`${this.projectId}_${pipelineId}_${item.latestBuildId}`]
                                feConfig = {
                                    ...feConfig,
                                    customBtns: isKnowCancelPipeline
                                        ? []
                                        : [{
                                            icon: 'check-1',
                                            text: this.$t('newlist.known'),
                                            handler: 'error-noticed'
                                        }],
                                    isRunning: !isKnowCancelPipeline,
                                    status: isKnowCancelPipeline ? 'known_cancel' : 'cancel'
                                }
                                break
                            }
                            case 'running':
                                feConfig = {
                                    ...feConfig,
                                    isRunning: true,
                                    customBtns: [{
                                        text: this.$t('terminate'),
                                        handler: 'terminate-pipeline'
                                    }]
                                }
                                break
                            case 'warning':
                                feConfig = {
                                    ...feConfig,
                                    customBtns: [
                                        {
                                            text: this.$t('resume'),
                                            handler: 'resume-pipeline'
                                        },
                                        {
                                            text: this.$t('terminate'),
                                            handler: 'terminate-pipeline'
                                        }
                                    ],
                                    isRunning: true
                                }
                                break
                            default:
                                feConfig.isRunning = false
                        }

                        feConfig = {
                            ...feConfig,
                            footer: [
                                {
                                    upperText: item.taskCount,
                                    lowerText: this.$t('newlist.totalAtomNums'),
                                    handler: this.goEditPipeline
                                },
                                {
                                    upperText: item.buildCount,
                                    lowerText: this.$t('newlist.execTimes'),
                                    handler: this.goHistory
                                }
                            ],
                            runningInfo: {
                                time: convertMStoStringByRule(status === 'error' ? (item.latestBuildEndTime - item.latestBuildStartTime) : (item.currentTimestamp - item.latestBuildStartTime)),
                                percentage: this.calcPercentage(item),
                                log: item.latestBuildTaskName,
                                buildCount: item.runningBuildCount || 0
                            },
                            projectId: this.projectId,
                            pipelineId,
                            buildId: item.latestBuildId || 0
                        }
                        if (!(this.pipelineFeConfMap[pipelineId] && this.pipelineFeConfMap[pipelineId].extMenu && this.pipelineFeConfMap[pipelineId].extMenu.length)) {
                            feConfig.extMenu = [
                                {
                                    text: this.$t('edit'),
                                    handler: this.goEditPipeline
                                },
                                {
                                    text: (item.hasCollect ? this.$t('uncollect') : this.$t('collect')),
                                    handler: this.togglePipelineCollect
                                },
                                {
                                    text: this.$t('newlist.copyAs'),
                                    handler: this.copyPipeline
                                },
                                {
                                    text: this.$t('newlist.saveAsTemp'),
                                    disable: !this.hasTemplatePermission,
                                    handler: this.copyAsTemplate
                                },
                                {
                                    text: this.$t('delete'),
                                    handler: this.deletePipeline
                                }
                            ]
                        }

                        if (this.pipelineFeConfMap[pipelineId] && !this.pipelineFeConfMap[pipelineId].extMenu.length && this.pipelineFeConfMap[pipelineId].isInstanceTemplate) {
                            feConfig.extMenu.splice((feConfig.extMenu.length - 1), 0, {
                                text: this.$t('newlist.jumpToTemp'),
                                handler: this.jumpToTemplate,
                                isJumpToTem: true
                            })
                        }

                        this.pipelineFeConfMap[pipelineId] = {
                            ...(this.pipelineFeConfMap[pipelineId] || {}),
                            ...feConfig
                        }
                    }
                })
            },
            /**
             * 触发执行流水线
             */
            async triggersExec (params, pipelineId) {
                const {
                    $store,
                    projectId,
                    pipelineList
                } = this
                const curPipeline = pipelineList.find(item => item.pipelineId === pipelineId)
                let message = ''
                let theme = ''

                try {
                    // 请求执行构建
                    const res = await $store.dispatch('pipelines/requestExecPipeline', {
                        projectId,
                        pipelineId,
                        params
                    })

                    message = this.$t('newlist.sucToStartBuild')
                    theme = 'success'
                    curPipeline.latestBuildId = res.id

                    // 重置执行进度

                    this.pipelineFeConfMap[pipelineId] = {
                        ...this.pipelineFeConfMap[pipelineId],
                        runningInfo: {
                            ...this.pipelineFeConfMap[pipelineId].runningInfo,
                            percentage: '1%',
                            time: '0秒',
                            log: '',
                            buildCount: 1
                        },
                        status: 'running',
                        isRunning: true,
                        buildId: res.id
                    }
                } catch (err) {
                    if (err.code === 403) { // 没有权限执行
                        // this.setPermissionConfig(`${this.$t('pipeline')}：${curPipeline.pipelineName}`, this.$t('exec'), curPipeline.pipelineId)
                        this.setPermissionConfig(this.$permissionResourceMap.pipeline, this.$permissionActionMap.execute, [{
                            id: curPipeline.pipelineId,
                            name: curPipeline.pipelineName
                        }])
                        return
                    } else {
                        this.pipelineFeConfMap[pipelineId] = {
                            ...this.pipelineFeConfMap[pipelineId],
                            status: 'known_error',
                            isRunning: false
                        }
                        message = err.message || err
                        theme = 'error'
                    }
                } finally {
                    message && this.$showTips({
                        message,
                        theme
                    })
                }
            },
            /**
             *  任务执行错误，点击我知道了的回调函数
             */
            async errorNoticed (pipelineId) {
                let knownErrorList = JSON.parse(localStorage.getItem('pipelineKnowError'))
                const target = this.pipelineFeConfMap[pipelineId]
                const key = `${this.projectId}_${target.pipelineId}_${target.buildId}`

                knownErrorList = {
                    ...(knownErrorList || {}),
                    [key]: 1
                }
                localStorage.setItem('pipelineKnowError', JSON.stringify(knownErrorList))
                // 更新DOM节点的样式
                if (this.pipelineFeConfMap[pipelineId]) {
                    // 取消状态流水线
                    if (this.pipelineFeConfMap[pipelineId].status === 'cancel') {
                        this.pipelineFeConfMap[pipelineId] = {
                            ...this.pipelineFeConfMap[pipelineId],
                            status: 'known_cancel',
                            isRunning: false
                        }
                    } else {
                        // 失败状态流水线
                        this.pipelineFeConfMap[pipelineId] = {
                            ...this.pipelineFeConfMap[pipelineId],
                            status: 'known_error',
                            isRunning: false
                        }
                    }
                }
            },
            /**
             *  终止任务
             */
            async terminatePipeline (pipelineId) {
                const { $store, projectId } = this
                const feConfig = this.pipelineFeConfMap[pipelineId]
                if (!feConfig.buttonAllow.terminatePipeline) return

                this.pipelineFeConfMap[pipelineId].buttonAllow.terminatePipeline = false

                try {
                    await $store.dispatch('pipelines/requestTerminatePipeline', {
                        projectId,
                        pipelineId,
                        buildId: feConfig.buildId
                    })

                    this.pipelineFeConfMap[pipelineId] = {
                        ...this.pipelineFeConfMap[pipelineId],
                        isRunning: false,
                        status: 'known_cancel'
                    }
                } catch (err) {
                    this.handleError(err, [{
                        actionId: this.$permissionActionMap.execute,
                        resourceId: this.$permissionResourceMap.pipeline,
                        instanceId: [{
                            id: pipelineId,
                            name: feConfig.pipelineName
                        }],
                        projectId
                    }])
                } finally {
                    this.pipelineFeConfMap[pipelineId].buttonAllow.terminatePipeline = true
                }
            },
            /**
             *  继续任务
             */
            async resumePipeline (pipelineId) {
                const { $store, projectId } = this

                if (!this.pipelineFeConfMap[pipelineId].buttonAllow.continuePipeline) return

                this.pipelineFeConfMap[pipelineId].buttonAllow.continuePipeline = false

                try {
                    await $store.dispatch('pipeline/requestResumePipeline', {
                        projectId,
                        pipelineId
                    })
                    this.pipelineFeConfMap[pipelineId] = {
                        ...this.pipelineFeConfMap[pipelineId],
                        isRunning: true,
                        status: 'running'
                    }
                } catch (err) {
                    this.$showTips({
                        message: err.data ? err.data.message : err,
                        theme: 'error'
                    })
                } finally {
                    this.pipelineFeConfMap[pipelineId].buttonAllow.continuePipeline = true
                }
            },
            /**
             *  点击卡片的title跳转详情
             */
            titleClickHandler (pipelineId) {
                this.$router.push({
                    name: 'pipelinesHistory',
                    params: {
                        projectId: this.projectId,
                        pipelineId: pipelineId
                    }
                })
            },

            /**
             * 另存为模板
             */
            copyAsTemplate (pipelineId) {
                const feConfig = this.pipelineFeConfMap[pipelineId]
                this.saveAsTemp.templateName = `${feConfig.pipelineName}_template`
                this.saveAsTemp.isShow = true
                this.saveAsTemp.pipelineId = pipelineId
            },

            /**
             * 跳转到模板
             * @param templateId 模板id
             */
            jumpToTemplate (templateId) {
                this.$router.push({
                    name: 'templateEdit',
                    params: {
                        templateId: templateId
                    }
                })
            },

            async saveAsConfirmHandler () {
                if (this.errors.has('saveTemplateName')) return
                const projectId = this.projectId
                const postData = {
                    pipelineId: this.saveAsTemp.pipelineId,
                    templateName: this.saveAsTemp.templateName,
                    isCopySetting: this.saveAsTemp.isCopySetting
                }

                this.$store.dispatch('pipelines/saveAsTemplate', { projectId, postData }).then(({ id }) => {
                    this.saveAsCancelHandler()
                    this.$showTips({ message: this.$t('newlist.saveAsTempSuc'), theme: 'success' })
                    this.$router.push({
                        name: 'templateEdit',
                        params: { templateId: id }
                    })
                }).catch((err) => {
                    const message = err.message || this.$t('newlist.saveAsTempFail')
                    this.$showTips({ message, theme: 'error' })
                })
            },

            saveAsCancelHandler () {
                this.saveAsTemp.isShow = false
                this.saveAsTemp.templateName = ''
                this.saveAsTemp.pipelineId = ''
                this.saveAsTemp.nameHasError = false
            },

            /**
             *  复制流水线
             */
            copyPipeline (pipelineId) {
                const {
                    copyDialogConfig
                } = this
                const curPipeline = this.pipelineFeConfMap[pipelineId]
                this.copyConfig.newPipelineName = `${curPipeline.pipelineName}_copy`
                copyDialogConfig.isShow = true
                copyDialogConfig.pipelineId = pipelineId
            },
            /**
             *  删除流水线
             */
            deletePipeline (pipelineId) {
                const {
                    $store
                } = this
                const curPipeline = this.pipelineFeConfMap[pipelineId]
                const content = `${this.$t('newlist.deletePipeline')}: ${curPipeline.pipelineName}?`

                navConfirm({ type: 'warning', content })
                    .then(() => {
                        let message, theme
                        this.togglePageLoading(true)
                        setTimeout(async () => {
                            try {
                                await $store.dispatch('pipelines/deletePipeline', {
                                    projectId: this.projectId,
                                    pipelineId: curPipeline.pipelineId
                                })
                                await this.$refs.infiniteScroll.updateList()
                                message = this.$t('deleteSuc')
                                theme = 'success'
                            } catch (err) {
                                this.handleError(err, [{
                                    actionId: this.$permissionActionMap.delete,
                                    resourceId: this.$permissionResourceMap.pipeline,
                                    instanceId: [{
                                        id: curPipeline.pipelineId,
                                        name: curPipeline.pipelineName
                                    }],
                                    projectId: this.projectId
                                }])
                            } finally {
                                message && this.$showTips({
                                    message,
                                    theme
                                })
                                this.togglePageLoading(false)
                            }
                        }, 1000)
                    }).catch(() => {})
            },
            /**
             *  复制流水线弹窗的确认回调函数
             */
            async copyConfirmHandler () {
                const {
                    copyConfig: {
                        newPipelineName,
                        newPipelineDesc
                    },
                    copyConfig,
                    projectId,
                    $store
                } = this
                let message = ''
                let theme = ''

                if (!newPipelineName) {
                    copyConfig.nameHasError = true
                    return false
                }
                const prePipeline = this.pipelineList.find(item => item.pipelineId === this.copyDialogConfig.pipelineId)

                try {
                    copyConfig.loading = true

                    await $store.dispatch('pipelines/copyPipeline', {
                        projectId,
                        pipelineId: this.copyDialogConfig.pipelineId,
                        args: {
                            name: newPipelineName,
                            desc: newPipelineDesc,
                            group: prePipeline.group,
                            hasCollect: false
                        }
                    })

                    message = this.$t('copySuc')
                    theme = 'success'
                    setTimeout(() => {
                        this.copyDialogConfig.isShow = false
                    }, 500)

                    this.$refs.infiniteScroll.queryList(1, this.pipelineList.length + 1)
                } catch (err) {
                    this.handleError(err, [{
                        actionId: this.$permissionActionMap.create,
                        resourceId: this.$permissionResourceMap.pipeline,
                        instanceId: [{
                            id: prePipeline.pipelineId,
                            name: prePipeline.pipelineName
                        }]
                    }, {
                        actionId: this.$permissionActionMap.edit,
                        resourceId: this.$permissionResourceMap.pipeline,
                        instanceId: [{
                            id: prePipeline.pipelineId,
                            name: prePipeline.pipelineName
                        }],
                        projectId
                    }])
                } finally {
                    setTimeout(() => {
                        copyConfig.loading = false
                        message && this.$showTips({
                            message,
                            theme
                        })
                    }, 300)
                }
            },
            /**
             *  复制流水线弹窗的取消回调
             */
            copyCancelHandler () {
                const {
                    copyConfig
                } = this

                copyConfig.newPipelineName = ''
                copyConfig.newPipelineDesc = ''
            }
        }
    }
</script>

<style lang='scss'>
    @import './../../scss/conf';

    .pipeline-list {
        height: calc(100% - 60px);
        padding-top: 2px;
        overflow: auto;
        .devops-empty-tips {
            .bk-button {
                width: 147px;
                height: 42px;
                line-height: 40px;
            }
        }
        &-wrapper {
            padding-top: 20px;
            margin: 0 auto;
            @media only screen and (max-width: 1359px) {
                width: 973px;
                .task-card:nth-child(3n) {
                    margin-right: 0;
                }

            }
            @media only screen and (min-width: 1360px) and (max-width: 1694px) {
                width: 1304px;
                .task-card:nth-child(4n) {
                    margin-right: 0;
                }
            }
            @media only screen and (min-width: 1695px) and (max-width: 2025px) {
                width: 1635px;
                .task-card:nth-child(5n) {
                    margin-right: 0;
                }
            }
            @media only screen and (min-width: 2026px) {
                width: 1966px;
                .task-card:nth-child(6n) {
                    margin-right: 0;
                }
            }
        }
        &-content {
            padding-top: 20px;
        }
        &-cards {
            display: flex;
            flex-wrap: wrap;
            .task-card {
                width: 311px;
                margin: 0 20px 20px 0;
            }
        }
        .loading-wrapper {
            min-height: calc(100% - 60px);
            height: 100%;
        }
        .toggle-layout {
            display: inline-block;
            margin-top: 8px;
            font-size: 24px;
            color: #c3cdd7;
            cursor: pointer;
        }
        .table-list-name {
            padding-left: 30px;
        }
        .table-list-progress {
            display: inline-block;
            width: 50%;
            & + .row-item-desc {
                vertical-align: bottom;
            }
        }
        .inline-component {
            display: inline-block;
        }
        .row-item-desc {
            display: inline-block;
            width: 80px;
            line-height: 32px;
            vertical-align: text-top;
        }
        .item-text-btn {
            display: inline-block;
            height: 32px;
            line-height: 32px;
            vertical-align: text-bottom;
            &.noticed {
                display: inline-block;
                height: 24px;
                line-height: 22px;
                border: 1px solid $primaryColor;
                padding: 0 10px;
                vertical-align: top;
                color: $primaryColor;
                transition: all .2s linear;
                &:hover {
                    box-shadow: 0 2px 2px 2px #f2f2f2;
                }
            }
        }
        .row-task-count,
        .row-build-count {
            width: 30px;
            height: 30px;
            line-height: 30px;
            font-size: 14px;
        }
        .row-status {
            &.success {
                color: $successColor;
            }
            &.cancel {
                color: $cancelColor;
            }
            &.error,
            &.known_error {
                color: $dangerColor;
            }
        }
        .table-list-name {
            position: relative;
            &:before {
                position: absolute;
                left: 18px;
                top: 50%;
                transform: translateY(-50%);
                content: '';
                width: 5px;
                height: 15px;
                border-radius: 3px;
                border: 1px solid transparent;
            }
            &.success,
            &.stage_success {
                &:before {
                    border-color: $successColor;
                    background-color: #cdffe2;
                }
            }
            &.cancel,
            &.known_cancel {
                &:before {
                    border-color: $cancelColor;
                    background-color: #c9ff83;
                }
            }
            &.error,
            &.known_error {
                &:before {
                    border-color: $dangerColor;
                    background-color: #febcbb;
                }
            }
            .text-link {
                font-size: 14px;
            }
            .build-status-tips {
                width: 8px;
                height: 20px;
                position: absolute;
                left: 18px;
                cursor: pointer;
            }
        }
    }
    .copy-pipeline {
        .bk-label {
            width: 150px;
            padding-right: 24px;
        }
        .bk-form-content {
            margin-left: 150px;
        }
        .auth-radio {
            margin: 0 30px 0 0;
        }
        .err-name {
            text-align: left;
            margin-left: 100px;
            margin-bottom: -21px;
        }
        .form-field-icon {
            position: relative;
            left: -16px;
            top: 14px;
        }
    }
</style>
