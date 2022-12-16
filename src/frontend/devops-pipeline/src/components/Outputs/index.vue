<template>
    <div class="pipeline-exec-outputs">
        <aside class="pipeline-exec-outputs-aside">
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
            <!-- <div class="pipeline-exec-outputs-filter">
                <i class="devops-icon icon-filter"></i>
                {{ $t('条件查询') }}
                <bk-tag class="output-filter-condition-count">2</bk-tag>
            </div> -->
            <ul class="pipeline-exec-outputs-list">
                <li
                    v-for="output in outputs"
                    :key="output.id"
                    :class="{
                        active: output.id === activeOutput.id
                    }"
                    @click="setActiveOutput(output)"
                >
                    <i :class="['devops-icon', `icon-${output.icon}`]"></i>
                    <span>{{output.name}}</span>
                </li>
            </ul>
        </aside>
        <section v-bkloading="{ isLoading }" class="pipeline-exec-outputs-section">
            <iframe-report v-if="isCustomizeReport" :index-file-url="activeOutput.indexFileUrl" />
            <third-party-report v-else-if="isThirdReport" :report-list="thirdPartyReportList" />
            <template v-else-if="activeOutputDetail">
                <div
                    class="pipeline-exec-output-header"
                >
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
                            {{btn.text}}
                        </bk-button>
                        <bk-popover theme="light" placement="bottom-end" v-if="activeOutputDetail.isApp">
                            <i class="devops-icon icon-qrcode" />
                            <qrcode slot="content" :text="activeOutputDetail.shortUrl" :size="100" />
                        </bk-popover>
                    </p>
                </div>
                <div
                    v-for="block in infoBlocks"
                    :key="block.title"
                    class="pipeline-exec-output-block"
                >
                    <h6 class="pipeline-exec-output-block-title">{{ block.title }}</h6>
                    <bk-table v-if="block.key === 'meta'" :data="block.value">
                        <bk-table-column :label="$t('view.key')" prop="key"></bk-table-column>
                        <bk-table-column :label="$t('view.value')" prop="value"></bk-table-column>
                        <bk-table-column :label="$t('desc')" prop="desc"></bk-table-column>
                    </bk-table>
                    <ul v-else slot="content" class="pipeline-exec-output-block-content">
                        <li v-for="row in block.block" :key="row.key"
                        >
                            <span class="pipeline-exec-output-block-row-label">
                                {{row.name}}：
                            </span>
                            <span class="pipeline-exec-output-block-row-value">
                                {{block.value[row.key] || '--'}}
                            </span>
                        </li>
                    </ul>
                </div>
            </template>
        </section>
    </div>
</template>

