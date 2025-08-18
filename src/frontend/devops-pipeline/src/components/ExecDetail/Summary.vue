<template>
    <header
        v-if="visible"
        class="exec-detail-summary"
    >
        <div class="exec-detail-summary-info">
            <div class="exec-detail-summary-info-material">
                <span class="exec-detail-summary-info-block-title">
                    {{ $t("details.triggerRepo") }}
                </span>
                <div
                    v-if="webhookInfo"
                    class="exec-detail-summary-info-material-list"
                >
                    <material-item
                        class="visible-material-row"
                        :material="webhookInfo"
                        is-webhook
                        :show-more="false"
                    >
                    </material-item>
                </div>
                <span
                    class="no-exec-material"
                    v-else
                >--</span>
            </div>
            <div class="exec-detail-summary-info-material">
                <span class="exec-detail-summary-info-block-title">{{
                    $t("details.material")
                }}</span>
                <div
                    v-if="visibleMaterial"
                    class="exec-detail-summary-info-material-list"
                >
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
                        <li
                            v-for="(material, index) in visibleMaterial"
                            :key="index"
                        >
                            <material-item :material="material" />
                        </li>
                    </ul>
                </div>
                <span
                    class="no-exec-material"
                    v-else
                >--</span>
            </div>
            <div style="overflow: hidden;">
                <span class="exec-detail-summary-info-block-title">{{ $t("history.tableMap.pipelineVersion") }}</span>
                <div class="exec-detail-summary-info-block-content">
                    <bk-popover
                        v-if="isConstraintTemplate"
                        trigger="click"
                        class="instance-template-info"
                        placement="bottom"
                        width="360"
                        theme="light"
                    >
                        <logo
                            class="template-info-entry"
                            name="constraint"
                            size="14"
                        />
                        <div
                            class="pipeline-template-info-popover"
                            slot="content"
                        >
                            <header class="template-info-header">{{ $t('newlist.constraintModeDesc') }}</header>
                            <section class="template-info-section">
                                <p
                                    v-for="row in templateRows"
                                    :key="row.id"
                                >
                                    <label>{{ row.id }}：</label>
                                    <span>{{ row.content }}</span>
                                    <router-link
                                        v-if="row.link"
                                        class="template-link-icon"
                                        :to="row.link"
                                        target="_blank"
                                    >
                                        <logo
                                            name="tiaozhuan"
                                            size="14"
                                        />
                                    </router-link>
                                </p>
                            </section>
                        </div>
                    </bk-popover>
                    <bk-popover
                        placement="top"
                        max-width="500"
                    >
                        <span class="pipeline-cur-version-span">
                            {{ execDetail.curVersionName }}
                        </span>
                        <div slot="content">
                            <p>
                                <label>{{ $t('versionNum') }}：</label>
                                <span>{{ execDetail.curVersionName }}</span>
                            </p>
                            <p>
                                <label>{{ $t('versionDesc') }}：</label>
                                <span>{{ curVersionDesc || '--' }}</span>
                            </p>
                        </div>
                    </bk-popover>
                </div>
            </div>
            <div class="exec-remark-block">
                <span class="exec-detail-summary-info-block-title">
                    {{ $t("history.remark") }}
                    <template v-if="!archiveFlag">
                        <i
                            v-if="!remarkEditable"
                            @click="showRemarkEdit"
                            class="devops-icon icon-edit exec-remark-edit-icon pointer"
                        />
                        <span
                            v-else
                            class="pipeline-exec-remark-actions"
                        >
                            <bk-button
                                text
                                theme="primary"
                                @click="handleRemarkChange"
                            >{{
                                $t("save")
                            }}</bk-button>
                            <bk-button
                                text
                                theme="primary"
                                @click="hideRemarkEdit"
                            >{{
                                $t("cancel")
                            }}</bk-button>
                        </span>
                    </template>
                </span>
                <div class="exec-detail-summary-info-block-content">
                    <bk-input
                        v-if="remarkEditable"
                        type="textarea"
                        v-model="tempRemark"
                        :maxlength="4096"
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
        <div
            class="part-quality-block"
            v-if="artifactQuality && Object.keys(artifactQuality).length"
        >
            <span class="part-quality-block-title">
                {{ $t("artifactQuality") }}
            </span>
            <ArtifactQuality
                :data="artifactQuality"
                @goOutputs="goOutputs"
            />
        </div>
    </header>
