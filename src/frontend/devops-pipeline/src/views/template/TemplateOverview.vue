<template>
    <div
        :class="['template-detail-entry', {
            'show-template-var': activeChild.showVar
        }]"
    >
        <header>
            <TemplateBreadCrumb
                :template-name="pipelineInfo?.name"
                :is-loading="!pipelineInfo"
            />
            <p class="template-version-area">
                <pac-tag
                    v-if="pacEnabled"
                    class="template-version-area-pac-tag"
                />
                <VersionSelector
                    :value="currentVersion"
                    ref="versionSelectorInstance"
                    @change="handleVersionChange"
                    @showAllVersion="showVersionSideSlider"
                    :include-draft="false"
                    refresh-list-on-expand
                    is-template
                />
            </p>

            <p class="template-operate-area">
                <bk-button
                    @click="goEditTemplate"
                    size="small"
                >
                    {{ $t('编辑模板') }}
                </bk-button>
                <bk-button
                    @click="switchToReleaseVersion"
                    theme="primary"
                    size="small"
                >
                    {{ $t('template.instantiate') }}
                </bk-button>
            </p>
        </header>
        <main class="template-detail-entry-main">
            <section class="template-detail-overview-section">
                <aside class="template-detail-entry-aside">
                    <header class="template-detail-entry-aside-header">
                        流水线模板
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
                                        active: child.active
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
        <VersionHistorySideSlider
            :show-version-sideslider="showVersionSideslider"
            is-template
            @close="closeVersionSideSlider"
        />
    </div>
</template>

<script>
    import Logo from '@/components/Logo'
    import PacTag from '@/components/PacTag'
    import {
        ChangeLog,
        PipelineConfig
    } from '@/components/PipelineDetailTabs'
    import VersionHistorySideSlider from '@/components/PipelineDetailTabs/VersionHistorySideSlider'
    import VersionSelector from '@/components/PipelineDetailTabs/VersionSelector'
    import { AuthorityTab, ShowVariable } from '@/components/PipelineEditTabs/'
    import TemplateBreadCrumb from '@/components/template/TemplateBreadCrumb'
    import Instance from '@/views/template/instance'
    import { mapActions, mapGetters, mapState } from 'vuex'

    export default {
        components: {
            Instance,
            VersionSelector,
            PipelineConfig,
            AuthorityTab,
            ChangeLog,
            Logo,
            PacTag,
            TemplateBreadCrumb,
            ShowVariable,
            VersionHistorySideSlider
        },
        data () {
            return {
                showVersionSideslider: false
            }
        },
        computed: {
            ...mapState('atom', ['pipeline', 'pipelineSetting', 'pipelineInfo']),
            ...mapGetters('atom', ['pacEnabled', 'yamlInfo', 'pipelineHistoryViewable']),
            projectId () {
                return this.$route.params.projectId
            },
            templateId () {
                return this.$route.params.templateId
            },
            currentVersion () {
                return parseInt(this.$route.params.version)
            },
            activeMenuItem () {
                return this.$route.params.type || 'instanceList'
            },
            activeChild () {
                return this.getNavComponent(this.activeMenuItem)
            },

            asideNav () {
                return [
                    {
                        title: this.$t('executeInfo'),
                        children: [
                            {
                                title: this.$t('实例列表'),
                                name: 'instanceList'
                            }
                        ].map((child) => ({
                            ...child,
                            active: this.activeMenuItem === child.name
                        }))
                    },
                    {
                        title: this.$t('模板配置'),
                        children: [
                            {
                                title: this.$t('pipelineModel'),
                                name: 'pipeline'
                            },
                            {
                                title: this.$t('triggerConf'),
                                name: 'trigger'
                            },
                            {
                                title: this.$t('notifyConf'),
                                name: 'notice'
                            },
                            {
                                title: this.$t('baseConf'),
                                name: 'setting'
                            }
                        ].map((child) => ({
                            ...child,
                            active: this.activeMenuItem === child.name
                        }))
                    },
                    {
                        title: this.$t('more'),
                        children: [
                            {
                                title: this.$t('authSetting'),
                                name: 'permission'
                            },
                            {
                                title: this.$t('operationLog'),
                                name: 'changeLog'
                            }
                        ].map((child) => ({
                            ...child,
                            active: this.activeMenuItem === child.name
                        }))
                    }
                ]
            },
            isDirectShowVersion () {
                return this.$route.params.isDirectShowVersion || false
            }
        },
        created () {
            if (!this.pipelineHistoryViewable) {
                this.$router.push({
                    name: 'templateEdit',
                    params: {
                        ...this.$route.params,
                        type: 'pipeline',
                        version: this.pipelineInfo?.version
                    }
                })
            } else {
                this.requestPipeline({
                    projectId: this.projectId,
                    templateId: this.templateId,
                    version: this.pipelineInfo?.version
                })
                this.selectPipelineVersion(this.pipelineInfo?.version)
            }
        },

        methods: {
            ...mapActions('atom', [
                'selectPipelineVersion',
                'requestPipeline',
                'setPipeline',
                'setPipelineWithoutTrigger',
                'resetAtomModalMap',
                'setShowVariable'
            ]),
            showVersionSideSlider () {
                this.setShowVariable(false)
                this.$refs?.versionSelectorInstance?.close?.()
                this.showVersionSideslider = true
            },
            closeVersionSideSlider () {
                this.showVersionSideslider = false
            },
            handleVersionChange () {
                // TODO:
            },
            getNavComponent (type) {
                switch (type) {
                    case 'pipeline':
                    case 'trigger':
                    case 'notice':
                    case 'setting':
                        return {
                            component: 'PipelineConfig',
                            showVar: type === 'pipeline'
                        }
                    case 'permission':
                        return {
                            component: 'AuthorityTab'
                        }
                    case 'versionHistory':
                        return {
                            component: 'VersionHistory'
                        }
                    case 'changeLog':
                        return {
                            component: 'ChangeLog'
                        }
                    default:
                        return {
                            component: 'Instance'
                        }
                }
            },
            switchType (child) {
                if (child.disabled) return
                this.$router.push({
                    name: 'TemplateOverview',
                    params: {
                        ...this.$route.params,
                        type: child.name
                    }
                })
            },
            switchToReleaseVersion () {
                this.$router.push({
                    params: {
                        ...this.$route.params,
                        version: this.pipelineInfo?.releaseVersion
                    }
                })
            },
            goEditTemplate () {
                this.$router.push({
                    name: 'templateEdit',
                    params: this.$route.params
                })
            }
        }
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
