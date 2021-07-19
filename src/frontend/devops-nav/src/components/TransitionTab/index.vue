<template>
    <bk-tab :active="activeTab" type="unborder-card" class="transition-tabs" @tab-change="tabChange">
        <template slot="setting">
            <slot name="tool"></slot>
        </template>
        <bk-tab-panel v-for="(panel, index) in panels" v-bind="panel" :key="index">
            <transition name="atom-fade">
                <ul v-if="activeTab === panel.name && panel.showChildTab" class="transition-child-tabs">
                    <li v-for="childPanel in panel.children.filter(x => !x.hidden)"
                        :key="childPanel.name"
                        @click="childTabClick(childPanel)"
                        :class="['transition-child-tab', { active: activeChildTab === childPanel.name }]"
                    >{{ childPanel.label }} <icon name="loadout" size="18" v-if="childPanel.link"></icon> </li>
                </ul>
            </transition>
            <slot></slot>
        </bk-tab-panel>
    </bk-tab>
</template>

<script>
    export default {
        props: {
            panels: Array,
            activeTab: String,
            activeChildTab: String
        },

        methods: {
            tabChange (tabName) {
                let curChildPanel
                this.panels.forEach((panel) => {
                    if (panel.name === tabName) {
                        curChildPanel = panel.children[0].name
                    }
                })
                this.$emit('update:activeTab', tabName)
                this.$emit('update:activeChildTab', curChildPanel)
                this.$emit('child-tab-change', curChildPanel)
                this.$emit('tab-change', tabName)
            },

            childTabClick (childPanel) {
                if (childPanel.link) {
                    window.open(childPanel.link, '_blank')
                } else {
                    this.childTabChange(childPanel.name)
                }
            },

            childTabChange (tabName) {
                const curPanel = this.panels.find((panel) => {
                    return panel.children.find((child) => (child.name === tabName))
                })
                this.$emit('update:activeTab', curPanel.name)
                this.$emit('update:activeChildTab', tabName)
                this.$emit('child-tab-change', tabName)
                this.$emit('tab-change', curPanel.name)
            }
        }
    }
</script>

<style lang="scss" scoped>
    .transition-tabs {
        width: 90%;
        margin: 32px auto 0;
        box-shadow: 1px 2px 3px 0 rgba(0,0,0,0.05);
        .transition-child-tabs {
            height: 47px;
            min-height: 40px;
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
                display: flex;
                align-items: center;
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
            height: 64px !important;
            line-height: 64px !important;
            background-image: linear-gradient(transparent 63px,#dcdee5 0) !important;
            .bk-tab-label-wrapper .bk-tab-label-list {
                height: 64px !important;
                .bk-tab-label-item {
                    line-height: 64px !important;
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
                height: 64px !important;
                line-height: 64px !important;
            }
        }
        /deep/ .bk-tab-section {
            padding: 0;
        }
    }
</style>
