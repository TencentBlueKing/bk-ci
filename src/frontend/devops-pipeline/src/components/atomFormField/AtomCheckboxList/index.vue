<template>
    <bk-checkbox-group :value="value" @change="handleSwitch">
        <bk-checkbox v-for="item in list" :key="item.value" :value="item.id" :disabled="item.disabled" v-bk-tooltips="{ content: item.desc, width: 300 }" class="atom-checkbox-list-item">
            {{ item.name }}
            <sup v-if="item.showIntro" class="introSup"> 荐</sup>
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
                default: []
            },
            value: {
                type: Array,
                default: []
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
        font-weight: bold;
        overflow: hidden;
        text-overflow:ellipsis;
        white-space: nowrap;
    }
    .introSup {
        color: red;
        font-weight: normal;
    }
    .underline-text {
        border-bottom: dashed 1px #c3cdd7;
        padding-bottom: 3px;
    }
    .disabled-color {
        color: #ccc;
    }
</style>
