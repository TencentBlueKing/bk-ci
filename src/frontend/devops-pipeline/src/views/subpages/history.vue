<template>
    <div class="pipeline-detail-entry">
        <aside class="pipeline-detail-entry-aside">
            <ul v-for="(item, index) in asideNav" :key="index">
                <li class="nav-item-title">
                    {{ item.title }}
                    <span class="nav-item-link" v-if="item.link" @click="item.link.handler">
                        <logo :name="item.link.icon" size="16"></logo>
                        {{ item.link.title }}
                    </span>
                </li>
                <ul class="nav-child-list" v-for="(child, cIndex) in item.children" :key="cIndex">
                    <li @click="switchType(child)" :class="['nav-child-title', {
                        active: child.active
                    }]">{{ child.title }}</li>
                </ul>
            </ul>
        </aside>
        <main class="pipeline-detail-entry-main">
            <component
                :is="activeChild.component"
                v-bind="activeChild.bindData"
            />
        </main>
    </div>
</template>

<script>
    import { mapActions } from 'vuex'
    import Logo from '@/components/Logo'
    import {
        BuildHistoryTab,
        TriggerEvent,
        PipelineConfig,
        PermissionConfig,
        VersionHistory,
        ChangeLog
    } from '@/components/PipelineDetailTabs'
    import Artifactory from '@/components/Outputs'
    export default {
        components: {
            BuildHistoryTab,
            TriggerEvent,
            Artifactory,
            PipelineConfig,
            PermissionConfig,
            VersionHistory,
            ChangeLog,
            Logo
        },

        props: {
            execHandler: Function
        },
        computed: {
            activeMenuItem () {
                return this.$route.params.type || 'history'
            },
            activeChild () {
                return this.getNavComponent(this.activeMenuItem)
            },
            asideNav () {
                return [{
                    title: this.$t('执行信息'),
                    children: [{
                        title: this.$t('执行历史'),
                        name: 'history'
                    }, {
                        title: this.$t('触发事件'),
                        name: 'triggerEvent'
                    }, {
                        title: this.$t('产出物'),
                        name: 'artifactory'
                    }].map(child => ({
                        ...child,
                        active: this.activeMenuItem === child.name
                    }))
                }, {
                    title: this.$t('流水线配置'),
                    link: {
                        handler: () => {
                            this.$router.push({
                                name: 'pipelinesEdit'
                            })
                        },
                        icon: 'edit-conf',
                        title: this.$t('编辑流水线')
                    },
                    children: [{
                        title: this.$t('流水线编排'),
                        name: 'pipeline'
                    }, {
                        title: this.$t('触发器配置'),
                        name: 'trigger'
                    }, {
                        title: this.$t('通知配置'),
                        name: 'notice'
                    }, {
                        title: this.$t('基础配置'),
                        name: 'setting'
                    }].map(child => ({
                        ...child,
                        active: this.activeMenuItem === child.name
                    }))
                }, {
                    title: this.$t('更多'),
                    children: [{
                        title: this.$t('权限设置'),
                        name: 'permission'
                    }, {
                        title: this.$t('版本历史'),
                        name: 'versionHistory'
                    }, {
                        title: this.$t('操作日志'),
                        name: 'changeLog'
                    }].map(child => ({
                        ...child,
                        active: this.activeMenuItem === child.name
                    }))
                }]
            }
        },
        beforeDestroy () {
            console.log('destory history')
            this.resetHistoryFilterCondition()
        },
        methods: {
            ...mapActions('pipelines', [
                'resetHistoryFilterCondition'
            ]),
            getNavComponent (type) {
                switch (type) {
                    case 'history':
                        return {
                            component: 'BuildHistoryTab'
                        }
                    case 'triggerEvent':
                        return {
                            component: 'TriggerEvent'
                        }
                    case 'artifactory':
                        return {
                            component: 'Artifactory'
                        }
                    case 'pipeline':
                    case 'trigger':
                    case 'notice':
                    case 'setting':
                        return {
                            component: 'PipelineConfig'
                        }
                    case 'permission':
                        return {
                            component: 'PermissionConfig'
                        }
                    case 'versionHistory':
                        return {
                            component: 'VersionHistory'
                        }
                    case 'changeLog':
                        return {
                            component: 'ChangeLog'
                        }
                }
            },
            switchType (child) {
                this.$router.push({
                    name: 'pipelinesHistory',
                    params: {
                        ...this.$route.params,
                        type: child.name
                    }
                })
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';

    .pipeline-detail-entry.biz-content {
        margin: 24px;
        height: 100%;
        overflow: hidden;
        display: grid;
        grid-auto-flow: column;
        grid-template-columns: 220px 1fr;
        box-shadow: 0 2px 2px 0 #00000026;
        background: #f6f7fa;
        .pipeline-detail-entry-aside {
            background: #FAFBFD;
            border-right: 1px solid #DCDEE5;
            padding: 4px 0;
            .nav-item-title {
                padding: 0 16px 0 22px;
                color: #C4C6CC;
                display: flex;
                align-items: center;
                justify-content: space-between;
                font-size: 12px;
                .nav-item-link {
                    color: #3A84FF;
                    cursor: pointer;
                    display: grid;
                    align-items: center;
                    grid-gap: 4px;
                    grid-auto-flow: column;
                    &:hover {
                        color: #699df4;
                    }
                }
            }
            .nav-item-title,
            .nav-child-title {
                line-height: 40px;
            }
            .nav-child-list {
                margin-bottom: 8px;
            }
            .nav-child-title {
                position: relative;
                padding-left: 32px;
                cursor: pointer;
                font-size: 14px;
                &:hover,
                &.active {
                    background: #E1ECFF;
                    &:after {
                        content: '';
                        position: absolute;
                        width: 2.75px;
                        height: 40px;
                        background: #3A84FF;
                        right: 0;
                    }
                }
            }
        }
        .pipeline-detail-entry-main {
            background: #fff;
            overflow: hidden;
        }
    }
</style>
