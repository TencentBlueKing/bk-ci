<template>
    <bk-dialog v-model="constructToolConf.isShow"
        :width="'640'"
        :close-icon="false">
        <div
            v-bkloading="{
                isLoading: loading.isLoading,
                title: loading.title
            }">
            <div class="machine-params-form" v-if="hasPermission">
                <bk-form label-width="80">
                    <bk-form-item
                        required
                        :label="$t('environment.nodeInfo.model')"
                    >
                        <bk-radio-group v-model="constructImportForm.model">
                            <bk-radio :value="'LINUX'" :disabled="isAgent">Linux</bk-radio>
                            <bk-radio :value="'MACOS'" :disabled="isAgent">macOS</bk-radio>
                            <bk-radio :value="'WINDOWS'" :disabled="isAgent">Windows</bk-radio>
                        </bk-radio-group>
                    </bk-form-item>
                    <bk-form-item
                        v-if="gatewayList.length"
                        required
                        :label="$t('environment.nodeInfo.location')"
                        :desc="$t('environment.nodeInfo.buildMachineLocationTips')"
                    >
                        <bk-radio-group v-model="constructImportForm.location">
                            <bk-radio v-for="(model, index) in gatewayList"
                                :key="index"
                                :value="model.zoneName"
                                :disabled="isAgent"
                            >{{ model.showName }}</bk-radio>
                        </bk-radio-group>
                    </bk-form-item>
                </bk-form>
                <p class="handler-prompt">{{ constructImportForm.model === 'WINDOWS' ? $t('environment.nodeInfo.referenceStep') : $t('environment.nodeInfo.executeCommandPrompt')}}:</p>
                <div class="construct-card-item command-tool-card" v-if="constructImportForm.model !== 'WINDOWS'">
                    <div class="command-line">
                        {{ constructImportForm.link || $t('environment.nodeInfo.fetchInstallCommandTips') }}
                    </div>
                    <div class="copy-button" v-if="constructImportForm.link">
                        <a class="text-link copy-command"
                            @click="copyCommand">
                            {{ $t('environment.clickToCopy') }}</a>
                    </div>
                </div>
                <div class="construct-card-item command-tool-card windows-node-card" v-if="constructImportForm.model === 'WINDOWS'">
                    <div class="command-line" v-if="constructImportForm.link">
                        1.<a class="refresh-detail" :href="constructImportForm.link">{{ $t('environment.click') }}</a>{{ $t('environment.download') }}Agent
                        <br>
                        2.{{ $t('environment.check') }}【<a class="refresh-detail" target="_blank" :href="installDocsLink">{{ $t('environment.nodeInfo.installBuildMachineTips') }}</a>】
                    </div>
                    <div class="command-line" v-else>
                        {{ $t('environment.nodeInfo.fetchInstallCommandTips') }}
                    </div>
                </div>
                <p class="handler-prompt">{{ $t('environment.nodeInfo.connectedNodes') }}</p>
                <div class="construct-card-item connection-node-card">
                    <p class="no-connection-node" v-if="connectNodeDetail.status === 'UN_IMPORT'">
                        {{ $t('environment.nodeInfo.noConnectedNodes') }}，<span class="refresh-detail" @click="requetConstructNode">{{ $t('environment.clickToRefresh') }}</span>
                    </p>
                    <div class="connected-node-msg" v-if="['UN_IMPORT_OK','IMPORT_EXCEPTION'].includes(connectNodeDetail.status)">
                        <div class="node-info">
                            <img :src="defaultMachineCover" class="node-icon">
                            <div class="node-msg-intro">
                                <p class="node-name">{{ connectNodeDetail.hostname }}</p>
                                <p>
                                    <span class="agent-status">Agent{{ $t('environment.status') }}：</span>
                                    <span :class="connectNodeDetail.status === 'UN_IMPORT_OK' ? 'normal-status-node' : 'abnormal-status-node' ">
                                        {{ connectNodeDetail.status === 'UN_IMPORT_OK' ? $t('environment.nodeInfo.normal') : $t('environment.nodeInfo.abnormal') }}
                                    </span>
                                    <span class="operating-system">{{ $t('environment.nodeInfo.os') }}</span>：
                                    <span>{{ connectNodeDetail.os }}</span>
                                </p>
                            </div>
                        </div>
                        <!-- <div class="delete-handler"><i class="devops-icon icon-close"></i></div> -->
                    </div>
                </div>
                <p v-if="isAgent" class="target-console-tips">{{ $t('environment.nodeInfo.loginMethod') }}：ssh -p36000 root@{{ nodeIp }} {{ $t('environment.nodeInfo.checkMails') }}！</p>
            </div>

            <empty-tips v-if="!hasPermission"
                :title="emptyTipsConfig.title"
                :desc="emptyTipsConfig.desc"
                :btns="emptyTipsConfig.btns">
            </empty-tips>
        </div>
        <div slot="footer">
            <div class="footer-handler">
                <bk-button theme="primary" :disabled="connectNodeDetail.status === 'UN_IMPORT'"
                    @click="confirmFn">{{ constructToolConf.importText }}</bk-button>
                <bk-button theme="default" @click="cancelFn">{{ $t('environment.cancel') }}</bk-button>
            </div>
        </div>
    </bk-dialog>
