<template>
    <div class="bkdevops-vertical-tab">
        <aside>
            <slot name="tab">
                <ul class="bkdevops-vertical-tab-list">
                    <li v-for="(tab, index) in tabs" :class="{ 'active': tabActiveIndex === index }" :key="tab.id" @click="handleTabClick(index)">
                        {{ tab.name }}
                    </li>
                </ul>
            </slot>
        </aside>
        <section>
            <slot name="panel" :tabActiveIndex="tabActiveIndex">
                <component :is="activeTab.component" v-bind="activeTab.componentProps"></component>
            </slot>
        </section>
    </div>
</template>

<script>
    import BaseInfo from '@/components/pipelineSetting/BaseInfo'
    import RunningLock from '@/components/pipelineSetting/RunningLock'
    import CleanPolicy from '@/components/pipelineSetting/CleanPolicy'
    import CodeRecordTable from '@/components/codeRecord/CodeRecordTable'
    import thirdPartyReport from '@/components/outputOption/thirdParty_report'
    import IframeReport from '@/components/outputOption/IframeReport'

    export default {
        name: 'vertical-tab',
        components: {
            BaseInfo,
            RunningLock,
            CleanPolicy,
            CodeRecordTable,
            thirdPartyReport,
            IframeReport
        },
        props: {
            tabs: {
                type: Array,
                default: []
            },
            initTabIndex: {
                type: Number,
                default: 0
            }
        },
        data () {
            return {
                tabActiveIndex: this.initTabIndex
            }
        },
        computed: {
            activeTab () {
                return this.tabs[this.tabActiveIndex]
            }
        },
        watch: {
            initTabIndex (index) {
                this.tabActiveIndex = index
            }
        },
        methods: {
            handleTabClick (index) {
                if (typeof this.activeTab.beforeLeave === 'function') {
                    this.activeTab.beforeLeave()
                }
                this.tabActiveIndex = index
            }
        }
    }
</script>

<style lang="scss">
    @import "../../scss/conf";
    .bkdevops-vertical-tab {
        display: flex;
        border: 1px solid $borderWeightColor;
        height: 100%;
        > aside {
            width: 240px;
            border-right: 1px solid $borderWeightColor;
            .bkdevops-vertical-tab-list {
                > li {
                    cursor: pointer;
                    height: 44px;
                    line-height: 44px;
                    padding-left: 30px;
                    font-size: 12px;
                    &.active {
                        color: $primaryColor;
                        background-color: #e0ecff;
                    }
                }
            }
        }
        > section {
            flex: 1;
            padding: 30px 44px;
            background-color: white;
            overflow: auto;

            .bk-form-item{
                margin: 0 0 30px 0;
                .bk-label {
                    width: 150px;
                    text-align: right;
                    float:left;
                    padding-right: 20px;
                }
                .bk-form-content {
                    display: block;
                    margin-left: 150px;
                }
            }
        }
    }
</style>
