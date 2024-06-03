<template>
    <article class="detail-artifactory-home" v-bkloading="{ isLoading }">
        <bk-table
            :data="artifactories"
            :outer-border="false"
            :header-border="false"
            :header-cell-style="{ background: '#f1f2f3' }"
        >
            <bk-table-column
                :label="$t('details.artifactName')"
                width="220"
                prop="name"
                show-overflow-tooltip
            >
                <template slot-scope="props">
                    <div class="table-part-item part-item-name">
                        <Logo
                            v-if="props.row.artifactoryType === 'IMAGE'"
                            class="image-icon"
                            name="docker-svgrepo-com"
                            size="30"
                        />
                        <i v-else :class="['devops-icon', `icon-${props.row.icon}`]"></i>
                        <span class="ml5" :title="props.row.name">{{ props.row.name }}</span>
                    </div>
                </template>
            </bk-table-column>
            <bk-table-column
                :label="$t('details.path')"
                prop="fullName"
                show-overflow-tooltip
            >
            </bk-table-column>
            <bk-table-column
                :label="$t('details.filesize')"
                width="150"
                prop="size"
                show-overflow-tooltip
            >
                <template slot-scope="props">
                    {{ !props.row.folder ? sizeFormatter(props.row.size) : sizeFormatter(getFolderSize(props.row)) }}
                </template>
            </bk-table-column>
            <bk-table-column
                :label="$t('details.repoType')"
                width="150"
                prop="artifactoryType"
                :formatter="repoTypeFormatter"
                show-overflow-tooltip
            >
            </bk-table-column>
            <bk-table-column :label="$t('operate')" width="150">
                <template slot-scope="props">
                    <artifact-download-button
                        :output="props.row"
                        :has-permission="hasPermission"
                        v-bind="props.row"
                        :artifactory-type="props.row.artifactoryType"
                    />
                </template>
            </bk-table-column>
        </bk-table>
    </article>
</template>

<script>
    import ArtifactDownloadButton from '@/components/ArtifactDownloadButton'
    import Logo from '@/components/Logo'
    import { extForFile } from '@/utils/pipelineConst'
    import { convertFileSize } from '@/utils/util'

    export default {
        components: {
            Logo,
            ArtifactDownloadButton
        },

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

        watch: {
            taskId () {
                console.log('chjange')
                this.$nextTick(() => {
                    this.initData()
                })
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
                ])
                    .then(([res, permission]) => {
                        this.artifactories
                            = res.records.map((item) => ({
                                ...item,
                                icon: item.folder ? 'folder' : extForFile(item.name),
                                size: item.folder ? this.sizeFormatter(this.getFolderSize(item)) : this.sizeFormatter(item.size)
                            })) || []
                        this.hasPermission = permission
                        if (this.artifactories.length > 0) {
                            this.$emit('toggle', true)
                        }
                    })
                    .catch((err) => {
                        this.$bkMessage({ theme: 'error', message: err.message || err })
                    })
                    .finally(() => {
                        this.isLoading = false
                        this.$emit('complete')
                    })
            },

            repoTypeFormatter (row, column, cellValue, index) {
                const typeMap = {
                    CUSTOM_DIR: this.$t('details.customRepo'),
                    PIPELINE: this.$t('details.pipelineRepo'),
                    IMAGE: this.$t('details.imageRepo')
                }
                return typeMap[cellValue]
            },

            sizeFormatter (cellValue) {
                return (cellValue >= 0 && convertFileSize(cellValue, 'B')) || ''
            },

            getFolderSize (payload) {
                if (!payload.folder) return '0'
                return this.getValuesByKey(payload.properties, 'size')
            },

            getValuesByKey (data, key) {
                for (const item of data) {
                    if (key.includes(item.key)) {
                        return item.value
                    }
                }
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
    td,
    th.is-leaf {
      border-bottom-color: #f0f1f5;
    }
    .bk-table-body-wrapper {
      max-height: calc(100% - 43px);
      overflow-y: auto;
    }
    .cell {
      overflow: hidden;
    }
    .bk-table-header,
    .bk-table-body {
      width: auto !important;
    }
  }
  .part-item-name {
    display: flex;
    align-items: center;
  }
}
</style>
