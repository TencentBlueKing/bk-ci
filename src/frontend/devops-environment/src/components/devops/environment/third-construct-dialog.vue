<template>
    <bk-dialog v-model="constructToolConf.isShow"
        :width="'640'"
        :close-icon="false">
        <div
            v-bkloading="{
                isLoading: loading.isLoading,
                title: loading.title
            }">
            <div class="bk-form machine-params-form" v-if="hasPermission">
                <div class="bk-form-item is-required">
                    <label class="bk-label env-item-label">机型：</label>
                    <div class="bk-form-content" style="margin-top:4px;">
                        <bk-radio-group v-model="constructImportForm.model">
                            <bk-radio :value="'MACOS'" :disabled="isAgent">macOS</bk-radio>
                            <bk-radio :value="'LINUX'" :disabled="isAgent">Linux</bk-radio>
                            <bk-radio :value="'WINDOWS'" :disabled="isAgent">Windows</bk-radio>
                        </bk-radio-group>
                    </div>
                </div>
                <div class="bk-form-item is-required" v-if="gatewayList.length">
                    <label class="bk-label env-item-label">
                        <!-- 地点： -->
                        <bk-popover placement="right">
                            <span style="padding-bottom: 3px; border-bottom: dashed 1px #c3cdd7;">地点</span>：
                            <template slot="content">
                                <p style="width: 300px; text-align: left; white-space: normal;word-break: break-all;font-weight: 400;">
                                    请根据构建机的归属地选择最近的接入点，这样可以提高构建机的归档和拉取构建产物的速度，并增加构建机的连接稳定性。PS:北京地区请选择天津。
                                </p>
                            </template>
                        </bk-popover>
                    </label>
                    <div class="bk-form-content" style="margin-top:4px;">
                        <bk-radio-group v-model="constructImportForm.location">
                            <bk-radio v-for="(model, index) in gatewayList"
                                :key="index"
                                :value="model.zoneName"
                                :disabled="isAgent"
                            >{{ model.showName }}</bk-radio>
                        </bk-radio-group>
                    </div>
                </div>
                <p class="handler-prompt">{{ constructImportForm.model === 'WINDOWS' ? '请参考以下步骤' : '在目标构建机任意路径下(工作空间)执行如下命令：'}}</p>
                <div class="construct-card-item command-tool-card" v-if="constructImportForm.model !== 'WINDOWS'">
                    <div class="command-line">
                        {{ constructImportForm.link }}
                    </div>
                    <div class="copy-button">
                        <a class="text-link copy-command"
                            @click="copyCommand">
                            点击复制</a>
                    </div>
                </div>
                <div class="construct-card-item command-tool-card windows-node-card" v-if="constructImportForm.model === 'WINDOWS'">
                    <div class="command-line">
                        1.<a class="refresh-detail" :href="constructImportForm.link">点击</a>下载Agent
                        <br>
                        2.查阅【<a class="refresh-detail" target="_blank" :href="installDocsLink">如何在Windows上安装蓝盾构建机Agent</a>】
                    </div>
                </div>
                <p class="handler-prompt">已连接的节点</p>
                <div class="construct-card-item connection-node-card">
                    <p class="no-connection-node" v-if="connectNodeDetail.status === 'UN_IMPORT'">
                        暂未连接任何节点，<span class="refresh-detail" @click="requetConstructNode">点击刷新</span>
                    </p>
                    <div class="connected-node-msg" v-if="['UN_IMPORT_OK','IMPORT_EXCEPTION'].includes(connectNodeDetail.status)">
                        <div class="node-info">
                            <img :src="defaultMachineCover" class="node-icon">
                            <div class="node-msg-intro">
                                <p class="node-name">{{ connectNodeDetail.hostname }}</p>
                                <p>
                                    <span class="agent-status">Agent状态：</span>
                                    <span :class="connectNodeDetail.status === 'UN_IMPORT_OK' ? 'normal-status-node' : 'abnormal-status-node' ">
                                        {{ connectNodeDetail.status === 'UN_IMPORT_OK' ? '正常' : '异常' }}
                                    </span>
                                    <span class="operating-system">操作系统：</span>
                                    <span>{{ connectNodeDetail.os }}</span>
                                </p>
                            </div>
                        </div>
                        <!-- <div class="delete-handler"><i class="bk-icon icon-close"></i></div> -->
                    </div>
                </div>
                <p v-if="isAgent" class="target-console-tips">目标机登录方式：ssh -p36000 root@{{ nodeIp }} 密码请查收邮件！</p>
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
                <bk-button theme="default" @click="cancelFn">取消</bk-button>
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
                installDocsLink: `${DOCS_URL_PREFIX}/所有服务/环境管理/如何在Windows上安装蓝盾构建机Agent.html`
            }
        },
        methods: {
            copyCommand () {
                if (copyText(this.constructImportForm.link)) {
                    this.$bkMessage({
                        theme: 'success',
                        message: '复制成功'
                    })
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import './../../../scss/conf';

    .machine-params-form {
        .bk-label {
            padding-right: 0;
            width: 44px;
            font-weight: normal;

            &:after {
                display: none;
            }
        }

        .bk-form-content {
            margin-left: 52px;
        }

        .bk-form-radio {
            margin-right: 20px;
        }

        .bk-radio-text {
            color: $fontWeightColor;
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
            height: 85px;
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
