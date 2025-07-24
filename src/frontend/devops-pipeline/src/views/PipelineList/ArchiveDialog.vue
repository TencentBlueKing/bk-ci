<template>
    <bk-dialog
        ext-cls="bk-devops-center-align-dialog archive"
        width="480"
        render-directive="if"
        v-model="isArchiveDialogShow"
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
                    {{ $t('archive.isArchiveConfirmedTitles', [pipelineList.length]) }}
                </span>
                <span v-else>{{ $t('archive.isArchiveConfirmedTitle') }}</span>
            </div>
        </template>
        <main>
            <bk-alert
                v-if="unableToArchivePipelines.length"
                type="warning"
                :show-icon="false"
                class="mb10"
            >
                <div
                    slot="title"
                    class="can-not-delete-tips"
                >
                    <span class="can-not-delete-tips-content">
                        {{ $t('archive.unableToArchive', [unableToArchivePipelines.length]) }}
                    </span>
                    <span
                        class="text-link"
                        @click="removeUnableToArchivePipeline"
                    >
                        {{ $t('archive.removeUnarchivablePipelines') }}
                    </span>
                </div>
            </bk-alert>
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
                    path="archive.archiveTips1"
                >
                    <span class="active-tip">{{ $t('archive.archiveTips2') }}</span>
                </i18n>
                <i18n
                    tag="p"
                    path="archive.archiveTips3"
                >
                    <span class="active-tip">{{ $t('archive.archiveTips4') }}</span>
                </i18n>
            </div>

            <ul
                v-if="isArchiveBatch && pipelineList.length"
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
                theme="warning"
                :loading="isSubmiting"
                :disabled="!!unableToArchivePipelines.length || !pipelineList.length"
                @click="submit"
            >
                {{ $t('archive.archive') }}
            </bk-button>
            <bk-button @click="cancel">
                {{ $t('cancel') }}
            </bk-button>
        </footer>
    </bk-dialog>
</template>

<script>
    import { mapActions } from 'vuex'
    import { statusAlias } from '@/utils/pipelineStatus'

    export default {
        name: 'archive-dialog',
        props: {
            isArchiveDialogShow: Boolean,
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
            projectId () {
                return this.$route.params.projectId
            },
            unableToArchivePipelines () {
                return this.pipelineList.filter(pipeline => pipeline.latestBuildStatus === statusAlias.RUNNING || pipeline.onlyDraftVersion || pipeline.runningBuildCount > 0)
            }
        },
        methods: {
            ...mapActions('pipelines', [
                'migrateArchivePipelineList',
                'batchMigrateArchivePipelineList',
                'requestGetGroupLists'
            ]),
            async handleSingleArchive (pipelineIds) {
                return await this.migrateArchivePipelineList({
                    projectId: this.projectId,
                    pipelineId: pipelineIds[0]
                })
            },
            async handleBatchArchive (pipelineIds) {
                return await this.batchMigrateArchivePipelineList({
                    projectId: this.projectId,
                    pipelineIds
                })
            },
            async submit () {
                if (this.isSubmiting) return

                try {
                    this.isSubmiting = true
                    const pipelineIds = this.pipelineList.map(pipeline => pipeline.pipelineId)
                    const res = this.isArchiveBatch
                        ? await this.handleBatchArchive(pipelineIds)
                        : await this.handleSingleArchive(pipelineIds)

                    if (res) {
                        const h = this.$createElement
                        const instance = this.$bkInfo({
                            type: 'success',
                            width: 460,
                            closeIcon: false,
                            showFooter: false,
                            title: this.$t('archive.submissionSuccess'),
                            subHeader: h('div', {
                                class: 'archiving-running'
                            }, [
                                h('p', this.$t('archive.archivingTaskRunning')),
                                h('p', { class: 'tips' }, this.$t('archive.deleteOrMoveYamlInRepoIfPacModeEnabled')),
                                h('bk-button', {
                                    class: 'button',
                                    on: {
                                        click: () => {
                                            this.$bkInfo.close(instance.id)
                                            this.requestGetGroupLists(this.$route.params)
                                            this.$emit('done')
                                        }
                                    }
                                }, this.$t('return'))
                            ])
                        })
                    }
                } catch (e) {
                    this.$showTips({
                        message: e.message || this.$t('archive.archivePipelineFail'),
                        theme: 'error'
                    })
                } finally {
                    this.isSubmiting = false
                    this.cancel()
                }
            },
            cancel () {
                this.$emit('cancel')
            },
            removeUnableToArchivePipeline () {
                this.$emit('toggleSelection', this.unableToArchivePipelines)
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
    width: 100%;
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
<style lang="scss">
.archiving-running {
    font-size: 14px;
    color: #63656e;
    text-align: left;

    .tips {
        margin: 10px 0 20px;
        padding: 5px 10px;
        background-color: #f5f6fa;
    }
    .button {
        float: right;
    }
}
.remove-Unarchivable {
    color: #3a84ff;
    cursor: pointer;

}
.can-not-delete-tips {
    display: flex;
    align-items: center;
    position: relative;
    grid-gap: 16px;
    .can-not-delete-tips-content {
        flex: 1;
    }
    >.text-link {
        color: #3a84ff;
        cursor: pointer;
        flex-shrink: 0;
    }
}
</style>
