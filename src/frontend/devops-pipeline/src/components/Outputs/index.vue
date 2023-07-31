<template>
    <div class="pipeline-exec-outputs">
        <aside :class="['pipeline-exec-outputs-aside', {
            'pipeline-exec-outputs-aside-collapse': asideCollpased
        }]">
            <ul class="pipeline-exec-output-classify-tab">
                <li
                    v-for="classify in outputClassifyList"
                    :key="classify.key"
                    :class="{
                        active: currentTab === classify.key
                    }"
                    @click="switchTab(classify.key)"
                >
                    {{ classify.label }}
                </li>
            </ul>
            <div :class="['pipeline-exec-outputs-filter', {
                'filter-active': filterConditionLength
            }]" @click="showOutputsFilterAside">
                <i class="devops-icon icon-filter"></i>
                {{ $t('条件查询') }}
                <bk-tag
                    v-if="filterConditionLength > 0"
                    class="output-filter-condition-count"
                    theme="info"
                >
                    {{ filterConditionLength }}
                </bk-tag>
            </div>
            <template v-if="outputs.length > 0 && !asideCollpased">
                <ArtifactsList v-if="!$route.params.buildNo" :outputs="outputs" />
                <ul v-else class="pipeline-exec-outputs-list">
                    <li
                        v-for="output in visibleOutputs"
                        :key="output.id"
                        :class="{
                            active: output.id === activeOutput.id
                        }"
                        @click="setActiveOutput(output)"
                    >
                        <i :class="['devops-icon', `icon-${output.icon}`]"></i>
                        <span :title="output.name">{{ output.name }}</span>
                        <output-qrcode
                            v-if="output.isApp"
                            class="output-hover-icon"
                            :output="output"
                        />
                        <i
                            v-else-if="output.downloadable"
                            class="output-hover-icon devops-icon icon-download"
                            @click.stop="downloadArtifact(output)"
                        />
                    </li>
                </ul>
            </template>

            <div v-else-if="!asideCollpased" class="no-outputs-placeholder">
                <logo name="empty" size="180" />
                <span>{{ $t("empty") }}</span>
            </div>
            <div @click="toggleCollapseAside" class="collapse-handler">
                <i class="aside-collapse-icon devops-icon icon-angle-double-left"></i>
            </div>
        </aside>
        <section v-bkloading="{ isLoading }" class="pipeline-exec-outputs-section">
            <iframe-report
                v-if="isCustomizeReport"
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
                        {{ activeOutputDetail.name }}
                    </span>
                    <bk-tag theme="info">{{ $t(activeOutputDetail.artifactoryTypeTxt) }}</bk-tag>
                    <p class="pipeline-exec-output-actions">
                        <bk-button
                            text
                            theme="primary"
                            v-for="btn in btns"
                            :key="btn.text"
                            @click="btn.handler"
                        >
                            {{ btn.text }}
                        </bk-button>
                        <output-qrcode
                            :output="activeOutput"
                            v-if="activeOutputDetail.isApp"
                        />

                        <ext-menu :data="activeOutputDetail" :config="artifactMoreActions"></ext-menu>
                    </p>
                </div>
                <div class="pipeline-exec-output-artifact">
                    <div
                        v-for="block in infoBlocks"
                        :key="block.title"
                        class="pipeline-exec-output-block"
                    >
                        <h6 class="pipeline-exec-output-block-title">{{ block.title }}</h6>
                        <bk-table v-if="block.key === 'meta'" :data="block.value">
                            <bk-table-column :label="$t('view.key')" prop="key"></bk-table-column>
                            <bk-table-column :label="$t('view.value')" prop="value"></bk-table-column>
                            <bk-table-column :label="$t('desc')" prop="description">
                                <template slot-scope="scope">
                                    <span>{{ scope.row.description || '--' }}</span>
                                </template>
                            </bk-table-column>
                        </bk-table>
                        <ul v-else slot="content" class="pipeline-exec-output-block-content">
                            <li v-for="row in block.block" :key="row.key">
                                <span class="pipeline-exec-output-block-row-label"> {{ row.name }}： </span>
                                <span class="pipeline-exec-output-block-row-value">
                                    {{ block.value[row.key] || "--" }}
                                </span>
                            </li>
                        </ul>
                    </div>
                </div>
            </template>
            <div v-else class="no-outputs-placeholder">
                <logo name="empty" size="180" />
                <span>{{ $t("empty") }}</span>
            </div>
        </section>
        <copy-to-custom-repo-dialog ref="copyToDialog" :artifact="activeOutput" />
        <aside :class="['pipeline-outputs-filter-aside', {
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
        </aside>
    </div>
