<template>
    <div class="atom-list-wrapper">
        <h3 class="market-home-title banner-nav">
            <icon class="title-icon" name="color-logo-store" size="25" />
            <p class="title-name">
                <span class="back-home" @click="toAtomStore"> {{ $t('store.研发商店') }} </span>
                <i class="right-arrow banner-arrow"></i>
                <span class="banner-des"> {{ $t('store.工作台') }} </span>
            </p>
            <template v-if="tabList[currentTab].showMore">
                <icon name="work-manage" size="20" class="work-more" @click.native="showMore = !showMore" />
                <section class="more-list" v-if="showMore" v-clickoutside="closeShowMore">
                    <a :href="more.link" v-for="more in tabList[currentTab].moreList" :key="more.name" target="_blank">{{ more.name }}</a>
                </section>
            </template>
            <a class="title-work" target="_blank" :href="tabList[currentTab].link" v-else>{{ tabList[currentTab].name }}</a>
        </h3>
        <div class="atomstore-list-content">
            <bk-tab :active.sync="currentTab" type="unborder-card">
                <bk-tab-panel :name="key" render-directive="if" v-for="(tab, key) in tabList" :key="tab.type">
                    <template slot="label">
                        <span class="work-label"><icon class="title-icon" :name="`store-${key}`" size="16" /> {{ tab.tabName }} </span>
                    </template>
                    <component :is="`${key}List`"></component>
                </bk-tab-panel>
            </bk-tab>
        </div>
    </div>
</template>

<script>
    import clickoutside from '@/directives/clickoutside'
    import { getQueryString } from '@/utils/index'
    import atomList from '@/components/common/workList/atom'
    import templateList from '@/components/common/workList/template'
    import imageList from '@/components/common/workList/image'
    let currentProjectCode = localStorage.getItem('projectId')
    if (!currentProjectCode) currentProjectCode = (window.projectList[0] || {}).projectCode

    export default {
        components: {
            atomList,
            templateList,
            imageList
        },

        directives: {
            clickoutside
        },

        data () {
            return {
                currentTab: 'atom',
                showMore: false,
                tabList: {
                    atom: {
                        tabName: this.$t('store.流水线插件'),
                        showMore: true,
                        moreList: [
                            { name: this.$t('store.插件指引'), link: 'http://iwiki.oa.com/pages/viewpage.action?pageId=15008942' },
                            { name: this.$t('store.debugTask'), link: `/console/pipeline/${currentProjectCode}/atomDebug` }
                        ]
                    },
                    template: { name: this.$t('store.模版指引'), tabName: this.$t('store.流水线模板'), link: 'http://iwiki.oa.com/pages/viewpage.action?pageId=15008944' },
                    image: { name: this.$t('store.镜像指引'), tabName: this.$t('store.容器镜像'), link: 'http://iwiki.oa.com/pages/viewpage.action?pageId=22118721' }
                }
            }
        },

        watch: {
            currentTab (val) {
                this.$router.replace({
                    name: 'atomList',
                    params: {
                        type: val
                    }
                })
            }
        },

        created () {
            this.currentTab = this.$route.params.type
            if (getQueryString('projectCode') && getQueryString('templateId')) {
                this.currentTab = 'template'
            }
        },

        methods: {
            toAtomStore () {
                this.$router.push({
                    name: 'atomHome'
                })
            },

            closeShowMore () {
                const target = event.target || {}
                const href = target.href || {}
                if (href.animVal !== '#work-manage' && !target.classList.contains('work-more')) this.showMore = false
            }
        }
    }
</script>

<style lang="scss">
    @import '@/assets/scss/conf.scss';
    
    .atom-list-wrapper {
        height: 100%;
        .work-more {
            margin-right: 30px;
            cursor: pointer;
        }
        .more-list {
            position: absolute;
            z-index: 500;
            right: 30px;
            top: 50px;
            background: $white;
            border: 1px solid $borderWeightColor;
            border-radius: 2px;
            box-shadow: 0 3px 6px rgba(51, 60, 72, 0.12);
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
                line-height: 32px;
                border-bottom: 1px solid $borderWeightColor;
                padding: 0 14px;
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
        .atomstore-list-content {
            padding: 8px 25px 25px;
            height: calc(100% - 50px);
            overflow: auto;
            .bk-table.bk-table-fit {
                border: 1px solid #dfe0e5;
            }
            .bk-page {
                padding: 0 20px;
            }
            .bk-tab-section {
                padding-bottom: 0px;
            }
            .bk-tab.bk-tab-unborder-card li.bk-tab-label-item {
                background-color: transparent;
            }
            .work-label {
                display: flex;
                align-items: center;
                svg {
                    margin-right: 3px;
                }
            }
        }
        .bk-tab2 {
            border: none;
            background: transparent;
        }
        .bk-tab2-head {
            height: 42px;
        }
        .bk-tab2 .bk-tab2-nav .tab2-nav-item {
            height: 42px;
            padding: 0 16px;
            line-height: 42px;
            .icon-tab {
                position: relative;
                top: 3px;
                font-size: 18px;
            }
        }
        .content-header {
            display: flex;
            .list-input {
                margin: 0;
                width: 240px;
                height: 36px;
                > input {
                    width: 240px;
                    height: 36px;
                }
                i {
                    top: 11px;
                }
            }
            .bk-button {
                padding: 0 15px;
                margin-right: 20px;
            }
        }
        .bk-tab2-content {
            padding-top: 20px;
        }
        .render-table {
            min-width: 1180px;
            margin: 20px 0;
            border: 1px solid $borderWeightColor;
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
                width: 97px;
                padding-right: 25px;
                font-weight: normal;
            }
            .bk-form-content {
                margin-left: 97px;
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
                margin-left: 97px;
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
                margin-left: 97px;
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
    }
</style>
