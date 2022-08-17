<template>
    <article class="agent-pools" @scroll.passive="mainScroll" v-bkloading="{ isLoading: isLoadingSysPools }">
        <h3 :class="{ 'pool-title': true, 'fix-top': scrollTop > 60 && scrollTop < 370 }">{{$t('setting.agent.defaultPool')}}</h3>
        <section class="agent-pools-container">
            <agent-pool-card class="agent-pool" :editable="false" v-for="pool in systemPools" :key="pool.envHashId" :pool="pool"></agent-pool-card>
        </section>

        <h3 :class="{ 'pool-title': true, 'fix-top': scrollTop > 370 }">
            {{$t('setting.agent.selfPool')}}
            <bk-button @click="showAddPool" class="add-pool" theme="primary" size="small" v-if="thirdPools.length">{{$t('setting.agent.addSelfPool')}}</bk-button>
        </h3>
        <section class="agent-pools-container" v-bkloading="{ isLoading: isLoadingThirdPools }">
            <agent-pool-card class="agent-pool" @refresh="getThirdPool" v-for="pool in thirdPools" :key="pool.envHashId" :pool="pool"></agent-pool-card>
            <section v-if="thirdPools.length <= 0" class="table-empty">
                <h3>{{$t('setting.agent.emptySelfPoolTitle')}}</h3>
                <h5>{{$t('setting.agent.emptySelfPoolTips')}}</h5>
                <bk-button theme="primary" @click="showAddPool">{{$t('setting.agent.addSelfPool')}}</bk-button>
            </section>
        </section>
        <add-pool :show.sync="isShowAddPool" @refresh="getThirdPool"></add-pool>
    </article>
</template>

<script>
    import { setting } from '@/http'
    import { mapState } from 'vuex'
    import agentPoolCard from '@/components/setting/agent-pool-card'
    import addPool from '@/components/setting/add-pool'

    export default {
        components: {
            agentPoolCard,
            addPool
        },

        data () {
            return {
                isShowAddPool: false,
                isLoadingThirdPools: false,
                isLoadingSysPools: false,
                scrollTop: 0,
                systemPools: [],
                thirdPools: []
            }
        },

        computed: {
            ...mapState(['projectId'])
        },

        created () {
            this.getSystemPool()
            this.getThirdPool()
        },

        methods: {
            getThirdPool () {
                this.isLoadingThirdPools = true
                setting.getEnvironmentList(this.projectId).then((thirdPool) => {
                    this.thirdPools = thirdPool || []
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isLoadingThirdPools = false
                })
            },

            getSystemPool () {
                this.isLoadingSysPools = true
                setting.getSystemPoolDetail().then((systemPool) => {
                    this.systemPools = []
                    const clusterLoad = systemPool.clusterLoad
                    for (const name in clusterLoad) {
                        const element = clusterLoad[name]
                        this.systemPools.push({ name, ...element })
                    }
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isLoadingSysPools = false
                })
            },

            showAddPool () {
                this.isShowAddPool = true
            },

            mainScroll (event) {
                this.scrollTop = event.target.scrollTop
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .agent-pools {
        overflow-y: auto;
        height: calc(100vh - 61px);
        padding: 0 16px 16px;
        margin: 0;
        width: 100%;
    }
    .pool-title {
        margin-top: 20px;
        color: #313328;
        font-size: 16px;
        line-height: 21px;
        width: 100%;
        .add-pool {
            margin-left: 30px;
        }
        &.fix-top {
            position: fixed;
            top: 60px;
            z-index: 2;
            background: #fff;
            line-height: 48px;
            height: 48px;
            box-shadow: 0 2px 5px 0 rgba(51,60,72,0.03);
            margin: 0 -24px;
            padding: 0 24px;
        }
    }
    .agent-pools-container {
        &:after {
            content: '';
            display: table;
            clear: both;
        }
        .agent-pool {
            float: left;
            margin-top: 20px;
            margin-left: 20px;
        }
    }
</style>
