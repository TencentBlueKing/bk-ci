<template>
    <div
        :class="['template-detail-entry', {
            'show-template-var': activeChild.showVar
        }]"
    >
        <header>
            <HisotryHeader />
            <ext-menu
                type="template"
                :data="pipelineInfo"
                :config="templateActions"
            />
        </header>
        <main class="template-detail-entry-main">
            <section class="template-detail-overview-section">
                <aside class="template-detail-entry-aside">
                    <header class="template-detail-entry-aside-header">
                        {{ $t('template.pipelineTemplate') }}
                    </header>
                    <ul
                        v-for="item in asideNav"
                        :key="item.title"
                    >
                        <li class="nav-item-title">
                            {{ item.title }}
                            <span
                                class="nav-item-link"
                                v-if="item.link"
                                @click="item.link.handler"
                            >
                                <logo
                                    :name="item.link.icon"
                                    size="16"
                                ></logo>
                                {{ item.link.title }}
                            </span>
                        </li>
                        <ul class="nav-child-list">
                            <li
                                @click="switchType(child)"
                                v-for="child in item.children"
                                :key="child.name"
                                :class="[
                                    'nav-child-title',
                                    {
                                        active: child.active,
                                        'nav-child-disabled': child.disabled
                                    }
                                ]"
                            >
                                {{ child.title }}
                            </li>
                        </ul>
                    </ul>
                </aside>

                <section class="template-detail-entry-center">
                    <component
                        v-if="pipelineHistoryViewable"
                        :is="activeChild.component"
                        v-bind="activeChild.props"
                    />
                </section>
            </section>
            <show-variable
                v-if="activeChild.showVar && pipeline"
                :editable="false"
                :pipeline-model="true"
                :pipeline="pipeline"
                :is-direct-show-version="isDirectShowVersion"
            />
        </main>
        <CopyTemplateDialog
            :copy-temp="copyTemp"
            @confirm="copyConfirmHandler"
            @cancel="copyCancelHandler"
        />
    </div>
</template>

<script setup>
    import { computed } from 'vue'
    import Logo from '@/components/Logo'
    import CopyTemplateDialog from '@/components/Template/CopyTemplateDialog.vue'
    import {
        ChangeLog,
        PipelineConfig
    } from '@/components/PipelineDetailTabs'
    import { AuthorityTab, ShowVariable } from '@/components/PipelineEditTabs/'
    import HisotryHeader from '@/components/PipelineHeader/HistoryHeader'
    import Instance from '@/views/Template/InstanceList'
    import ExtMenu from './List/extMenu'
    import UseInstance from '@/hook/useInstance'
    import useTemplateActions from '@/hook/useTemplateActions'
    import {
        RESOURCE_ACTION,
        TEMPLATE_RESOURCE_ACTION
    } from '@/utils/permission'

    const {
        copyTemp,
        copyTemplate,
        exportTemplate,
        deleteTemplate,
        copyConfirmHandler,
        copyCancelHandler
    } = useTemplateActions()
    const { proxy, t } = UseInstance()

    const pipeline = computed(() => proxy.$store?.state?.atom?.pipeline)
    const pipelineInfo = computed(() => proxy.$store?.state?.atom?.pipelineInfo)

    const pipelineHistoryViewable = computed(() => proxy.$store?.getters['atom/pipelineHistoryViewable'])
    const isReleaseVersion = computed(() => proxy.$store?.getters['atom/isReleaseVersion'])
    const isBranchVersion = computed(() => proxy.$store?.getters['atom/isBranchVersion'])

    const projectId = computed(() => proxy.$route.params.projectId)
    const activeMenuItem = computed(() => proxy.$route.params.type || 'instanceList')
    const activeChild = computed(() => getNavComponent(activeMenuItem.value))
    const canEdit = computed(() => pipelineInfo.value.canEdit)
    const canDelete = computed(() => pipelineInfo.value?.canDelete)
    const templateId = computed(() => pipelineInfo.value?.id)
    const isDirectShowVersion = computed(() => proxy.$route.params.isDirectShowVersion || false)
    const asideNav = computed(() => [
        {
            title: t('executeInfo'),
            children: [
                {
                    title: t('template.instanceList'),
                    name: 'instanceList'
                }
            ].map((child) => ({
                ...child,
                active: activeMenuItem.value === child.name,
                disabled: !isReleaseVersion.value && !isBranchVersion.value
            }))
        },
        {
            title: t('template.templateConfig'),
            children: [
                {
                    title: t('pipelineModel'),
                    name: 'pipeline'
                },
                {
                    title: t('triggerConf'),
                    name: 'trigger'
                },
                {
                    title: t('notifyConf'),
                    name: 'notice'
                },
                {
                    title: t('baseConf'),
                    name: 'setting'
                }
            ].map((child) => ({
                ...child,
                active: activeMenuItem.value === child.name
            }))
        },
        {
            title: t('more'),
            children: [
                {
                    title: t('authSetting'),
                    name: 'permission'
                },
                {
                    title: t('operationLog'),
                    name: 'changeLog'
                }
            ].map((child) => ({
                ...child,
                active: activeMenuItem.value === child.name,
                disabled: !isReleaseVersion.value
            }))
        }
    ])
    const templateActions = computed(() => [
        {
            text: t('template.export'), // 导出
            handler: () => exportTemplate(pipelineInfo.value),
            hasPermission: !canEdit.value,
            disablePermissionApi: true,
            isShow: true,
            permissionData: {
                projectId: projectId.value,
                resourceType: 'pipeline_template',
                resourceCode: templateId,
                action: TEMPLATE_RESOURCE_ACTION.EDIT
            }
        },
        {
            text: t('copy'), // 复制
            handler: () => copyTemplate(pipelineInfo.value),
            hasPermission: !canEdit.value,
            disablePermissionApi: true,
            isShow: true,
            permissionData: {
                projectId: projectId.value,
                resourceType: 'pipeline_template',
                resourceCode: projectId.value,
                action: RESOURCE_ACTION.CREATE
            }
        },
        {
            text: t('delete'),
            handler: () => deleteTemplate(pipelineInfo.value, goTemplateManageList),
            hasPermission: !canDelete.value,
            disablePermissionApi: true,
            isShow: true,
            permissionData: {
                projectId: projectId.value,
                resourceType: 'pipeline_template',
                resourceCode: templateId,
                action: TEMPLATE_RESOURCE_ACTION.EDIT
            }
        }
    ])

    function getNavComponent (type) {
        console.log(pipelineInfo.value, 'pipelineInfo')
        switch (type) {
            case 'pipeline':
            case 'trigger':
            case 'notice':
            case 'setting':
                return {
                    component: PipelineConfig,
                    showVar: type === 'pipeline'
                }
            case 'permission':
                return {
                    component: AuthorityTab
                }
            case 'changeLog':
                return {
                    component: ChangeLog
                }
            default:
                return {
                    component: Instance
                }
        }
    }
    function switchType (child) {
        if (child.disabled) return
        proxy.$router.push({
            name: 'TemplateManageList'
        })
    }
    function goTemplateManageList () {
        proxy.$router.push({
            name: 'TemplateManageList'
        })
    }
