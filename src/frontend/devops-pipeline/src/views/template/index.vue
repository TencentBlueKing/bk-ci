<template>
    <div class="biz-container pipeline-subpages">
        <div class="biz-side-bar">
            <side-bar
                :nav="nav"
                :side-menu-list="sideMenuList"
                :sub-system-name="'pipelines'">
            </side-bar>
        </div>
        <router-view class="biz-content" :is-enabled-permission="isEnabledPermission"></router-view>
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
                isEnabledPermission: false,
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
            const projectId = this.$route.params.projectId
            this.$store.dispatch('requestProjectDetail', { projectId })
            this.$store.dispatch('pipelines/enableTemplatePermissionManage', projectId).then((res) => {
                if (res.data) {
                    this.isEnabledPermission = res.data
                    this.sideMenuList[0].list.push({
                        id: 'templatePermission',
                        name: this.$t('template.permissionSetting'),
                        icon: 'icon-cog'
                    })
                }
            })
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
