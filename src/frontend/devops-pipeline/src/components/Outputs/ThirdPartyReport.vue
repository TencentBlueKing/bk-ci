<template>
    <bk-table :data="reportList">
        <bk-table-column :label="$t('name')">
            <bk-link
                slot-scope="props"
                class="report-td-name"
                target="_blank"
                theme="primary"
                :href="props.row.indexFileUrl"
            >
                {{ props.row.name }}
                <logo name="tiaozhuan" size="16" class="jump-icon" />
            </bk-link>
        </bk-table-column>
    </bk-table>
</template>

<script>
    import Logo from '@/components/Logo'

    export default {
        components: { Logo },
        props: {
            reportList: Array
        }
    }
</script>
<style lang="scss">
    @import '@/scss/conf';
    .report-td-name {
        .bk-link-text {
            display: flex;
            align-items: center;
            justify-content: flex-start;
        }
        .jump-icon {
            color: $primaryColor;
        }
    }
</style>
