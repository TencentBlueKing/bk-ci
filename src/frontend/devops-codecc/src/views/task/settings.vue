<template>
    <div>
        <bk-tab type="border-card" :active.sync="active" :before-toggle="beforeToggle">
            <bk-tab-panel
                v-for="(panel, index) in panels"
                v-bind="panel"
                render-directive="if"
                :key="index"
            >
                <router-view></router-view>
            </bk-tab-panel>
        </bk-tab>
    </div>
</template>

<script>
    import { mapState } from 'vuex'
    export default {
        data () {
            return {
                panels: [
                    { name: 'basic', label: this.$t('nav.基础信息') },
                    { name: 'tools', label: this.$t('nav.工具管理') },
                    { name: 'code', label: this.$t('nav.代码库配置') },
                    { name: 'trigger', label: this.$t('nav.扫描触发') },
                    { name: 'ignore', label: this.$t('nav.路径屏蔽') },
                    { name: 'manage', label: this.$t('nav.任务管理') }
                ],
                active: this.$route.name.split('-').pop()
            }
        },
        beforeRouteUpdate (to, from, next) {
            this.active = to.name.split('-').pop()
            next()
        },
        computed: {
            ...mapState('task', {
                taskDetail: 'detail'
            })
        },
        methods: {
            beforeToggle (name) {
                if (this.taskDetail.createFrom.indexOf('pipeline') !== -1) {
                    if (name === 'tools' || name === 'code' || name === 'manage' || name === 'trigger') {
                        const titleMap = {
                            'tools': this.$t('st.此代码检查任务为流水线创建，工具需前往相应流水线添加。'),
                            'code': this.$t('st.此代码检查任务为流水线创建，代码库需前往相应流水线配置。'),
                            'manage': this.$t('st.此代码检查任务为流水线创建，任务需前往相应流水线管理。'),
                            'trigger': this.$t('st.此代码检查任务为流水线创建，扫描触发需前往相应流水线配置。')
                        }
                        let that = this
                        this.$bkInfo({
                            title: this.$t('st.温馨提示'),
                            subTitle: titleMap[name],
                            maskClose: true,
                            confirmFn (name) {
                                window.open(`${window.DEVOPS_SITE_URL}/console/pipeline/${that.taskDetail.projectId}/${that.taskDetail.pipelineId}/edit#codecc`, '_blank')
                            }
                        })
                        return false
                    } else {
                        this.$router.push({ name: `task-settings-${name}` })
                    }
                } else {
                    this.$router.push({ name: `task-settings-${name}` })
                }
            }
        }
    }
</script>

<style>
    /* hack tab内容区域高度 */
    .bk-tab-section {
        min-height: calc(100% - 43px);
        background: #fff;
    }

</style>

<style lang="postcss" scoped>
</style>
