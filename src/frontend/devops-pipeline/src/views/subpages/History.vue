<template>
    <div class="pipeline-detail-entry">
        <aside class="pipeline-detail-entry-aside">
            <ul v-for="item in asideNav" :key="item.name">
                <li class="nav-item-title">
                    {{ item.title }}
                    <span class="nav-item-link" v-if="item.link" @click="item.link.handler">
                        <logo :name="item.link.icon" size="16"></logo>
                        {{ item.link.title }}
                    </span>
                </li>
                <ul class="nav-child-list" v-for="(child, cIndex) in item.children" :key="cIndex">
                    <li
                        @click="switchType(child)"
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
        <main class="pipeline-detail-entry-main">
            <component :is="activeChild.component" />
        </main>
    </div>
</template>

<script>
    import { mapActions, mapState } from 'vuex'
    import Logo from '@/components/Logo'
    import {
        BuildHistoryTab,
        TriggerEvent,
        PipelineConfig,
        ChangeLog
    } from '@/components/PipelineDetailTabs'
    import { AuthorityTab } from '@/components/PipelineEditTabs/'

    export default {
        components: {
            BuildHistoryTab,
            TriggerEvent,
            PipelineConfig,
            AuthorityTab,
            ChangeLog,
            Logo
        },
        computed: {
            ...mapState('atom', ['pipelineInfo']),
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
                                title: this.$t('pipelinesHistory'),
                                name: 'history'
                            },
                            {
                                title: this.$t('triggerEvent'),
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
            }
        },
        beforeDestroy () {
            this.resetHistoryFilterCondition()
        },
        methods: {
            ...mapActions('pipelines', ['resetHistoryFilterCondition']),
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
                            component: 'PipelineConfig'
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
                            component: 'BuildHistoryTab'
                        }
                }
            },
            switchType (child) {
                this.$router.push({
                    name: 'pipelinesHistory',
                    params: {
                        ...this.$route.params,
                        type: child.name
                    }
                })
            }
        }
    }
</script>

<style lang="scss">
@import "./../../scss/conf";

.pipeline-detail-entry.biz-content {
  margin: 24px;
  height: 100%;
  overflow: hidden;
  display: grid;
  grid-auto-flow: column;
  grid-template-columns: 220px 1fr;
  box-shadow: 0 2px 2px 0 #00000026;
  background: #f6f7fa;
  .pipeline-detail-entry-aside {
    background: #fafbfd;
    border-right: 1px solid #dcdee5;
    padding: 4px 0;
    height: 100%;
    overflow: auto;
    overflow: overlay;

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
      &:hover,
      &.active {
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
  }
}
</style>
