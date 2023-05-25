<template>
    <article class="pipeline-content"
        v-bkloading="{
            isLoading: pageLoading
        }"
    >
        <pipeline-header>
            <span class="default-subheader-icon" slot="logo">
                <logo size="32" name="pipeline" />
            </span>
            <bk-breadcrumb slot="title" separator-class="devops-icon icon-angle-right">
                <bk-breadcrumb-item
                    class="pipeline-breadcrumb-item"
                    :to="pipelineListRoute"
                >
                    {{$t('pipeline')}}
                </bk-breadcrumb-item>
                <!-- <bk-breadcrumb-item
                    v-if="$route.meta.breadcrumbs"
                    class="pipeline-breadcrumb-item"
                    v-for="(item, index) in $route.meta.breadcrumbs"
                    :key="index"
                    :to="item"
                >
                    {{item}}
                </bk-breadcrumb-item>
                <bk-breadcrumb-item
                >
                    {{$t($route.name)}}
                </bk-breadcrumb-item> -->
            </bk-breadcrumb>

            <bk-dropdown-menu slot="right" class="default-link-list" trigger="click">
                <div slot="dropdown-trigger">
                    <span
                        class="pipeline-dropdown-trigger"
                        :class="{ 'active': dropTitle !== 'more' }"
                    >
                        {{ $t(dropTitle) }}
                        <i :class="['devops-icon icon-angle-down', {
                            'icon-flip': toggleIsMore
                        }]"></i>
                    </span>
                </div>
                <ul class="bk-dropdown-list" slot="dropdown-content">
                    <li
                        v-for="menu in dropdownMenus"
                        :class="{
                            'active': menu.routeName === routeName
                        }"
                        :key="menu.label"
                        @click="go(menu.routeName)"
                    >
                        <a href="javascript:;">
                            {{$t(menu.label)}}
                        </a>
                    </li>

                </ul>

            </bk-dropdown-menu>
        </pipeline-header>
        <router-view></router-view>
    </article>

</template>

<script>
    import { mapState } from 'vuex'
    import pipelineHeader from '@/components/devops/pipeline-header'
    import Logo from '@/components/Logo'
    import { getCacheViewId } from '@/utils/util'

    export default {
        components: {
            'pipeline-header': pipelineHeader,
            Logo
        },
        data () {
            return {
                isLoading: false,
                toggleIsMore: false
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
            dropTitle () {
                return this.dropdownMenus.find(menu => menu.routeName === this.routeName)?.label ?? 'more'
            },
            pipelineListRoute () {
                return {
                    name: 'PipelineManageList',
                    params: {
                        viewId: this.viewId,
                        ...this.$route.params
                    }
                }
            },
            viewId () {
                return getCacheViewId(this.$route.params.projectId)
            },
            dropdownMenus () {
                return [
                    {
                        label: 'labelManage',
                        routeName: 'pipelinesGroup'
                    },
                    {
                        label: 'templateManage',
                        routeName: 'pipelinesTemplate'
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
            }

        },
        created () {
            if (!this.$route.params.viewId) {
                this.$router.replace({
                    ...this.$route,
                    params: {
                        ...this.$route.params,
                        viewId: this.viewId
                    }
                })
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
</style>
