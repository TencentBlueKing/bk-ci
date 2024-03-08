<template>
    <div :class="['pipeline-detail-entry', {
        'show-pipeline-var': activeChild.showVar
    }]" v-bkloading="{ isLoading }">
        <aside class="pipeline-detail-entry-aside">
            <ul v-for="item in asideNav" :key="item.title">
                <li class="nav-item-title">
                    {{ item.title }}
                    <span class="nav-item-link" v-if="item.link" @click="item.link.handler">
                        <logo :name="item.link.icon" size="16"></logo>
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
            <div ref="tool" id="disable-nav-child-item-tooltips">
                {{$t('switchToReleaseVersionCheck')}}
                <span @click="switchToReleaseVersion" class="text-link">{{ $t('newlist.view') }}</span>
            </div>
        </aside>

        <main class="pipeline-detail-entry-main">
            <component :is="activeChild.component" v-bind="activeChild.props" />
        </main>
        <show-variable v-if="activeChild.showVar && pipeline" :editable="false" :pipeline="pipeline" />

    </div>
</template>

<script>
    import { mapActions, mapState, mapGetters } from 'vuex'
    import Logo from '@/components/Logo'
    import {
        BuildHistoryTab,
        TriggerEvent,
        PipelineConfig,
        ChangeLog
    } from '@/components/PipelineDetailTabs'
    import { ShowVariable, AuthorityTab } from '@/components/PipelineEditTabs/'

    export default {
        components: {
            BuildHistoryTab,
            TriggerEvent,
            PipelineConfig,
            AuthorityTab,
            ChangeLog,
            Logo,
            ShowVariable
        },
        computed: {
            ...mapState('atom', ['pipelineInfo', 'pipeline', 'activePipelineVersion']),
            ...mapGetters('atom', ['isActiveDraftVersion', 'isOutdatedVersion']),
            activeMenuItem () {
                return this.$route.params.type || 'history'
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
                                title: this.$t(this.isActiveDraftVersion ? 'draftExecRecords' : 'pipelinesHistory'),
                                disabled: this.isOutdatedVersion,
                                disableTooltip: {
                                    content: this.$refs?.tool,
                                    interactive: true,
                                    disabled: !this.isOutdatedVersion
                                },
                                name: 'history'
                            },
                            {
                                title: this.$t('triggerEvent'),
                                disabled: this.isOutdatedVersion,
                                disableTooltip: {
                                    disabled: true
                                },
                                name: 'triggerEvent'
                            }
                            // , {
                            //     title: this.$t('details.outputs'),
                            //     name: 'artifactory'
                            // }
                        ].map((child) => ({
                            ...child,
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
                                title: this.$t('authSetting'),
                                name: 'permission'
                            },
                            {
                                title: this.$t('operationLog'),
                                name: 'changeLog'
                            }
                        ].map((child) => ({
                            ...child,
                            disableTooltip: {
                                disabled: true
                            },
                            active: this.activeMenuItem === child.name
                        }))
                    }
                ]
            },
            isLoading () {
                return !this.activePipelineVersion?.version
            }
        },
        beforeDestroy () {
            this.resetHistoryFilterCondition()
        },
        methods: {
            ...mapActions('pipelines', ['resetHistoryFilterCondition']),
            ...mapActions('atom', ['selectPipelineVersion']),
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
                            showVar: true
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
                            component: 'BuildHistoryTab',
                            props: {
                                isDebug: this.isActiveDraftVersion
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
                    }
                })
            },
            switchToReleaseVersion () {
                this.selectPipelineVersion({
                    version: this.pipelineInfo.releaseVersion,
                    versionName: this.pipelineInfo.releaseVersionName,
                    isDraft: false,
                    displayName: this.pipelineInfo.releaseVersionName
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
    #disable-nav-child-item-tooltips {
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
