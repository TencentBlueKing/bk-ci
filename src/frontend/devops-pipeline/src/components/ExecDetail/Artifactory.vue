<template>
    <article class="detail-artifactory-home" v-bkloading="{ isLoading }">
        <bk-table :data="artifactories"
            :outer-border="false"
            :header-border="false"
            :header-cell-style="{ background: '#f1f2f3' }"
        >
            <bk-table-column :label="$t('details.artifactName')" prop="name" show-overflow-tooltip></bk-table-column>
            <bk-table-column :label="$t('details.path')" prop="path" show-overflow-tooltip></bk-table-column>
            <bk-table-column :label="$t('details.filesize')" prop="size" show-overflow-tooltip></bk-table-column>
            <bk-table-column :label="$t('details.repoType')" prop="artifactoryType" :formatter="repoTypeFormatter" show-overflow-tooltip></bk-table-column>
            <bk-table-column :label="$t('operate')" width="150">
                <template slot-scope="props">
                    <a :href="props.row.downloadUrl" target="_blank" class="download-link">{{ $t('download') }}</a>
                </template>
            </bk-table-column>
        </bk-table>
    </article>
</template>

<script>
    export default {
        props: {
            taskId: String
        },

        data () {
            return {
                isLoading: false,
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
                    params: [
                        { key: 'buildId', value: routeParam.buildNo },
                        { key: 'pipelineId', value: routeParam.pipelineId },
                        { key: 'taskId', value: this.taskId }
                    ]
                }
                this.isLoading = true
                this.$store.dispatch('pipelines/getArtifactories', postData).then((res) => {
                    const data = res.data || {}
                    this.artifactories = data.records || []
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isLoading = false
                })
            },

            repoTypeFormatter (row, column, cellValue, index) {
                const typeMap = {
                    CUSTOM_DIR: this.$t('details.customRepo'),
                    PIPELINE: this.$t('details.pipelineRepo')
                }
                return typeMap[cellValue]
            }
        }
    }
</script>

<style lang="scss" scoped>
    .detail-artifactory-home {
        padding: 32px;
        height: calc(100% - 59px);
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
        }
        .download-link {
            color: #3c96ff;
            cursor: pointer;
        }
    }
</style>
