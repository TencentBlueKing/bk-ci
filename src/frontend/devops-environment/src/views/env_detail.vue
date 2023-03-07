<template>
    <div class="env-detail-wrapper">
        <content-header class="env-detail-header">
            <template slot="left">
                <i class="devops-icon icon-arrows-left" @click="toEnvList"></i>
                {{ curEnvDetail.name }}
            </template>
        </content-header>

        <div class="env-detail-container"
            v-bkloading="{
                isLoading: loading.isLoading,
                title: loading.title
            }">
            <div class="env-parameter-tab">
                <div class="env-detail-nav">
                    <div
                        v-for="tab in tabs"
                        :key="tab.tab"
                        :class="['tab-nav-item', tab.cls, { activeItem: curItemTab === tab.tabName }]"
                        @click="changeTab(tab.tabName)">{{ $t(`environment.${tab.label}`) }}
                    </div>
                </div>
                <component
                    class="env-detail-tab-content"
                    :is="activeTabComp"
                    :cur-env-detail="curEnvDetail"
                    :request-env-detail="requestEnvDetail"
                    :project-id="projectId"
                    :env-hash-id="envHashId"
                />
            </div>
        </div>
       
    </div>
</template>

<script>
    
    import baseTab from '@/components/envTabs/baseTab'
    import configTab from '@/components/envTabs/configTab'
    import nodeTab from '@/components/envTabs/nodeTab'
    import settingTab from '@/components/envTabs/settingTab'
    import { convertTime } from '@/utils/util'

    export default {
        components: {
            baseTab,
            configTab,
            nodeTab,
            settingTab
        },
        data () {
            return {
                curItemTab: 'node', // 当前tab(节点/配置项)
                curEnvDetail: {}, // 当前环境信息
                loading: {
                    isLoading: false,
                    title: ''
                }
            }
        },
        computed: {
            isBuildEnv () {
                return this.curEnvDetail && this.curEnvDetail.envType === 'BUILD'
            },
            tabs () {
                const tabs = [{
                    cls: 'node-list',
                    tabName: 'node',
                    label: 'node',
                    comp: nodeTab
                }, {
                    cls: 'config-item',
                    tabName: 'config',
                    label: 'configItem',
                    comp: configTab
                }, {
                    cls: 'base-item',
                    tabName: 'base',
                    label: 'basicInfo',
                    comp: baseTab
                }]
                if (this.isBuildEnv) {
                    tabs.push({
                        cls: 'base-item',
                        tabName: 'setting',
                        label: 'setting',
                        comp: settingTab
                    })
                }
                return tabs
            },
            activeTabComp () {
                const activeTab = this.tabs.find(tab => tab.tabName === this.curItemTab)
                return activeTab && activeTab.comp ? activeTab.comp : null
            },
            projectId () {
                return this.$route.params.projectId
            },
            envHashId () {
                return this.$route.params.envId
            }
        },
        watch: {
            projectId: async function (val) {
                this.$router.push({ name: 'envList' })
            }
        },
        created () {
            // 获取环境详情
            this.requestEnvDetail()
        },
        
        methods: {
            toEnvList () {
                this.$router.push({ name: 'envList' })
            },
            /**
             * 获取环境详情
             */
            async requestEnvDetail () {
                try {
                    this.loading.isLoading = true
                    this.loading.title = this.$t('environment.loadingTitle')
                    const res = await this.$store.dispatch('environment/requestEnvDetail', {
                        projectId: this.projectId,
                        envHashId: this.envHashId
                    })

                    this.curEnvDetail = res

                    res.envVars.forEach(item => {
                        item.isCreateItem = false
                        item.isEditItem = false

                        if (item.secure) {
                            item.isSecure = 'ciphertext'
                        } else {
                            item.isSecure = 'plaintext'
                        }
                    })
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    this.loading.isLoading = false
                }
            },
            /**
             * 切换节点/配置项
             */
            changeTab (curTab) {
                this.curItemTab = curTab

                this.$nextTick(() => {
                    const obj = this.$el.querySelector('.config-item')

                    if (curTab === 'config') {
                        obj.className += ' ' + 'config-active'
                    } else {
                        obj.classList.remove('.config-active')
                    }
                })
            },
           
            /**
             * 处理时间格式
             */
            localConvertTime (timestamp) {
                return convertTime(timestamp * 1000)
            }
        }
    }
</script>

