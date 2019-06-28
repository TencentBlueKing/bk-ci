<template>
    <form>
        <bk-radio-group @change="handleSelect" :value="value" :name="name">
            <bk-radio v-for="item in list" :key="item.id" class="bkdevops-radio" :value="item.value" v-bk-tooltips="{ content: item.tips || null }" :disabled="disabled || item.disabled">
                {{ item.label }}
            </bk-radio>
        </bk-radio-group>
    </form>
</template>

<script>
    import atomFieldMixin from '../atomFieldMixin'
    export default {
        name: 'enum-input',
        mixins: [atomFieldMixin],
        props: {
            list: {
                type: Array,
                default: []
            },
            value: [Number, Boolean, String]
        },
        methods: {
            handleSelect (value) {
                if (value === 'true') {
                    value = true
                } else if (value === 'false') {
                    value = false
                }
                this.handleChange(this.name, value)
            }
        }
    }
</script>

<style lang="scss" scoped>
    .bkdevops-radio {
        margin-right: 10px;
    }
</style>
