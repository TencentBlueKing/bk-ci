<template>
    <section class="long-param-input">
        <template v-if="showCompact">
            <div class="long-param-input-compact">
                <span
                    class="long-param-input-compact-text"
                    :title="compactText"
                >
                    {{ compactText }}
                </span>
                <bk-button
                    text
                    size="small"
                    :disabled="isOverflowRef"
                    @click="showDetail = true"
                >
                    {{ $t('detail') }}
                </bk-button>
                <bk-button
                    text
                    size="small"
                    :disabled="disabled"
                    @click="handleClearAndReenter"
                >
                    {{ $t('details.longParamValueClearAndReenter') }}
                </bk-button>
            </div>
            <bk-sideslider
                quick-close
                :width="640"
                :title="$t('details.paramDetail')"
                :is-show.sync="showDetail"
            >
                <div
                    slot="content"
                    class="long-param-input-detail"
                >
                    <p>{{ name }}</p>
                    <pre>{{ value }}</pre>
                </div>
            </bk-sideslider>
        </template>
        <component
            v-else
            :is="inputComponent"
            v-bind="$attrs"
            :name="name"
            :value="value"
            :disabled="disabled"
            :placeholder="placeholder"
            :handle-change="handleChange"
            v-on="$listeners"
        />
    </section>
</template>

<script>
    import VuexInput from '@/components/atomFormField/VuexInput'
    import VuexTextarea from '@/components/atomFormField/VuexTextarea'
    import {
        getDisplayValueLength,
        isLongInputValue,
        isOverflowReference
    } from '@/utils/buildParamLongValue'

    export default {
        name: 'LongParamInput',
        components: {
            VuexInput,
            VuexTextarea
        },
        inheritAttrs: false,
        props: {
            name: {
                type: String,
                required: true
            },
            value: {
                type: [String, Number],
                default: ''
            },
            disabled: {
                type: Boolean,
                default: false
            },
            placeholder: {
                type: String,
                default: ''
            },
            handleChange: {
                type: Function,
                default: () => {}
            },
            // STRING -> VuexInput, TEXTAREA -> VuexTextarea
            inputType: {
                type: String,
                default: 'STRING'
            }
        },
        data () {
            return {
                showDetail: false,
                forceEdit: false
            }
        },
        computed: {
            isOverflowRef () {
                return isOverflowReference(this.value)
            },
            isLong () {
                return isLongInputValue(this.value)
            },
            showCompact () {
                // 清空后进入编辑态；引用串始终紧凑展示，避免把 __BK_OVF__ 填进 input
                if (this.isOverflowRef) return true
                return this.isLong && !this.forceEdit
            },
            compactText () {
                const length = getDisplayValueLength(this.value)
                if (this.isOverflowRef) {
                    return this.$t('details.longParamValueHiddenWithLength', [length || '--'])
                }
                return this.$t('details.longParamValueEntered', [length])
            },
            inputComponent () {
                return this.inputType === 'TEXTAREA' ? 'VuexTextarea' : 'VuexInput'
            }
        },
        watch: {
            value (val) {
                if (!isLongInputValue(val)) {
                    this.forceEdit = false
                }
            }
        },
        methods: {
            handleClearAndReenter () {
                this.forceEdit = true
                this.showDetail = false
                this.handleChange(this.name, '')
                this.$emit('input', '')
            }
        }
    }
</script>

<style lang="scss" scoped>
@import '@/scss/mixins/ellipsis';

.long-param-input {
    width: 100%;
}
.long-param-input-compact {
    display: flex;
    align-items: center;
    min-height: 32px;
    padding: 0 8px;
    border: 1px solid #c4c6cc;
    border-radius: 2px;
    background: #fafbfd;
    gap: 8px;
}
.long-param-input-compact-text {
    @include ellipsis();
    flex: 1;
    color: #979ba5;
    font-size: 12px;
}
</style>

<style lang="scss">
.long-param-input-detail {
    display: flex;
    flex-direction: column;
    padding: 24px;
    font-size: 12px;
    height: 100%;
    > p {
        flex-shrink: 0;
    }
    > pre {
        flex: 1;
        margin: 6px 0;
        padding: 6px 10px;
        background: #fafbfd;
        border: 1px solid #dcdee5;
        border-radius: 2px;
        overflow-y: auto;
        white-space: pre-wrap;
        word-wrap: break-word;
    }
}
</style>