<style lang='scss'>
    @import './../scss/conf';

    %flex {
        display: flex;
        align-items: center;
    }

    .env-detail-wrapper {
        height: 100%;
        overflow: hidden;

        .env-detail-header {
            .icon-arrows-left {
                margin-right: 4px;
                cursor: pointer;
                color: $iconPrimaryColor;
                font-size: 16px;
                font-weight: 600;
            }
        }

        .env-detail-container {
            padding: 20px;
            height: 92%;
            overflow: auto;
        }

        .env-parameter-tab {
            border: 1px solid $borderWeightColor;
            height: 100%;
            overflow: hidden;
            background-color: #fff;
            display: flex;
            flex-direction: column;

            .paas-ci-empty {
                height: 94%;
            }
        }

        .env-detail-nav {
            height: 42px;
            border-bottom: 1px solid $borderWeightColor;
            background-color: rgb(250, 251, 253);
        }

        .env-detail-tab-content {
            flex: 1;
            padding: 12px;
            overflow: auto;
        }

        .tab-nav-item {
            float: left;
            width: 100px;
            height: 100%;
            line-height: 42px;
            text-align: center;
            background-color: rgb(250, 251, 253);
            font-size: 14px;
            color: $fontWeightColor;
            cursor: pointer;
        }

        .activeItem {
            height: 42px;
            background: #fff;
            color: $primaryColor;
        }

        .node-list {
            border-right: 1px solid $borderWeightColor;
        }

        .base-item {
            border-left: 1px solid $borderWeightColor;
            border-right: 1px solid $borderWeightColor;
        }

        .config-active {
            border-right: none;
        }

        .config-content-wrapper,
        .node-content-wrapper {
            height: 94%;
            overflow: hidden;
        }

        .node-content-header,
        .config-content-header {
            padding: 0 20px;

            .bk-button {
                width: 76px;
                height: 32px;
                line-height: 30px;
            }
        }

        .config-content-header {

            .bk-button {
                padding: 0 8px;
                width: 90px;
            }
        }

        .table-head,
        .table-row {
            padding: 0 20px;
            @extend %flex;
            height: 43px;
            font-size: 12px;
            color: #333C48;
        }

        .table-row {
            border-top: 1px solid $borderWeightColor;
            color: $fontWeightColor;
            font-size: 12px;

            &:last-child {
                border-bottom: 1px solid $borderWeightColor;
            }
        }

        .config-table {
            height: calc(95% - 32px);
            overflow: auto;
        }

        .config-row,
        .config-head {
            padding-right: 0
        }

        .node-item-os {
            flex: 3;
        }

        .node-item-ip,
        .node-item-name,
        .node-item-type,
        .node-item-agstatus {
            flex: 2;
        }

        .node-item-area,
        .node-item-status {
            flex: 1;
        }

        .prompt-operator,
        .edit-operator {
            padding-right: 10px;
            color: #ffbf00;

            .devops-icon {
                margin-right: 6px;
            }
        }

        .edit-operator {
            cursor: pointer;
        }

        .node-item-handler {
            flex: 1;
            padding-right: 20px;
            text-align: right;
        }

        .node-header-head {
            position: relative;
            right: 16px;
            left: 2px;
        }

        .table-config-item {
            padding-right: 20px;
        }

        .config-item-key,
        .config-item-value,
        .config-item-type {
            flex: 5
        }

        .config-item-value {
            position: relative;

            .devops-icon {
                position: absolute;
                top: 9px;
                right: 30px;
                font-size: 16px;
                cursor: pointer;
            }
        }

        .config-item-handler {
            flex: 1;
            min-width: 88px;
        }

        .item-handler-head {
            position: relative;
            right: 34px;
            text-align: right;
        }

        .node-status-icon {
            display: inline-block;
            margin-left: 2px;
            width: 10px;
            height: 10px;
            border: 2px solid #30D878;
            border-radius: 50%;
            -webkit-border-radius: 50%;
        }

        .abnormal-stutus-icon {
            border-color: $failColor;
        }

        .normal-status-node {
            color: #30D878;
        }

        .abnormal-status-node {
            color: $failColor;
        }

        .node-delete,
        .config-edit,
        .text-type {
            color: $primaryColor;
            cursor: pointer;
        }

        .install-btn {
            color: $primaryColor;
            cursor: pointer;
        }

        .install-btn {
            margin-left: 4px;
        }

        .config-edit {
            margin-right: 10px;
        }

        .config-input {
            width: 100%;
            height: 32px;
            border-color: $lineColor;
            font-size: 12px;
        }

        .bk-selector,
        .bk-selector-input {
            width: 120px;
            height: 32px;
            font-size: 12px;
        }

        .conf-error-tips {
            position: absolute;
        }

        .config-text-type {
            width: 120px;
        }

        .is-danger {
            border-color: #ff5656;
            background-color: #fff4f4;
            color: #ff5656;
        }

        .base-message-wrapper {
            .base-env-form {
                margin: 0 20px;
                border: 1px solid $borderWeightColor;
            }
            .bk-form-item {
                margin-top: 6px;
                padding-bottom: 6px;
                padding-left: 20px;
                border-bottom: 1px solid $borderWeightColor;
                &:last-child {
                    border: none;
                }
            }
            .bk-label {
                width: 90px;
                font-weight: normal;
                &:after {
                    position: absolute;
                    content: '';
                    top: -6px;
                    left: 110px;
                    height: 84px;
                    width: 1px;
                    background-color: $borderWeightColor;
                }
            }
            .create-user-item {
                .bk-label:after {
                    height: 45px;
                }
            }
            .bk-form-content {
                display: flex;
                margin-left: 110px;
            }
            .env-name-input {
                width: 320px;
            }
            .env-desc-input {
                padding: 10px;
                width: 540px;
                min-height: 60px;
                line-height: 20px;
            }
            .env-base,
            .handler-btn {
                line-height: 32px;
                font-size: 14px;
            }
            .env-type-radio {
                position: relative;
                top: 6px;
                margin-right: 10px;
            }
            .handler-btn {
                display: flex;
                align-items: center;
                margin-left: 20px;
                .icon-edit {
                    &:hover {
                        color: $primaryColor;
                        cursor: pointer;
                    }
                }
            }
            .edit-base {
                margin-right: 8px;
                color: $primaryColor;
                cursor: pointer;
            }
            .env-desc-content,
            .env-name-content {
                display: inline-block;
                line-height: 18px;
                margin-top: 9px;
                max-width: 540px;
                word-break: break-all;
            }
        }
    }
</style>
