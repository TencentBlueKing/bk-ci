<template>
    <bk-dialog
        ext-cls="bk-devops-center-align-dialog archive"
        width="480"
        render-directive="if"
        v-model="isArchiveDialogShow"
        :mask-close="false"
        :auto-close="false"
        :draggable="false"
        :loading="isSubmiting"
        footer-position="center"
        theme="warning"
        :ok-text="$t('archive.archive')"
        :on-close="cancel"
        @confirm="submit"
        @cancel="cancel"
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
            <p
                v-if="!isArchiveBatch"
                class="active-tip-head"
            >
                <span class="label">{{ $t('pipeline') }}：</span>
                <span class="value">{{ pipeline?.pipelineName }}</span>
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
                v-if="isArchiveBatch"
                class="archive-pipeline-list"
            >
                <li
                    v-for="pipeline in pipelineList"
                    :key="pipeline.pipelineId"
                >
                    {{ pipeline.pipelineName }}
                </li>
            </ul>
        </main>
    </bk-dialog>
</template>

<script>
    import { mapActions } from 'vuex'
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
                return this.$route.params
            }
        },
        methods: {
            ...mapActions('pipelines', ['migrateArchivePipelineList']),
            async handleSingleArchive () {
                const { pipelineId } = this.pipelineList[0]
                await this.migrateArchivePipelineList({
                    projectId: this.projectId,
                    pipelineId
                })
            },
            async handleBatchArchive () {
                const pipelineIds = this.pipelineList.map(pipeline => pipeline.pipelineId)
                console.log('🚀 ~ archiveBatchHandle ~ pipelineIds:', pipelineIds)
                // await this.migrateArchivePipelineBatch({ this.projectId, })
            },
            async submit () {
                if (this.isSubmiting) return
                let message = this.$t('archive.archivePipelineSuccess')
                let theme = 'success'

                try {
                    this.isSubmiting = true

                    if (this.isArchiveBatch) {
                        await this.handleBatchArchive()
                    } else {
                        await this.handleSingleArchive()
                    }

                    this.$emit('done')
                    this.$emit('cancel')
                } catch (e) {
                    message = e.message || this.$t('archive.archivePipelineFail')
                    theme = 'error'
                } finally {
                    this.$showTips({
                        message,
                        theme
                    })
                    this.isSubmiting = false
                }
            },
            cancel () {
                this.$emit('cancel')
            }
        }
    }
</script>

<style lang="scss">
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
    }
  }
  .active-tip-block {
    width: 416px;
    height: 68px;
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
            width: 100%;
            height: 40px;
            padding: 0 8px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            grid-gap: 12px;
            overflow: hidden;
            text-align: left;
            border-bottom: 1px solid #DCDEE5;

            &:last-child {
                border-bottom: 0;
            }
        }
    }
}
</style>
