<template>
    <div class="custom-tabs">
        <div
            v-for="tab in tabs"
            :key="tab.name"
            :class="['custom-tab-item', { 'is-active': activeTab === tab.name }]"
            @click="handleTabClick(tab.name)"
        >
            <span class="tab-label">{{ tab.label }}</span>
            <span
                v-if="tab.count !== undefined"
                class="tab-count"
            >{{ tab.count }}</span>
        </div>
    </div>
</template>

<script>
    export default {
        name: 'CustomTabs',
        props: {
            activeTab: {
                type: String,
                required: true
            },
            tabs: {
                type: Array,
                required: true,
                validator: (tabs) => {
                    return tabs.every(tab => tab.name && tab.label)
                }
            }
        },
        methods: {
            handleTabClick (name) {
                this.$emit('tab-change', name)
            }
        }
    }
</script>

<style lang="scss" scoped>
.custom-tabs {
    display: inline-flex;
    align-items: center;
    background: #F0F1F5;
    border-radius: 2px;
    padding: 4px;
    gap: 4px;

    .custom-tab-item {
        display: inline-flex;
        align-items: center;
        padding: 5px 12px;
        border-radius: 2px;
        font-size: 12px;
        cursor: pointer;
        transition: all 0.2s ease;
        white-space: nowrap;

        .tab-label {
            color: #4D4F56;
        }

        .tab-count {
            margin-left: 4px;
            color: #4D4F56;
            padding: 0px 6px;
            border-radius: 8px;
            background-color: #fff;
        }

        &.is-active {
            background: #FFFFFF;
            box-shadow: 0 2px 4px 0 #0000001a;

            .tab-label {
                color: #3A84FF;
            }

            .tab-count {
                color: #3A84FF;
                background: #E1ECFF;
            }
        }
    }
}
</style>
