<template>
    <bk-checkbox
        :disabled="disabled"
        :title="title"
        :value="value"
        @change="handleSwitch"
    >
        {{ text ? text : $t('editPage.checkOrNot') }}
        <bk-popover
            v-if="desc"
            placement="top"
        >
            <i
                @click.stop
                class="bk-icon icon-info-circle"
            ></i>
            <div
                slot="content"
                style="white-space: pre-wrap;max-width: 300px"
            >
                <div
                    v-if="!customTip"
                    :class="{ 'checkbox-tips-content': true,'disabled-color': disabled }"
                >
                    {{ desc }}
                </div>
                <div
                    v-else
                    class="checkbox-tips-content"
                >
                    <p
                        v-for="(tip, index) in desc"
                        :key="index"
                    >
                        {{ tip }}
                    </p>
                </div>
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
                type: [String, Array],
                default: ''
            },
            disabled: {
                type: Boolean,
                default: false
            },
            customTip: {
                type: Boolean,
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
    .checkbox-tips-content {
        white-space: normal;
    }
</style>