</template>

<script>
    import Logo from '@/components/Logo'
    import CopyToCustomRepoDialog from '@/components/Outputs/CopyToCustomRepoDialog'
    import IframeReport from '@/components/Outputs/IframeReport'
    import ThirdPartyReport from '@/components/Outputs/ThirdPartyReport'
    import OutputQrcode from '@/components/Outputs/OutputQrcode'
    import ExtMenu from '@/components/pipelineList/extMenu'
    import { extForFile, repoTypeMap, repoTypeNameMap } from '@/utils/pipelineConst'
    import { convertFileSize, convertTime } from '@/utils/util'
    import { mapActions } from 'vuex'
    import ArtifactsList from './ArtifactsList'

    export default {
        components: {
            Logo,
            ThirdPartyReport,
            IframeReport,
            ExtMenu,
            CopyToCustomRepoDialog,
            OutputQrcode,
            ArtifactsList
        },
        data () {
            return {
                isCopyDialogShow: false,
                isCopying: false,
                currentTab: '',
                outputs: [],
                activeOutput: '',
                activeOutputDetail: null,
                hasPermission: false,
                isLoading: false,
                asideCollpased: false,
                outputsFilterAsideVisible: false,
                pagination: {
                    page: 1,
                    count: 0,
                    pageSize: 20
                },
                filtering: false,
                filterConditionMap: {
                    timeRange: [],
                    buildNo: '',
                    filename: '',
                    creator: '',
                    property: null
                }
            }
        },
        computed: {
            outputClassifyList () {
                return [
                    {
                        key: '',
                        label: this.$t('editPage.all')
                    },
                    {
                        key: 'ARTIFACT',
                        label: this.$t('details.artifact')
                    },
                    {
                        key: 'REPORT',
                        label: this.$t('details.report')
                    }
                ]
            },
            thirdPartyReportList () {
                return this.outputs.filter((report) => this.isThirdReport(report.reportType))
            },
            hasNext () {
                return this.pagination.count < this.outputs.length
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
                return [
                    ...this.outputs.filter((output) => !this.isThirdReport(output.reportType)),
                    ...thirdReportList
                ]
            },
            isActiveThirdReport () {
                return this.isThirdReport(this.activeOutput?.reportType)
            },
            isCustomizeReport () {
                return this.activeOutput?.reportType === 'INTERNAL'
            },
            btns () {
                const defaultBtns = [
                    {
                        text: this.$t('details.goRepo'),
                        handler: () => {
                            const pos = this.activeOutput.fullPath.lastIndexOf('/')
                            const fileName = this.activeOutput.fullPath.substring(0, pos)
                            const repoName = repoTypeNameMap[this.activeOutput.artifactoryType]
                            window.open(
                                `${WEB_URL_PREFIX}/repo/${
                                    this.$route.params.projectId
                                }/generic?repoName=${repoName}&path=${encodeURIComponent(
                                    `${fileName}/default`
                                )}`,
                                '_blank'
                            )
                        }
                    }
                ]
                if (this.hasPermission && this.activeOutput.type === 'ARTIFACT') {
                    switch (true) {
                        case this.activeOutput.artifactoryType !== 'IMAGE':
                            defaultBtns.unshift({
                                text: this.$t('download'),
                                handler: () => window.open(this.activeOutputDetail.url, '_blank')
                            })
                            break
                    }
                }
                return defaultBtns
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
                return [
                    {
                        key: 'baseInfo',
                        title: this.$t('settings.baseInfo'),
                        block: this.baseInfoRows,
                        value: this.activeOutputDetail
                    },
                    {
                        key: 'checkSum',
                        title: this.$t('details.checkSum'),
                        block: this.checkSumRows,
                        value: this.activeOutputDetail.checksums
                    },
                    {
                        key: 'meta',
                        title: this.$t('metaData'),
                        value: this.activeOutputDetail.nodeMetadata
                    }
                ]
            },
            baseInfoRows () {
                return [
                    { key: 'name', name: this.$t('details.name') },
                    { key: 'fullName', name: this.$t('details.filePath') },
                    { key: 'size', name: this.$t('details.size') },
                    { key: 'createdTime', name: this.$t('createdTime') },
                    { key: 'modifiedTime', name: this.$t('details.lastModified') }
                ]
            },
            checkSumRows () {
                return [
                    { key: 'sha256', name: 'SHA256' },
                    { key: 'sha1', name: 'SHA1' },
                    { key: 'md5', name: 'MD5' }
                ]
            },
            filterConditionLength () {
                if (!this.filtering) return 0
                return Object.keys(this.filterConditionMap).filter(key => {
                    if (Array.isArray(this.filterConditionMap[key])) {
                        return this.filterConditionMap[key].length > 0
                    }
                    return !!this.filterConditionMap[key]
                }).length
            },
            conditions () {
                return [
                    {
                        id: 'triggerTime',
                        label: this.$t('details.triggerTime'),
                        component: 'bk-date-picker',
                        props: {
                            type: 'datetimerange',
                            shortcuts: this.shortcuts,
                            value: this.filterConditionMap.timeRange
                        },
                        listeners: {
                            change: (range) => {
                                this.filterConditionMap.timeRange = range
                            }
                        }
                    },
                    {
                        id: 'buildNo',
                        label: this.$t('构建号'),
                        component: 'bk-input',
                        props: {
                            value: this.filterConditionMap.buildNo
                        },
                        listeners: {
                            change: (buildNo) => {
                                this.filterConditionMap.buildNo = buildNo
                            }
                        }
                    },
                    {
                        id: 'filename',
                        label: this.$t('文件名'),
                        component: 'bk-input',
                        props: {
                            value: this.filterConditionMap.filename
                        },
                        listeners: {
                            change: (filename) => {
                                this.filterConditionMap.filename = filename
                            }
                        }
                    },
                    {
                        id: 'creator',
                        label: this.$t('触发人'),
                        component: 'bk-input',
                        props: {
                            value: this.filterConditionMap.creator
                        },
                        listeners: {
                            change: (creator) => {
                                this.filterConditionMap.creator = creator
                            }
                        }
                    },
                    {
                        id: 'property',
                        label: this.$t('元数据'),
                        component: 'bk-input',
                        props: {
                            value: this.filterConditionMap.property
                        },
                        listeners: {
                            change: (property) => {
                                this.filterConditionMap.property = property
                            }
                        }
                    }
                ]
            },
            shortcuts () {
                return [
                    {
                        text: '今天',
                        value () {
                            const end = new Date()
                            const start = new Date(end.getFullYear(), end.getMonth(), end.getDate())
                            return [start, end]
                        },
                        onClick: picker => {
                            console.log(picker)
                        }
                    },
                    {
                        text: '近7天',
                        value () {
                            const end = new Date()
                            const start = new Date()
                            start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
                            return [start, end]
                        }
                    },
                    {
                        text: '近15天',
                        value () {
                            const end = new Date()
                            const start = new Date()
                            start.setTime(start.getTime() - 3600 * 1000 * 24 * 15)
                            return [start, end]
                        }
                    }
                ]
            }
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
                this.$nextTick(this.init)
            }
        },
        mounted () {
            this.init()
        },
        methods: {
            ...mapActions('common', [
                'requestFileInfo',
                'requestOutputs',
                'requestExecPipPermission',
                'requestExternalUrl',
                'requestDownloadUrl'
            ]),
            toggleCollapseAside () {
                console.log(this.asideCollpased)
                this.asideCollpased = !this.asideCollpased
            },
            closeOutputsFilterAside (reset = false) {
                this.outputsFilterAsideVisible = false
                if (reset) {
                    this.filtering = false
                    this.filterConditionMap = {
                        timeRange: [],
                        buildNo: '',
                        filename: '',
                        creator: '',
                        property: null
                    }
                }
            },
            showOutputsFilterAside () {
                this.outputsFilterAsideVisible = true
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
                            pipelineOutputType: this.currentTab,
                            ...(Object.keys(this.filterConditionMap).filter(key => !!this.filterConditionMap[key]).reduce((result, key) => {
                                result[key] = this.filterConditionMap[key]
                                return result
                            }, {})),
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
                        const icon = isReportOutput ? 'order' : extForFile(item.name)
                        const id = isReportOutput ? (item.createTime + item.indexFileUrl) : item.fullPath
                        const type = this.isArtifact(item.artifactoryType) ? 'ARTIFACT' : ''
                        return {
                            type,
                            ...item,
                            id,
                            icon,
                            isApp: ['ipafile', 'apkfile'].includes(icon),
                            downloadable: hasPermission && this.isArtifact(item.artifactoryType) && item.artifactoryType !== 'IMAGE'
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
                const { projectId } = this.$route.params
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
                        size: res.size > 0 ? convertFileSize(res.size, 'B') : '--',
                        createdTime: convertTime(res.createdTime * 1000),
                        modifiedTime: convertTime(res.modifiedTime * 1000),
                        icon: extForFile(res.name)
                    }
                    this.isLoading = false
                } catch (err) {
                    this.handleError(err, [
                        {
                            actionId: this.$permissionActionMap.view,
                            resourceId: this.$permissionResourceMap.artifactory,
                            instanceId: [],
                            projectId: projectId
                        }
                    ])
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
            async submitFilter () {
                console.log(this.filterConditionMap)
                this.filtering = true
                await this.init()
                this.closeOutputsFilterAside(false)
            },

            switchTab (tab) {
                this.currentTab = tab
            },
            isArtifact (artifactoryType) {
                return ['PIPELINE', 'CUSTOM_DIR', 'IMAGE'].includes(artifactoryType)
            },
            isThirdReport (reportType) {
                return ['THIRDPARTY'].includes(reportType)
            },
            async downloadArtifact (output) {
                try {
                    const res = await this.requestDownloadUrl({
                        projectId: this.$route.params.projectId,
                        artifactoryType: output.artifactoryType,
                        path: output.fullPath
                    })

                    window.open(res.url2, '_blank')
                } catch (error) {
                    console.error(error)
                }
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
  .no-outputs-placeholder {
    color: #979ba5;
    display: flex;
    height: 100%;
    align-items: center;
    justify-content: center;
    flex-direction: column;
    > span {
      font-size: 12px;
      margin-top: -46px;
    }
  }
  .pipeline-exec-outputs-aside {
    position: relative;
    width: 30vw;
    flex-shrink: 0;
    padding: 16px 11px;
    border-right: 1px solid #dcdee5;
    display: flex;
    flex-direction: column;
    transition: all 0.3s;

    &.pipeline-exec-outputs-aside-collapse {
        width: 0;
        padding: 0;
        .pipeline-exec-output-classify-tab,
        .pipeline-exec-outputs-filter {
            display: none;
        }
        .aside-collapse-icon {
            transform: rotate(180deg);
        }
    }
    .collapse-handler {
        position: absolute;
        right: -16px;
        top: 50%;
        display: flex;
        cursor: pointer;
        align-items: center;
        width: 16px;
        height: 100px;
        background: #DCDEE5;
        border-radius: 0 4px 4px 0;
        transform: translateY(-50px);
        color: white;
        font-size: 12px;
        justify-content: center;
        font-weight: 700;
        z-index: 2;
        .aside-collapse-icon {
            transition: all 0.3s;
        }
    }
    .pipeline-exec-output-classify-tab {
      display: flex;
      align-items: center;
      background: #f0f1f5;
      border-radius: 2px;
      padding: 4px;
      flex-shrink: 0;
      > li {
        position: relative;
        flex: 1;
        text-align: center;
        border-radius: 2px;
        cursor: pointer;
        transition: all 0.5s ease;
        font-size: 12px;
        line-height: 24px;
        &.active {
          background: white;
          color: $primaryColor;
          &:after {
            display: none;
          }
        }
        &:not(:last-child):after {
          content: "";
          position: absolute;
          width: 1px;
          height: 12px;
          top: 6px;
          right: 0;
          background: #dcdee5;
        }
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
      > li {
        height: 32px;
        display: flex;
        align-items: center;
        padding: 10px 19px;
        cursor: pointer;
        border-radius: 2px;
        font-size: 12px;
        margin-bottom: 10px;
        > .devops-icon,
        .output-hover-icon {
          display: inline-flex;
          font-size: 16px;
          margin-right: 4px;
          flex-shrink: 0;
        }
        .output-hover-icon {
            font-size: 12px;
            display: none;
        }
        > span {
          flex: 1;
          @include ellipsis();
        }
        &.active,
        &:hover {
            color: $primaryColor;
            background: #f5f7fa;
            .output-hover-icon  {
                display: inline-flex;
            }
        }
      }
    }
  }
  .pipeline-exec-outputs-section {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    .pipeline-exec-output-header {
      display: flex;
      align-items: center;
      height: 48px;
      background: #fafbfd;
      padding: 0 24px;
      flex-shrink: 0;
      &-name {
        display: flex;
        align-items: center;
        font-size: 16px;
        color: #313238;
        padding-right: 16px;
        > i {
          padding-right: 12px;
        }
      }
      .pipeline-exec-output-actions {
        display: grid;
        grid-gap: 16px;
        grid-auto-flow: column;
        align-items: center;
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
      .pipeline-exec-output-block-title {
        font-size: 14px;
        border-bottom: 1px solid #dcdee5;
        margin: 0 0 16px 0;
        line-height: 24px;
      }
      .pipeline-exec-output-block-content {
        font-size: 12px;
        > li {
          margin-bottom: 16px;
          .pipeline-exec-output-block-row-label {
            color: #979ba5;
            text-align: right;
            @include ellipsis();
            width: 100px;
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
    > header {
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
    > footer {
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