<script>
    // import Logo from '@/components/Logo'
    import qrcode from '@/components/devops/qrcode'
    import { mapActions } from 'vuex'
    import ThirdPartyReport from '@/components/Outputs/ThirdPartyReport'
    import IframeReport from '@/components/Outputs/IframeReport'
    import { convertTime, convertFileSize } from '@/utils/util'
    import { extForFile, repoTypeMap } from '@/utils/pipelineConst'
    
    export default {
        components: {
            ThirdPartyReport,
            IframeReport,
            qrcode
        },
        data () {
            return {
                currentTab: 'all',
                reports: [],
                artifacts: [],
                activeOutput: '',
                activeOutputDetail: null,
                hasPermission: false,
                isLoading: false
            }
        },
        computed: {
            outputClassifyList () {
                return [{
                    key: 'all',
                    label: this.$t('editPage.all')
                }, {
                    key: 'artifact',
                    label: this.$t('details.artifact')
                }, {
                    key: 'report',
                    label: this.$t('execDetail.report')
                }]
            },
            outputs () {
                switch (this.currentTab) {
                    case 'artifact':
                        return this.artifacts
                    case 'report':
                        return this.reports
                    default:
                        return [
                            ...this.artifacts,
                            ...this.customizeReportList,
                            ...(this.thirdPartyReportList.length > 0
                                ? [{
                                    id: 'THIRDPARTY',
                                    type: 'THIRDPARTY',
                                    name: this.$t('details.thirdReport'),
                                    icon: 'bar-chart'
                                }]
                                : [])
                        ]
                }
            },
            isThirdReport () {
                return this.activeOutput?.type === 'THIRDPARTY'
            },
            isCustomizeReport () {
                return this.activeOutput?.type === 'INTERNAL'
            },
            thirdPartyReportList () {
                return this.reports.filter(report => report.type === 'THIRDPARTY')
            },
            customizeReportList () {
                return this.reports.filter(report => report.type === 'INTERNAL')
            },
            btns () {
                const defaultBtns = [{
                    text: this.$t('details.goRepo'),
                    handler: () => {}
                }]
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
                console.log(defaultBtns)
                return defaultBtns
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
                        value: Object.keys(this.activeOutputDetail.meta).map((key) => ({
                            key,
                            value: this.activeOutputDetail.meta[key],
                            desc: 'desc'
                        }))
                    }
                ]
            },
            baseInfoRows () {
                return [
                    { key: 'name', name: 'Name' },
                    { key: 'fullName', name: 'Path' },
                    { key: 'size', name: 'Size' },
                    { key: 'createdTime', name: 'Created' },
                    { key: 'modifiedTime', name: 'Last Modified' }
                ]
            },
            checkSumRows () {
                return [
                    { key: 'sha256', name: 'SHA256' },
                    { key: 'sha1', name: 'SHA1' },
                    { key: 'md5', name: 'MD5' }
                ]
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
            async init () {
                const { projectId, pipelineId, buildNo: buildId } = this.$route.params

                try {
                    this.isLoading = true
                    const [, res] = await Promise.all([
                        this.requestHasPermission(),
                        this.requestOutputs({
                            projectId,
                            pipelineId,
                            buildId
                        })
                    ])
                    
                    this.artifacts = res.artifacts.map((item) => {
                        const icon = extForFile(item.name)
                        return {
                            ...item,
                            id: item.fullPath,
                            type: 'ARTIFACT',
                            icon,
                            isApp: ['ipafile', 'apkfile'].includes(icon)
                        }
                    })

                    this.reports = res.reports.map((item) => ({
                        ...item,
                        id: item.taskId,
                        icon: 'order'
                    }))
                } catch (err) {
                    this.$showTips({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
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
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$showTips({
                        message,
                        theme
                    })
                }
            },
            async getDownloadUrl () {
                try {
                    const params = {
                        projectId: this.$route.params.projectId,
                        ...this.activeOutput
                    }
                    const [download, external] = await Promise.all([
                        this.requestDownloadUrl(params),
                        ...(this.activeOutput.isApp
                            ? [
                                this.requestExternalUrl(params)
                            ]
                            : [])
                    ])
                    return [
                        download.url,
                        external?.url2
                    ]
                } catch (err) {
                    this.handleError(err, [{
                        actionId: this.$permissionActionMap.download,
                        resourceId: this.$permissionResourceMap.pipeline,
                        instanceId: [{
                            id: this.$route.params.pipelineId,
                            name: this.$route.params.pipelineId
                        }],
                        projectId: this.$route.params.projectId
                    }])
                    return []
                }
            },
            async showDetail (output) {
                const { projectId } = this.$route.params
                try {
                    this.isLoading = true
                    const res = await this.requestFileInfo({
                        projectId,
                        type: output.artifactoryType,
                        path: `${output.fullPath}`
                    })
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
                    this.handleError(err, [{
                        actionId: this.$permissionActionMap.view,
                        resourceId: this.$permissionResourceMap.artifactory,
                        instanceId: [],
                        projectId: projectId
                    }])
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
            switchTab (tab) {
                this.currentTab = tab
            }
        }
    }
</script>

<style lang="scss">
    @import '@/scss/conf';
    @import '@/scss/mixins/ellipsis';
    .pipeline-exec-outputs {
        height: 100%;
        display: flex;
        .pipeline-exec-outputs-aside {
            width: 295px;
            flex-shrink: 0;
            padding: 16px 11px;
            border-right: 1px solid #DCDEE5;
            display: flex;
            flex-direction: column;
            .pipeline-exec-output-classify-tab {
                display: flex;
                align-items: center;
                background: #F0F1F5;
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
                        content: '';
                        position: absolute;
                        width: 1px;
                        height: 12px;
                        top: 6px;
                        right: 0;
                        background: #DCDEE5;
                    }
                }
            }
            .pipeline-exec-outputs-filter {
                position: relative;
                margin: 16px 0 21px 0;
                width: 100%;
                display: flex;
                align-items: center;
                justify-content: center;
                height: 32px;
                background: white;
                border: 1px solid #C4C6CC;
                border-radius: 2px;
                font-size: 14px;
                cursor: pointer;
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
                    >.devops-icon {
                        display: inline-block;
                        font-size: 16px;
                        margin-right: 4px;
                    }
                    &.active,
                    &:hover {
                        color: $primaryColor;
                        background: #F5F7FA;
                    }
                }
            }
        }
        .pipeline-exec-outputs-section {
            flex: 1;
            overflow: auto;
            .pipeline-exec-output-header {
                display: flex;
                align-items: center;
                height: 48px;
                background: #FAFBFD;
                padding: 0 24px;
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
            .pipeline-exec-output-block {
                padding: 16px 24px 0 24px;
                .pipeline-exec-output-block-title {
                    font-size: 14px;
                    border-bottom: 1px solid #DCDEE5;
                    margin:  0 0 16px 0;
                    line-height: 24px;
                }
                .pipeline-exec-output-block-content {
                    font-size: 12px;
                    > li {
                        margin-bottom: 16px;
                        .pipeline-exec-output-block-row-label {
                            color: #979BA5;
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
    }
</style>
