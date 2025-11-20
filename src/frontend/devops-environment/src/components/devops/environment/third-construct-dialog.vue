<template>
    <bk-dialog
        v-model="constructToolConf.isShow"
        width="1000"
        :position="dialogPositionConfig"
        :close-icon="false"
        :title="$t('environment.nodeInfo.importBuildNode')"
        header-position="left"
    >
        <div
            v-bkloading="{
                isLoading: loading.isLoading,
                title: loading.title
            }"
        >
            <div
                class="machine-params-form"
                v-if="hasPermission"
            >
                <bk-form :label-width="160">
                    <bk-form-item
                        required
                        label="OS"
                    >
                        <bk-radio-group v-model="constructImportForm.model">
                            <bk-radio
                                :value="'LINUX'"
                                :disabled="isAgent"
                            >
                                Linux
                            </bk-radio>
                            <bk-radio
                                :value="'MACOS'"
                                :disabled="isAgent"
                            >
                                macOS
                            </bk-radio>
                            <bk-radio
                                :value="'WINDOWS'"
                                :disabled="isAgent"
                            >
                                Windows
                            </bk-radio>
                        </bk-radio-group>
                    </bk-form-item>
                    <bk-form-item
                        v-if="gatewayList.length"
                        required
                        :label="$t('environment.nodeInfo.location')"
                        :desc="$t('environment.nodeInfo.buildMachineLocationTips')"
                    >
                        <bk-radio-group v-model="constructImportForm.location">
                            <bk-radio
                                v-for="(model, index) in gatewayList"
                                :key="index"
                                :value="model.zoneName"
                                :disabled="isAgent"
                            >
                                {{ model.showName }}
                            </bk-radio>
                        </bk-radio-group>
                    </bk-form-item>
                    <bk-form-item
                        v-if="isWindowOs"
                        required
                        :label="$t('environment.nodeInfo.agentInstallMode.label')"
                    >
                        <bk-radio-group v-model="constructImportForm.installType">
                            <bk-radio
                                v-for="(item, index) in installTypeList"
                                :key="index"
                                :value="item.id"
                                class="install-type-item"
                                v-bk-tooltips="item.tips"
                            >
                                {{ item.label }}
                            </bk-radio>
                        </bk-radio-group>
                    </bk-form-item>
                    <!-- <bk-form-item
                        v-if="isWindowOs && installModeAsService"
                        required
                        :label="$t('environment.nodeInfo.switchRunningAccount')"
                        :desc="$t('environment.nodeInfo.switchAccountTips')"
                    >
                        <bk-radio-group v-model="constructImportForm.autoSwitchAccount">
                            <bk-radio
                                v-for="(item, index) in loginAsUserList"
                                :key="index"
                                :value="item.id"
                            >
                                {{ item.label }}
                            </bk-radio>
                        </bk-radio-group>
                    </bk-form-item>
                    <bk-form-item
                        v-if="isWindowOs && constructImportForm.autoSwitchAccount && installModeAsService"
                        required
                        :label="$t('environment.nodeInfo.runAccount')"
                    >
                        <div class="run-account-inputs">
                            <bk-input
                                v-model="constructImportForm.loginName"
                                :placeholder="$t('environment.nodeInfo.accountUsernameTips')"
                                @blur="requestDevCommand"
                            />
                            <bk-input
                                v-model="constructImportForm.loginPassword"
                                :placeholder="$t('environment.nodeInfo.accountPasswordTips')"
                                @blur="requestDevCommand"
                            />
                        </div>
                    </bk-form-item> -->
                </bk-form>
                <p class="handler-prompt">
                    <i18n
                        tag="span"
                        path="environment.nodeInfo.executeCommandPrompt"
                    >
                        <span style="font-weight: bold;">
                            {{ $t('environment.nodeInfo.nonSystemPath') }}
                        </span>
                    </i18n>
                </p>
                <div
                    class="construct-card-item command-tool-card"
                    v-if="!isWindowOs"
                >
                    <p class="command-title">
                        {{ $t('environment.nodeInfo.createDirectory') }}
                    </p>
                    <div class="command-line">
                        $ mkdir /data/landun && cd /data/landun
                    </div>
                    <p class="command-title">
                        {{ $t('environment.nodeInfo.downloadAndInstallAgent') }}
                    </p>
                    <div class="command-line">
                        {{ constructImportForm.link }}
                        <span
                            v-if="constructImportForm.link"
                            @click="copyCommand"
                        >
                            <Icon
                                name="copy2"
                                size="26"
                                class="copy-icon"
                            />
                        </span>
                    </div>
                </div>
                <div
                    class="construct-card-item command-tool-card windows-node-card"
                    v-if="isWindowOs"
                >
                    <template v-if="showRunAccountTips">
                        <div class="command-line">
                            {{ $t('environment.nodeInfo.pleaseEnterRunAccount') }}
                        </div>
                    </template>
                    <template v-else>
                        <div class="command-line">
                            {{ $t('environment.nodeInfo.windowsInstallationCommand.tip1') }}
                        </div>
                        <div class="command-line">
                            {{ $t('environment.nodeInfo.windowsInstallationCommand.tip2') }}
                        </div>
                        <div class="command-line">
                            {{ $t('environment.nodeInfo.windowsInstallationCommand.tip3') }}
                        </div>
                        <div class="command-line">
                            {{ constructImportForm.link }}
                            <span
                                v-if="constructImportForm.link"
                                @click="copyCommand"
                            >
                                <Icon
                                    name="copy2"
                                    size="26"
                                    class="copy-icon"
                                />
                            </span>
                        </div>
                        <div
                            class="command-line"
                            v-if="installModeAsService && !constructImportForm.autoSwitchAccount"
                        >
                            {{ $t('environment.nodeInfo.windowsInstallationCommand.tip4', ['{agent_id}']) }}
                        </div>
                    </template>
                </div>
                <p class="handler-prompt">{{ $t('environment.nodeInfo.installSuccessfullyTips') }}</p>
                <bk-button
                    class="mt10"
                    text
                    @click="handleToHelpDocument"
                >
                    {{ $t('environment.nodeInfo.installFailedDocument') }}
                </bk-button>
            </div>

            <empty-tips
                v-if="!hasPermission"
                :title="emptyTipsConfig.title"
                :desc="emptyTipsConfig.desc"
                :btns="emptyTipsConfig.btns"
            >
            </empty-tips>
        </div>
        <div slot="footer">
            <div class="footer-handler">
                <bk-button
                    theme="primary"
                    @click="confirmFn"
                >
                    {{ $t('environment.nodeInfo.close') }}
                </bk-button>
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
            constructImportForm: Object,
            emptyTipsConfig: Object,
            gatewayList: Array,
            confirmFn: Function,
            hasPermission: Boolean,
            isAgent: Boolean,
            nodeIp: String,
            requestDevCommand: Function
        },
        data () {
            return {
                defaultMachineCover: require('../../../scss/logo/machine.svg'),
                installDocsLink: this.BKCI_DOCS.WIN_AGENT_GUIDE
            }
        },
        computed: {
            showRunAccountTips () {
                const { autoSwitchAccount, loginName, loginPassword, link } = this.constructImportForm
                return this.installModeAsService && autoSwitchAccount & (!loginName || !loginPassword || !link)
            },
            isWindowOs () {
                return this.constructImportForm.model === 'WINDOWS'
            },
            installModeAsService () {
                return this.constructImportForm.installType === 'SERVICE'
            },
            loginAsUserList () {
                return [
                    {
                        id: true,
                        label: this.$t('environment.nodeInfo.yes')
                    },
                    {
                        id: false,
                        label: this.$t('environment.nodeInfo.no')
                    }
                ]
            },
            installTypeList () {
                return [
                    {
                        id: 'SERVICE',
                        label: this.$t('environment.nodeInfo.agentInstallMode.windowsServices'),
                        tips: this.$t('environment.nodeInfo.agentInstallMode.widowsTips'),
                    },
                    {
                        id: 'TASK',
                        label: this.$t('environment.nodeInfo.agentInstallMode.scheduledTasks'),
                        tips: this.$t('environment.nodeInfo.agentInstallMode.scheduledTasksTips'),
                    }
                ]
            },
            dialogPositionConfig () {
                return {
                    top: this.constructImportForm.model === 'WINDOWS' && this.constructImportForm.link ? '120' : '200'
                }
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
            },
            handleToHelpDocument () {
                window.open(this.BKCI_DOCS?.BUILD_NODE_GUIDE_DOC, '_blank')
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
            margin-top: 8px;
            padding: 14px;
            width: 100%;
            border: 1px solid $borderColor;
            background-color: $bgHoverColor;
        }
        .command-title {
            text-align: left;
            color: #b4bac2;
            margin-bottom: 5px;
        }
        .command-line {
            display: flex;
            align-items: center;
            padding-right: 12px;
            word-break: break-all;
            margin-bottom: 12px;
        }
        .copy-icon {
            margin-left: 20px;
            cursor: pointer;
        }

        .connection-node-card {
            display: inline-block;
            height: 90px;
        }

        .windows-node-card {
            display: inline-block;
            height: 100%;
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
    .run-account-inputs {
        display: flex;
        width: 85%;
    }
</style>
<style lang="scss">
    .install-type-item {
        .bk-radio-text {
            border-bottom: 1px dashed #979ba5;
            cursor: pointer;
        }
    }
</style>
