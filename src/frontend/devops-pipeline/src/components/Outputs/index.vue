<template>
    <div
        style="height: 100%;width: 100%;"
        v-bkloading="{ isLoading }"
    >
        <bk-resize-layout
            :key="currentTab"
            :collapsible="true"
            class="pipeline-exec-outputs"
            :initial-divide="initWidth"
            :min="260"
            :max="800"
        >
            <aside
                slot="aside"
                class="pipeline-exec-outputs-aside"
            >
                <div class="pipeline-exec-outputs-filter-input">
                    <div
                        class="artifact-search"
                        v-if="currentTab === 'artifacts'"
                    >
                        <p>{{ $t('metaData') }}</p>
                        <bk-search-select
                            class="select-search"
                            :show-condition="false"
                            :data="artifactFilterData"
                            :placeholder="$t('settings.itemPlaceholder')"
                            v-model="artifactValue"
                            @change="updateSearchKey"
                        />
                    </div>
                    <bk-input
                        class="input-search"
                        clearable
                        right-icon="bk-icon icon-search"
                        :placeholder="filterPlaceholder"
                        v-model="keyWord"
                    />
                </div>
                <!-- <div class="pipeline-exec-outputs-filter">
                    <i class="devops-icon icon-filter"></i>
                    {{ $t('条件查询') }}
                    <bk-tag class="output-filter-condition-count">2</bk-tag>
                </div> -->
                <ul
                    v-if="visibleOutputs.length > 0"
                    class="pipeline-exec-outputs-list"
                >
                    <li
                        v-for="output in visibleOutputs"
                        :key="output.id"
                        :class="{
                            active: output.id === activeOutput.id
                        }"
                        @click="setActiveOutput(output)"
                    >
                        <i :class="['devops-icon', `icon-${output.icon}`]"></i>
                        <span
                            class="output-name"
                            :title="output.name"
                        >{{ output.name }}</span>
                        <span class="output-size">{{ output.size }}</span>
                        <p class="output-hover-icon-box">
                            <artifact-download-button
                                v-if="output.downloadable"
                                :output="output"
                                download-icon
                                :has-permission="hasPermission"
                                :path="output.fullPath"
                                :name="output.name"
                                :artifactory-type="output.artifactoryType"
                            />
                            <i
                                v-if="output.isReportOutput"
                                class="devops-icon icon-full-screen"
                                @click.stop="fullScreenViewReport(output)"
                            />
                        </p>
                    </li>
                </ul>

                <div
                    v-else
                    class="no-outputs-placeholder"
                >
                    <logo
                        name="empty"
                        size="180"
                    />
                    <span>{{ $t("empty") }}</span>
                </div>
            </aside>
            <section
                slot="main"
                class="pipeline-exec-outputs-section"
            >
                <iframe-report
                    v-if="isCustomizeReport"
                    ref="iframeReport"
                    :report-name="activeOutput.name"
                    :index-file-url="activeOutput.indexFileUrl"
                />
                <third-party-report
                    v-else-if="isActiveThirdReport"
                    :report-list="thirdPartyReportList"
                />
                <template v-else-if="activeOutputDetail">
                    <div class="pipeline-exec-output-header">
                        <span class="pipeline-exec-output-header-name">
                            <i :class="`devops-icon icon-${activeOutputDetail.icon}`" />
                            <span
                                :title="activeOutputDetail.name"
                                class="output-detail-name"
                            >
                                {{ activeOutputDetail.name }}
                            </span>
                        </span>
                        <bk-tag theme="info">{{ $t(activeOutputDetail.artifactoryTypeTxt) }}</bk-tag>
                        <p class="pipeline-exec-output-actions">
                            <artifact-download-button
                                v-if="activeOutput.downloadable"
                                :output="activeOutput"
                                :has-permission="hasPermission"
                                :path="activeOutput.fullPath"
                                :name="activeOutput.name"
                                :artifactory-type="activeOutput.artifactoryType"
                            />
                            <bk-button
                                text
                                theme="primary"
                                v-for="btn in btns"
                                :key="btn.text"
                                @click="btn.handler"
                            >
                                {{ btn.text }}
                            </bk-button>

                            <ext-menu
                                v-if="!activeOutputDetail.folder"
                                :data="activeOutputDetail"
                                :config="artifactMoreActions"
                            ></ext-menu>
                        </p>
                    </div>
                    <div class="pipeline-exec-output-artifact">
                        <div
                            v-for="block in infoBlocks"
                            :key="block.title"
                            class="pipeline-exec-output-block"
                        >
                            <h6 class="pipeline-exec-output-block-title">{{ block.title }}</h6>
                            <bk-table
                                v-if="block.key === 'meta'"
                                :data="block.value"
                            >
                                <bk-table-column
                                    :label="$t('view.key')"
                                    prop="key"
                                ></bk-table-column>
                                <bk-table-column
                                    :label="$t('view.value')"
                                    prop="value"
                                >
                                    <template slot-scope="props">
                                        <span
                                            v-if="props.row.color"
                                            :style="{ backgroundColor: props.row.color }"
                                            class="color-block"
                                        ></span>
                                        <span>{{ props.row.value }}</span>
                                    </template>
                                </bk-table-column>
                                <bk-table-column
                                    :label="$t('desc')"
                                    prop="description"
                                >
                                    <template slot-scope="scope">
                                        <span>{{ scope.row.description || '--' }}</span>
                                    </template>
                                </bk-table-column>
                            </bk-table>
                            <ul
                                v-else
                                slot="content"
                                class="pipeline-exec-output-block-content"
                            >
                                <li
                                    v-for="row in block.block"
                                    :key="row.key"
                                    :style="{ 'align-items': row.key === 'fullName' ? 'baseline' : 'center' }"
                                >
                                    <span class="pipeline-exec-output-block-row-label"> {{ row.name }}： </span>
                                    <span
                                        class="pipeline-exec-output-block-row-full_name"
                                        v-if="row.key === 'fullName'"
                                    >
                                        {{ block.value[row.key] || "--" }}
                                    </span>
                                    <span
                                        v-else
                                        class="pipeline-exec-output-block-row-value"
                                    >
                                        {{ block.value[row.key] || "--" }}
                                    </span>
                                </li>
                            </ul>
                        </div>
                    </div>
                </template>
                <div
                    v-else
                    class="no-outputs-placeholder"
                >
                    <logo
                        name="empty"
                        size="180"
                    />
                    <span>{{ $t("empty") }}</span>
                </div>
            </section>
        </bk-resize-layout>
        <copy-to-custom-repo-dialog
            ref="copyToDialog"
            :artifact="activeOutput"
        />
        <!-- <aside :class="['pipeline-outputs-filter-aside', {
            'pipeline-outputs-filter-aside-show': outputsFilterAsideVisible
        }]">
            <header>
                {{$t('条件查询')}}
                <i class="devops-icon icon-close" @click="closeOutputsFilterAside"></i>
            </header>
            <section class="pipeline-outputs-filter-conditions">
                <bk-form form-type="vertical" :model="filterConditionMap">
                    <bk-form-item
                        v-for="condition in conditions"
                        :key="condition.id"
                        :label="condition.label"
                    >
                        <component :is="condition.component" v-bind="condition.props" v-on="condition.listeners" />
                    </bk-form-item>
                </bk-form>
            </section>
            <footer>
                <bk-button
                    size="large"
                    theme="primary"
                    @click="submitFilter"
                >
                    {{$t('查询')}}
                </bk-button>
                <bk-button
                    size="large"
                    @click="closeOutputsFilterAside(true)"
                >
                    {{$t('history.reset')}}
                </bk-button>
            </footer>
        </aside> -->
    </div>
