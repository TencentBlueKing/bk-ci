<template>
    <div class="template-detail-entry">
        <header>
            <HistoryHeader class="template-detail-entry-history-header" />
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
                :can-edit-param="false"
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
        <TemplateUpgradeStrategyDialog />
    </div>
</template>

<script setup>
    import Logo from '@/components/Logo'
    import {
        ChangeLog,
        PipelineConfig
    } from '@/components/PipelineDetailTabs'
    import { AuthorityTab, ShowVariable } from '@/components/PipelineEditTabs/'
    import HistoryHeader from '@/components/PipelineHeader/HistoryHeader.vue'
    import CopyTemplateDialog from '@/components/Template/CopyTemplateDialog.vue'
    import StoreTemplateRelated from '@/components/Template/StoreTemplateRelated'
    import UseInstance from '@/hook/useInstance'
    import useTemplateActions from '@/hook/useTemplateActions'
    import {
        TEMPLATE_RESOURCE_ACTION
    } from '@/utils/permission'
    import { getTemplateCacheViewId } from '@/utils/util'
    import Instance from '@/views/Template/InstanceList'
    import { computed, onMounted } from 'vue'
    import { RESOURCE_TYPE } from '../../utils/permission'
    import ExtMenu from './List/extMenu'

    const {
        copyTemp,
        copyTemplate,
        exportTemplate,
        toRelativeStore,
        convertToCustom,
        deleteTemplate,
        copyConfirmHandler,
        copyCancelHandler
    } = useTemplateActions()
    const { proxy, t } = UseInstance()

    const pipeline = computed(() => proxy.$store?.state?.atom?.pipeline)
    const pipelineInfo = computed(() => proxy.$store?.state?.atom?.pipelineInfo)
    const storeStatus = computed(() => proxy.$store?.state?.atom?.storeStatus)

    const pipelineHistoryViewable = computed(() => proxy.$store?.getters['atom/pipelineHistoryViewable'])
    const isReleaseVersion = computed(() => proxy.$store?.getters['atom/isReleaseVersion'])
    const isBranchVersion = computed(() => proxy.$store?.getters['atom/isBranchVersion'])

    const projectId = computed(() => proxy.$route.params.projectId)
    const activeMenuItem = computed(() => proxy.$route.params.type || 'instanceList')
    const activeChild = computed(() => getNavComponent(activeMenuItem.value))
    const canEdit = computed(() => pipelineInfo.value?.permissions?.canEdit ?? false)
    const canDelete = computed(() => pipelineInfo.value?.permissions?.canDelete ?? false)
    const templateId = computed(() => pipelineInfo.value?.id)
    const isDirectShowVersion = computed(() => proxy.$route.params.isDirectShowVersion || false)
    const isFromStoreTemplate = computed(() => !!pipelineInfo.value?.pipelineTemplateMarketRelatedInfo)
    const asideNav = computed(() => [
        {
            title: t('template.instanceManage'),
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
        ...(isFromStoreTemplate.value
            ? [{
                title: t('store'),
                children: [
                    {
                        title: t('template.relatedSetting'),
                        name: 'related'
                    }
                ].map((child) => ({
                    ...child,
                    active: activeMenuItem.value === child.name
                }))
            }]
            : []),
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
    const templateActions = computed(() => {
        const editPerm = {
            hasPermission: canEdit.value,
            disablePermissionApi: true,
            permissionData: {
                projectId: projectId.value,
                resourceType: RESOURCE_TYPE.TEMPLATE,
                resourceCode: templateId.value,
                action: TEMPLATE_RESOURCE_ACTION.EDIT
            }
        }
        return [
            {
                text: t('template.export'), // 导出
                handler: () => exportTemplate(pipelineInfo.value),
                isShow: true,
                ...editPerm
            },
            {
                text: t('copy'), // 复制
                handler: () => copyTemplate({
                    ...pipelineInfo.value,
                    ...pipelineInfo.value?.permissions
                }),
                isShow: true,
                ...editPerm,
            },
            {
                text: t(`template.${pipelineInfo.value.storeFlag ? 'upgradeOnStore' : 'shelfStore'}`),
                handler: () => toRelativeStore({
                    ...pipelineInfo.value,
                    ...pipelineInfo.value?.permissions
                }, storeStatus.value),
                disable: (pipelineInfo.value.storeFlag && !pipelineInfo.value.publishFlag) || pipelineInfo.value.latestVersionStatus === 'COMMITTING',
                isShow: pipelineInfo.value.mode === 'CUSTOMIZE',
                ...editPerm
            },
            {
                text: t('template.convertToCustom'),
                handler: () => convertToCustom({
                    ...pipelineInfo.value,
                    ...pipelineInfo.value?.permissions
                }, goTemplateManageList),
                isShow: pipelineInfo.value.mode === 'CONSTRAINT',
                ...editPerm
            },
            {
                text: t('delete'),
                handler: () => deleteTemplate({
                    ...pipelineInfo.value,
                    ...pipelineInfo.value?.permissions
                }, goTemplateManageList),
                isShow: true,
                hasPermission: canDelete.value,
                disablePermissionApi: true,
                permissionData: {
                    projectId: projectId.value,
                    resourceType: RESOURCE_TYPE.TEMPLATE,
                    resourceCode: templateId.value,
                    action: TEMPLATE_RESOURCE_ACTION.DELETE
                }
            }
        ]
    })

    function getNavComponent (type) {
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
            case 'related':
                return {
                    component: StoreTemplateRelated
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
            params: {
                ...proxy.$route.params,
                type: child.name
            }
        })
    }
    function goTemplateManageList () {
        proxy.$router.push({
            name: 'TemplateManageList',
            params: {
                templateViewId: getTemplateCacheViewId()
            }
        })
    }

    onMounted(() => {
        if (pipelineInfo.value) {
            proxy.$updateTabTitle?.(`${pipelineInfo.value.name || ''} | ${proxy.$t('template.template')}`)
        }
    })

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
        padding-right: 24px;
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
        .template-detail-entry-history-header {
            padding-right: 10px;
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
            overflow: hidden;
        }
        .template-detail-entry-center {
            background: #fff;
            flex: 1;
            overflow: hidden;
        }
    }
}
</style>
