<template>
    <section class="agent-pool-card">
        <header class="card-header">
            <h5 class="header-info">
                <span class="info-title">{{ pool.name }}</span>
                <span class="info-num" v-if="editable">{{$t('setting.agent.agent')}}：{{ pool.nodeCount }}</span>
            </h5>

            <opt-menu v-if="editable">
                <li @click="toPage('agentList')">{{$t('setting.agent.selfAgents')}}</li>
                <li @click="toPage('poolSettings')">{{$t('settings')}}</li>
                <li @click="showDelete = true">{{$t('setting.agent.deletePool')}}</li>
            </opt-menu>
        </header>

        <ul class="card-useages" v-if="!editable">
            <li class="useage-item" v-for="usage in cpuUsages" :key="usage.name">
                <span class="item-header">
                    <span class="header-title">{{ usage.name }}</span>
                    <span class="header-val">{{ usage.showVal }}%</span>
                </span>
                <bk-progress :theme="getTheme(usage.val)" :percent="usage.val" :show-text="false"></bk-progress>
            </li>
        </ul>
        <div v-else style="height: 150px; padding: 7px 24px">
            {{pool.desc || $t('noDesc')}}
        </div>
        <div v-if="editable" class="operate-btns">
            <bk-button @click="addAgent" class="card-button">{{$t('setting.agent.addAgent')}}</bk-button>
            <bk-button @click="importNewNode" class="card-button">{{$t('setting.agent.importAgent')}}</bk-button>
        </div>
        <bk-dialog v-model="showDelete"
            theme="danger"
            :mask-close="false"
            :loading="isDeleteing"
            @confirm="deletePool"
            title="Delete">
            Are you sure to delete【{{pool.name}}】?
        </bk-dialog>

        <node-select :node-select-conf="nodeSelectConf"
            :row-list="importNodeList"
            :select-handlerc-conf="selectHandlercConf"
            :confirm-fn="confirmFn"
            :toggle-all-select="toggleAllSelect"
            :loading="nodeDialogLoading"
            :cancel-fn="cancelFn"
            :query="query">
        </node-select>
    </section>
</template>

<script>
    import optMenu from '@/components/opt-menu'
    import nodeSelect from './node-select-dialog'
    import nodeSelectMixin from './node-select-mixin.js'
    import { setting } from '@/http'
    import { mapState } from 'vuex'

    export default {
        components: {
            optMenu,
            nodeSelect
        },

        mixins: [nodeSelectMixin],

        props: {
            editable: {
                type: Boolean,
                default: true
            },
            pool: Object
        },

        data () {
            return {
                showDelete: false,
                isDeleteing: false
            }
        },

        computed: {
            ...mapState(['projectId']),

            envHashId () {
                return this.pool.envHashId
            },

            cpuUsages () {
                return [
                    { name: this.$t('setting.agent.cpuUsage'), showVal: this.pool.averageCpuLoad, val: this.pool.averageCpuLoad / 100 },
                    { name: this.$t('setting.agent.memoryUsage'), showVal: this.pool.averageMemLoad, val: this.pool.averageMemLoad / 100 },
                    { name: this.$t('setting.agent.diskUsage'), showVal: this.pool.averageDiskLoad, val: this.pool.averageDiskLoad / 100 }
                ]
            }
        },

        created () {
            if (this.pool.nodeCount > 0) {
                setting.getNodeList(this.projectId, this.pool.envHashId).then((res) => {
                    this.nodeList = res
                })
            }
        },

        methods: {
            deletePool () {
                this.isDeleteing = true
                setting.deleteEnvironment(this.projectId, this.pool.envHashId).then(() => {
                    this.showDelete = false
                    this.$emit('refresh')
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isDeleteing = false
                })
            },

            addAgent () {
                this.$router.push({
                    name: 'addAgent',
                    params: {
                        poolId: this.pool.envHashId,
                        poolName: this.pool.name
                    }
                })
            },

            getTheme (val) {
                let theme = 'success'
                if (val > 0.5) {
                    theme = 'warning'
                }
                if (val > 0.8) {
                    theme = 'danger'
                }
                return theme
            },

            toPage (routeName) {
                this.$router.push({ name: routeName, params: { poolId: this.pool.envHashId, poolName: this.pool.name } })
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .agent-pool-card {
        width: 350px;
        height: 310px;
        background: #FFFFFF;
        border: 1px solid #dde4eb;
        border-radius: 2px;
    }
    .card-header {
        height: 75px;
        padding: 17px 10px 17px 24px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        border-bottom: 1px solid #dde4eb;
        .info-title {
            font-size: 16px;
            color: #333C48;
            line-height: 21px;
            margin-bottom: 4px;
            display: block;
        }
        .info-num {
            font-size: 12px;
            line-height:16px;
            color: #c3cdd7;
        }
    }
    .card-useages {
        padding: 7px 24px 24px;
        .useage-item {
            margin-top: 22px;
            .item-header {
                font-size: 12px;
                line-height: 14px;
                height: 14px;
                color: #7b7d8a;
                display: inline-block;
                width: 100%;
                margin-bottom: 7px;
            }
            .header-val {
                float: right;
                color: #979ba5;
            }
        }
    }
    .operate-btns {
        display: flex;
        justify-content: space-between;
        .card-button {
            margin: 0 24px;
            width: 160px;
            font-size: 12px;
        }
    }
    
</style>
