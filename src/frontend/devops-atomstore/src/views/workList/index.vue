<template>
    <article class="g-store-main">
        <bread-crumbs :bread-crumbs="navList" :type="currentTabName.slice(0, -4)"></bread-crumbs>

        <transition-tab :panels="tabList"
            @tab-change="tabChange"
        >
            <template v-slot:tool>
                <a class="title-work" target="_blank" :href="link.link" v-for="link in currentTab.links" :key="link.name">{{ link.name }}</a>
            </template>
        </transition-tab>

        <main class="g-store-body">
            <router-view class="g-store-route work-list-main"></router-view>
        </main>
    </article>
</template>

<script>
    import transitionTab from '@/components/transition-tab.vue'
    import breadCrumbs from '@/components/bread-crumbs.vue'
    import cookie from 'js-cookie'
    let currentProjectCode = cookie.get(X_DEVOPS_PROJECT_ID)
    if (!currentProjectCode) currentProjectCode = (window.projectList[0] || {}).projectCode

    export default {
        components: {
            transitionTab,
            breadCrumbs
        },

        data () {
            return {
                currentTabName: this.$route.name,
                tabList: [
                    {
                        name: 'atomWork',
                        label: this.$t('store.流水线插件'),
                        links: [
                            {
                                name: this.$t('store.插件指引'),
                                link: this.BKCI_DOCS.PLUGIN_GUIDE_DOC
                            },
                            {
                                name: this.$t('store.debugTask'),
                                link: `/console/pipeline/${currentProjectCode}/atomDebug`
                            }
                        ]
                    },
                    {
                        name: 'templateWork',
                        label: this.$t('store.流水线模板'),
                        links: [
                            {
                                name: this.$t('store.模版指引'),
                                link: this.BKCI_DOCS.TEMPLATE_GUIDE_DOC
                            }
                        ]
                    },
                    {
                        name: 'imageWork',
                        label: this.$t('store.容器镜像'),
                        links: [
                            {
                                name: this.$t('store.镜像指引'),
                                link: this.BKCI_DOCS.IMAGE_GUIDE_DOC
                            }
                        ]
                    }
                ]
            }
        },

        computed: {
            currentTab () {
                return this.tabList.find(x => x.name === this.currentTabName)
            },

            navList () {
                const type = this.currentTab.name
                let name
                switch (type) {
                    case 'templateWork':
                        name = this.$t('store.流水线模板')
                        break
                    case 'imageWork':
                        name = this.$t('store.容器镜像')
                        break
                    default:
                        name = this.$t('store.流水线插件')
                        break
                }
                return [
                    { name: this.$t('store.工作台') },
                    { name }
                ]
            }
        },

        watch: {
            currentTabName (name) {
                this.$router.push({ name })
            }
        },

        methods: {
            tabChange (name) {
                this.currentTabName = name
            }
        }
    }
</script>

<style lang="scss">
    @import '@/assets/scss/conf.scss';

    .work-list-main {
        background: #fff;
        padding: 16px 32px;
    }

    .banner-more {
        height: 26px;
        width: 26px;
        position: absolute;
        right: 8px;
        top: 18px;
        line-height: 26px;
        &:hover {
            color: #1592ff;
            .more-list {
                display: block;
            }
        }
    }
    .icon-more {
        font-size: 26px;
        cursor: pointer;
    }
    .more-list {
        display: none;
        transition: display 200ms;
        position: absolute;
        z-index: 500;
        right: 5px;
        top: 27px;
        background: $white;
        border: 1px solid $borderWeightColor;
        border-radius: 2px;
        box-shadow: 0 3px 6px rgba(51, 60, 72, 0.12);
        &:hover {
            display: block;
        }
        &::before {
            content: '';
            position: absolute;
            right: 2px;
            top: -6px;
            width: 10px;
            height: 10px;
            transform: rotate(45deg);
            background: $white;
            border-top: 1px solid $borderWeightColor;
            border-left: 1px solid $borderWeightColor;
        }
        a {
            display: block;
            min-width: 88px;
            line-height: 36px;
            border-bottom: 1px solid $borderWeightColor;
            padding: 0 14px;
            margin: 0;
            color: $fontWeightColor;
            white-space: nowrap;
            cursor: pointer;
            &:hover {
                color: $primaryColor;
            }
            &:last-child {
                border: 0;
            }
        }
    }
    .title-work {
        cursor: pointer;
        color: #1592ff;
        margin-right: 16px;
        &:last-child {
            margin-right: 32px;
        }
    }
    .content-header {
        display: flex;
        .search-input {
            width: 240px;;
        }
        .bk-button {
            padding: 0 15px;
            margin-right: 20px;
        }
    }
    .create-atom-slider,
    .offline-atom-slider {
        .bk-sideslider-content {
            height: calc(100% - 90px);
            .bk-form-content .bk-tooltip {
                color: #63656e;
            }
        }
        .create-atom-form,
        .offline-atom-form,
        .relate-template-form {
            margin: 30px 50px 20px 28px;
        }
        .bk-label {
            width: 100px;
            padding-right: 25px;
            font-weight: normal;
        }
        .bk-form-content {
            margin-left: 100px;
            line-height: 30px;
        }
        .bk-selector {
            min-width: 100%;
        }
        .is-tooltips {
            display: flex;
        }
        .bk-tooltip {
            color: $fontLigtherColor;
            p {
                max-width: 250px;
                text-align: left;
                white-space: normal;
                word-break: break-all;
                font-weight: 400;
            }
        }
        .atom-tip {
            margin-top: 16px;
        }
        .prompt-oauth {
            margin-top: 16px;
            i {
                position: relative;
                top: 2px;
                margin-right: 4px;
                font-size: 16px;
                color: #FCB728;
            }
            span {
                color: $fontWeightColor;
            }
        }
        .form-footer {
            margin-top: 26px;
            margin-left: 100px;
            button {
                height: 32px;
                line-height: 30px;
            }
        }
        .form-tips {
            margin-left: 21px;
        }
    }
    .offline-atom-slider {
        .offline-atom-form {
            margin: 30px 30px 20px 30px;
        }
        .content-value {
            line-height: 30px;
            color: #333C48;
        }
        .is-required {
            margin-top: 20px;
        }
        .prompt-offline {
            margin-left: 100px;
            margin-top: 20px;
            i {
                position: relative;
                top: 2px;
                margin-right: 2px;
                font-size: 16px;
                color: #FCB728;
            }
            span {
                color: $fontWeightColor;
            }
            .prompt-line {
                margin-left: 22px;
            }
        }
        .form-tips {
            margin-top: 24px;
            margin-left: 34px;
        }
    }
</style>
