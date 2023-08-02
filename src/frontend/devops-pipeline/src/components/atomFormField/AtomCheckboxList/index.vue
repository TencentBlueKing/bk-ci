<template>
    <bk-checkbox-group :key="value" :value="value" @change="handleSwitch">
        <bk-checkbox v-for="item in list" :key="item.id" :value="item.id" :disabled="item.disabled" class="atom-checkbox-list-item">
            <span v-if="item.desc" v-bk-tooltips="{ content: item.desc, width: 300, allowHTML: false }" :class="{ 'disabled-color': item.disabled }">{{ item.name }}</span>
            <span v-else :class="{ 'disabled-color': item.disabled }">{{ item.name }}</span>
        </bk-checkbox>
    </bk-checkbox-group>
</template>

<script>
    import atomFieldMixin from '../atomFieldMixin'
    export default {
        name: 'atom-checkbox-list',
        mixins: [atomFieldMixin],
        props: {
            list: {
                type: Array,
                default: () => []
            },
            value: {
                type: Array,
                default: () => []
            }
        },
        methods: {
            handleSwitch (value) {
                const { name, handleChange } = this
                this.$emit('input', value)
                handleChange(name, value)
            }
        }
    }
</script>

<style lang="scss">
    .atom-checkbox-list-item {
        width: 170px;
        padding: 0 20px 10px 0;
        overflow: hidden;
        text-overflow:ellipsis;
        white-space: nowrap;
    }
    .underline-text {
        border-bottom: dashed 1px #c3cdd7;
        padding-bottom: 3px;
    }
    .disabled-color {
        color: #ccc;
    }
</style>
