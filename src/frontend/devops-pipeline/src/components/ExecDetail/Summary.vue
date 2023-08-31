<template>
    <header class="exec-detail-summary">
        <div v-if="visible" class="exec-detail-summary-info">
            <div class="exec-detail-summary-info-material">
                <span class="exec-detail-summary-info-block-title">
                    {{ $t("details.triggerRepo") }}
                </span>
                <div v-if="webhookInfo" class="exec-detail-summary-info-material-list">
                    <material-item
                        class="visible-material-row"
                        :material="webhookInfo"
                        is-webhook
                        :show-more="false"
                    >
                    </material-item>
                </div>
                <span class="no-exec-material" v-else>--</span>
            </div>
            <div class="exec-detail-summary-info-material">
                <span class="exec-detail-summary-info-block-title">{{
                    $t("details.material")
                }}</span>
                <div v-if="visibleMaterial" class="exec-detail-summary-info-material-list">
                    <material-item
                        class="visible-material-row"
                        :material="visibleMaterial[0]"
                        @mouseenter="showMoreMaterial"
                        :show-more="visibleMaterial.length > 1"
                    />
                    <ul
                        v-show="isShowMoreMaterial"
                        class="all-exec-material-list"
                        @mouseleave="hideMoreMaterial"
                    >
                        <li v-for="(material, index) in visibleMaterial" :key="index">
                            <material-item :material="material" />
                        </li>
                    </ul>
                </div>
                <span class="no-exec-material" v-else>--</span>
            </div>
            <!-- <div>
                <span class="exec-detail-summary-info-block-title">{{ $t("总耗时") }}</span>
                <div class="exec-detail-summary-info-block-content">
                    {{ executeTime }}
                </div>
            </div> -->
            <div>
                <span class="exec-detail-summary-info-block-title">{{ $t("history.tableMap.pipelineVersion") }}</span>
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
                        class="devops-icon icon-edit exec-remark-edit-icon pointer"
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
                    <span
                        v-else
                        v-bk-tooltips="{
                            content: remark,
                            disabled: !remark,
                            allowHTML: false,
                            delay: [300, 0]
                        }"
                        class="exec-remark"
                    >
                        {{ remark || "--" }}
                    </span>
                </div>
            </div>
        </div>
    </header>
</template>

<script>
    import { convertMStoString } from '@/utils/util'
    import { mapActions } from 'vuex'
    import MaterialItem from './MaterialItem'
    export default {
        components: {
            MaterialItem
        },
        props: {
            visible: {
                type: Boolean,
                default: true
            },
            execDetail: {
                type: Object,
                required: true
            }
        },
        data () {
            return {
                remarkEditable: false,
                tempRemark: this.execDetail.remark,
                remark: this.execDetail.remark,
                isChangeRemark: false,
                isShowMoreMaterial: false
            }
        },
        computed: {
            executeTime () {
                return this.execDetail.model?.timeCost?.totalCost
                ? convertMStoString(this.execDetail.model?.timeCost?.totalCost)
                : '--'
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
                return this.execDetail?.webhookInfo ?? null
            }
        },
        watch: {
            execDetail: function (val) {
                if (val.remark !== this.tempRemark) {
                    this.tempRemark = val.remark
                    this.remark = val.remark
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
                    if (this.tempRemark !== this.remark) {
                        this.isChangeRemark = true
                        await this.updateBuildRemark({
                            ...this.$route.params,
                            buildId: this.$route.params.buildNo,
                            remark: this.tempRemark
                        })
                        this.remark = this.tempRemark
                        this.$showTips({
                            theme: 'success',
                            message: this.$t('updateSuc')
                        })
                    }
                } catch (e) {
                    this.tempRemark = this.remark
                    this.$showTips({
                        theme: 'error',
                        message: this.$t('updateFail')
                    })
                } finally {
                    this.isChangeRemark = false
                    this.hideRemarkEdit()
                }
            }
        }
    }
</script>

<style lang="scss">
@import "@/scss/conf";
@import "@/scss/mixins/ellipsis";

.exec-detail-summary {
  background: white;
  position: relative;
  padding: 0 24px;
  box-shadow: 0 2px 2px 0 rgba(0, 0, 0, 0.15);
  &-info {
    display: grid;
    grid-auto-flow: column;
    grid-template-columns: 8fr 8fr 3fr minmax(168px, 4fr);
    font-size: 12px;
    grid-gap: 100px;
    padding-top: 18px;

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
            z-index: 11;
            width: 100%;
            border: 1px solid #dcdee5;
            border-radius: 2px;
            background: white;
            top: -1px;
            padding: 0 8px;
            left: 0;
            :not(:first-child) {
              .exec-more-material {
                opacity: 0;
              }
            }
          }
          .exec-material-row {
            // padding: 0 0 8px 0;
            display: grid;
            grid-gap: 20px;
            grid-auto-flow: column;
            height: 38px;
            grid-auto-columns: minmax(auto, max-content) 36px;
            .material-row-info-spans {
                display: grid;
                grid-auto-flow: column;
                grid-gap: 20px;
                grid-auto-columns: minmax(auto, max-content);
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
            }
            &.visible-material-row {
              border: 1px solid transparent;
              padding-bottom: 0px;
              align-items: center;

            }
            .exec-more-material {
                display: inline-flex;
                align-items: center;

            }

            .mr-source-target {
                display: grid;
                align-items: center;
                grid-auto-flow: column;
                grid-gap: 6px;
                .icon-arrows-right {
                    color: #C4C6CC;
                    font-weight: 800;
                }
                > span {
                    @include ellipsis();
                }
            }
            .material-span-tooltip-box {
                flex: 1;
                overflow: hidden;
                > .bk-tooltip-ref {
                    width: 100%;
                    .material-span {
                        width: 100%;
                    }
                }
            }
            .material-span {
              @include ellipsis();
              .bk-link-text {
                font-size: 12px;
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
      line-height: 48px;

      .exec-remark {
        width: 100%;
        align-items: center;
        display: inline-block;
        @include ellipsis();

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
