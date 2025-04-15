<template>
    <section class="render-sort-category-params">
        <details open>
            <summary class="category-collapse-trigger">
                <div>
                    {{ name }}
                </div>
                <i class="devops-icon icon-angle-down"></i>
            </summary>
            <div class="collapse-content">
                <slot name="content" />
            </div>
        </details>
    </section>
</template>

<script>
    export default {
        name: 'RenderSortCategoryParams',
        props: {
            name: {
                type: String,
                default: ''
            }
        }
    }
</script>

<style lang="scss" scoped>
    .render-sort-category-params {
        margin-bottom: 20px;
    }
    .category-collapse-trigger {
        display: flex;
        justify-content: space-between;
        align-items: center;
        cursor: pointer;
        height: 28px;
        background: #EAEBF0;
        border-radius: 2px;
        padding: 0 16px;
        font-size: 12px;
        color: #4D4F56;
        font-weight: 700;
        .icon-angle-down {
            font-size: 10px;
        }
    }

    .collapse-content {
        padding: 16px 16px 0;
        display: grid;
        grid-template-columns: repeat(2, minmax(200px, 1fr));
        grid-gap: 0 24px;
    }

    details:not([open]) .collapse-content {
        display: none;
    }

    details:not([open]) .category-collapse-trigger .icon-angle-down {
        transform: rotate(-90deg);
    }
</style>
