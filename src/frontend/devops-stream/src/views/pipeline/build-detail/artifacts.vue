<template>
    <article class="detail-artifactory-home" v-bkloading="{ isLoading }">
        <bk-table :data="artifactories"
            :outer-border="false"
            :header-border="false"
            :header-cell-style="{ background: '#f1f2f3' }"
            :empty-text="$t('pipeline.noArtifacts')"
        >
            <bk-table-column :label="$t('name')" width="220" show-overflow-tooltip>
                <template slot-scope="props">
                    <i v-if="props.row.artifactoryType === 'IMAGE'" class="stream-icon stream-docker"></i>
                    <i v-else class="stream-icon stream-file"></i>
                    <span>{{ props.row.name }}</span>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('pipeline.path')" show-overflow-tooltip>
                <template slot-scope="props">
                    <span v-if="props.row.artifactoryType === 'PIPELINE'">{{ props.row.name }}</span>
                    <span v-else-if="props.row.artifactoryType === 'IMAGE'">{{ getRepoName(props.row) }}</span>
                    <span v-else>{{ props.row.fullName }}</span>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('pipeline.size')" width="150" prop="size" :formatter="sizeFormatter" show-overflow-tooltip></bk-table-column>
            <bk-table-column :label="$t('operation')" width="300">
                <template slot-scope="props">
                    <bk-button text
                        v-if="props.row.artifactoryType !== 'IMAGE'"
                        v-bk-tooltips="{
                            content: $t('pipeline.downloadFailTips'),
                            disabled: hasPermission
                        }"
                        :disabled="!hasPermission"
                        @click="downLoadFile(props.row)"
                    >
                        {{$t('pipeline.download')}}
                    </bk-button>
                    <bk-popover
                        v-if="props.row.isApkOrIpa"
                        theme="light"
                        trigger="click"
                        placement="top"
                    >
                        <bk-button
                            class="ml5"
                            text
                            :disabled="!hasPermission"
                            @click="requestArtifactExternalUrl(props.row)"
                        >
                            {{$t('pipeline.qrCode')}}
                        </bk-button>
                        <div
                            slot="content"
                            v-bkloading="{ isLoading: fetchingUrl }"
                        >
                            <qr-code v-if="hasPermission" class="qrcode-view" :text="qrCodeUrl" :size="100"></qr-code>
                            <p v-else>{{$t('pipeline.downloadFailTips')}}</p>
                        </div>
                    </bk-popover>
                    <bk-button
                        text
                        class="ml5"
                        @click="goToRepo(props.row)"
                    >{{$t('pipeline.location')}}</bk-button>
                </template>
            </bk-table-column>
        </bk-table>
    </article>
</template>

<script>
    import { convertFileSize } from '@/utils'
    import { pipelines } from '@/http'
    import QrCode from '@/components/QrCode'
    import { mapState } from 'vuex'

    export default {
        components: {
            QrCode
        },
        data () {
            return {
                hasPermission: true,
                isLoading: true,
                qrCodeUrl: '',
                artifactories: [],
                fetchingUrl: false
            }
        },

        computed: {
            ...mapState(['projectId'])
        },

        watch: {
            '$route.params.buildId' () {
                this.initData()
            }
        },

        created () {
            this.initData()
        },

        methods: {
            initData () {
                const postData = {
                    projectId: this.projectId,
                    params: {
                        props: {
                            buildId: this.$route.params.buildId,
                            pipelineId: this.$route.params.pipelineId
                        }
                    }
                }
                const permissionData = {
                    projectId: this.projectId,
                    pipelineId: this.$route.params.pipelineId,
                    permission: 'DOWNLOAD'
                }
                this.isLoading = true
                Promise.all([
                    pipelines.requestPartFile(postData),
                    pipelines.requestExecPipPermission(permissionData)
                ]).then(([res, permission]) => {
                    this.artifactories = res.records.map(artifact => ({
                        ...artifact,
                        isApkOrIpa: this.isApkOrIpa(artifact)
                    }))
                    this.hasPermission = permission
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isLoading = false
                    this.$emit('complete')
                })
            },
            downLoadFile (row) {
                Promise.all([
                    // pipelines.requestDevnetGateway(),
                    pipelines.requestDownloadUrl({
                        projectId: this.projectId,
                        artifactoryType: row.artifactoryType,
                        path: row.path
                    })
                ]).then(([res]) => {
                    // const url = isDevnet ? res.url : res.url2
                    window.open(res.url, '_blank')
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                })
            },
            async requestArtifactExternalUrl (row, key, index, type) {
                try {
                    this.qrCodeUrl = ''
                    this.fetchingUrl = true
                    const res = await pipelines.requestArtifactExternalUrl({
                        projectId: this.projectId,
                        artifactoryType: row.artifactoryType,
                        path: row.path
                    })
                    console.log(res.url)
                    this.qrCodeUrl = res.url
                } catch (err) {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                } finally {
                    this.fetchingUrl = false
                }
            },
            sizeFormatter (row, column, cellValue, index) {
                return (cellValue >= 0 && convertFileSize(cellValue, 'B')) || ''
            },
            isApkOrIpa (row) {
                const type = row.name.toUpperCase().substring(row.name.lastIndexOf('.') + 1)
                return type === 'APK' || type === 'IPA'
            },
            getRepoName (row) {
                const properties = row.properties || []
                const projectInfo = properties.find(property => property.key === 'projectId') || {}
                const repoName = properties.find(property => property.key === 'repoName')
                return `${row.registry}/${projectInfo.value}/${repoName.value}/${row.fullName}`
            },
            goToRepo (row) {
                const properties = row.properties || []
                const projectInfo = properties.find(property => property.key === 'projectId') || {}
                const artifactoryType = row.artifactoryType === 'CUSTOM_DIR' ? 'custom' : 'pipeline'
                if (row.artifactoryType === 'IMAGE') {
                    const repoName = properties.find(property => property.key === 'repoName')
                    const [packageKey, version] = row.fullName.split(':')
                    window.open(`https://${BKREPO_HOST}/ui/${projectInfo.value}/docker/package?repoName=${repoName.value}&packageKey=docker%3A%2F%2F${packageKey}&version=${version}`, '_blank')
                } else if (projectInfo.value && artifactoryType) {
                    window.open(`https://${BKREPO_HOST}/ui/${projectInfo.value}/generic?repoName=${artifactoryType}&path=${window.encodeURIComponent(row.fullPath)}`, '_blank')
                }
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .detail-artifactory-home {
        padding: 32px;
        /deep/ .bk-table {
            border: none;
            height: 100%;
            &::before {
                background-color: #fff;
            }
            td, th.is-leaf {
                border-bottom-color: #f0f1f5;
            }
            .bk-table-body-wrapper {
                max-height: calc(100% - 43px);
                overflow-y: auto;
            }
            .cell {
                overflow: hidden;
            }
            .bk-table-header, .bk-table-body {
                width: auto !important;
            }
        }
    }
</style>
