<template>
    <div
        :class="['pipeline-detail-entry', {
            'show-pipeline-var': activeChild.showVar
        }]"
    >
        <aside class="pipeline-detail-entry-aside">
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
                        v-bk-tooltips="child.disableTooltip"
                    >
                        {{ child.title }}
                    </li>
                </ul>
            </ul>
            <div
                v-for="i in [1,2,3,4]"
                :key="i"
                ref="disableToolTips"
                class="disable-nav-child-item-tooltips"
            >
                {{ $t('switchToReleaseVersion') }}
                <span
                    v-if="isReleasePipeline"
                    @click="switchToReleaseVersion"
                    class="text-link"
                >{{ $t('switch') }}</span>
            </div>
        </aside>

        <main class="pipeline-detail-entry-main">
            <component
                :is="activeChild.component"
                v-bind="activeChild.props"
            />
        </main>
        <show-variable
            v-if="activeChild.showVar && pipeline"
            :editable="false"
            :pipeline-model="true"
            :pipeline="pipeline"
            :is-direct-show-version="isDirectShowVersion"
        />
    </div>
</template>

<script>
    import Logo from '@/components/Logo'
    import {
        BuildHistoryTab,
        ChangeLog,
        PipelineConfig,
        TriggerEvent,
        DelegationPermission
    } from '@/components/PipelineDetailTabs'
    import { ShowVariable } from '@/components/PipelineEditTabs/'
    import { mapActions, mapGetters, mapState } from 'vuex'

    export default {
        components: {
            BuildHistoryTab,
            TriggerEvent,
            PipelineConfig,
            ChangeLog,
            Logo,
            ShowVariable,
            DelegationPermission
        },
        data () {
            return {
                shouldRetainArchiveFlag: false
            }
        },
        computed: {
            ...mapState('atom', ['pipelineInfo', 'pipeline', 'pipelineSetting', 'activePipelineVersion', 'switchingVersion']),
            ...mapGetters('atom', ['isActiveDraftVersion', 'isReleaseVersion', 'isReleasePipeline', 'isBranchVersion']),
            activeMenuItem () {
                return this.$route.params.type || 'history'
            },
            activeChild () {
                return this.getNavComponent(this.activeMenuItem)
            },
            archiveFlag () {
                return this.$route.query.archiveFlag
            },
            asideNav () {
                return [
                    {
                        title: this.$t('executeInfo'),
                        children: [
                            {
                                title: this.$t('pipelinesHistory'),
                                disableTooltip: {
                                    content: this.$refs.disableToolTips?.[0],
                                    disabled: this.isReleaseVersion || this.isBranchVersion,
                                    delay: [300, 0]
                                },
                                name: 'history'
                            },
                            {
                                title: this.$t('triggerEvent'),
                                disableTooltip: {
                                    content: this.$refs.disableToolTips?.[1],
                                    disabled: this.isReleaseVersion || this.isBranchVersion,
                                    delay: [300, 0]
                                },
                                name: 'triggerEvent'
                            }
                            // , {
                            //     title: this.$t('details.outputs'),
                            //     name: 'artifactory'
                            // }
                        ].filter((child) => child.name !== 'triggerEvent' || !this.archiveFlag).map((child) => ({
                            ...child,
                            disabled: !this.isReleaseVersion && !this.isBranchVersion,
                            active: this.activeMenuItem === child.name
                        }))
                    },
                    {
                        title: this.$t('pipelineConf'),
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
                            disableTooltip: {
                                disabled: true
                            },
                            active: this.activeMenuItem === child.name
                        }))
                    },
                    {
                        title: this.$t('more'),
                        children: [
                            {
                                title: this.$t('delegationPermission'),
                                name: 'delegation'
                            },
                            {
                                title: this.$t('operationLog'),
                                disableTooltip: {
                                    content: this.$refs.disableToolTips?.[3],
                                    disabled: this.isReleaseVersion,
                                    delay: [300, 0]
                                },
                                name: 'changeLog'
                            }
                        ].filter(child => !this.archiveFlag || child.name === 'changeLog').map((child) => ({
                            ...child,
                            disabled: !this.isReleaseVersion,
                            active: this.activeMenuItem === child.name
                        }))
                    }
                ]
            },
            isDirectShowVersion () {
                return this.$route.params.isDirectShowVersion || false
            }
        },
        beforeDestroy () {
            this.resetHistoryFilterCondition({ retainArchiveFlag: this.shouldRetainArchiveFlag })
            this.selectPipelineVersion(null)
            this.resetAtomModalMap()
        },
        beforeRouteLeave (to, from, next) {
            // 判断目标路由是否需要保留 archiveFlag
            const routesToKeepArchiveFlag = ['pipelinesDetail', 'draftDebugRecord']
            this.shouldRetainArchiveFlag = routesToKeepArchiveFlag.includes(to.name) && to.query.archiveFlag !== undefined
            next()
        },
        methods: {
            ...mapActions('pipelines', ['resetHistoryFilterCondition']),
            ...mapActions('atom', [
                'selectPipelineVersion',
                'resetAtomModalMap'
            ]),
            getNavComponent (type) {
                switch (type) {
                    case 'triggerEvent':
                        return {
                            component: 'TriggerEvent'
                        }
                    // case 'artifactory':
                    //     return {
                    //         component: 'Artifactory'
                    //     }
                    case 'pipeline':
                    case 'trigger':
                    case 'notice':
                    case 'setting':
                        return {
                            component: 'PipelineConfig',
                            showVar: type === 'pipeline'
                        }
                    case 'versionHistory':
                        return {
                            component: 'VersionHistory'
                        }
                    case 'changeLog':
                        return {
                            component: 'ChangeLog'
                        }
                    case 'delegation':
                        return {
                            component: 'DelegationPermission'
                        }
                    default:
                        return {
                            component: 'BuildHistoryTab',
                            props: {
                                isDebug: this.isActiveDraftVersion,
                                pipelineName: this.pipelineSetting?.pipelineName
                            }
                        }
                }
            },
            switchType (child) {
                if (child.disabled) return
                this.$router.push({
                    name: 'pipelinesHistory',
                    params: {
                        ...this.$route.params,
                        type: child.name
                    },
                    query: this.$route.query
                })
            },
            switchToReleaseVersion () {
                this.$router.push({
                    params: {
                        ...this.$route.params,
                        version: this.pipelineInfo?.releaseVersion
                    },
                    query: this.$route.query
                })
            }
        }
    }
</script>

<style lang="scss">
@import "./../../scss/conf";

.pipeline-detail-entry.biz-content {
  height: 100%;
  overflow: hidden;
  display: flex;
  box-shadow: 0 2px 2px 0 #00000026;
  background: #f6f7fa;
  .pipeline-detail-entry-aside {
    width: 220px;
    flex-shrink: 0;
    background: #fafbfd;
    border-right: 1px solid #dcdee5;
    padding: 4px 0;
    overflow: auto;
    overflow: overlay;
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
  .pipeline-detail-entry-main {
    background: #fff;
    overflow: hidden;
    flex: 1;
  }
}
</style>
