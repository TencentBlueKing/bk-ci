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
                                name: '编辑',
                                icon: 'icon-edit'
                            },
                            {
                                id: 'templateSetting',
                                name: '设置',
                                icon: 'icon-cog'
                            },
                            {
                                id: 'templateInstance',
                                name: '实例管理',
                                icon: 'icon-list',
                                isSelected: false,
                                showChildren: false,
                                children: [
                                    {
                                        id: 'createInstance',
                                        name: '新增实例',
                                        icon: 'icon-list'
                                    }
                                ]
                            }
                        ]
                    }
                ],
                nav: {
                    backUrl: 'pipelinesTemplate',
                    title: '模板管理',
                    icon: ''
                }
            }
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
