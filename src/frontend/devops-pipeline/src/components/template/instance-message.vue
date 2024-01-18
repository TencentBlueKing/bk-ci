<template>
    <bk-dialog
        ext-cls="instance-message-dialog"
        padding="0"
        width="600"
        v-model="showInstanceMessage"
        :has-header="instanceMessageConfig.hasHeader"
        :show-footer="instanceMessageConfig.hasFooter"
        :close-icon="instanceMessageConfig.closeIcon"
        :mask-close="instanceMessageConfig.quickClose">
        <template>
            <section class="create-pipeline-content"
                v-bkloading="{
                    isLoading: instanceMessageConfig.loading
                }">
                <i class="devops-icon icon-close" @click="cancel()"></i>
                <div class="message-title">{{ message }}</div>
                <div class="fail-pipeline-content">
                    <span>{{ $t('template.instantiationFailMsg') }}：</span>
                    <ul class="fail-list">
                        <li class="item-row" v-for="(row, index) in failList" :key="index">
                            <div class="pipeline-item">
                                <div class="name" v-bk-overflow-tips>
                                    {{ row }}
                                </div>：
                                <div class="error-message">{{ failMessage[row] }}</div>
                            </div>
                        </li>
                    </ul>
                </div>
            </section>
        </template>
    </bk-dialog>
</template>

<script>
    export default {
        props: {
            showInstanceMessage: Boolean,
            failList: Array,
            successList: Array,
            failMessage: Object
        },
        data () {
            return {
                instanceMessageConfig: {
                    loading: false,
                    hasHeader: false,
                    hasFooter: false,
                    closeIcon: false,
                    quickClose: false
                }
            }
        },
        computed: {
            message () {
                let msg
                if (this.successList.length) {
                    msg = `${this.$t('template.instantiationSucTips', [this.successList.length])}${this.$t('template.instantiationFailTips', [this.failList.length])}`
                } else if (!this.successList.length) {
                    msg = this.$t('template.instantiationFailTips', [this.failList.length])
                }
                return msg
            }
        },
        methods: {
            cancel () {
                this.$emit('cancel')
            }
        }
    }
</script>

<style lang='scss'>
    @import './../../scss/conf';
    .instance-message-dialog {
        .bk-dialog-tool {
            display: none;
        }
        .bk-dialog-body {
            margin: 0px;
        }
        .create-pipeline-content {
            padding: 30px 20px;
            min-height: 360px;
        }
        .pipeline-item {
            display: flex;
            .name {
                text-wrap: nowrap;
                max-width: 300px;
                overflow: hidden;
                text-overflow: ellipsis;
            }
        }
        .icon-close {
            position: absolute;
            right: 0px;
            top: 10px;
            font-size: 12px;
            color: $fontLighterColor;
            cursor: pointer;
        }
        .message-title {
            margin-bottom: 20px;
            font-size: 16px;
            color: #333C48;
        }
        .fail-list {
            padding-left: 20px;
            margin-top: 10px;
        }
        .item-row {
            list-style: outside;
        }
        .error-message {
            color: $fontLighterColor;
        }
        .form-footer {
            padding: 10px;
            text-align: right;
            background: #FAFBFD;
            border-top: 1px solid #DDE4EB;
        }
    }
</style>
