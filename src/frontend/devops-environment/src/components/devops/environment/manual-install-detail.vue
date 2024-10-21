<template>
    <bk-tab
        :active.sync="active"
        ext-cls="manual-install-tab"
        type="unborder-card"
        v-bkloading="{ isLoading: !commandStep.type }"
    >
        <bk-tab-panel
            name="method"
            :label="$t('environment.安装方式')"
        >
            <a
                class="iwiki-link"
                :href="commandStep.networkPolicyDocLink"
                target="_blank"
            >
                {{ $t('environment.无法连通网络？') }}
            </a>
            <div class="form-item">
                <label class="label">
                    {{ $t('environment.安装方式：') }}
                </label>
                <div class="content">
                    <span class="install-type">
                        <span>
                            {{ commandStep.type }}
                        </span>
                    </span>
                    <span class="install-desc">{{ commandStep.description }}</span>
                </div>
            </div>
    
            <div class="form-item mt40">
                <label class="label">
                    {{ $t('environment.在目标主机通过 shell 安装：') }}
                </label>
                <div class="line-content">
                    <ul
                        class="bk-line"
                        v-for="(item, index) in commandStep.steps"
                        :key="item"
                    >
                        <li class="line-dot">
                            <div class="line-index">{{ index + 1 }}</div>
                            <div class="line-section">
                                <div class="title">{{ item.description }}</div>
                                <div class="content">
                                    <span @click="handleCopy(item.contents[0].text)">
                                        <Icon
                                            class="copy-icon"
                                            name="copy"
                                            size="16"
                                        />
                                    </span>
                                    {{ item.contents[0].text }}
                                </div>
                            </div>
                        </li>
                    </ul>
                </div>
            </div>
        </bk-tab-panel>
        <!-- <bk-tab-panel name="policy" :label="$t('environment.网络策略')">
            <p>{{ $t('environment.1、Linux/Unix 系統使用非 root 安装时，要求可以免密sudo执行/tmp/setup_agent.sh 脚本') }}</p>
            <p>{{ $t('environment.2、直连 Agent 端：保证与蓝鯨服务端以下端口互通，可使用telnet来确认端口是否可通') }}</p>

            <bk-table class="mt20">
                <bk-table-column :label="$t('environment.源地址')">
                </bk-table-column>
                <bk-table-column :label="$t('environment.目标地址')">
                </bk-table-column>
                <bk-table-column :label="$t('environment.端口')">
                </bk-table-column>
                <bk-table-column :label="$t('environment.协议')">
                </bk-table-column>
                <bk-table-column :label="$t('environment.用途')">
                </bk-table-column>
            </bk-table>
        </bk-tab-panel> -->
        <bk-tab-panel
            name="log"
            :label="$t('environment.安装日志')"
        >
            <div class="install-status">
                <template v-if="['PENDING', 'RUNNING'].includes(installStatus)">
                    <Icon
                        class="icon"
                        name="loading"
                    />
                    <span>{{ $t('environment.installingTips') }}</span>
                </template>
                <template v-else-if="installStatus === 'SUCCESS'">
                    <i class="bk-icon import-status-icon icon-check-1 success icon"></i>
                    <span>{{ $t('environment.installSuccessTips') }}</span>
                </template>
                <template v-else>
                    <i class="bk-icon import-status-icon icon-close error icon"></i>
                    <span>{{ $t('environment.installFailTips') }}</span>
                    <!-- <bk-button
                        text
                        class="ml10"
                        @click="$emit('retry')"
                    >
                        {{ $t('environment.retry') }}
                    </bk-button> -->
                </template>
            </div>
            <div
                ref="editor"
                class="log-wrapper"
            >
                <div
                    ref="executeScriptLog2"
                    v-once
                    id="executeScriptLog2"
                    style="height: 100%;"
                />
            </div>
        </bk-tab-panel>
    </bk-tab>
</template>

<script>
    import { copyText } from '@/utils/util'
    export default {
        props: {
            commandStep: Object,
            installStatus: String,
            stepStatus: String
        },
        data () {
            return {
                active: 'method'
            }
        },
        created () {
            if (['SUCCESS', 'FAILED'].includes(this.stepStatus)) {
                this.active = 'log'
            }
        },
        methods: {
            handleCopy (text) {
                if (copyText(text)) {
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
    .form-item {
        .label {
            font-size: 14px;
            font-weight: 700;
        }
        .content {
            margin-top: 10px;
            display: flex;
        }
        .line-content {
            margin-top: 10px;
        }
        .install-type {
            display: inline-block;
            background: #f1f2f6;
            padding: 12px 6px;
            border-radius: 5px;
            span {
                background: #FFF;
                padding: 6px 6px;
                position: static;
                font-size: 14px;
                border-radius: 5px;
                font-weight: 700;
                color: #5889f7;
            }
        }
        .install-desc {
            margin-left: 10px;
            display: flex;
            align-items: center;
            word-break: break-all;
        }
        .bk-line {
            list-style: none;
            margin: 40px 0 0;
            padding: 0;
            text-align: left;
            line-height: normal;
            font-weight: 400;
            font-style: normal;
        }
        .bk-line:last-child {
            .line-dot {
                border-left: none;
            }
        }
        .line-dot {
            position: relative;
            border-left: 1px solid #d8d8d8;
            padding-left: 16px;
            left: 5px;
        }
        .line-index {
            position: absolute;
            top: -24px;
            left: -9px;
            width: 20px;
            height: 20px;
            border-radius: 50%;
            background-color: #5788f7;
            color: #FFF;
            text-align: center;
            line-height: 20px;
        }
        .line-section {
            position: relative;
            top: -25px;
            left: 8px;
            .title {
                font-size: 14px;
            }
            .content {
                width: 100%;
                padding: 10px;
                background-color: #f6f7fa;
                word-break: break-all;
            }
            .copy-icon {
                flex-shrink: 0;
                margin-right: 5px;
                cursor: pointer;
            }
        }
    }
</style>

<style lang="scss">
    .manual-install-tab {
        height: 100%;
        .bk-tab-section {
            height: 95%;
        }
        .bk-tab-content {
            height: 100%;
        }
    }
    .iwiki-link {
        flex-direction: row-reverse;
        display: flex;
        color: #3a84ff;
        cursor: pointer;
    }
    
</style>