</template>

<script>
    import ArtifactDownloadButton from '@/components/ArtifactDownloadButton'
    import Logo from '@/components/Logo'
    import CopyToCustomRepoDialog from '@/components/Outputs/CopyToCustomRepoDialog'
    import IframeReport from '@/components/Outputs/IframeReport'
    import ThirdPartyReport from '@/components/Outputs/ThirdPartyReport'
    import ExtMenu from '@/components/pipelineList/extMenu'
    import { extForFile, repoTypeMap, repoTypeNameMap } from '@/utils/pipelineConst'
    import { convertFileSize, convertTime } from '@/utils/util'
    import { mapActions, mapState } from 'vuex'

    export default {
        components: {
            Logo,
            ThirdPartyReport,
            IframeReport,
            ExtMenu,
            CopyToCustomRepoDialog,
            // ArtifactsList
            ArtifactDownloadButton
        },
        props: {
            currentTab: {
                type: String,
                default: 'artifacts'
            }
        },
        data () {
            return {
                isCopyDialogShow: false,
                isCopying: false,
                outputs: [],
                keyWord: '',
                activeOutput: '',
                activeOutputDetail: null,
                hasPermission: false,
                isLoading: false,
                artifactValue: [],
                artifactFilterData: [],
                qualityMetadata: {}
            }
        },
        computed: {
            ...mapState('atom', ['execDetail']),
            filterQuery () {
                const uniqueKeys = new Set()
                const result = []

                this.artifactValue.forEach(item => {
                    item.values?.forEach(value => {
                        const keyValue = `${item.id}:${value.id}`
                        if (!uniqueKeys.has(keyValue)) {
                            uniqueKeys.add(keyValue)
                            result.push({ key: item.id, value: value.id })
                        }
                    })
                })

                return result
            },
            isDebugExec () {
                return this.execDetail?.debug ?? false
            },
            initWidth () {
                return this.currentTab === 'reports' ? '300px' : '40%'
            },
            filterPlaceholder () {
                return this.$t(`${this.currentTab}FilterPlaceholder`)
            },
            thirdPartyReportList () {
                return this.outputs.filter((report) => this.isThirdReport(report.reportType))
            },
            hasNext () {
                return this.pagination.count < this.outputs.length
            },
            reports () {
                return this.outputs.filter(
                    (item) =>
                        item.artifactoryType === 'REPORT' && !this.isThirdReport(item.reportType)
                )
            },
            artifacts () {
                return this.outputs.filter((item) => this.isArtifact(item.artifactoryType))
            },
            visibleOutputs () {
                const thirdReportList
                    = this.thirdPartyReportList.length > 0
                        ? [
                            {
                                id: 'THIRDPARTY',
                                type: 'REPORT',
                                reportType: 'THIRDPARTY',
                                name: this.$t('details.thirdReport'),
                                icon: 'bar-chart'
                            }
                        ]
                        : []
                let visibleOutputs = [
                    ...this.outputs.filter((output) => !this.isThirdReport(output.reportType)),
                    ...thirdReportList
                ]
                switch (this.currentTab) {
                    case 'artifacts':
                        visibleOutputs = this.artifacts
                        break
                    case 'reports':
                        visibleOutputs = [...this.reports, ...thirdReportList]
                        break
                }

                return visibleOutputs.filter(output => output.name.toLowerCase().includes(this.keyWord.toLowerCase())).map(({ size, ...rest }) => ({
                    ...rest,
                    size: size ? convertFileSize(size, 'B') : '--'
                }))
            },
            isActiveThirdReport () {
                return this.isThirdReport(this.activeOutput?.reportType)
            },
            isCustomizeReport () {
                return this.activeOutput?.reportType === 'INTERNAL'
            },
            btns () {
                return [
                    {
                        text: this.$t('details.goRepo'),
                        handler: () => {
                            const urlPrefix = `${WEB_URL_PREFIX}/repo/${this.$route.params.projectId}`
                            const pos = this.activeOutput.fullPath.lastIndexOf('/')
                            const fileName = this.activeOutput.fullPath.substring(0, pos)
                            const repoName = repoTypeNameMap[this.activeOutput.artifactoryType]
                            let url = `${urlPrefix}/generic?repoName=${repoName}&path=${encodeURIComponent(fileName)}/default`

                            if (this.activeOutput.isImageOutput) {
                                const imageVerion = this.activeOutput.fullName.slice(this.activeOutput.fullName.lastIndexOf(':') + 1)
                                url = `${urlPrefix}/docker/package?repoName=${repoName}&packageKey=${encodeURIComponent(`docker://${this.activeOutput.name}`)}&version=${imageVerion}`
                            }
                            window.open(url, '_blank')
                        }
                    }
                ]
            },
            artifactMoreActions () {
                return [
                    {
                        text: this.$t('details.copyTo'),
                        handler: this.$refs?.copyToDialog.show
                    }
                ]
            },
            infoBlocks () {
                if (this.activeOutputDetail.folder) {
                    return [
                        {
                            key: 'baseInfo',
                            title: this.$t('settings.baseInfo'),
                            block: this.baseInfoRows,
                            value: this.activeOutputDetail
                        }
                    ]
                }
                return [
                    {
                        key: 'baseInfo',
                        title: this.$t('settings.baseInfo'),
                        block: this.baseInfoRows,
                        value: this.activeOutputDetail
                    },
                    {
                        key: 'meta',
                        title: this.$t('metaData'),
                        value: this.activeOutputDetail.nodeMetadata
                    },
                    {
                        key: 'checkSum',
                        title: this.$t('details.checkSum'),
                        block: this.checkSumRows,
                        value: this.activeOutputDetail.checksums
                    }
                ]
            },
            baseInfoRows () {
                return this.activeOutputDetail.folder
                    ? [
                        { key: 'name', name: this.$t('details.directoryName') },
                        { key: 'fullName', name: this.$t('details.directoryPath') },
                        { key: 'size', name: this.$t('details.size') },
                        { key: 'include', name: this.$t('details.include') },
                        { key: 'createdTime', name: this.$t('details.created') },
                        { key: 'modifiedTime', name: this.$t('details.lastModified') }
                    ]
                    : [
                        { key: 'name', name: this.$t('details.name') },
                        { key: 'fullName', name: this.$t('details.filePath') },
                        { key: 'size', name: this.$t('details.size') },
                        { key: 'createdTime', name: this.$t('details.created') },
                        { key: 'modifiedTime', name: this.$t('details.lastModified') }
                    ]
            },
            checkSumRows () {
                return [
                    { key: 'sha256', name: 'SHA256' },
                    { key: 'sha1', name: 'SHA1' },
                    { key: 'md5', name: 'MD5' }
                ]
            }
        // filterConditionLength () {
        //     if (!this.filtering) return 0
        //     return Object.keys(this.filterConditionMap).filter(key => {
        //         if (Array.isArray(this.filterConditionMap[key])) {
        //             return this.filterConditionMap[key].length > 0
        //         }
        //         return !!this.filterConditionMap[key]
        //     }).length
        // },
        // conditions () {
        //     return [
        //         {
        //             id: 'triggerTime',
        //             label: this.$t('details.triggerTime'),
        //             component: 'bk-date-picker',
        //             props: {
        //                 type: 'datetimerange',
        //                 shortcuts: this.shortcuts,
        //                 value: this.filterConditionMap.timeRange
        //             },
        //             listeners: {
        //                 change: (range) => {
        //                     this.filterConditionMap.timeRange = range
        //                 }
        //             }
        //         },
        //         {
        //             id: 'buildNo',
        //             label: this.$t('构建号'),
        //             component: 'bk-input',
        //             props: {
        //                 value: this.filterConditionMap.buildNo
        //             },
        //             listeners: {
        //                 change: (buildNo) => {
        //                     this.filterConditionMap.buildNo = buildNo
        //                 }
        //             }
        //         },
        //         {
        //             id: 'filename',
        //             label: this.$t('文件名'),
        //             component: 'bk-input',
        //             props: {
        //                 value: this.filterConditionMap.filename
        //             },
        //             listeners: {
        //                 change: (filename) => {
        //                     this.filterConditionMap.filename = filename
        //                 }
        //             }
        //         },
        //         {
        //             id: 'creator',
        //             label: this.$t('触发人'),
        //             component: 'bk-input',
        //             props: {
        //                 value: this.filterConditionMap.creator
        //             },
        //             listeners: {
        //                 change: (creator) => {
        //                     this.filterConditionMap.creator = creator
        //                 }
        //             }
        //         },
        //         {
        //             id: 'property',
        //             label: this.$t('元数据'),
        //             component: 'bk-input',
        //             props: {
        //                 value: this.filterConditionMap.property
        //             },
        //             listeners: {
        //                 change: (property) => {
        //                     this.filterConditionMap.property = property
        //                 }
        //             }
        //         }
        //     ]
        // },
        // shortcuts () {
        //     return [
        //         {
        //             text: '今天',
        //             value () {
        //                 const end = new Date()
        //                 const start = new Date(end.getFullYear(), end.getMonth(), end.getDate())
        //                 return [start, end]
        //             },
        //             onClick: picker => {
        //                 console.log(picker)
        //             }
        //         },
        //         {
        //             text: '近7天',
        //             value () {
        //                 const end = new Date()
        //                 const start = new Date()
        //                 start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
        //                 return [start, end]
        //             }
        //         },
        //         {
        //             text: '近15天',
        //             value () {
        //                 const end = new Date()
        //                 const start = new Date()
        //                 start.setTime(start.getTime() - 3600 * 1000 * 24 * 15)
        //                 return [start, end]
        //             }
        //         }
        //     ]
        // }
        },
        watch: {
            visibleOutputs (outputs) {
                if (outputs.length > 0) {
                    this.setActiveOutput(outputs[0])
                } else {
                    this.activeOutputDetail = null
                }
            },
            currentTab: function () {
                this.keyWord = ''
                this.artifactValue = []
                this.$nextTick(this.init)
            },
            '$route.params.buildNo' () {
                this.$nextTick(this.init)
            },
            '$route.query.metadataKey': {
                handler (newVal) {
                    if (newVal) {
                        this.qualityMetadata = {
                            labelKey: newVal,
                            values: this.$route.query.metadataValues?.split(',')
                        }
                        this.initializeArtifactValue()
                    }
                },
                immediate: true
            }
        },
        async mounted () {
            await this.getArtifactDate()
            if (!this.$route.query.metadataKey) {
                this.init()
            }
        },
        methods: {
            ...mapActions('common', [
                'requestFileInfo',
                'requestOutputs',
                'getMetadataLabel',
                'requestExecPipPermission'
            ]),
            initializeArtifactValue () {
                if (!Object.keys(this.qualityMetadata).length) return

                const { labelKey, values } = this.qualityMetadata
                if (labelKey && values) {
                    this.artifactValue = [{
                        id: labelKey,
                        name: labelKey,
                        multiable: true,
                        values: values.map(item => ({
                            id: item,
                            name: item
                        }))
                    }]
                } else {
                    this.artifactValue = []
                }
                this.init()
            },

            async updateSearchKey (value) {
                const metadataKey = this.$route.query.metadataKey
                const hasMetadataKey = value.some(item => item.id === metadataKey)
                const query = { ...this.$route.query }

                if (!hasMetadataKey) {
                    delete query.metadataKey
                    delete query.metadataValues
                    this.$router.replace({ query })
                    this.init()
                }
            },
            async getArtifactDate () {
                const repoList = await this.getMetadataLabel({
                    projectId: this.$route.params.projectId,
                    pipelineId: this.$route.params.pipelineId,
                    ...(this.isDebugExec ? {debug: this.isDebugExec} : {})
                })

                this.artifactFilterData = repoList.map(item => {
                    const labelColorMapKeys = Object.keys(item.labelColorMap)
                    return {
                        id: item.labelKey,
                        name: item.labelKey,
                        multiable: true,
                        ...(item.enumType
                            ? {
                                children: labelColorMapKeys.map(key => ({
                                    id: key,
                                    name: key
                                }))
                            }
                            : {})
                    }
                })
            },

            async init () {
                const { projectId, pipelineId, buildNo: buildId } = this.$route.params

                try {
                    this.isLoading = true
                    const [hasPermission, outputsResponse] = await Promise.all([
                        this.requestHasPermission(),
                        this.requestOutputs({
                            projectId,
                            pipelineId,
                            buildId,
                            // pipelineOutputType: this.currentTab,
                            // ...(Object.keys(this.filterConditionMap).filter(key => !!this.filterConditionMap[key]).reduce((result, key) => {
                            //     result[key] = this.filterConditionMap[key]
                            //     return result
                            // }, {})),
                            qualityMetadata: this.filterQuery,
                            ...this.pagination
                        })
                    ])
                    const { records, page, pageSize, count } = outputsResponse
                    this.pagination = {
                        page,
                        pageSize,
                        count
                    }
                    this.outputs = records.map((item) => {
                        const isReportOutput = item.artifactoryType === 'REPORT'
                        const isImageOutput = item.artifactoryType === 'IMAGE'
                        const icon = isReportOutput ? 'order' : item.folder ? 'folder' : extForFile(item.name)
                        const id = isReportOutput ? (item.createTime + item.indexFileUrl) : item.fullPath
                        const type = this.isArtifact(item.artifactoryType) ? 'ARTIFACT' : ''
                        return {
                            type,
                            ...item,
                            id,
                            icon,
                            isReportOutput,
                            isApp: ['ipafile', 'apkfile'].includes(icon),
                            downloadable: hasPermission && this.isArtifact(item.artifactoryType) && !isImageOutput,
                            isImageOutput
                        }
                    })
                    console.log(this.outputs)
                } catch (err) {
                    this.$showTips({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                    this.outputs = [
                        {
                            name: 'output1',
                            createTime: '2022-07-06',
                            buildNum: '22'
                        },
                        {
                            name: 'output2',
                            createTime: '2022-07-06',
                            buildNum: '22'
                        },
                        {
                            name: 'output3',
                            createTime: '2022-07-06',
                            buildNum: '21'
                        },
                        {
                            name: 'output4',
                            createTime: '2022-07-06',
                            buildNum: '21'
                        },
                        {
                            name: 'output5',
                            createTime: '2022-07-06',
                            buildNum: '21'
                        },
                        {
                            name: 'output6',
                            createTime: '2022-07-05',
                            buildNum: '20'
                        },
                        {
                            name: 'output7',
                            createTime: '2022-07-05',
                            buildNum: '20'
                        },
                        {
                            name: 'output8',
                            createTime: '2022-07-05',
                            buildNum: '20'
                        },
                        {
                            name: 'output9',
                            createTime: '2022-07-03',
                            buildNum: '19'
                        },
                        {
                            name: 'output10',
                            createTime: '2022-07-03',
                            buildNum: '19'
                        }, {
                            name: 'output11',
                            createTime: '2022-07-02',
                            buildNum: '18'
                        }, {
                            name: 'output12',
                            createTime: '2022-07-01',
                            buildNum: '17'
                        }
                    ]
                } finally {
                    this.isLoading = false
                }
            },
            async requestHasPermission () {
                try {
                    const res = await this.requestExecPipPermission({
                        ...this.$route.params,
                        permission: 'DOWNLOAD'
                    })

                    this.hasPermission = res
                    return res
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$showTips({
                        message,
                        theme
                    })
                }
            },

            async showDetail (output) {
                const { projectId, pipelineId } = this.$route.params
                try {
                    this.isLoading = true
                    const params = {
                        projectId,
                        type: output.artifactoryType,
                        path: output.fullPath
                    }
                    const res = await this.requestFileInfo(params)
                    this.activeOutputDetail = {
                        ...output,
                        ...res,
                        artifactoryTypeTxt: repoTypeMap[output.artifactoryType] ?? '--',
                        size: output.folder ? convertFileSize(this.getFolderSize(output), 'B') : res.size > 0 ? convertFileSize(res.size, 'B') : '--',
                        createdTime: convertTime(res.createdTime * 1000),
                        modifiedTime: convertTime(res.modifiedTime * 1000),
                        icon: !output.folder ? extForFile(res.name) : 'folder',
                        include: this.getInclude(output)
                    }
                } catch (err) {
                    this.handleError(err, {
                        projectId,
                        resourceCode: pipelineId,
                        action: this.$permissionResourceAction.EXECUTE
                    })
                } finally {
                    this.isLoading = false
                }
            },
            getFolderSize (payload) {
                if (!payload.folder) return '0'
                return this.getValuesByKey(payload.properties, 'size')
            },
            getInclude (payload) {
                if (!payload.folder) return '--'
                const fileCount = this.getValuesByKey(payload.properties, 'fileCount')
                const folderCount = this.getValuesByKey(payload.properties, 'folderCount')
                return this.$t('details.fileAndFolder', [fileCount, folderCount])
            },
            getValuesByKey (data, key) {
                for (const item of data) {
                    if (key.includes(item.key)) {
                        return item.value
                    }
                }
            },
            setActiveOutput (output) {
                this.activeOutput = output
                switch (output.type) {
                    case 'THIRDPARTY':
                    case 'INTERNAL':
                        break
                    case 'ARTIFACT':
                        this.showDetail(output)
                        break
                }
            },
            isArtifact (artifactoryType) {
                return ['PIPELINE', 'CUSTOM_DIR', 'IMAGE'].includes(artifactoryType)
            },
            isThirdReport (reportType) {
                return ['THIRDPARTY'].includes(reportType)
            },
            fullScreenViewReport (output) {
                this.setActiveOutput(output)
                this.$nextTick(() => {
                this.$refs.iframeReport?.toggleFullScreen?.()
                })
            }
        }
    }
</script>

<style lang="scss">
@import "@/scss/conf";
@import "@/scss/mixins/ellipsis";

.pipeline-exec-outputs {
    height: 100%;
    display: flex;
    border: 0;

    .no-outputs-placeholder {
        color: #979ba5;
        display: flex;
        height: 100%;
        align-items: center;
        justify-content: center;
        flex-direction: column;

        >span {
            font-size: 12px;
            margin-top: -46px;
        }
    }

    .pipeline-exec-outputs-aside {
        position: relative;
        height: 100%;
        flex-shrink: 0;
        padding: 16px 11px;
        display: flex;
        flex-direction: column;

        .pipeline-exec-outputs-filter-input {
            display: flex;
            width: 100%;
            margin: 12px 0;
            flex-shrink: 0;

            .artifact-search {
                display: flex;
                height: 32px;
                margin-right: 4px;
                flex: 0 0 40%;

                p {
                    min-width: 62px;
                    text-align: center;
                    height: 32px;
                    line-height: 32px;
                    background-color: #fafbfd;
                    font-size: 12px;
                    border: 1px solid #c4c6cc;
                    border-radius: 2px;
                    border-right: none;
                }

                .select-search {
                    flex: 1;
                    ::placeholder {
                        font-size: 12px;
                        color: #c4c6cc;
                    }
                    .search-tag-box {
                        white-space: break-spaces;
                    }
                    .search-select-wrap {
                        overflow-y: auto;
                    }
                }
            }

            .input-search {
                flex-shrink: 0;
                flex: 1;
            }
        }

        .pipeline-exec-outputs-filter {
            position: relative;
            margin: 16px 0 6px 0;
            width: 100%;
            display: flex;
            align-items: center;
            justify-content: center;
            height: 32px;
            background: white;
            border: 1px solid #c4c6cc;
            border-radius: 2px;
            font-size: 14px;
            cursor: pointer;

            &.filter-active {
                color: $primaryColor;
                border-color: $primaryColor;
            }

            .output-filter-condition-count {
                margin: 0;
                position: absolute;
                right: 16px;
            }
        }

        .pipeline-exec-outputs-list {
            overflow: auto;
            flex: 1;
            padding-top: 10px;

            >li {
                display: flex;
                align-items: baseline;
                padding: 10px 19px;
                cursor: pointer;
                border-radius: 2px;
                font-size: 12px;
                margin-bottom: 10px;
                grid-gap: 10px;

                >.devops-icon {
                    display: inline-flex;
                    font-size: 16px;
                    flex-shrink: 0;
                    align-items: center;
                }

                .output-hover-icon-box {
                    display: flex;
                    align-items: baseline;
                    grid-gap: 6px;

                    :hover {
                        color: $primaryColor;
                    }
                }

                .output-name {
                    flex: 1;
                    @include multiline-ellipsis();
                }

                &.active,
                &:hover {
                    color: $iconPrimaryColor;
                    background: #f5f7fa;
                }
            }
        }
    }

    .pipeline-exec-outputs-section {
        height: 100%;
        display: flex;
        flex-direction: column;
        overflow: hidden;

        .pipeline-exec-output-header {
            display: flex;
            background: #fafbfd;
            padding: 10px 24px;
            flex-shrink: 0;

            &-name {
                display: flex;
                max-width: 60%;
                align-items: baseline;
                font-size: 16px;
                color: #313238;
                padding-right: 16px;

                .output-detail-name {
                    @include multiline-ellipsis();
                }

                >i {
                    padding-right: 12px;
                }
            }

            .pipeline-exec-output-actions {
                display: grid;
                grid-gap: 16px;
                grid-auto-flow: column;
                align-items: baseline;
                justify-self: flex-end;
                margin-left: auto;
            }
        }

        .pipeline-exec-output-artifact {
            flex: 1;
            overflow: auto;
        }

        .pipeline-exec-output-block {
            padding: 16px 24px;

            .color-block {
                display: inline-block;
                margin-right: 8px;
                width: 8px;
                height: 8px;
                border-radius: 50%;
            }

            .pipeline-exec-output-block-title {
                font-size: 14px;
                border-bottom: 1px solid #dcdee5;
                margin: 0 0 16px 0;
                line-height: 24px;
            }

            .pipeline-exec-output-block-content {
                font-size: 12px;

                >li {
                    display: flex;
                    margin-bottom: 16px;

                    .pipeline-exec-output-block-row-label {
                        color: #979ba5;
                        text-align: right;
                        @include ellipsis();
                        width: 110px;
                        flex-shrink: 0;
                    }

                    .pipeline-exec-output-block-row-full_name {
                        flex: 1;
                        line-height: 1.5;
                        word-break: break-all;
                    }

                    .pipeline-exec-output-block-row-value {
                        @include ellipsis();
                    }
                }
            }
        }
    }

    .pipeline-outputs-filter-aside {
        width: 480px;
        background: white;
        position: absolute;
        right: 0;
        top: 0;
        height: 100%;
        border: 1px solid #DCDEE5;
        display: grid;
        grid-template-rows: 52px 1fr 48px;
        transform: translateX(100%);
        transition: all 0.3s ease;

        &.pipeline-outputs-filter-aside-show {
            transform: translateX(0);
        }

        >header {
            padding: 0 24px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            background: #FFFFFF;
            box-shadow: inset 0 -1px 0 0 #DCDEE5;
            color: #313238;
        }

        .pipeline-outputs-filter-conditions {
            padding: 16px 24px;
        }

        >footer {
            padding: 0 24px;
            display: flex;
            align-items: center;
            grid-gap: 8px;
            background: #FAFBFD;
            box-shadow: 0 -1px 0 0 #DCDEE5;
        }
    }
}
</style>
