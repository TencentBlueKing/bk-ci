<template>
    <article class="pipeline-content"
        v-bkloading="{
            isLoading: pageLoading
        }"
    >
        <pipeline-header :title="header.title">
            <span class="default-subheader-icon"
                slot="logo">
                <logo size="32" name="pipeline"></logo>
            </span>

            <bk-tab class="header-list" :active.sync="currentViewId" type="unborder-card" slot="center" @tab-change="changePageType" :scroll-step="700">
                <bk-tab-panel
                    v-for="(panel, index) in currentViewList"
                    :name="panel.id"
                    :label="panel.name"
                    :key="index">
                    <template slot="label">
                        <span :title="panel.name">{{panel.name}}</span>
                    </template>
                </bk-tab-panel>
                <div class="manage-view-btn" v-show="currentViewId" slot="setting">
                    <i class="devops-icon icon-plus" @click="toggleShowViewManage()"></i>
                </div>
            </bk-tab>

            <div class="default-link-list" slot="right">
                <div class="dropdown-trigger" @click.stop="toggleIsMoreHandler">
                    <span class="more-handler" id="moreHeaderHandler" :class="{ 'selectde-title': dropdownTitle !== $t('more') }">{{ dropdownTitle }}
                        <i :class="['devops-icon icon-angle-down', { 'icon-flip': toggleIsMore }, { 'selectde-title': dropdownTitle !== $t('more') }]"
                            id="toggleHeaderIcon"></i>
                    </span>
                </div>
                <div class="dropdown-list" v-if="toggleIsMore">
                    <ul class="list-wrapper">
                        <li>
                            <a href="javascript:;" class="text-link" id="toggleLabels"
                                :class="{ 'selected-item': routeName === 'pipelinesGroup' }"
                                @click="routerToManage('pipelinesGroup')">{{$t('labelManage')}}</a>
                        </li>
                        <li>
                            <a href="javascript:;" class="text-link" id="toggleViews"
                                :class="{ 'selected-item': routeName === 'pipelinesView' }"
                                @click="routerToManage('pipelinesView')">{{$t('viewManage')}}</a>
                        </li>
                        <li>
                            <a href="javascript:;" class="text-link" id="toggleTemplates"
                                :class="{ 'selected-item': routeName === 'pipelinesTemplate' }"
                                @click="routerToManage('pipelinesTemplate')">{{$t('templateManage')}}</a>
                        </li>
                        <li>
                            <a href="javascript:;" class="text-link" id="toggleManage"
                                :class="{ 'selected-item': routeName === 'atomManage' }"
                                @click="routerToManage('atomManage')">{{$t('pluginManage')}}</a>
                        </li>
                        <li>
                            <a href="javascript:;" class="text-link" id="toggleManage"
                                :class="{ 'selected-item': routeName === 'pipelinesRestore' }"
                                @click="routerToManage('pipelinesRestore')">{{$t('restore.recycleBin')}}</a>
                        </li>
                        <li>
                            <a href="javascript:;" class="text-link" id="toggleAudit"
                                :class="{ 'selected-item': routeName === 'pipelinesAudit' }"
                                @click="routerToManage('pipelinesAudit')">{{$t('operatorAudit')}}</a>
                        </li>
                    </ul>
                </div>
            </div>
        </pipeline-header>

        <view-manage v-if="showViewManage"></view-manage>

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
                    title: this.$t('pipeline')
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
                switch (this.routeName) {
                    case 'pipelinesGroup':
                        title = this.$t('labelManage')
                        break
                    case 'pipelinesView':
                        title = this.$t('viewManage')
                        break
                    case 'pipelinesTemplate':
                        title = this.$t('templateManage')
                        break
                    case 'atomManage':
                        title = this.$t('pluginManage')
                        break
                    case 'pipelinesRestore':
                        title = this.$t('restore.recycleBin')
                        break
                    case 'pipelinesAudit':
                        title = this.$t('operatorAudit')
                        break
                    default:
                        title = this.$t('more')
                        break
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
            }
        },
        async created () {
            const currentViewId = (this.$route.params || {}).type
            await this.initListPage(currentViewId)
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
                window.open(`${IWIKI_DOCS_URL}/x/RY6j`, '_blank')
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
            async initListPage (currentViewId) {
                try {
                    this.$store.commit('pipelines/showPageLoading', true)
                    const viewSetting = await this.requestViewSettingInfo({ projectId: this.projectId })
                    viewSetting.currentViewId = currentViewId || viewSetting.currentViewId || 'myPipeline'

                    if (!currentViewId) {
                        this.$router.replace({
                            params: {
                                type: viewSetting.currentViewId
                            }
                        })
                    }
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
        width: 30%;
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
        max-width: 80%;
        height: 59px;
        .manage-view-btn {
            margin-top: 17px;
            width: 24px;
            height: 24px;
            border: 2px dotted $borderWeightColor;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            .icon-plus {
                color: $fontColor;
            }
        }
        &.bk-tab .bk-tab-header {
            height: 59px;
            line-height: 59px;
            background-image: none !important;
            .bk-tab-label-wrapper .bk-tab-label-list {
                height: 59px;
                li.bk-tab-label-item {
                    line-height: 59px;
                    color: #666;
                    min-width: 60px;
                    max-width: 276px;
                    &::after {
                        height: 3px;
                    }
                    &.active {
                        color: #3a84ff;
                    }
                    .bk-tab-label {
                        font-size: 16px;
                        overflow: hidden;
                        text-overflow: ellipsis;
                        white-space: nowrap;
                        max-width: 244px;
                    }
                }
            }
            .bk-tab-header-setting, .bk-tab-scroll-controller {
                height: 59px;
                line-height: 59px;
            }
            .bk-tab-scroll-controller {
                box-shadow: none;
                border-bottom: none;
                font-size: 26px;
            }
            .bk-tab-header-setting {
                margin-left: 10px;
            }
            .prev,
            .next {
                height: 59px !important;
                line-height: 59px !important;
            }
        }
    }
    .bk-tab-label-list-has-bar {
        height: 59px !important;
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
            .devops-icon {
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
