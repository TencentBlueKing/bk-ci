<template>
    <article class="pipeline-content"
        v-bkloading="{
            isLoading: pageLoading
        }">
        <pipeline-header
            :title="header.title"
            :links="header.links">
            <span class="default-subheader-icon"
                slot="logo">
                <logo size="32" name="pipeline"></logo>
            </span>

            <ul class="header-list" slot="center">
                <li v-for="(obj, key) of currentViewList" :key="key">
                    <a @click="changePageType(obj.id)"
                        :class="(obj.id === currentViewId && routeName === 'pipelinesList') ? 'active-item' : ''"
                        :title=" obj.name">{{ obj.name }}</a>
                </li>
                <li>
                    <div class="manage-view-btn" v-show="currentViewId">
                        <i class="bk-icon icon-plus" @click="toggleShowViewManage()"></i>
                        <view-manage v-if="showViewManage"></view-manage>
                    </div>
                </li>
            </ul>

            <div class="default-link-list" slot="right">
                <div class="dropdown-trigger" @click.stop="toggleIsMoreHandler">
                    <span class="more-handler" id="moreHeaderHandler" :class="{ 'selectde-title': dropdownTitle !== '更多' }">{{ dropdownTitle }}
                        <i :class="['bk-icon icon-angle-down', { 'icon-flip': toggleIsMore }, { 'selectde-title': dropdownTitle !== '更多' }]"
                            id="toggleHeaderIcon"></i>
                    </span>
                </div>
                <div class="dropdown-list" v-if="toggleIsMore">
                    <ul class="list-wrapper">
                        <li>
                            <a href="javascript:;" class="text-link" id="toggleLabels"
                                :class="{ 'selected-item': routeName === 'pipelinesGroup' }"
                                @click="routerToManage('pipelinesGroup')">标签管理</a>
                        </li>
                        <li>
                            <a href="javascript:;" class="text-link" id="toggleViews"
                                :class="{ 'selected-item': routeName === 'pipelinesView' }"
                                @click="routerToManage('pipelinesView')">视图管理</a>
                        </li>
                        <li>
                            <a href="javascript:;" class="text-link" id="toggleTemplates"
                                :class="{ 'selected-item': routeName === 'pipelinesTemplate' }"
                                @click="routerToManage('pipelinesTemplate')">模板管理</a>
                        </li>
                    </ul>
                </div>
            </div>
        </pipeline-header>

        <div class="view-manage-background" v-if="showViewManage"></div>

        <router-view v-if="currentViewId" style="width: 100%"></router-view>
    </article>

</template>

