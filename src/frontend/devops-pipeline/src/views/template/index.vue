<template>
    <div class="biz-container pipeline-subpages">
        <div class="biz-side-bar">
            <side-bar
                :nav="nav"
                :side-menu-list="sideMenuList"
                :sub-system-name="'pipelines'">
            </side-bar>
        </div>
        <router-view class="biz-content"></router-view>
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
                sideMenuList: [
                    {
                        list: [
                            {
                                id: 'templateEdit',
                                name: this.$t('edit'),
                                icon: 'icon-edit'
                            },
                            {
                                id: 'templateSetting',
                                name: this.$t('template.settings'),
                                icon: 'icon-cog'
                            },
                            {
                                id: 'templateInstance',
                                name: this.$t('template.instanceManage'),
                                icon: 'icon-list',
                                isSelected: false,
                                showChildren: false,
                                children: [
                                    {
                                        id: 'createInstance',
                                        name: this.$t('template.addInstance'),
                                        icon: 'icon-list'
                                    }
                                ]
                            }
                        ]
                    }
                ],
                nav: {
                    backUrl: 'pipelinesTemplate',
                    title: this.$t('templateManage'),
                    icon: ''
                }
            }
        },
        created () {
            this.$store.dispatch('requestProjectDetail', { projectId: this.$route.params.projectId })
        }
    }
</script>

<style lang="scss">
    .pipeline-subpages {
        min-height: 100%;
        .bk-exception {
            position: absolute;
        }
    }
</style>