</template>

<script>
    import Logo from '@/components/Logo'
    import { mapActions } from 'vuex'
    import MaterialItem from './MaterialItem'
    import ArtifactQuality from './artifactQuality'

    export default {
        components: {
            MaterialItem,
            Logo,
            ArtifactQuality
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
                isShowMoreMaterial: false,
                curVersionDesc: ''
            }
        },
        computed: {
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
            },
            instanceFromTemplate () {
                return this.execDetail?.model.instanceFromTemplate ?? false
            },
            isConstraintTemplate () {
                return this.instanceFromTemplate && this.execDetail?.templateInfo?.instanceType === 'CONSTRAINT'
            },
            templateRows () {
                return [
                    {
                        id: this.$t('templateName'),
                        content: this.execDetail?.templateInfo?.templateName ?? '--'
                    },
                    {
                        id: this.$t('templateVersion'),
                        content: this.execDetail?.templateInfo?.versionName ?? '--',
                        link: {
                            name: 'templateEdit',
                            params: {
                                templateId: this.execDetail?.templateInfo?.templateId
                            }
                        }
                    }]
            },
            artifactQuality () {
                return this.execDetail?.artifactQuality
            },
            archiveFlag () {
                return this.$route.query.archiveFlag
            }
        },
        watch: {
            execDetail: {
                handler: function (val, oldVal) {
                    if (val.remark !== this.tempRemark) {
                        this.tempRemark = val.remark
                        this.remark = val.remark
                    }
                    if (val?.curVersion !== oldVal?.curVersion) {
                        this.updateCurVersionDesc()
                    }
                },
                immediate: true
            }
        },
        methods: {
            ...mapActions({
                fetchVersionDetail: 'atom/getPipelineVersionInfo',
                updateBuildRemark: 'pipelines/updateBuildRemark'
            }),
            async updateCurVersionDesc () {
                try {
                    const result = await this.fetchVersionDetail({
                        version: this.execDetail.curVersion,
                        ...this.$route.params,
                        ...this.$route.query
                    })
                    this.curVersionDesc = result.data.description
                } catch (error) {
                    this.$showTips({
                        message: error.message,
                        theme: 'error'
                    })
                }
            },
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
            },
            goOutputs (values) {
                this.$router.push({
                    name: 'pipelinesDetail',
                    params: {
                        ...this.routerParams,
                        type: 'outputs'
                    },
                    query: {
                        metadataKey: values[0].labelKey,
                        metadataValues: values.map(item => item.value).join(',')
                    }
                })
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
  .instance-template-info {
    display: inline-flex;
    margin-right: 6px;
    line-height: 52px;
    height: 100%;
  }
  .template-info-entry {
    color: #979ba5;
    cursor: pointer;
    &:hover {
        color: $primaryColor;
    }
  }

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
          line-height: 48px;
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
      align-self: stretch;
      display: flex;
      align-items: center;
      height: 48px;
      .pipeline-cur-version-span {
        display: inline-block;
        line-height: 48px;
        @include ellipsis();
        text-decoration: underline;
        text-decoration-skip-ink: none;
        cursor: pointer;
      }

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

          &.bk-textarea-wrapper {
            margin-bottom: 14px;
          }
        }
      }
    }
  }
}
.pipeline-template-info-popover {
    .template-info-header {
        color: #979ba5;
    }
    .template-info-section {
        padding: 8px;
        background: #f0f1f5;
        border-radius: 2px;
        display: flex;
        flex-direction: column;
        grid-gap: 10px;
        margin-top: 12px;
        > p {
            display: flex;
            align-items: center;
            grid-gap: 8px;
            > label {
                color: #979ba5;
                flex-shrink: 0;
            }
            .template-link-icon {
                font-size: 0;
                flex-shrink: 0;
                cursor: pointer;
                font-weight: bold;
                color: $primaryColor;
            }
            > span {
                font-weight: bold;
                @include ellipsis();
            }
        }
    }
}
.part-quality-block {
    display: flex;
    flex-wrap: wrap;
    padding-top: 6px;
    padding-bottom: 16px;
    font-size: 12px;
    
    .part-quality-block-title {
        color: #979ba5;
        margin-right: 24px;
        margin-top: 3px;
        padding-top: 4px;
        flex-shrink: 0;
    }
}
</style>
