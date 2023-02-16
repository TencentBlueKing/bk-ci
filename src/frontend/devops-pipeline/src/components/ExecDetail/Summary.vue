<template>
    <header class="exec-detail-summary">
        <div class="exec-detail-summary-row">
            <span
                :class="{
                    'exec-detail-build-summary-anchor': true,
                    [execDetail.status]: execDetail.status
                }"
            ></span>
            <aside class="exec-detail-summary-title">
                <bk-tag class="exec-status-tag" type="stroke" :theme="statusTheme">
                    <span class="exec-status-label">
                        {{ statusLabel }}
                        <span
                            v-if="execDetail.status === 'CANCELED'"
                            v-bk-tooltips="`${$t('details.canceller')}：${execDetail.cancelUserId}`"
                            class="devops-icon icon-info-circle"
                        >
                        </span>
                    </span>
                </bk-tag>
                <span class="exec-detail-summary-title-build-msg">
                    {{ execDetail.buildMsg }}
                </span>
            </aside>
            <aside class="exec-detail-summary-trigger">
                <img v-if="execDetail.triggerUserProfile" class="exec-trigger-profile" />
                <logo class="exec-trigger-profile" name="default-user" size="24" />
                <span v-if="execDetail.triggerUser">
                    {{
                        $t("details.executorInfo", [
                            execDetail.triggerUser,
                            execDetail.trigger,
                            execFormatStartTime
                        ])
                    }}
                </span>
            </aside>
        </div>
        <div class="exec-detail-summary-info">
            <div class="exec-detail-summary-info-material">
                <span class="exec-detail-summary-info-block-title">{{
                    $t("details.triggerRepo")
                }}</span>
                <div v-if="webhookInfo" class="exec-detail-summary-info-material-list">
                    <material-item class="visible-material-row" :material="webhookInfo">
                    </material-item>
                </div>
                <span class="no-exec-material" v-else>--</span>
            </div>
            <div class="exec-detail-summary-info-material">
                <span class="exec-detail-summary-info-block-title">{{
                    $t("editPage.material")
                }}</span>
                <div v-if="visibleMaterial" class="exec-detail-summary-info-material-list">
                    <material-item
                        class="visible-material-row"
                        :material="visibleMaterial[0]"
                        @mouseenter="showMoreMaterial"
                        :show-more="visibleMaterial.length <= 1"
                    />
                    <ul
                        v-show="isShowMoreMaterial"
                        class="all-exec-material-list"
                        @mouseleave="hideMoreMaterial"
                    >
                        <li
                            v-for="(material, index) in visibleMaterial"
                            :key="material.newCommitId"
                        >
                            <material-item :show-more="index === 0" :material="material" />
                        </li>
                    </ul>
                </div>
                <span class="no-exec-material" v-else>--</span>
            </div>
            <div>
                <span class="exec-detail-summary-info-block-title">{{ $t("总耗时") }}</span>
                <div class="exec-detail-summary-info-block-content">
                    {{ executeTime }}
                </div>
            </div>
            <div>
                <span class="exec-detail-summary-info-block-title">{{ $t("编排版本号") }}</span>
                <div class="exec-detail-summary-info-block-content">
                    v.{{ execDetail.curVersion }}
                </div>
            </div>
            <div class="exec-remark-block">
                <span class="exec-detail-summary-info-block-title">
                    {{ $t("history.remark") }}
                    <i
                        v-if="!remarkEditable"
                        @click="showRemarkEdit"
                        class="devops-icon icon-edit exec-remark-edit-icon"
                    />
                    <span v-else class="pipeline-exec-remark-actions">
                        <bk-button text theme="primary" @click="handleRemarkChange">{{
                            $t("save")
                        }}</bk-button>
                        <bk-button text theme="primary" @click="hideRemarkEdit">{{
                            $t("cancel")
                        }}</bk-button>
                    </span>
                </span>
                <div class="exec-detail-summary-info-block-content">
                    <bk-input
                        v-if="remarkEditable"
                        type="textarea"
                        v-model="tempRemark"
                        :placeholder="$t('details.addRemarkForBuild')"
                        class="exec-remark"
                    />
                    <span class="exec-remark" v-else>{{ tempRemark || "--" }}</span>
                </div>
            </div>
        </div>
    </header>
