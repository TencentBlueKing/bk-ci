<template>
    <bk-dialog v-if="showInstanceMessage"
        width="600"
        ext-cls="instance-message-dialog"
        padding="0"
        :is-show.sync="showInstanceMessage"
        :has-header="instanceMessageConfig.hasHeader"
        :has-footer="instanceMessageConfig.hasFooter"
        :close-icon="instanceMessageConfig.closeIcon"
        :quick-close="instanceMessageConfig.quickClose">
        <template
            slot="content">
            <section class="create-pipeline-content"
                v-bkloading="{
                    isLoading: instanceMessageConfig.loading
                }">
                <i class="bk-icon icon-close" @click="cancel()"></i>
                <div class="message-title">{{ message }}</div>
                <div class="fail-pipeline-content">
                    <span>以下是实例化失败的流水线名称和失败原因：</span>
                    <ul class="fail-list">
                        <li class="item-row" v-for="(row, index) in failList" :key="index">
                            <div class="pipeline-item">{{ row }}：
                                <span class="error-message">{{ failMessage[row] }}</span>
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
                if (!this.successList.length) {
                    msg = `你已成功实例化${this.successList.length}条流水线，${this.failList.length}条流水线失败`
                } else if (this.successList.length) {
                    msg = `你实例化${this.failList.length}条流水线失败`
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
        .create-pipeline-content {
            padding: 30px 20px;
            min-height: 360px;
        }
        .icon-close {
            position: absolute;
            right: 20px;
            top: 20px;
            font-size: 12px;
            color: $fontLigtherColor;
            cursor: pointer;
        }
        .message-title {
            margin-bottom: 20px;
            font-size: 16px;
            color: #333C48;
        }
        .fail-list {
            padding-left: 20px;
        }
        .item-row {
            list-style: outside;
        }
        .error-message {
            color: $fontLigtherColor;
        }
        .form-footer {
            padding: 10px;
            text-align: right;
            background: #FAFBFD;
            border-top: 1px solid #DDE4EB;
        }
    }
</style>