<script>
    import { mapActions, mapState, mapMutations } from 'vuex'
    import pipelineHeader from '@/components/devops/pipeline_header'
    import Logo from '@/components/Logo'
    import ViewManage from '@/components/pipelineList/view_manage'

    export default {
        components: {
            'pipeline-header': pipelineHeader,
            Logo,
            ViewManage
        },
        data () {
            return {
                isLoading: false,
                toggleIsMore: false,
                header: {
                    title: '流水线',
                    links: [
                        {
                            title: '入门指南',
                            handler: this.tutorial
                        }
                    ]
                }
            }
        },
        computed: {
            ...mapState('pipelines', [
                'currentViewList',
                'currentViewId',
                'showViewManage',
                'pageLoading'
            ]),
            pageType () {
                return this.$route.params.type
            },
            dropdownTitle () {
                let title
                if (this.routeName === 'pipelinesGroup') {
                    title = '标签管理'
                } else if (this.routeName === 'pipelinesView') {
                    title = '视图管理'
                } else if (this.routeName === 'pipelinesTemplate') {
                    title = '模板管理'
                } else {
                    title = '更多'
                }
                return title
            },
            routeName () {
                return this.$route.name
            },
            projectId () {
                return this.$route.params.projectId
            }
        },
        watch: {
            projectId: async function () {
                await this.initListPage()
                this.$store.commit('pipelines/toggleShowViewManage', false)
                this.$router.push({
                    name: 'pipelinesList',
                    params: {
                        type: this.currentViewId
                    }
                })
            }
        },
        async created () {
            await this.initListPage()
            this.addClickListenr()
        },
        beforeDestroy () {
            this.removeClickListenr()
        },
        methods: {
            ...mapActions('pipelines', [
                'requestViewSettingInfo'
            ]),
            ...mapMutations('pipelines', [
                'updateViewSettingInfo',
                'updateCurrentViewId'
            ]),
            addClickListenr () {
                document.addEventListener('mouseup', this.clickHandler)
            },
            removeClickListenr () {
                document.removeEventListener('mouseup', this.clickHandler)
            },
            toggleIsMoreHandler () {
                if (!this.showViewManage) {
                    this.toggleIsMore = !this.toggleIsMore
                }
            },
            clickHandler (event) {
                if (event.target.id !== 'moreHeaderHandler' && event.target.id !== 'toggleHeaderIcon') {
                    setTimeout(() => {
                        this.toggleIsMore = false
                    }, 100)
                }
            },
            tutorial () {
                window.open(`${DOCS_URL_PREFIX}/所有服务/流水线/什么是流水线/summary.html`, '_blank')
            },
            routerToManage (type) {
                this.$router.push({ name: type })
            },
            toggleShowViewManage () {
                if (!this.showViewManage) {
                    this.$store.commit('pipelines/toggleShowViewManage', true)
                }
            },
            changePageType (type) {
                this.updateCurrentViewId(type)
                this.$router.push({
                    name: 'pipelinesList',
                    params: {
                        type
                    }
                })
            },
            goToGroup () {
                this.$router.push({
                    name: 'pipelinesGroup'
                })
            },
            async initListPage () {
                try {
                    this.$store.commit('pipelines/showPageLoading', true)
                    const viewSetting = await this.requestViewSettingInfo({ projectId: this.projectId })
                    if (viewSetting.currentViewId) {
                        this.updateViewSettingInfo(viewSetting)
                    }
                } catch (err) {
                    this.$showTips({
                        theme: 'error',
                        message: err.message || err
                    })
                } finally {
                    this.$store.commit('pipelines/showPageLoading', false)
                }
            }
        }
    }
</script>
<style lang="scss">
    @import './../../scss/conf';

    .pipeline-content {
        height: 100%;
    }
    .view-manage-background {
        position: fixed;
        top: 0px;
        bottom: 0;
        left: 0;
        right: 0;
        width: 100%;
        background-color: rgba(0,0,0,0.6);
        z-index: 1500;
    }
    .header-list {
        display: flex;
        justify-content: center;
        width: 100%;
        li {
            float: left;
            padding: 19px 0;
            a {
                color: $fontWeightColor;
                cursor: pointer;
                font-size: 16px;
                line-height: 21px;
                margin: 0 20px;
                padding: 18px 0;
                max-width: 112px;
                display: inline-block;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
                position: relative;
                top: 3px;
            }
            .manage-view-btn {
                margin-top: 20px;
                width: 24px;
                height: 24px;
                border: 2px dotted $borderWeightColor;
                cursor: pointer;
                .icon-plus {
                    color: $fontColor;
                }
            }
        }
        .active-item {
            color: $primaryColor;
            border-bottom: 2px solid $primaryColor;
        }
    }
    .label-button {
        margin-right: -5px;
    }
    .active-group {
        cursor: default;
    }
    .default-link-list {
        .dropdown-trigger {
            span{
                font-size: 14px;
                cursor: pointer;
            }
            .bk-icon {
                display: inline-block;
                transition: all ease 0.2s;
                margin-left: 4px;
                font-size: 12px;
                &.icon-flip {
                    transform: rotate(180deg);
                }
            }
            .selectde-title {
                color: $primaryColor;
            }
        }
        .dropdown-list {
            position: absolute;
            top: 48px;
            right: 30px;
            max-height: 250px;
            background: #fff;
            padding: 0;
            margin: 0;
            z-index: 99;
            overflow: auto;
            border-radius: 2px;
            border: 1px solid #c3cdd7;
            transition: all .3s ease;
            box-shadow: 0 2px 6px rgba(51,60,72,.1);
            a {
                display: block;
                line-height: 41px;
                padding: 0 15px;
                color: #63656E;
                font-size: 14px;
                text-decoration: none;
                white-space: nowrap;
                &:hover {
                    background-color: #ebf4ff;
                    color: $primaryColor;
                }
            }
            .selected-item {
                background-color: #ebf4ff;
                color: $primaryColor;
            }
        }
    }
    .selectde-title {
        color: $primaryColor;
    }
    @media screen and (max-width: 1360px) {
        .header-list li a {
            margin: 0 12px;
        }
    }
</style>
