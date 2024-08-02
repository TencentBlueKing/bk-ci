<template>
    <div class="bkdevops-vertical-tab">
        <aside>
            <slot name="tab">
                <ul class="bkdevops-vertical-tab-list">
                    <li v-for="(tab, index) in tabs" :class="{ 'active': tabActiveIndex === index }" :key="tab.id" @click="handleTabClick(index)" :title="tab.name">
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
    import NotifySetting from '@/components/pipelineSetting/NotifySetting'
    import BaseInfo from '@/components/pipelineSetting/BaseInfo'
    import RunningLock from '@/components/pipelineSetting/RunningLock'
    import CodeRecordTable from '@/components/codeRecord/CodeRecordTable'

    export default {
        name: 'vertical-tab',
        components: {
            NotifySetting,
            BaseInfo,
            RunningLock,
            CodeRecordTable
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
    @import '../../scss/mixins/ellipsis';
    .bkdevops-vertical-tab {
        display: flex;
        border: 1px solid $borderWeightColor;
        height: 100%;
        > aside {
            width: 240px;
            border-right: 1px solid $borderWeightColor;
            height: 100%;
            .bkdevops-vertical-tab-list {
                height: 100%;
                overflow-y: auto;
                > li {
                    cursor: pointer;
                    height: 44px;
                    line-height: 44px;
                    font-size: 14px;
                    padding: 0 20px;
                    width: 100%;
                    @include ellipsis();
                    &.active {
                        color: $primaryColor;
                        background-color: #e0ecff;
                    }
                }
            }
        }
        > section {
            flex: 1;
            padding: 30px 25px;
            background-color: white;
            overflow: auto;
        }
    }
</style>
