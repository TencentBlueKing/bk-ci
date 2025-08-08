<template>
    <section class="pipeline-table-column-setting">
        <h2>{{ $t('tableColSettings') }}</h2>
        <draggable
            class="pipeline-table-column-ul"
            v-model="allTableColumns"
        >
            <li
                v-for="col in allTableColumns"
                :key="col.id"
            >
                <bk-checkbox
                    class="pipeline-table-column-column-checkbox"
                    :checked="col.checked"
                    @change="handleColumnCheck(col.id)"
                    :label="col.id"
                >
                    {{ $t(col.label) }}
                </bk-checkbox>
                <i class="devops-icon icon-drag column-drag-icon"></i>
            </li>
        </draggable>
        <footer>
            <bk-button
                theme="primary"
                @click="handleConfirm"
            >
                {{ $t('confirm') }}
            </bk-button>
            <bk-button @click="handleReset">{{ $t('history.reset') }}</bk-button>
        </footer>
    </section>
</template>

<script setup>
    import { ref, watch } from 'vue'
    import draggable from 'vuedraggable'

    const props = defineProps({
        selectedColumnKeys: {
            type: Array,
            default: () => []
        },
        allTableColumnMap: {
            type: Array,
            default: () => ({})
        }
    })

    const emit = defineEmits(['change', 'reset'])

    const allTableColumns = ref(generateColumnList(props.selectedColumnKeys, props.allTableColumnMap))
    watch(() => props.selectedColumnKeys, (newVal) => {
        allTableColumns.value = generateColumnList(newVal, props.allTableColumnMap)
    })

    function handleColumnCheck (id) {
        const col = allTableColumns.value.find(col => col.id === id)
        col.checked = !col.checked
    }
    function handleConfirm () {
        emit('change', allTableColumns.value.filter((col) => col.checked).map((col) => col.id))
    }
    function handleReset () {
        emit('reset')
    }
    function generateColumnList (checkedIds, allColumnMap) {
        const shownColumns = new Set(checkedIds)
        return [
            ...checkedIds.map((key) => ({
                ...allColumnMap[key],
                checked: true
            })),
            ...Object.values(allColumnMap).filter(col => !shownColumns.has(col.id)).map(col => ({
                ...col,
                checked: false
            }))
        ]
    }
</script>

<style lang="scss">
    .pipeline-table-column-setting {
        padding: 20px;
        width: 600px;
        h2 {
            font-size: 16px;
            font-weight: 500;
            margin: 0 0 20px 0;
        }
        .pipeline-table-column-ul {
            list-style: none;
            padding: 0;
            margin: 0;
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 20px;
            li {
                display: flex;
                align-items: center;
                justify-content: space-between;

                &:hover {
                    .column-drag-icon {
                        display: block;
                    }
                }
                .column-drag-icon {
                    cursor: move;
                    display: none;
                }
            }
        }
        > footer {
            margin-top: 20px;
        }
    }
</style>
