<template>
    <div class="node-detail-wrapper"
        v-bkloading="{
            isLoading: loading.isLoading,
            title: loading.title
        }">
        <div class="info-header">
            <div slot="left">
                <bk-breadcrumb separator-class="bk-icon icon-angle-right">
                    <bk-breadcrumb-item v-for="(item,index) in navList" :key="index" :to="item.link">{{item.title}}</bk-breadcrumb-item>
                </bk-breadcrumb>
            </div>
            <div slot="right" class="node-handle">
                <span class="copy-btn" @click="copyHandle">
                    {{ nodeDetails.os === 'WINDOWS' ? $t('setting.agent.copyDownloadLink') : $t('setting.agent.copyInstallCommand')}}
                </span>
                <span class="download-btn" v-if="nodeDetails.os === 'WINDOWS'" @click="downloadHandle">{{$t('setting.agent.downloadPackage')}}</span>
                <i class="bk-icon icon-refresh" @click="refresh"></i>
            </div>
        </div>
        <div class="sub-view-port" v-show="showContent">
            <ul class="base-prototype-list">
                <li v-for="(entry, index) in basePrototypeList" :key="index">
                    <div class="info-title">{{ entry.name }}：</div>
                    <div class="info-value" :title="entry.value">{{ entry.value }}</div>
                </li>
            </ul>
            <node-overview-chart></node-overview-chart>
            <node-detail-tab></node-detail-tab>
        </div>
    </div>
</template>

<script>
    import { mapState } from 'vuex'
    import { bus } from '@/utils/bus'
    import { copyText } from '@/utils/util'
    import { setting } from '@/http'
    import nodeDetailTab from '@/components/setting/agent-detail/node-detail-tab'
    import nodeOverviewChart from '@/components/setting/agent-detail/node-overview-chart'

    export default {
        components: {
            nodeDetailTab,
            nodeOverviewChart
        },
        data () {
            return {
                nodeDetails: {},
                showContent: false,
                basePrototypeList: [
                    { id: 'hostname', name: this.$t('setting.agent.hostName'), value: '' },
                    { id: 'ip', name: 'IP', value: '' },
                    { id: 'ncpus', name: 'CPU', value: '' },
                    { id: 'memTotal', name: this.$t('setting.agent.memory'), value: '' },
                    { id: 'createdUser', name: 'Owner', value: '' },
                    { id: 'osName', name: 'OS', value: '' }
                ],
                loading: {
                    isLoading: false,
                    title: ''
                }
            }
        },
        computed: {
            ...mapState(['projectId']),
            nodeHashId () {
                return this.$route.params.agentId
            },
            agentLink () {
                return this.nodeDetails.os === 'WINDOWS' ? this.nodeDetails.agentUrl : this.nodeDetails.agentScript
            },
            navList () {
                return [
                    { link: { name: 'agentPools' }, title: this.$t('setting.agent.agentPools') },
                    { link: { name: 'agentList' }, title: this.$t('setting.agent.agentList') },
                    { link: '', title: this.nodeDetails.displayName }
                ]
            }
        },
        watch: {
            projectId: async function (val) {
                this.$router.push({ name: 'envList' })
            },
            nodeDetails (val) {
                this.basePrototypeList.forEach(item => {
                    item.value = val[item.id]
                })
            }
        },
        async mounted () {
            this.requestNodeDetail()
        },
        methods: {
            toNodeList () {
                this.$router.push({ name: 'nodeList' })
            },
            async requestNodeDetail () {
                this.loading.isLoading = true

                setting.requestNodeDetail(this.projectId, this.nodeHashId).then((res) => {
                    this.nodeDetails = res
                }).catch((err) => {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }).finally(() => {
                    this.loading.isLoading = false
                    this.showContent = true
                })
            },
            copyHandle () {
                if (copyText(this.agentLink)) {
                    this.$bkMessage({
                        theme: 'success',
                        message: 'Copy successfully'
                    })
                }
            },
            downloadHandle () {
                window.location.href = this.nodeDetails.agentUrl
            },
            refresh () {
                this.requestNodeDetail()
                bus.$emit('refreshEnv')
                bus.$emit('refreshBuild')
                bus.$emit('refreshAction')
                bus.$emit('refreshCharts')
            }
        }
    }
</script>

<style lang="postcss">
    @import '@/css/conf';
    .node-detail-wrapper {
        height: 100%;
        overflow-y: auto;
        .info-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            width: 100%;
            height: 60px;
            padding: 0 20px;
            border-bottom: 1px solid #dde4eb;
            background-color: #fff;
            box-shadow: 0px 2px 5px 0px rgb(51 60 72 / 3%);
            .node-handle {
                color: $primaryColor;
                span {
                    margin-left: 10px;
                    cursor: pointer;
                }
            }
            .icon-refresh {
                margin-left: 10px;
                cursor: pointer;
            }
        }
        .base-prototype-list {
            display: flex;
            width: 100%;
            border: 1px solid #DDE4EB;
            background-color: #FFF;
            li {
                flex: 1;
                height: 76px;
                padding: 16px 20px;
                overflow: hidden;
                border-right: 1px solid #DDE4EB;
                color: $fontWeightColor;
                &:last-child {
                    border-right: none;
                }
                .info-title {
                    font-weight: bold;
                }
                .info-value {
                    width: 100%;
                    margin-top: 8px;
                    white-space: nowrap;
                    overflow: hidden;
                    text-overflow: ellipsis;
                }
            }
        }
    }
</style>
