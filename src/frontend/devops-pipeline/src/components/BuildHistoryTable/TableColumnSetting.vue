<template>
    <section class="pipeline-table-column-setting">
        <h2>{{ $t('tableColSettings') }}</h2>
        <draggable class="pipeline-table-column-ul" v-model="allTableColumns" @change="handleSortColumn">
            <li v-for="col in allTableColumns" :key="col.id">
                <bk-checkbox
                    class="pipeline-table-column-column-checkbox"
                    :checked="shownColumns.has(col.id)"
                    @change="handleColumnCheck(col.id)"
                    :label="col.id"
                >
                    {{ $t(col.label) }}
                </bk-checkbox>
                <i class="devops-icon icon-drag column-drag-icon"></i>
            </li>
        </draggable>
        <footer>
            <bk-button theme="primary" @click="handleConfirm">{{$t('confirm')}}</bk-button>
            <bk-button @click="handleReset">{{$t('history.reset')}}</bk-button>
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
    console.log('props', props)
    const emit = defineEmits(['change', 'reset'])
    const shownColumns = ref(new Set(props.selectedColumnKeys))

    const allTableColumns = ref([
        ...props.selectedColumnKeys.map((key) => props.allTableColumnMap[key]),
        ...Object.values(props.allTableColumnMap).filter(col => !props.selectedColumnKeys.includes(col.id))
    ])

    watch(() => props.selectedColumnKeys, (newVal) => {
        shownColumns.value = new Set(newVal)
        allTableColumns.value = [
            ...newVal.map((key) => props.allTableColumnMap[key]),
            ...Object.values(props.allTableColumnMap).filter(col => !newVal.includes(col.id))
        ]
    })

    function handleColumnCheck (id) {
        if (shownColumns.value.has(id)) {
            shownColumns.value.delete(id)
        } else {
            shownColumns.value.add(id)
        }
    }
    function handleConfirm () {
        emit('change', allTableColumns.value.filter((col) => shownColumns.value.has(col.id)).map((col) => col.id))
    }
    function handleReset () {
        console.log('reset')
        emit('reset')
    }

    function handleSortColumn (...args) {
        console.log('handleSortColumn', allTableColumns.value.filter((col) => shownColumns.value.has(col.id)))
        // emit('change', allTableColumns.value.filter((col) => shownColumns.has(col.id)))
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
