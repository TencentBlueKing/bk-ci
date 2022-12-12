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
            <div class="pipeline-exec-outputs-filter">
                <i class="devops-icon icon-filter"></i>
                {{ $t('条件查询') }}
                <bk-tag class="output-filter-condition-count">2</bk-tag>
            </div>
            <ul class="pipeline-exec-outputs-list">
                <li
                    v-for="output in outputs"
                    :key="output.fullPath"
                    :class="{
                        active: output.fullPath === activeOutputFullPath
                    }"
                    @click="activeOutput(output)"
                >
                    <i :class="['devops-icon', `icon-${output.icon}`]"></i>
                    <span>{{output.name}}</span>
                </li>
            </ul>
        </aside>
        <section class="pipeline-exec-outputs-section">
            <template v-if="activeOutputDetail">
                <div
                    class="pipeline-exec-output-header"
                >
                    <span class="pipeline-exec-output-header-name">
                        <i :class="`devops-icon icon-${activeOutputDetail.icon}`" />
                        {{ activeOutputDetail.name }}
                    </span>
                    <bk-tag theme="info">{{ $t('流水线仓库') }}</bk-tag>
                    <p class="pipeline-exec-output-actions">
                        <bk-button
                            text
                            theme="primary"
                            v-for="btn in btns"
                            :key="btn.text"
                        >
                            {{btn.text}}
                        </bk-button>
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
    import { mapActions } from 'vuex'
    import { convertTime, convertFileSize } from '@/utils/util'
    const fileExtIconMap = {
        txt: ['.json', '.txt', '.md'],
        zip: ['.zip', '.tar', '.tar.gz', '.tgz', '.jar', '.gz'],
        apkfile: ['.apk'],
        ipafile: ['.ipa']
    }
    export default {
        components: {
            // Logo
        },
        data () {
            return {
                currentTab: 'all',
                outputs: [],
                activeOutputFullPath: '',
                activeOutputDetail: null,
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
            outputList () {
                return [
                    {
                        id: 1,
                        name: 'docker',
                        icon: 'docker-shape'
                    },
                    {
                        id: 2,
                        name: 'test.log',
                        icon: this.extForFile('test.log')
                    },
                    {
                        id: 3,
                        name: 'devops.apk',
                        icon: this.extForFile('devops.apk')
                    },
                    {
                        id: 7,
                        name: 'devops.ipa',
                        icon: this.extForFile('devops.ipa')
                    },
                    {
                        id: 4,
                        name: 'dist.tar.gz',
                        icon: this.extForFile('dist.tar.gz')
                    },
                    {
                        id: 5,
                        name: 'output.tar',
                        icon: this.extForFile('output.tar')
                    },
                    {
                        id: 6,
                        name: '代码分析报告',
                        icon: 'order'
                    }
                ]
            },
            btns () {
                return [{
                    text: this.$t('download'),
                    handler: () => {}
                }, {
                    text: this.$t('details.shareOutput'),
                    handler: () => {}
                }, {
                    text: this.$t('details.goRepo'),
                    handler: () => {}
                }]
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
                'requestPartFile',
                'requestFileInfo'
            ]),
            async init () {
                const { projectId, pipelineId, buildNo: buildId } = this.$route.params

                try {
                    this.isLoading = true
                    const res = await this.requestPartFile({
                        projectId,
                        params: {
                            props: {
                                pipelineId,
                                buildId
                            }
                        }
                    })

                    this.outputs = res.records.map((item) => ({
                        ...item,
                        icon: this.extForFile(item.name)
                    }))
                    console.log(this.outputs, 'this.outputs')
                } catch (err) {
                    this.$showTips({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                } finally {
                    this.isLoading = false
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
                        ...res,
                        size: res.size > 0 ? convertFileSize(res.size, 'B') : '--',
                        createdTime: convertTime(res.createdTime * 1000),
                        modifiedTime: convertTime(res.modifiedTime * 1000),
                        icon: this.extForFile(res.name)
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
            activeOutput (output) {
                this.activeOutputFullPath = output.fullPath
                this.showDetail(output)
            },
            switchTab (tab) {
                this.currentTab = tab
            },
            /**
             * 判断文件类型
             */
            extForFile (name) {
                const defaultIcon = 'file'
                const pos = name.lastIndexOf('.')
                if (pos > -1) {
                    const ext = name.substring(pos)
                    return Object.keys(fileExtIconMap).find(key => {
                        const arr = fileExtIconMap[key]
                        return arr.includes(ext)
                    }) ?? defaultIcon
                }
                return defaultIcon
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
