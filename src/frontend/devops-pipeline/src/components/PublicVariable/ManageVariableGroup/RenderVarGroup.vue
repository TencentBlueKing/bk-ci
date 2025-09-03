<template>
    <section class="render-var-group-main">
        <details open>
            <summary class="category-collapse-trigger">
                <bk-icon
                    type="right-shape"
                    class="icon-angle-right"
                />
                <div>
                    <p class="group-name">{{ '测试测试' }}</p>
                    <p class="group-desc">123</p>
                </div>
            </summary>
            <div
                class="collapse-content"
            >
                123
            </div>
        </details>
    </section>
</template>

<script setup>
    const props = defineProps({
        isShow: Boolean,
        data: Object,
        readOnly: Boolean,
        newParamId: String,
        showRefField: {
            type: Boolean,
            default: true
        }
    })
</script>

<style lang="scss">
.render-var-group-main {
    .category-collapse-trigger {
        display: flex;
        align-items: center;
        cursor: pointer;
        background: #F5F7FA;
        border-radius: 2px 2px 0 0;
        padding: 8px 12px;
        font-size: 12px;
        color: #4D4F56;
        font-weight: 700;
        .icon-angle-right {
            font-size: 10px;
            margin-right: 5px;
            transform: rotate(90deg);
            color: #979BA5;
            position: relative;
            top: -12px;
        }
        .group-name {
            font-weight: 400;
            font-size: 14px;
            color: #4D4F56;
        }
        .group-desc {
            font-size: 12px;
            color: #979BA5;
            margin: 8px 0 0px !important;
        }
    }
    details:not([open]) .collapse-content {
        display: none;
    }

    .collapse-content {
        background: #F5F7FA;
        padding: 0 24px;
    }

    details:not([open]) .category-collapse-trigger > .icon-angle-right {
        transform: rotate(0deg);
    }
    .variable-list-table {
        .is-new {
            background-color: #f2fff4 !important;
        }
    }
}
</style>