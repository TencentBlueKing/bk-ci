<template>
    <div :class="{ 'mini': active === 'tools' }">
        <bk-tab type="border-card" class="cc-settings" :active.sync="active" :before-toggle="beforeToggle">
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
                    { name: 'code', label: this.$t('基础信息') },
                    { name: 'checkerset', label: this.$t('规则集配置') },
                    // { name: 'tools', label: this.$t('工具管理') },
                    // { name: 'report', label: this.$t('通知报告') },
                    { name: 'trigger', label: this.$t('扫描触发') },
                    { name: 'ignore', label: this.$t('路径屏蔽') },
                    // { name: 'authority', label: this.$t('人员权限') },
                    { name: 'manage', label: this.$t('任务管理') }
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
                    if (name === 'tools' || name === 'manage') {
                        const titleMap = {
                            'tools': this.$t('此代码检查任务为流水线创建，工具需前往相应流水线添加。'),
                            'manage': this.$t('此代码检查任务为流水线创建，任务需前往相应流水线管理。')
                        }
                        const that = this
                        this.$bkInfo({
                            title: this.$t('温馨提示'),
                            subTitle: titleMap[name],
                            maskClose: true,
                            confirmFn (name) {
                                window.open(`${window.DEVOPS_SITE_URL}/console/pipeline/${that.taskDetail.projectId}/${that.taskDetail.pipelineId}/edit#${that.taskDetail.atomCode}`, '_blank')
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
    .main-content.mini {
        max-width: calc(100% - 350px);
        min-width: 1085px;
        >>>.params-side {
            top: 57px;
            max-height: calc(100vh - 152px);
            toolparams {
                max-height: calc(100vh - 262px);
            }
        }
    }
    >>> .cc-settings>.bk-tab-header>.bk-tab-label-wrapper>.bk-tab-label-list>.bk-tab-label-item {
        &:nth-of-type(1), &:nth-of-type(2) {
            >.bk-tab-label::after {
                content: "*";
                color: #ff5656;
                position: relative;
                margin: 2px -7px 0 2px;
                display: inline-block;
                vertical-align: middle;
            }
        }
    }
</style>
