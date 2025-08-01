<template>
    <bk-tab
        :active="activeTab"
        type="unborder-card"
        class="transition-tabs"
        @tab-change="tabChange"
    >
        <template slot="setting">
            <slot name="tool"></slot>
        </template>
        <bk-tab-panel
            v-for="(panel, index) in panels"
            v-bind="panel"
            :key="index"
        >
        </bk-tab-panel>
    </bk-tab>
</template>

<script>
    export default {
        props: {
            panels: {
                type: Array,
                required: true
            },
            activeTab: {
                type: String,
                required: true
            }
        },

        methods: {
            tabChange (tabName) {
                this.$emit('tab-change', tabName)
            }
        }
    }
</script>

<style lang="scss" scoped>
    .transition-tabs {
        width: 100%;
        margin: 24px 0;
        overflow: hidden;
        box-shadow: 1px 2px 3px 0 rgba(0,0,0,0.05);
        background-color: #fff;
        ::v-deep .bk-tab-section {
            display: none;
        }
    }
</style>
