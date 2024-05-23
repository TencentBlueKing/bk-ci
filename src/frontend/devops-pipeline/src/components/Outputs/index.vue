<template>
    <div style="height: 100%;width: 100%;">
        <bk-resize-layout
            :collapsible="true"
            class="pipeline-exec-outputs"
            :initial-divide="initWidth"
            :min="260"
            :max="800"
        >
            <aside slot="aside" class="pipeline-exec-outputs-aside">
                <div class="pipeline-exec-outputs-filter-input">
                    <bk-input
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
                <ul v-if="visibleOutputs.length > 0" class="pipeline-exec-outputs-list">
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
                        <p class="output-hover-icon-box">
                            <output-qrcode
                                v-if="output.isApp"
                                :output="output"
                            />
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
    
                <div v-else class="no-outputs-placeholder">
                    <logo name="empty" size="180" />
                    <span>{{ $t("empty") }}</span>
                </div>
            
            </aside>
            <section slot="main" v-bkloading="{ isLoading }" class="pipeline-exec-outputs-section">
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
                            {{ activeOutputDetail.name }}
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
                            <output-qrcode
                                :output="activeOutput"
                                v-if="activeOutputDetail.isApp"
                            />

                            <ext-menu v-if="!activeOutputDetail.folder" :data="activeOutputDetail" :config="artifactMoreActions"></ext-menu>
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
        </bk-resize-layout>
        <copy-to-custom-repo-dialog ref="copyToDialog" :artifact="activeOutput" />
    </div>
</template>

<script>
    import ArtifactDownloadButton from '@/components/ArtifactDownloadButton'
    import Logo from '@/components/Logo'
    import CopyToCustomRepoDialog from '@/components/Outputs/CopyToCustomRepoDialog'
    import IframeReport from '@/components/Outputs/IframeReport'
    import OutputQrcode from '@/components/Outputs/OutputQrcode'
    import ThirdPartyReport from '@/components/Outputs/ThirdPartyReport'
    import ExtMenu from '@/components/pipelineList/extMenu'
    import { extForFile, repoTypeMap, repoTypeNameMap } from '@/utils/pipelineConst'
    import { convertFileSize, convertTime } from '@/utils/util'
    import { mapActions } from 'vuex'
    
    export default {
        components: {
            Logo,
            ThirdPartyReport,
            IframeReport,
            ExtMenu,
            CopyToCustomRepoDialog,
            OutputQrcode,
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
                keyWord: '',
                isCopyDialogShow: false,
                isCopying: false,
                outputs: [],
                activeOutput: '',
                activeOutputDetail: null,
                hasPermission: false,
                isLoading: false
            }
        },
        computed: {
            initWidth () {
                return this.currentTab === 'reports' ? '300px' : '40%'
            },
            filterPlaceholder () {
                return this.$t(`${this.currentTab}FilterPlaceholder`)
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
            thirdPartyReportList () {
                return this.outputs.filter((report) => this.isThirdReport(report.reportType))
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
                return visibleOutputs.filter(output => output.name.toLowerCase().includes(this.keyWord.toLowerCase()))
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
        },
        watch: {
            visibleOutputs (outputs) {
                if (outputs.length > 0) {
                    this.setActiveOutput(outputs[0])
                } else {
                    this.activeOutputDetail = null
                }
            },
            '$route.params.buildNo': function () {
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
                'requestExecPipPermission'
            ]),
            
            async init () {
                const { projectId, pipelineId, buildNo: buildId } = this.$route.params

                try {
                    this.isLoading = true
                    const [hasPermission, res] = await Promise.all([
                        this.requestHasPermission(),
                        this.requestOutputs({
                            projectId,
                            pipelineId,
                            buildId
                        })
                    ])

                    this.outputs = res.map((item) => {
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
                    this.isLoading = false
                } catch (err) {
                    this.handleError(err, {
                        projectId,
                        resourceCode: pipelineId,
                        action: this.$permissionResourceAction.EXECUTE
                    })
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
    height: 100%;
    flex-shrink: 0;
    padding: 16px 11px;
    display: flex;
    flex-direction: column;
    .pipeline-exec-outputs-filter-input {
        margin: 12px 0;
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
      border: 1px solid #c4c6cc;
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
        display: flex;
        align-items: center;
        padding: 10px 19px;
        cursor: pointer;
        border-radius: 2px;
        font-size: 12px;
        margin-bottom: 10px;
        grid-gap: 10px;
        > .devops-icon {
          display: inline-flex;
          font-size: 16px;
          flex-shrink: 0;
          align-items: center;
        }
        .output-hover-icon-box {
            display: flex;
            align-items: center;
            grid-gap: 6px;
            :hover {
                color:$primaryColor;
            }
        }
        > span {
            flex: 1;
            display: -webkit-box;
            -webkit-box-orient: vertical;
            -webkit-line-clamp: 2;
            overflow: hidden;
            word-break: break-all;
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
            display: flex;
            align-items: center;
          margin-bottom: 16px;
          .pipeline-exec-output-block-row-label {
            color: #979ba5;
            text-align: right;
            @include ellipsis();
            width: 110px;
            flex-shrink: 0;
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
