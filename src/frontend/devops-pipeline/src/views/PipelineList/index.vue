<template>
    <article
        class="pipeline-content"
        v-bkloading="{
            isLoading: pageLoading
        }"
    >
        <pipeline-header>
            <template v-if="routeName === 'PipelineListAuth'">
                <logo
                    size="24"
                    name="pipeline"
                    slot="logo"
                />
                <bk-breadcrumb
                    slot="title"
                    separator-class="devops-icon icon-angle-right"
                >
                    <bk-breadcrumb-item
                        class="pipeline-breadcrumb-item"
                        :to="pipelineListRoute"
                    >
                        {{ $t('pipeline') }}
                    </bk-breadcrumb-item>
                    <template>
                        <bk-breadcrumb-item
                            class="pipeline-breadcrumb-item"
                        >
                            {{ $t('pipelineGroup') }}
                        </bk-breadcrumb-item>
                        <bk-breadcrumb-item
                            class="pipeline-breadcrumb-item"
                        >
                            {{ groupName }}
                        </bk-breadcrumb-item>
                    </template>
                </bk-breadcrumb>
            </template>

            <div
                slot="middle"
                v-else
            >
                <bk-tab
                    :active.sync="activeTab"
                    type="unborder-card"
                    :label-height="60"
                    ext-cls="middle-panel"
                    @tab-change="go"
                >
                    <bk-tab-panel
                        v-for="(panel, index) in modelMenu"
                        v-bind="panel"
                        :key="index"
                    >
                    </bk-tab-panel>
                </bk-tab>
            </div>

            <bk-dropdown-menu
                slot="right"
                class="default-link-list"
                trigger="click"
            >
                <div slot="dropdown-trigger">
                    <span
                        class="pipeline-dropdown-trigger"
                        :class="{ 'active': dropTitle !== 'more' }"
                    >
                        {{ $t(dropTitle) }}
                        <i
                            :class="['devops-icon icon-angle-down', {
                                'icon-flip': toggleIsMore
                            }]"
                        ></i>
                    </span>
                </div>
                <ul
                    class="bk-dropdown-list"
                    slot="dropdown-content"
                >
                    <li
                        v-for="menu in dropdownMenus"
                        :class="{
                            'active': menu.routeName === routeName
                        }"
                        :key="menu.label"
                        @click="go(menu.routeName)"
                    >
                        <a href="javascript:;">
                            {{ $t(menu.label) }}
                        </a>
                    </li>
                </ul>
            </bk-dropdown-menu>
        </pipeline-header>
        <router-view />
    </article>
</template>

<script>
    import Logo from '@/components/Logo'
    import pipelineHeader from '@/components/devops/pipeline-header'
    import { mapState } from 'vuex'

    export default {
        components: {
            'pipeline-header': pipelineHeader,
            Logo
        },
        data () {
            return {
                isLoading: false,
                toggleIsMore: false,
                activeTab: 'PipelineManageList'
            }
        },
        computed: {
            ...mapState('pipelines', [
                'currentViewList',
                'currentViewId',
                'showViewManage',
                'pageLoading'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            routeName () {
                return this.$route.name
            },
            groupName () {
                return this.$route.params.groupName
            },
            dropTitle () {
                return this.dropdownMenus.find(menu => menu.routeName === this.routeName)?.label ?? 'more'
            },
            pipelineListRoute () {
                return {
                    name: 'PipelineManageList',
                    params: this.$route.params,
                    query: this.$route.query
                }
            },
            dropdownMenus () {
                return [
                    {
                        label: 'labelManage',
                        routeName: 'pipelinesGroup'
                    },
                    {
                        label: 'pluginManage',
                        routeName: 'atomManage'
                    },
                    {
                        label: 'operatorAudit',
                        routeName: 'pipelinesAudit'
                    }
                ]
            },
            modelMenu () {
                return [
                    {
                        label: this.$t('pipeline'),
                        name: 'PipelineManageList'
                    },
                    {
                        label: this.$t('templateName'),
                        name: 'TemplateManageList'
                    }
                ]
            }

        },
        watch: {
            '$route.name': {
                handler (name) {
                    const curModel = this.modelMenu.find(i => i.name === name)
                    if (curModel) {
                        this.activeTab = curModel.name
                        this.go(curModel.name)
                    }
                },
                immediate: true
            }
        },

        methods: {
            go (name) {
                this.$router.push({ name })
            }
        }
    }
</script>
<style lang="scss">
    @import './../../scss/conf';

    .pipeline-content {
        display: flex;
        flex-direction: column;
        height: 100%;
    }

    .pipeline-breadcrumb-item {
        display: flex;
        align-items: center;
    }

    .default-link-list {
        display: flex;
        .pipeline-dropdown-trigger {
            font-size: 14px;
            cursor: pointer;
            .devops-icon {
                display: inline-block;
                transition: all ease 0.2s;
                margin-left: 4px;
                font-size: 12px;
                &.icon-flip {
                    transform: rotate(180deg);
                }
            }
            &.active {
                color: $primaryColor;
            }
        }
    }
    .middle-panel {
        .bk-tab-section {
            display: none;
        }
        .bk-tab-header {
            background-image: none !important;
        }
    }
</style>