</template>

<script>
    import { mapActions } from 'vuex'
    import Logo from '@/components/Logo'
    import MaterialItem from './MaterialItem'
    import {
        convertMStoStringByRule,
        convertTime
    } from '@/utils/util'
    export default {
        components: {
            Logo,
            MaterialItem
        },
        props: {
            execDetail: {
                type: Object,
                required: true
            }
        },
        data () {
            return {
                remarkEditable: false,
                tempRemark: this.execDetail.remark,
                isChangeRemark: false,
                isShowMoreMaterial: false
            }
        },
        computed: {
            execFormatStartTime () {
                return convertTime(this.execDetail?.startTime)
            },
            executeTime () {
                return this.execDetail?.executeTime
        ? convertMStoStringByRule(this.execDetail?.executeTime)
                : '--'
            },
            statusLabel () {
                return this.execDetail?.status
        ? this.$t(`details.statusMap.${this.execDetail?.status}`)
                : ''
            },
            statusTheme () {
                switch (this.execDetail?.status) {
                    case 'CANCELED':
                    case 'REVIEW_ABORT':
                        return 'warning'
                    case 'SUCCEED':
                    case 'REVIEW_PROCESSED':
                    case 'STAGE_SUCCESS':
                        return 'success'
                    case 'FAILED':
                    case 'TERMINATE':
                    case 'HEARTBEAT_TIMEOUT':
                    case 'QUALITY_CHECK_FAIL':
                    case 'QUEUE_TIMEOUT':
                    case 'EXEC_TIMEOUT':
                        return 'danger'
                    case 'QUEUE':
                    case 'RUNNING':
                    case 'REVIEWING':
                    case 'PREPARE_ENV':
                    case 'LOOP_WAITING':
                    case 'CALL_WAITING':
                        return 'info'
                    default:
                        return ''
                }
            },
            visibleMaterial () {
                if (
                    Array.isArray(this.execDetail?.material)
                    && this.execDetail?.material.length > 0
                ) {
                    return this.execDetail?.material
                }
                return null
            },
            webhookInfo () {
                return this.execDetail?.webhookInfo
                ? {
                    aliasName: this.execDetail.webhookInfo.webhookAliasName,
                    branchName: this.execDetail.webhookInfo.webhookBranch,
                    newCommitId: this.execDetail.webhookInfo.webhookCommitId
                }
                : null
            }
        },
        watch: {
            execDetail: function (val) {
                if (val.remark !== this.tempRemark) {
                    this.tempRemark = val.remark
                }
            }
        },
        methods: {
            ...mapActions('pipelines', ['updateBuildRemark']),
            showRemarkEdit () {
                this.remarkEditable = true
            },
            hideRemarkEdit () {
                this.remarkEditable = false
            },
            showMoreMaterial () {
                this.isShowMoreMaterial = true
            },
            hideMoreMaterial () {
                this.isShowMoreMaterial = false
            },
            async handleRemarkChange (row) {
                if (this.isChangeRemark) return
                try {
                    if (this.tempRemark && this.tempRemark !== this.execDetail.remark) {
                        this.isChangeRemark = true
                        await this.updateBuildRemark({
                            ...this.$route.params,
                            buildId: this.$route.params.buildNo,
                            remark: this.tempRemark
                        })
                        this.$showTips({
                            theme: 'success',
                            message: this.$t('updateSuc')
                        })
                    }
                } catch (e) {
                    this.$showTips({
                        theme: 'error',
                        message: this.$t('updateFail')
                    })
                } finally {
                    this.hideRemarkEdit()
                }
            }
        }
    }
</script>

