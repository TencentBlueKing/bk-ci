<template>
    <article class="add-agent-home">
        <header class="add-agent-head">
            <bk-breadcrumb separator-class="bk-icon icon-angle-right">
                <bk-breadcrumb-item v-for="(item,index) in navList" :key="index" :to="item.link">{{item.title}}</bk-breadcrumb-item>
            </bk-breadcrumb>
        </header>

        <main class="add-agent-body">
            <h3 class="agent-tips">
                <span>{{$t('setting.agent.importAgentTips')}}</span>
                <bk-link theme="primary" :href="LINK_CONFIG.SELF_HOSTED_AGENT" target="_blank">{{$t('setting.agent.linkTips')}}</bk-link>
            </h3>

            <section class="agent-filter">
                <span class="filter-title">{{$t('setting.agent.system')}}</span>
                <bk-select @change="getThirdAgentLink" v-model="machine.system" :loading="isLoading" :clearable="false" behavior="simplicity" class="filter-select">
                    <bk-option v-for="option in operateSystems"
                        :key="option.id"
                        :id="option.id"
                        :name="option.name">
                    </bk-option>
                </bk-select>
                <span class="filter-title">{{$t('setting.agent.architecture')}}</span>
                <bk-select v-model="machine.architecture" :clearable="false" behavior="simplicity" class="filter-select">
                    <bk-option v-for="option in architectures"
                        :key="option"
                        :id="option"
                        :name="option">
                    </bk-option>
                </bk-select>
            </section>

            <section class="agent-info use-tip">
                <section v-html="computedHtml"></section>

                <h3>{{$t('setting.agent.connectedAgent')}}</h3>
                <p v-bkloading="{ isLoading: isRefresh }" class="agent-status">
                    <span class="agent-refresh" v-if="agentStatus.status === 'UN_IMPORT'">{{$t('setting.agent.noConnected')}}，<bk-button text @click="getAgentStatus">{{$t('refresh')}}</bk-button></span>
                    <section v-else class="agent-status-info">
                        <span class="agent-title">{{ agentStatus.hostname }}</span>
                        <span class="agent-os">
                            <span class="title">{{$t('setting.agent.agentStatus')}}</span>
                            <span>{{ agentStatus.status === 'UN_IMPORT_OK' ? 'normal' : 'abnormal' }}</span>
                            <span class="title">{{$t('setting.agent.operatingSystem')}} :</span>
                            <span>{{ agentStatus.os }}</span>
                        </span>
                    </section>
                </p>
            </section>

            <h3 class="self-hosted-agent">{{$t('setting.agent.useAgent')}}</h3>
            <section class="agent-info">
                <p>
                    <span class="gray">{{$t('setting.agent.useAgentTips')}}</span>
                    <span class="block">runs-on:</span>
                    <span class="block">&nbsp;&nbsp;self-hosted: true</span>
                    <span class="block">&nbsp;&nbsp;pool-name: {{$route.params.poolName}}</span>
                </p>
            </section>
        </main>

        <bk-button class="bottom-btn" theme="primary" @click="importNode" :loading="isAdding" :disabled="agentStatus.status === 'UN_IMPORT'">{{$t('import')}}</bk-button>
        <bk-button class="bottom-btn" @click="backToAgentList">{{$t('setting.agent.backToList')}}</bk-button>
    </article>
</template>

