<template>
    <div class="node-create-wrapper">
        <header-process :process-head="processHeadConf"></header-process>
        <section class="create-node-contain">
            <node-model v-if="!processHeadConf.current"></node-model>
            <node-affirmance v-else></node-affirmance>
        </section>
    </div>
</template>

<script>
    import { mapGetters } from 'vuex'
    import headerProcess from '@/components/devops/headerProcess'
    import nodeModel from '@/components/devops/node-model'
    import nodeAffirmance from '@/components/devops/node-affirmance'

    export default {
        components: {
            headerProcess,
            nodeModel,
            nodeAffirmance
        },
        data () {
            return {
                showContent: false,
                loading: {
                    isLoading: false,
                    title: this.$t('environment.loadingTitle')
                }
            }
        },
        computed: {
            ...mapGetters('environment', [
                'getProcessHeadConf'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            processHeadConf () {
                return this.getProcessHeadConf()
            }
        },
        watch: {
            projectId: async function (val) {
                this.$router.push({ name: 'envList' })
            }
        },
        async created () {
            this.requestVmQuta() // 获取配额数量
        },
        methods: {
            toNodeList () {
                this.$router.push({ name: 'nodeList' })
            },
            async requestVmQuta () {
                try {
                    const res = await this.$store.dispatch('environment/requestVmQuta', {
                        projectId: this.projectId
                    })

                    this.$store.commit('environment/updateDevCloudQuta', { res })
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            }
        }
    }
</script>

<style lang="scss">
    @import './../scss/conf';
    .node-create-wrapper {
        height: 100%;
        overflow: hidden;
        .node-create-header {
            display: flex;
            justify-content: space-between;
            padding: 18px 20px;
            width: 100%;
            height: 60px;
            border-bottom: 1px solid #DDE4EB;
            background-color: #fff;
            box-shadow:0px 2px 5px 0px rgba(51,60,72,0.03);
            .header-text {
                font-size: 16px;
            }
            .icon-arrows-left {
                margin-right: 4px;
                cursor: pointer;
                color: $iconPrimaryColor;
                font-size: 16px;
                font-weight: 600;
            }
        }
        .create-node-contain {
            padding: 20px;
            height: calc(100% - 60px);
            overflow: auto;
        }
    }
</style>
