<template>
    <header class="exec-detail-summary">
        <div class="exec-detail-summary-row">
            <span :class="{
                'exec-detail-build-summary-anchor': true,
                [execDetail.status]: execDetail.status
            }"></span>
            <aside class="exec-detail-summary-title">
                <bk-tag type="stroke" :theme="statusTheme">
                    {{statusLabel}}
                    <span
                        v-if="execDetail.status === 'CANCELED'"
                        v-bk-tooltips="`${$t('details.canceller')}：${execDetail.cancelUserId}`"
                        class="devops-icon icon-info"
                    >
                    </span>
                </bk-tag>
                <span class="exec-detail-summary-title-build-msg">
                    {{execDetail.buildMsg}}
                </span>
            </aside>
            <aside class="exec-detail-summary-trigger">
                <img
                    class="exec-trigger-profile"
                />
                <span v-if="execDetail.triggerUser">
                    {{$t('details.executorInfo', [execDetail.triggerUser, execDetail.trigger, execFormatStartTime])}}
                </span>
            </aside>
        </div>
        <div class="exec-detail-summary-info">
            <div class="exec-detail-summary-info-trigger-lib">
                <span class="exec-detail-summary-info-block-title">{{$t('details.triggerRepo')}}</span>
                <div class="exec-detail-summary-info-block-content">
                    <span>
                        <i class="devops-icon icon-info-circle" />
                        Tencent/bk-ci
                    </span>
                    <span>
                        <i class="devops-icon icon-info-circle" />
                        feat_branch1
                    </span>
                    <span>
                        <i class="devops-icon icon-info-circle" />
                        a660c6l
                    </span>
                </div>
            </div>
            <div class="exec-detail-summary-info-material">
                <span class="exec-detail-summary-info-block-title">{{$t('editPage.material')}}</span>
                <div
                    v-if="visibleMaterial"
                    :class="{
                        'exec-detail-summary-info-material-list': true
                    }">
                    <div class="exec-material-row visible-material-row">
                        <span v-for="field in materialInfos" :key="field">
                            <i class="devops-icon icon-info-circle" />
                            {{ visibleMaterial[0][field] }}
                        </span>
                        <span @mouseenter="showMoreMaterial">...</span>
                    </div>
                    <ul v-show="isShowMoreMaterial" class="all-exec-material-list" @mouseleave="hideMoreMaterial">
                        <li
                            v-for="material in visibleMaterial"
                            class="exec-material-row"
                            :key="material.newCommitId"
                        >
                            <span v-for="field in materialInfos" :key="field">
                                <i class="devops-icon icon-info-circle" />
                                {{ material[field] }}
                            </span>
                            <span class="exec-more-material">...</span>
                        </li>
                    </ul>
                </div>
                <span class="no-exec-material" v-else>--</span>
            </div>
            <div>
                <span class="exec-detail-summary-info-block-title">{{$t('总耗时')}}</span>
                <div class="exec-detail-summary-info-block-content">
                    {{ executeTime }}
                </div>
            </div>
            <div>
                <span class="exec-detail-summary-info-block-title">{{$t('编排版本号')}}</span>
                <div class="exec-detail-summary-info-block-content">
                    v.{{execDetail.curVersion}}
                </div>
            </div>
            <div class="exec-remark-block">
                <span class="exec-detail-summary-info-block-title">
                    {{$t('history.remark')}}
                    <i
                        v-if="!remarkEditable"
                        @click="showRemarkEdit"
                        class="devops-icon icon-edit exec-remark-edit-icon"
                    />
                    <span v-else class="pipeline-exec-remark-actions">
                        <bk-button text theme="primary" @click="handleRemarkChange">{{$t('save')}}</bk-button>
                        <bk-button text theme="primary" @click="hideRemarkEdit">{{$t('cancel')}}</bk-button>
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
                    <span class="exec-remark" v-else>{{tempRemark}}</span>
                </div>
            </div>
        </div>
    </header>
</template>

<script>
    import { mapActions } from 'vuex'
    import { convertMStoStringByRule } from '@/utils/util'
    export default {
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
            executeTime () {
                return this.execDetail?.executeTime ? convertMStoStringByRule(this.execDetail?.executeTime) : '--'
            },
            statusLabel () {
                return this.execDetail?.status ? this.$t(`details.statusMap.${this.execDetail?.status}`) : ''
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
                if (Array.isArray(this.execDetail?.material) && this.execDetail?.material.length > 0) {
                    return [
                        ...this.execDetail?.material,
                        ...this.execDetail?.material,
                        ...this.execDetail?.material
                    ]
                }
                return null
            },
            hiddenMaterial () {
                if (Array.isArray(this.execDetail?.material) && this.execDetail?.material.length > 0) {
                    return this.execDetail?.material.slice(0)
                }
                return []
            },
            materialInfos () {
                return [
                    'aliasName',
                    'branchName',
                    'newCommitId'
                ]
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
            ...mapActions('pipelines', [
                'updateBuildRemark'
            ]),
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
        box-shadow: 0 2px 2px 0 rgba(0,0,0,0.15);
        &-row {
            position: relative;
            display: flex;
            justify-content: space-between;
            margin-bottom: 24px;
            .exec-detail-build-summary-anchor {
                @include build-status();
                position: absolute;
                content: '';
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
            .exec-trigger-profile {
                width: 24px;
                height: 24px;
                border-radius: 12px;
            }
        }
        &-info {
            display: grid;
            grid-auto-flow: column;
            grid-template-columns: repeat(5, fit-content);
            font-size: 12px;
            grid-gap: 100px;
            > div {
                display: flex;
                flex-direction: column;
                &.exec-detail-summary-info-trigger-lib {
                    .exec-detail-summary-info-block-content {
                        display: grid;
                        grid-gap: 20px;
                        align-items: center;
                        grid-template-columns: repeat(3, fit-content(160px));
                        > span {
                            @include ellipsis();
                        }
                    }

                }
                &.exec-detail-summary-info-material {
                    .no-exec-material {
                        display: flex;
                        margin-left: 8px;
                        flex: 1;
                        align-items: center;
                    }
                    .exec-detail-summary-info-material-list {
                        position: relative;
                        width: 100%;
                        .all-exec-material-list {
                            position: absolute;
                            z-index: 6;
                            width: 100%;
                            border: 1px solid #DCDEE5;
                            border-radius: 2px;
                            background: white;
                            top: 2px;
                            left: 0;
                            padding-top: 8px;
                        }
                        .exec-material-row {
                            padding: 0 8px 8px 8px;
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
                            }
                            &:not(:first-child) {
                                .exec-more-material {
                                    opacity: 0;
                                }
                            }
                        }
                    }
                }
                &.exec-remark-block {
                    width: 265px;
                }
            }
            &-block-title {
                padding: 0 8px;
                display: flex;
                color: #979BA5;
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
                padding: 0 8px;

                .exec-remark {
                    width: 100%;
                    display: flex;
                    height: 38px;
                    align-items: center;

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