</script>

<style lang="scss">
@import "./../../scss/conf";

.template-detail-entry.biz-content {
    height: 100%;
    overflow: hidden;
    display: flex;
    box-shadow: 0 2px 2px 0 #00000026;
    flex-direction: column;
    background: #f6f7fa;
    >header {
        height: 48px;
        background: white;
        display: flex;
        align-items: center;
        padding: 0 24px;
        box-shadow: 0 2px 5px 0 #333c4808;
        border-bottom: 1px solid #eaebf0;

        .template-version-area {
            margin-left: 18px;
            display: flex;
            align-items: center;
            grid-gap: 8px;
            &-pac-tag {
                display: flex;
            }
        }
        .template-operate-area {
            margin-left: auto;
            justify-self: flex-end;
        }
    }
    .template-detail-entry-aside {
        width: 220px;
        flex-shrink: 0;
        background: #fafbfd;
        border-right: 1px solid #dcdee5;
        padding: 4px 0;
        overflow: auto;
        overflow: overlay;
        &-header {
            height: 50px;
            display: flex;
            align-items: center;
            padding: 0 24px;
            border-left: 3px solid #C4C6CC;
            border-bottom: 1px solid #EAEBF0;
            margin-top: -4px;
            background-color: white;
            font-weight: 700;
            font-size: 14px;
            color: #313238;
        }
        .disable-nav-child-item-tooltips {
            display: none;
        }

        .nav-item-title {
            padding: 0 16px 0 22px;
            color: #c4c6cc;
            display: flex;
            align-items: center;
            justify-content: space-between;
            font-size: 12px;
            .nav-item-link {
                color: #3a84ff;
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
            margin-bottom: 4px;
        }

        .nav-child-title {
            position: relative;
            padding-left: 32px;
            cursor: pointer;
            font-size: 14px;
            &.nav-child-disabled {
                color: #c4c6cc;
                cursor: not-allowed;
            }
            &:hover:not(.nav-child-disabled),
            &.active:not(.nav-child-disabled) {
                background: #e1ecff;
                color: #3A84FF;
                &:after {
                content: "";
                position: absolute;
                width: 2.75px;
                height: 40px;
                background: #3a84ff;
                right: 0;
                }
            }
        }
    }
    .template-detail-entry-main {
        overflow: hidden;
        display: flex;
        flex: 1;
        position: relative;
        .template-detail-overview-section {
            display: flex;
            margin: 24px 24px 0 24px;
            box-shadow: 0 2px 2px 0 #00000026;
            flex: 1;
        }
        .template-detail-entry-center {
            background: #fff;
            flex: 1;
            overflow: hidden;
        }
    }
}
</style>
