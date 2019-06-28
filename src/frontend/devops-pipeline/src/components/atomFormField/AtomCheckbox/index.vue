<template>
    <bk-checkbox :disabled="disabled" :title="title" :value="value" @change="handleSwitch">
        {{ text ? text : '是否选中' }}
        <bk-popover v-if="desc" placement="right">
            <i class="bk-icon icon-info-circle"></i>
            <div slot="content" style="white-space: pre-wrap;max-width: 300px">
                <div :class="{ 'disabled-color': disabled }"> {{ desc }} </div>
            </div>
        </bk-popover>
    </bk-checkbox>
</template>

<script>
    import atomFieldMixin from '../atomFieldMixin'
    export default {
        name: 'atom-checkbox',
        mixins: [atomFieldMixin],
        props: {
            value: {
                type: Boolean,
                required: true,
                default: false
            },
            text: {
                type: String
            },
            desc: {
                type: String,
                default: false
            },
            title: {
                type: String,
                default: false
            }
        },
        methods: {
            handleSwitch (checked) {
                const { name, handleChange } = this
                handleChange(name, checked)
            }
        }
    }
</script>

<style lang="scss">
    .atom-checkbox {
        display: flex;
        font-weight: bold;
        padding: 5px 20px 0 0;
    }
    .disabled-color {
        color: #ccc;
    }
</style>
