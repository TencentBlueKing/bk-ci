<template>
    <div class="atom-list-wrapper">
        <h3 class="market-home-title banner-nav">
            <icon class="title-icon" name="color-logo-store" size="25" />
            <p class="title-name">
                <span class="back-home" @click="toAtomStore">{{ $t('store.store') }}</span>
                <i class="right-arrow banner-arrow"></i>
                <span class="banner-des">{{ $t('store.workbench') }}</span>
            </p>
            <a class="title-work" target="_blank" :href="docLink[currentTab].link">{{ docLink[currentTab].name }}</a>
        </h3>
        <div class="atomstore-list-content">
            <bk-tab :active.sync="currentTab" @tab-change="changeTab" type="unborder-card">
                <bk-tab-panel name="atom" render-directive="if">
                    <template slot="label">
                        <span class="work-label"><icon class="title-icon" :name="`store-atom`" size="16" />{{ $t('store.pipelineAtom') }}</span>
                    </template>
                    <atom-list></atom-list>
                </bk-tab-panel>
                <bk-tab-panel name="template" render-directive="if">
                    <template slot="label">
                        <span class="work-label"><icon class="title-icon" :name="`store-template`" size="16" />{{ $t('store.pipelineTemplate') }}</span>
                    </template>
                    <template-list></template-list>
                </bk-tab-panel>
            </bk-tab>
        </div>
    </div>
</template>

<script>
    import { getQueryString } from '@/utils/index'
    import atomList from '@/components/common/workList/atom'
    import templateList from '@/components/common/workList/template'

    export default {
        components: {
            atomList,
            templateList
        },

        data () {
            return {
                currentTab: 'atom',
                docLink: {
                    atom: { name: this.$t('store.atomGuide'), link: `${DOCS_URL_PREFIX}/${this.$t('allService')}/${this.$t('store.pipelineAtom')}Store/${this.$t('store.quickStart')}.html` },
                    template: { name: this.$t('store.templateGuide'), link: `${DOCS_URL_PREFIX}/${this.$t('allService')}/${this.$t('store.pipelineTemplate')}/summary.html` }
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
            }
        }
    }
</script>

<style lang="scss" scope>
    @import '@/assets/scss/conf.scss';
    
    .atom-list-wrapper {
        height: 100%;
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
                height: 100%;
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
