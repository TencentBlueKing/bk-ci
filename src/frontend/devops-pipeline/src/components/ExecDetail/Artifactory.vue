<template>
    <article class="detail-artifactory-home" v-bkloading="{ isLoading }">
        <bk-table :data="artifactories"
            :outer-border="false"
            :header-border="false"
            :header-cell-style="{ background: '#f1f2f3' }"
        >
            <bk-table-column :label="$t('details.artifactName')" width="220" prop="name" show-overflow-tooltip></bk-table-column>
            <bk-table-column :label="$t('details.path')" prop="fullName" show-overflow-tooltip></bk-table-column>
            <bk-table-column :label="$t('details.filesize')" width="150" prop="size" :formatter="sizeFormatter" show-overflow-tooltip></bk-table-column>
            <bk-table-column :label="$t('details.repoType')" width="150" prop="artifactoryType" :formatter="repoTypeFormatter" show-overflow-tooltip></bk-table-column>
            <bk-table-column :label="$t('operate')" width="150">
                <template slot-scope="props">
                    <bk-button text
                        @click="downLoadFile(props.row)"
                        :disabled="!hasPermission"
                        v-bk-tooltips="{ content: $t('details.noDownloadPermTips'), disabled: hasPermission }"
                    >{{ $t('download') }}</bk-button>
                </template>
            </bk-table-column>
        </bk-table>
    </article>
</template>

<script>
    import { convertFileSize } from '@/utils/util'

    export default {
        props: {
            taskId: String
        },

        data () {
            return {
                hasPermission: true,
                isLoading: true,
                artifactories: []
            }
        },

        created () {
            this.initData()
        },

        methods: {
            initData () {
                const routeParam = this.$route.params || {}
                const postData = {
                    projectId: routeParam.projectId,
                    params: {
                        props: {
                            buildId: routeParam.buildNo,
                            pipelineId: routeParam.pipelineId,
                            taskId: this.taskId
                        }
                    }
                }
                const permissionData = {
                    projectId: routeParam.projectId,
                    pipelineId: routeParam.pipelineId,
                    permission: 'DOWNLOAD'
                }
                this.isLoading = true
                Promise.all([
                    this.$store.dispatch('common/requestPartFile', postData),
                    this.$store.dispatch('common/requestExecPipPermission', permissionData)
                ]).then(([res, permission]) => {
                    this.artifactories = res.records || []
                    this.hasPermission = permission
                    if (this.artifactories.length <= 0) {
                        this.$emit('hidden')
                    }
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isLoading = false
                    this.$emit('complete')
                })
            },

            downLoadFile (row) {
                this.$store.dispatch('common/requestDownloadUrl', {
                    projectId: this.$route.params.projectId,
                    artifactoryType: row.artifactoryType,
                    path: row.path
                }).then((res) => {
                    const url = res.url2
                    window.location.href = url
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                })
            },

            repoTypeFormatter (row, column, cellValue, index) {
                const typeMap = {
                    CUSTOM_DIR: this.$t('details.customRepo'),
                    PIPELINE: this.$t('details.pipelineRepo')
                }
                return typeMap[cellValue]
            },

            sizeFormatter (row, column, cellValue, index) {
                return (cellValue >= 0 && convertFileSize(cellValue, 'B')) || ''
            }
        }
    }
</script>

<style lang="scss" scoped>
    .detail-artifactory-home {
        padding: 32px;
        height: calc(100% - 59px);
        ::v-deep .bk-table {
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
