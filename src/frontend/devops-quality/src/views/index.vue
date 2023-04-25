<template>
    <div class="biz-container quality-level-wrapper">
        <div class="biz-side-bar">
            <side-bar
                :nav="sideMenuNav"
                :side-menu-list="sideMenuList"
                :sub-system-name="'qualityLevel'">
            </side-bar>
        </div>
        <router-view style="width: 100%"></router-view>
    </div>
</template>

<script>
    import sideBar from '@/components/devops/side-nav'

    export default {
        components: {
            'side-bar': sideBar
        },
        data () {
            return {
                sideMenuNav: {
                    icon: 'quality',
                    title: this.$t('quality.quality')
                },
                sideMenuList: [
                    {
                        list: [
                            {
                                id: 'qualityOverview',
                                name: this.$t('quality.总览'),
                                icon: 'devops-icon icon-overview',
                                showChildren: false
                            },
                            {
                                id: 'ruleList',
                                name: this.$t('quality.红线规则'),
                                icon: 'devops-icon icon-rule',
                                showChildren: false,
                                children: [
                                    {
                                        id: 'createRule',
                                        name: this.$t('quality.创建红线规则'),
                                        icon: 'devops-icon icon-rule'
                                    },
                                    {
                                        id: 'editRule',
                                        name: this.$t('quality.编辑规则'),
                                        icon: 'devops-icon icon-rule'
                                    }
                                ]
                            },
                            {
                                id: 'interceptHistory',
                                name: this.$t('quality.红线记录'),
                                icon: 'devops-icon icon-intercept-history',
                                showChildren: false
                            },
                            {
                                id: 'metadataList',
                                name: this.$t('quality.指标列表'),
                                icon: 'devops-icon icon-rule',
                                showChildren: false,
                                children: [
                                    {
                                        id: 'createMeta',
                                        name: this.$t('quality.创建脚本任务指标'),
                                        icon: 'devops-icon icon-rule'
                                    },
                                    {
                                        id: 'editMeta',
                                        name: this.$t('quality.编辑脚本任务指标'),
                                        icon: 'devops-icon icon-rule'
                                    }
                                ]
                            },
                            {
                                id: 'noticeGroup',
                                name: this.$t('quality.通知组'),
                                icon: 'devops-icon icon-notification',
                                showChildren: false
                            }
                        ]
                    }
                ]
            }
        }
    }
</script>

<style lang="scss">
    @import '@/scss/conf.scss';

    .quality-level-wrapper {
        min-width: 1200px;
        height: 100%;
    }
    .sub-view-port {
        height: calc(100% - 60px);
        overflow: auto;
    }
</style>