</template>

<script>
    import emptyTips from '@/components/devops/emptyTips'
    import { copyText } from '@/utils/util'

    export default {
        components: {
            'empty-tips': emptyTips
        },
        props: {
            loading: Object,
            constructToolConf: Object,
            connectNodeDetail: Object,
            constructImportForm: Object,
            emptyTipsConfig: Object,
            gatewayList: Array,
            requetConstructNode: Function,
            confirmFn: Function,
            cancelFn: Function,
            hasPermission: Boolean,
            isAgent: Boolean,
            nodeIp: String
        },
        data () {
            return {
                defaultMachineCover: require('../../../scss/logo/machine.svg'),
                installDocsLink: this.BKCI_DOCS.WIN_AGENT_GUIDE
            }
        },
        methods: {
            copyCommand () {
                if (copyText(this.constructImportForm.link)) {
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('environment.successfullyCopyed')
                    })
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import './../../../scss/conf';

    .machine-params-form {

        .bk-form-radio {
            margin-right: 20px;
        }
        .handler-prompt {
            margin-top: 24px;
            text-align: left;
        }

        .construct-card-item {
            display: flex;
            justify-content: space-between;
            margin-top: 8px;
            padding: 0 14px;
            width: 100%;
            height: 48px;
            line-height: 48px;
            border: 1px solid $borderColor;
            background-color: $bgHoverColor;
        }

        .command-line {
            padding-right: 24px;
            width: 490px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .copy-button {
            color: $primaryColor;

            a {
                cursor: pointer;
            }
        }

        .connection-node-card {
            display: inline-block;
            height: 90px;
        }

        .windows-node-card {
            display: inline-block;
            height: 100%;
            .command-line {
                line-height: 40px
            }
        }

        .no-connection-node {
            margin-top: 18px;
            width: 100%;
            color: $fontLigtherColor;
        }

        .refresh-detail {
            color: $primaryColor;
            cursor: pointer;
        }

        .connected-node-msg {
            display: flex;
            justify-content: space-between;
            align-items: center;
            height: 100%;

            .node-icon {
                float: left;
                width: 40px;
                height: 46px;
                display: block;
                object-fit: cover;
            }

            p {
                line-height: 24px;
                text-align: left;
            }

            .node-msg-intro {
                float: left;
                margin-left: 14px;
            }

            .node-name {
                font-weight: bold;
            }

            .operating-system {
                margin-left: 14px;
            }

            .normal-status-node {
                color: #30D878;
            }

            .abnormal-status-node {
                color: $failColor;
            }

            .icon-close {
                position: relative;
                top: -26px;
                color: $fontLigtherColor;
                font-size: 12px;
                cursor: pointer;
            }
        }

        .target-console-tips {
            margin-top: 16px;
            text-align: left;
            font-size: 12px;
        }
    }

    .footer-handler {

        .bk-button {
            height: 32px;
            line-height: 32px;
        }
    }

    .devops-empty-tips {
        margin: 68px auto 72px;
        width: auto;
    }
</style>
