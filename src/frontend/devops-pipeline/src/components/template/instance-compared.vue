<template>
    <bk-dialog
        ext-cls="template-compared-dialog"
        v-model="showComparedInstance"
        :show-footer="comparedDialogConfig.hasFooter"
        :close-icon="comparedDialogConfig.closeIcon"
        width="1380"
        :position="{ top: '100' }"
    >
        <template>
            <section class="create-pipeline-content">
                <div class="info-header">
                    <div class="title">差异对比</div>
                    <i class="bk-icon icon-close" @click="cancelHandler()"></i>
                </div>
                <div class="compared-content"
                    v-bkloading="{
                        isLoading: loading
                    }">
                    <div class="update-version-compared">
                        <div class="update-before">
                            更新前<i class="bk-icon icon-minus"></i>{{ curVersion }}
                        </div>
                        <div class="update-after version-selector">
                            <div class="label">更新后<i class="bk-icon icon-minus"></i></div>
                            <bk-dropdown :placeholder="'请选择版本'"
                                :list="versionList"
                                :display-key="'versionName'"
                                :setting-key="'version'"
                                :searchable="true"
                                :selected.sync="instanceVersion"
                                @item-selected="selectedVersion">
                            </bk-dropdown>
                        </div>
                    </div>
                    <section v-if="curParamsList.length || targetParamsList.length || curTplParamsList.length || targetTplParamsList.length">
                        <div class="update-version-compared compared-title">
                            <div class="update-before">变量</div>
                            <!-- <div class="update-after change-total">
                                <span class="total-text modify-count">2 变更</span>
                                <span class="total-text add-count">3 新增</span>
                                <span class="total-text remove-count">1 删除</span>
                            </div> -->
                        </div>
                        <div class="update-version-compared compared-params">
                            <div class="update-before params-list">
                                <key-value-normal :value="curParamsList" :disabled="true"></key-value-normal>
                                <div class="cut-line" v-if="curTplParamsList.length"></div>
                                <key-value-normal :value="curTplParamsList" :disabled="true"></key-value-normal>
                            </div>
                            <div class="update-after params-list">
                                <key-value-normal :value="targetParamsList" :disabled="true"></key-value-normal>
                                <div class="cut-line" v-if="targetTplParamsList.length"></div>
                                <key-value-normal :value="targetTplParamsList" :disabled="true"></key-value-normal>
                            </div>
                        </div>
                    </section>
                    <div class="update-version-compared pipeline-preview">
                        <div class="update-before">编排预览</div>
                        <!-- <div class="update-after change-total">
                            <span class="total-text modify-count">2 变更</span>
                            <span class="total-text add-count">3 新增</span>
                            <span class="total-text remove-count">1 删除</span>
                        </div> -->
                    </div>
                    <div class="update-version-compared compared-pipeline-preview">
                        <div class="update-before previre-pipeline">
                            <div class="prevent-content">
                                <stages :stages="curStages" :editable="false" v-if="curStages"></stages>
                            </div>
                        </div>
                        <div class="update-after previre-pipeline">
                            <div class="prevent-content">
                                <stages :stages="targetStages" :editable="false" v-if="targetStages"></stages>
                            </div>
                        </div>
                    </div>
                </div>
            </section>
        </template>
    </bk-dialog>
</template>

<script>
    import Stages from '@/components/Stages'
    import KeyValueNormal from '@/components/atomFormField/KeyValueNormal'

    export default {
        components: {
            Stages,
            KeyValueNormal
        },
        props: {
            showComparedInstance: Boolean,
            loading: Boolean,
            instanceVersion: String,
            curVersion: String,
            versionList: Array,
            curParamsList: Array,
            curTplParamsList: Array,
            targetParamsList: Array,
            targetTplParamsList: Array,
            curStages: Array,
            targetStages: Array,
            selectedVersion: Function
        },
        data () {
            return {
                comparedDialogConfig: {
                    hasHeader: false,
                    hasFooter: false,
                    closeIcon: false,
                    quickClose: false
                }
            }
        },
        methods: {
            cancelHandler () {
                this.$emit('cancel')
            }
        }
    }
</script>

<style lang='scss'>
    @import './../../scss/conf';
    .template-compared-dialog {
        .bk-dialog-tool {
            display: none;
        }
        .bk-dialog-body {
            padding: 0;
        }
        .info-header {
            display: flex;
            justify-content: space-between;
            padding: 18px 20px;
            border-bottom: 1px solid #EBF0F5;
            font-size: 18px;
            .icon-close{
                color: #000000;
                font-size: 14px;
                cursor: pointer;
            }
        }
        .compared-content {
            padding: 20px;
            min-height: 560px;
            max-height: 580px;
            overflow: auto;
        }
        .update-version-compared {
            display: flex;
            width: 100%;
            color: #333C48;
            .update-before,
            .update-after {
                width: 48%;
            }
            .prevent-content {
                pointer-events: none;
            }
            .update-after {
                margin-left: 2%;
            }
            .icon-minus {
                position: relative;
                top: 1px;
            }
        }
        .version-selector {
            display: flex;
            .label {
                min-width: 56px;
            }
            .bk-selector {
                position: relative;
                top: -7px;
                width: 317px;
                .bk-selector-input,
                .bk-selector-list .text {
                    font-size: 12px;
                }
            }
        }
        .compared-title,
        .pipeline-preview {
            margin-bottom: 10px;
            font-size: 12px;
        }
        .pipeline-preview {
            margin-top: 10px;
        }
        .change-total {
            text-align: right;
            margin-top: 10px;
            .total-text {
                margin-left: 6px;
            }
            .modify-count {
                color: #FFB400;
            }
            .add-count {
                color: #30D878;
            }
            .remove-count {
                color: #FF5656;
            }
        }
        .compared-params {
            max-height: 180px;
            overflow: auto;
        }
        .compared-pipeline-preview {
            min-height: 260px;
            overflow: auto;
        }
        .params-list,
        .previre-pipeline {
            overflow: auto;
            .bk-form-item {
                margin-top: 0;
            }
        }
        .params-list {
            transform: scale(1, 0.9);
            transform-origin: left top;
        }
        .previre-pipeline {
            padding: 10px;
            background: #fafbfd;
            .soda-stage-list {
                padding-right: 0;
            }
        }
        .cut-line {
            width: 100%;
            height: 1px;
            margin-bottom: 10px;
            border-top: 1px dashed $borderWeightColor;
        }
        .container-name {
            text-align: left;
        }
        @media only screen and (max-width: 1359px) {
           .bk-dialog-content {
                width: 1180px !important;
            }
            .previre-pipeline .soda-stage-list {
                transform: scale(0.60);
                transform-origin: left top;
            }
        }
        @media only screen and (min-width: 1360px) and (max-width: 1694px) {
           .bk-dialog-content {
                width: 1380px !important;
            }
            .previre-pipeline .soda-stage-list {
                transform: scale(0.68);
                transform-origin: left top;
            }
        }
        @media only screen and (min-width: 1695px) {
            .bk-dialog-content {
                width: 1500px !important;
            }
            .previre-pipeline .soda-stage-list {
                transform: scale(0.75);
                transform-origin: left top;
            }
        }
    }
</style>
