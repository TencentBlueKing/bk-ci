<template>
    <div class="biz-container pipeline-subpages" v-bkloading="{ isLoading }">
        <div class="biz-side-bar">
            <side-bar
                :nav="nav"
                :side-menu-list="sideMenuList"
                :sub-system-name="'pipelines'">
            </side-bar>
        </div>
        <template v-if="!isLoading">
            <router-view
                v-if="hasViewPermission"
                class="biz-content"
                :is-enabled-permission="isEnabledPermission"
            >
            </router-view>
            <empty-tips
                v-else
                :title="$t('template.accessDeny.title')"
                :desc="$t('template.accessDeny.desc')"
                show-lock
            >
                <bk-button
                    theme="primary"
                    @click="handleApply"
                >
                    {{ $t('template.accessDeny.apply') }}
                </bk-button>
            </empty-tips>
        </template>
    </div>
</template>

<script>
    import { handleTemplateNoPermission, TEMPLATE_RESOURCE_ACTION } from '@/utils/permission'
    import sideBar from '@/components/devops/side-nav'
    import emptyTips from '@/components/template/empty-tips'

    export default {
        components: {
            'side-bar': sideBar,
            emptyTips
        },

        data () {
            return {
                isLoading: true,
                isEnabledPermission: false,
                hasViewPermission: true,
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
            this.$updateTabTitle?.(this.$t('documentTitlePipeline'))
            const { projectId, templateId } = this.$route.params
            this.$store.dispatch('requestProjectDetail', { projectId })
            this.$store.dispatch('pipelines/enableTemplatePermissionManage', projectId).then((res) => {
                if (res.data) {
                    this.isEnabledPermission = res.data
                    this.sideMenuList[0].list.push({
                        id: 'templatePermission',
                        name: this.$t('template.permissionSetting'),
                        icon: 'permission'
                    })
                }
            })
            this.$store.dispatch('pipelines/getTemplateHasViewPermission', {
                projectId,
                templateId
            }).then(async res => {
                this.hasViewPermission = res.data
                if (!this.hasViewPermission) await this.handleApply()
            }).finally(() => {
                this.isLoading = false
            })
        },
        methods: {
            handleApply () {
                const { projectId, templateId } = this.$route.params
                handleTemplateNoPermission({
                    projectId,
                    resourceCode: templateId,
                    action: TEMPLATE_RESOURCE_ACTION.VIEW
                })
            }
        }
    }
</script>

<style lang="scss">
    .pipeline-subpages {
        min-height: 100%;
    }
    .biz-content {
        width: 100%;
        height: 100%;
        .group-table {
            padding: 20px;
        }
    }
</style>