<script>
    import { setting } from '@/http'
    import { mapState } from 'vuex'
    import LINK_CONFIG from '@/conf/link-config.js'

    export default {
        data () {
            return {
                operateSystems: [
                    { id: 'MACOS', name: 'macOS' },
                    { id: 'LINUX', name: 'Linux' },
                    { id: 'WINDOWS', name: 'Windows' }
                ],
                navList: [
                    { link: { name: 'agentPools' }, title: this.$t('setting.agent.agentPools') },
                    { link: { name: 'agentList' }, title: this.$route.params.poolName },
                    { link: '', title: this.$t('setting.agent.addAgent') }
                ],
                architectures: ['x64'],
                machine: {
                    system: 'MACOS',
                    architecture: 'x64',
                    zone: 'shenzhen',
                    link: '',
                    agentId: ''
                },
                agentStatus: {
                    hostname: '',
                    ip: '',
                    os: '',
                    status: 'UN_IMPORT'
                },
                isLoading: false,
                isRefresh: false,
                isAdding: false,
                LINK_CONFIG
            }
        },

        computed: {
            ...mapState(['projectId']),

            computedHtml () {
                const unixHtml = `
                    <h3>${this.$t('setting.agent.downloadAndInstall')}</h3>
                    <p>
                        <span class="gray"># Create a folder</span>
                        <span class="mb10">$ mkdir /data/landun && cd  /data/landun</span>
                        <span class="gray"># Download & Install the latest agent package, and run it!</span>
                        <span class="mb10">$ ${this.machine.link}</span>
                    </p>
                `
                const windowHtml = `
                    <h3>${this.$t('setting.agent.downloadAndInstall')}</h3>
                    <p>
                        <span class="mb10">1. Download the latest agent<a href="${this.machine.link}" target="_blank">Click here to download agent</a></span>
                        <span class="mb10">2. Create a folder, such as D:\\data\\landun</span>
                        <span class="mb10">3. Extract the installer to D:\\data\\landun</span>
                        <span class="mb10">4. Execute install.bat by administrator</span>
                        <span class="mb10">5. In order to read user environment, please change the setup user from system to the login user, such as tencent\\zhangsan<a href="${this.LINK_CONFIG.WINDOWS_AGENT}" target="_blank">Learn more</a></span>
                    </p>
                `
                return this.machine.system === 'WINDOWS' ? windowHtml : unixHtml
            }
        },

        created () {
            this.getThirdAgentLink()
        },

        methods: {
            backToAgentList () {
                this.$router.push({ name: 'agentList' })
            },

            getThirdAgentLink () {
                return setting.getThirdAgentLink(this.projectId, this.machine.system, this.machine.zone).then((res) => {
                    const data = res || {}
                    this.machine.link = data.link
                    this.machine.agentId = data.agentId
                    return this.getAgentStatus()
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                })
            },

            getAgentStatus () {
                this.isRefresh = true
                return setting.getThirdAgentStatus(this.projectId, this.machine.agentId).then((res) => {
                    this.agentStatus = res || {}
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isRefresh = false
                })
            },

            importNode () {
                this.isAdding = true
                setting.addNodeToSystem(this.projectId, this.machine.agentId).then(() => {
                    return setting.getSystemNodeList(this.projectId).then((res) => {
                        const curNode = res.find((node) => (node.agentHashId === this.machine.agentId)) || {}
                        const params = [curNode.nodeHashId]
                        return setting.addNodeToPool(this.projectId, this.$route.params.poolId, params).then(() => {
                            this.backToAgentList()
                            this.$bkMessage({ theme: 'success', message: 'Imported successfully' })
                        })
                    })
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isAdding = false
                })
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .add-agent-head {
        height: 49px;
        line-height: 49px;
        background: #fff;
        box-shadow: 0 2px 5px 0 rgba(51,60,72,0.03);
        padding: 0 25.5px;
    }
    .add-agent-body {
        padding: 16px;
        overflow-y: auto;
        max-height: calc(100vh - 176px);
        .agent-tips {
            line-height: 20px;
            font-size: 14px;
            color: #313328;
            a {
                margin-left: 12px;
            }
        }
        .agent-filter {
            background: #fff;
            height: 64px;
            display: flex;
            align-items: center;
            padding: 0 19px;
            margin-top: 16px;
            .filter-title {
                color: #7b7d8a;
                display: inline-block;
                margin-right: 8px;
            }
            .filter-select {
                width: 160px;
                margin-right: 32px;
                /deep/ .bk-select-name {
                    font-weight: bold;
                    color: #313328;
                }
            }
        }
        .use-tip {
            margin-top: 16px;
        }
        .self-hosted-agent {
            margin: 24px 0 10px;
            color: #313328;
        }
        .agent-info {
            background: #fff;
            overflow: hidden;
            padding-top: 12px;
            /deep/ h3 {
                color: #313328;
                margin: 5px 0 0 20px;
            }
            /deep/ p {
                margin: 8.6px 20px 24px;
                background: #fafbfd;
                border: 1px solid #e1e3e9;
                padding: 12px;
                line-height: 17px;
                font-size: 12px;
                color: #7b7d8a;
                .gray {
                    color: #979ba5;
                    display: block;
                    margin-bottom: 4px;
                }
                .mb10 {
                    display: block;
                    margin-bottom: 10px;
                    a {
                        color: #3a84ff;
                        margin-left: 10px;
                    }
                }
                .block {
                    display: block;
                }
            }
            .agent-status {
                min-height: 60px;
                .agent-refresh {
                    width: 100%;
                    height: 100%;
                    display: inline-block;
                    line-height: 36px;
                    font-size: 14px;
                    text-align: center;
                }
                .agent-status-info {
                    color: #63656e;
                    .agent-title {
                        font-size: 14px;
                        line-height: 20px;
                        display: block;
                        margin-bottom: 5px;
                    }
                    .agent-os {
                        display: flex;
                        align-items: center;
                        font-size: 12px;
                        .title {
                            color: #979ba5;
                            display: inline-block;
                            margin: 0 7px 0 20px;
                            &:first-child {
                                margin-left: 0;
                            }
                        }
                    }
                }
            }
        }
    }
    .bottom-btn {
        margin: 12px 16px 16px;
        +button {
            margin-left: 0;
        }
    }
    /deep/ .bk-link-text {
        font-size: 12px;
    }
</style>
