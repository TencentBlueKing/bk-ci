<template>
    <div class="pipeline-exec-outputs">
        
        <aside :class="['pipeline-exec-outputs-aside', {
            'pipeline-exec-outputs-aside-collapse': asideCollpased
        }]">
            <template v-if="!asideCollpased">
                <div class="pipeline-exec-outputs-filter-input">
                    <bk-input
                        clearable
                        right-icon="bk-icon icon-search"
                        :placeholder="$t('outputsFilterPlaceholder')"
                        v-model="keyWord"
                    />
                </div>
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
                <ul v-if="outputs.length > 0" class="pipeline-exec-outputs-list">
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
    
                <div v-else class="no-outputs-placeholder">
                    <logo name="empty" size="180" />
                    <span>{{ $t("empty") }}</span>
                </div>
            </template>
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
    </div>
</template>

<script>
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
            OutputQrcode
        },
        data () {
            return {
                keyWord: '',
                isCopyDialogShow: false,
                isCopying: false,
                currentTab: 'all',
                outputs: [],
                activeOutput: '',
                activeOutputDetail: null,
                hasPermission: false,
                isLoading: false,
                asideCollpased: false
            }
        },
        computed: {
            outputClassifyList () {
                return [
                    {
                        key: 'all',
                        label: this.$t('editPage.all')
                    },
                    {
                        key: 'artifact',
                        label: this.$t('details.artifact')
                    },
                    {
                        key: 'report',
                        label: this.$t('details.report')
                    }
                ]
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
                    case 'artifact':
                        visibleOutputs = this.artifacts
                        break
                    case 'report':
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
                'requestExecPipPermission',
                'requestExternalUrl',
                'requestDownloadUrl'
            ]),
            toggleCollapseAside () {
                console.log(this.asideCollpased)
                this.asideCollpased = !this.asideCollpased
            },
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
                        const icon = isReportOutput ? 'order' : extForFile(item.name)
                        const id = isReportOutput ? item.createTime : item.fullPath
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
        .pipeline-exec-output-classify-tab {
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
}
</style>