<style lang="scss">
@import "@/scss/conf";
@import "@/scss/mixins/ellipsis";
@import "@/scss/buildStatus";
.exec-detail-summary {
  background: white;
  padding: 18px 24px;
  box-shadow: 0 2px 2px 0 rgba(0, 0, 0, 0.15);
  &-row {
    position: relative;
    display: flex;
    justify-content: space-between;
    margin-bottom: 24px;
    .exec-detail-build-summary-anchor {
      @include build-status();
      position: absolute;
      content: "";
      width: 6px;
      height: 100%;
      left: -24px;
    }
  }
  &-title {
    height: 24px;
    display: flex;
    align-items: center;
    flex: 1;
    margin: 0;
    overflow: hidden;

    .exec-status-tag {
      margin: 0;
    }

    .exec-status-label {
      display: grid;
      align-items: center;
      grid-auto-flow: column;
      grid-gap: 6px;
    }

    &-build-msg {
      flex: 1;
      margin: 0 24px 0 8px;
      @include ellipsis();
      min-width: auto;
    }
  }
  &-trigger {
    display: flex;
    align-items: center;
    flex-shrink: 0;
    font-size: 12px;
    .exec-trigger-profile {
      width: 24px;
      height: 24px;
      border-radius: 12px;
      margin-right: 6px;
      color: #c4c6cc;
    }
  }
  &-info {
    display: grid;
    grid-auto-flow: column;
    grid-template-columns: minmax(320px, 1fr) minmax(320px, 1fr) 1fr 1fr 1fr;
    font-size: 12px;
    grid-gap: 100px;

    > div {
      display: flex;
      flex-direction: column;
      &:first-child {
        margin-left: -16px;
      }

      &.exec-detail-summary-info-material {
        padding: 0 8px;
        .no-exec-material {
          display: flex;
          flex: 1;
          align-items: center;
          padding-left: 8px;
        }
        .exec-detail-summary-info-block-title {
          padding-left: 8px;
        }
        .exec-detail-summary-info-material-list {
          position: relative;
          width: 100%;
          padding: 0 8px;
          .all-exec-material-list {
            position: absolute;
            z-index: 6;
            width: 100%;
            border: 1px solid #dcdee5;
            border-radius: 2px;
            background: white;
            top: 2px;
            left: 0;
            padding: 8px 8px 0 8px;
          }
          .exec-material-row {
            padding: 0 0 8px 0;
            display: grid;
            grid-gap: 20px;
            grid-auto-flow: column;
            &.visible-material-row {
              border: 1px solid transparent;
              padding-bottom: 0px;
              height: 38px;
              align-items: center;
            }
            > span {
              @include ellipsis();
              display: inline-flex;
              min-width: auto;
              align-items: center;
              > svg {
                flex-shrink: 0;
                margin-right: 6px;
              }
            }
            .material-link {
              color: $primaryColor;
            }
            .material-link,
            .material-span {
              @include ellipsis();
            }
            .material-link {
              color: $primaryColor;
            }
            .material-link,
            .material-span {
              @include ellipsis();
            }
            &:not(:first-child) {
              .exec-more-material {
                opacity: 0;
              }
            }
          }
        }
      }
    }
    &-block-title {
      display: flex;
      color: #979ba5;
      height: 22px;
      align-items: center;
      .exec-remark-edit-icon {
        margin-left: 6px;
      }
      .pipeline-exec-remark-actions {
        display: flex;
        align-items: center;
        margin-left: auto;
        > :first-child {
          margin-right: 6px;
        }
        .bk-button-text {
          font-size: 12px;
        }
      }
    }

    &-block-content {
      flex: 1;
      align-self: stretch;
      display: flex;
      align-items: center;

      .exec-remark {
        width: 100%;
        align-items: center;
        overflow: hidden;
        display: -webkit-box;
        text-overflow: ellipsis;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;

        .bk-form-textarea,
        .bk-textarea-wrapper {
          min-height: auto;
          width: 100%;
          &.bk-form-textarea {
            height: 32px;
          }
        }
      }
    }
  }
}
</style>
