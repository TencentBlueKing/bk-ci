<template>
    <div class="layou-outer">
        <!-- <nav-top /> -->
        <header class="page-header">
            <div class="app-logo">
                <img @click="$router.push({ name: 'task-list' })" :src="logo" :alt="this.$t('代码检查中心')">
                <div class="breadcrumb">{{$t('代码检查中心')}}
                    <!-- <a class="sub-header-link" target="_blank"
                        :href="iwikiCodeccHome">
                        <span class="bk-icon icon-question-circle"></span>
                    </a> -->
                </div>
            </div>
            <bk-tab ext-cls="cc-panels" :active.sync="currentNavTab" type="unborder-card" @tab-change="changeTab">
                <bk-tab-panel
                    v-for="(panel, index) in panels"
                    v-bind="panel"
                    :key="index">
                    <span slot="label" @click="handleRedPoint(panel.name)">
                        <span>{{panel.label}}</span>
                        <i v-if="panel.name === 'checker' && !hasRedPointStore" class="red-point"></i>
                    </span>
                </bk-tab-panel>
            </bk-tab>
            <div @click="handlerVersionChange" class="app-version">
                <!-- <span v-bk-tooltips="toolTips.version" class="icon codecc-icon icon-change"></span> -->
            </div>
            <!-- <div class="breadcrumb">{{title}}</div> -->
        </header>
        <main class="page-main" :class="{ 'has-banner': !isBannerClose }">
            <div class="page-content">
                <div class="main-container">
                    <slot />
                </div>
            </div>
        </main>
    </div>
</template>

<script>
    import logo from '@/images/logo.svg'
    import { mapGetters } from 'vuex'
    // import NavTop from './nav-top'

    export default {
        components: {
            // NavTop
        },
        data () {
            return {
                logo,
                toolTips: {
                    version: {
                        content: this.$t('切到旧版CodeCC')
                    }
                },
                panels: [
                    { name: 'task', label: '任务' },
                    { name: 'checkerset', label: '规则集' },
                    { name: 'checker', label: '规则' }
                ],
                iwikiCodeccHome: window.IWIKI_CODECC_HOME,
                hasRedPointStore: window.localStorage.getItem('redtips-tab-cloc-20200704')
            }
        },
        computed: {
            ...mapGetters(['isBannerClose']),
            title () {
                const title = this.$route.meta.title
                return this.$t(`${title}`)
            },
            currentNavTab () {
                const routeName = this.$route.name
                const navMap = {
                    'task-list': 'task',
                    'checker-list': 'checker',
                    'checkerset-list': 'checkerset',
                    'checkerset-manage': 'checkerset'
                }
                return navMap[routeName] || 'task'
            }
        },
        methods: {
            handlerVersionChange () {
                const projectId = this.$route.params.projectId
                window.location.href = window.OLD_CODECC_SITE_URL + '/coverity/myproject?projectId=' + projectId
            },
            changeTab (name) {
                if (name === 'checker') {
                    this.$router.push({ name: 'checker-list' })
                } else if (name === 'task') {
                    this.$router.push({ name: 'task-list' })
                } else if (name === 'checkerset') {
                    this.$router.push({ name: 'checkerset-list' })
                }
            },
            handleRedPoint (name) {
                if (name === 'checker') {
                    window.localStorage.setItem('redtips-tab-cloc-20200704', '1')
                    this.hasRedPointStore = true
                }
            }
        }
    }
</script>

<style lang="postcss" scoped>
    @import '../../css/variable.css';

    .layou-outer {
        --headerHeight: 60px;
        .page-header {
            display: flex;
            height: var(--headerHeight);
            align-items: center;
            padding: 0 28px;
            background: #fff;
            border-bottom: 1px solid #DCDEE5;;
            .app-logo {
                display: flex;
                flex: 2;
                img {
                    height: 25px;
                    cursor: pointer;
                    margin-top: 15px;
                }
            }
            .breadcrumb {
                margin-left: 8px;
                color: #63656e;
                line-height: 60px;
            }
            >>>.cc-panels {
                display: flex;
                flex: 5;
                justify-content: center;
                .bk-tab-header,
                .bk-tab-label-item {
                    height: 59px !important;
                }
                .bk-tab-label-item {
                    padding: 10px 18px;
                }
                .bk-tab-label-item,
                .bk-tab-label-item.active {
                    background-color: #fff;
                }
                .bk-tab-label {
                    font-size: 16px;
                }
                .bk-tab-section {
                    display: none;
                }
            }
            .app-version {
                display: flex;
                flex: 2;
                cursor: pointer;
                font-size: 22px;
                &:hover {
                    color: #3a84ff;
                }
            }
            .sub-header-link span {
                padding-left: 4px;
                color: #c4cdd6;
                cursor: pointer;
                font-size: 14px;
            }
        }
        .page-main {
            height: calc(100vh - var(--navTopHeight));
            overflow: auto;
            &.has-banner {
                height: calc(100vh - var(--navTopHeight) - var(--bannerHeight));
            }

            .page-content,
            .main-container,
            .main-content {
                height: 100%;
            }

            .main-container {
                padding: 20px 0;
            }
        }
        .red-point {
            margin-bottom: 10px;
            margin-left: -3px;
        }
    }
</style>
