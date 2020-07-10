<template>
    <bk-tab :active="activeTab" type="unborder-card" class="transition-tabs" @tab-change="tabChange">
        <template slot="setting">
            <slot name="tool"></slot>
        </template>
        <bk-tab-panel v-for="(panel, index) in panels" v-bind="panel" :key="index">
            <transition name="fade">
                <ul v-if="activeTab === panel.name && panel.showChildTab" class="transition-child-tabs">
                    <li v-for="childPanel in panel.children.filter(x => !x.hidden)"
                        :key="childPanel.name"
                        @click="childTabChange(childPanel.name)"
                        :class="['transition-child-tab', { active: activeChildTab === childPanel.name }]"
                    >{{ childPanel.label }}</li>
                </ul>
            </transition>
            <slot></slot>
        </bk-tab-panel>
    </bk-tab>
</template>

<script>
    export default {
        props: {
            panels: Array
        },

        data () {
            return {
                activeTab: '',
                activeChildTab: ''
            }
        },

        watch: {
            '$route.name': {
                handler (name) {
                    this.panels.forEach((panel, index) => {
                        if (name === panel.name) {
                            this.activeTab = name
                        }
                        if (panel.children) {
                            panel.children.forEach((childPanel) => {
                                if (name === childPanel.name) {
                                    this.activeTab = panel.name
                                    this.activeChildTab = name
                                }
                            })
                        }
                    })
                },
                immediate: true
            }
        },

        methods: {
            tabChange (tabName) {
                this.activeTab = tabName
                this.$emit('tab-change', tabName)
            },

            childTabChange (tabName) {
                this.activeChildTab = tabName
                this.$emit('child-tab-change', tabName)
            }
        }
    }
</script>

<style lang="scss" scoped>
    .transition-tabs {
        width: 14.6rem;
        margin: 0 auto;
        box-shadow: 1px 2px 3px 0 rgba(0,0,0,0.05);
        .transition-child-tabs {
            height: 47px;
            line-height: 47px;
            padding: 12px 16px;
            background-color: #fff;
            &::after {
                content: '';
                display: table;
                clear: both;
            }
            .transition-child-tab {
                float: left;
                padding: 0 16px;
                font-size: 16px;
                line-height: 22px;
                color: #666;
                cursor: pointer;
                &.active {
                    color: #1a6df3;
                }
                &:not(:last-child) {
                    border-right: 1px solid #ebedf0;
                }
            }
        }
        /deep/ .bk-tab-header {
            background-color: #fff;
            height: 64px;
            line-height: 64px;
            background-image: linear-gradient(transparent 63px,#dcdee5 0);
            .bk-tab-label-wrapper .bk-tab-label-list {
                height: 64px;
                .bk-tab-label-item {
                    line-height: 64px;
                    color: #666;
                    &::after {
                        height: 3px;
                    }
                    &.active {
                        color: #3a84ff;
                    }
                    .bk-tab-label {
                        font-size: 16px;
                    }
                }
            }
            .bk-tab-header-setting {
                height: 64px;
                line-height: 64px;
            }
        }
        /deep/ .bk-tab-section {
            padding: 0;
        }
    }
</style>
