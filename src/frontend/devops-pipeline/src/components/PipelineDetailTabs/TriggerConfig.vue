<template>
    <bk-table
        v-bk-loading="{ isLoading }"
        :data="triggerList"
    >
        <bk-table-column prop="triggerName" :label="$t('triggerName')">
            <span class="pipeline-trigger-name-cell" slot-scope="{ row }">
                <logo name="CODE_GIT" size="16" />
                <span>{{ row.name }}</span>
            </span>
        </bk-table-column>
        <bk-table-column prop="isEnable" :label="$t('triggerStatus')">
            <span slot-scope="{ row }">
                {{ row.isEnable ? $t('enable') : $t('disable') }}
            </span>
        </bk-table-column>
    </bk-table>
</template>

<script>
    import { mapGetters } from 'vuex'
    import Logo from '@/components/Logo'
    export default {
        components: {
            Logo
        },
        data () {
            return {
                isLoading: false
            }
        },
        computed: {
            ...mapGetters('pipelines', {
                triggerList: 'getPipelineTriggers'
            })
        }

    }
</script>
<style lang="scss">
    .pipeline-trigger-name-cell {
        display: grid;
        grid-gap: 6px;
        grid-auto-flow: column;
        grid-template-columns: min-content auto;
    }
</style>
