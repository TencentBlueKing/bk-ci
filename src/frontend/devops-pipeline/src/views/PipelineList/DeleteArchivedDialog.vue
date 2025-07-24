<template>
    <bk-dialog
        ext-cls="bk-devops-center-align-dialog archive"
        width="480"
        render-directive="if"
        v-model="isShowDeleteArchivedDialog"
        :mask-close="false"
        :auto-close="false"
        :draggable="false"
        footer-position="center"
        :on-close="cancel"
    >
        <template #header>
            <span class="pipeline-warning-icon">
                <i class="devops-icon icon-exclamation" />
            </span>
            <div class="archive-title">
                <span v-if="isArchiveBatch">
                    {{ $t('deleteTheNumberOfPipelineConfirm', [pipelineList.length]) }}
                </span>
                <span v-else>{{ $t('deletePipelineConfirm') }}</span>
            </div>
        </template>
        <main>
            <p
                v-if="!isArchiveBatch"
                class="active-tip-head"
            >
                <span class="label">{{ $t('pipeline') }}：</span>
                <span class="value">{{ pipelineName }}</span>
            </p>

            <div class="active-tip-block">
                <i18n
                    tag="p"
                    path="archive.afterArchivedPipelineDeletion"
                >
                    <span class="active-tip">{{ $t('archive.cannotBeRecovered') }}</span>
                </i18n>
            </div>

            <ul
                v-if="isArchiveBatch"
                class="archive-pipeline-list"
            >
                <li
                    v-for="pipeline in pipelineList"
                    :key="pipeline.pipelineId"
                    :title="pipeline.pipelineName"
                >
                    {{ pipeline.pipelineName }}
                </li>
            </ul>
        </main>
        <footer slot="footer">
            <bk-button
                theme="danger"
                :loading="isSubmiting"
                @click="submit"
            >
                {{ $t('delete') }}
            </bk-button>
            <bk-button @click="cancel">
                {{ $t('cancel') }}
            </bk-button>
        </footer>
    </bk-dialog>
</template>

<script>
    import { mapActions } from 'vuex'
    export default {
        name: 'delete-archive-dialog',
        props: {
            isShowDeleteArchivedDialog: Boolean,
            pipelineList: {
                type: Array,
                requied: true
            },
            type: String
        },
        data () {
            return {
                isSubmiting: false
            }
        },
        computed: {
            isArchiveBatch () {
                return this.type === 'archiveBatch'
            },
            pipelineName () {
                return this.pipelineList[0]?.pipelineName
            },
            pipelineId  () {
                return this.pipelineList[0]?.pipelineId
            },
            projectId () {
                return this.$route.params.projectId
            }
        },
        methods: {
            ...mapActions('pipelines', [
                'deleteMigrateArchive',
                'batchDeleteMigrateArchive',
                'requestGetGroupLists'
            ]),
            async handleSingleDeleteArchive () {
                return await this.deleteMigrateArchive({
                    projectId: this.projectId,
                    pipelineId: this.pipelineId
                })
            },
            async handleBatchDeleteArchive () {
                const pipelineIds = this.pipelineList.map(pipeline => pipeline.pipelineId)
                return await this.batchDeleteMigrateArchive({
                    projectId: this.projectId,
                    pipelineIds
                })
            },
            async submit () {
                if (this.isSubmiting) return
                let message = this.$t('archive.pipelineDeletionSuccess')
                let theme = 'success'

                try {
                    this.isSubmiting = true
                    let res
                    if (this.isArchiveBatch) {
                        res = await this.handleBatchDeleteArchive()
                    } else {
                        res = await this.handleSingleDeleteArchive()
                    }
                    if (!res) {
                        throw Error(this.$t('archive.pipelineDeletionFailure'))
                    }
                    this.requestGetGroupLists(this.$route.params)
                    this.$emit('done')
                } catch (e) {
                    message = e.message || this.$t('archive.pipelineDeletionFailure')
                    theme = 'error'
                } finally {
                    this.$showTips({
                        message,
                        theme
                    })
                    this.isSubmiting = false
                    this.cancel()
                }
            },
            cancel () {
                this.$emit('cancel')
            }
        }
    }
</script>

<style lang="scss" scoped>
.archive {
  .bk-dialog-body {
    padding: 3px 32px 24px;
  }
  .bk-dialog-footer {
    background-color: #fff;
    border: none;
    padding-bottom: 24px;
  }
  .pipeline-warning-icon {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      background-color: #FFE8C3;
      color: #FF9C01;
      width: 42px;
      height: 42px;
      font-size: 24px;
      border-radius: 50%;
      flex-shrink: 0;
  }
  .archive-title {
      font-size: 20px;
      line-height: 32px;
      color: #313238;
      margin: 19px 0 8px 0;
  }
  .active-tip-head {
    font-size: 14px;
    margin-bottom: 16px;
    .label {
      color: #63656E;
    }
    .value {
      color: #313238;
      word-break: break-all;
    }
  }
  .active-tip-block {
    width: 416px;
    background: #F5F6FA;
    border-radius: 2px;
    padding: 12px 16px;
    font-size: 14px;
    color: #63656E;
    line-height: 22px;
  
    .active-tip {
      font-weight: 700;
      font-size: 14px;
      color: #FF9C01;
    }
  }
  .archive-pipeline-list {
    border: 1px solid #DCDEE5;
    border-radius: 2px;
    margin-top: 16px;
    overflow: auto;
    flex: 1;
    width: 100%;
    max-height: 320px;

    >li {
        width: 430px;
        height: 40px;
        line-height: 40px;
        padding: 0 16px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        text-align: left;
        border-bottom: 1px solid #DCDEE5;

        &:last-child {
            border-bottom: 0;
        }
    }
  }
}
</style>
