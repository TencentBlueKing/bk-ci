<template>
    <section class="long-param-input">
        <template v-if="showCompact">
            <div
                class="long-param-input-compact"
                :class="highlightClass"
            >
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
                    @click="openDetail"
                >
                    {{ $t('detail') }}
                </bk-button>
            </div>
            <bk-sideslider
                quick-close
                :width="640"
                :title="$t('details.paramDetail')"
                :is-show.sync="showDetail"
                :before-close="beforeCloseDetail"
            >
                <div
                    slot="content"
                    class="long-param-input-detail"
                >
                    <p>{{ name }}</p>
                    <bk-input
                        v-if="canEditDetail"
                        type="textarea"
                        v-model="draftValue"
                    />
                    <pre v-else>{{ value }}</pre>
                </div>
                <div
                    v-if="canEditDetail"
                    slot="footer"
                    class="long-param-input-detail-footer"
                >
                    <bk-button
                        theme="primary"
                        @click="saveDetail"
                    >
                        {{ $t('save') }}
                    </bk-button>
                    <bk-button @click="showDetail = false">
                        {{ $t('cancel') }}
                    </bk-button>
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
            :class="highlightClass"
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
            },
            isDiffParam: {
                type: Boolean,
                default: false
            },
            isChangeParam: {
                type: Boolean,
                default: false
            },
            isNewParam: {
                type: Boolean,
                default: false
            },
            isDeleteParam: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                showDetail: false,
                draftValue: '',
                detailSaved: false
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
                return this.isLong
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
            },
            canEditDetail () {
                return !this.disabled && !this.isOverflowRef
            },
            isDetailDirty () {
                return this.canEditDetail && this.draftValue !== String(this.value ?? '')
            },
            highlightClass () {
                return {
                    'is-diff-param': this.isDiffParam,
                    'is-change-param': this.isChangeParam,
                    'is-new-param': this.isNewParam,
                    'is-delete-param': this.isDeleteParam
                }
            }
        },
        methods: {
            openDetail () {
                this.draftValue = String(this.value ?? '')
                this.detailSaved = false
                this.showDetail = true
            },
            saveDetail () {
                this.detailSaved = true
                this.handleChange(this.name, this.draftValue)
                this.$emit('input', this.draftValue)
                this.showDetail = false
            },
            beforeCloseDetail () {
                if (this.detailSaved || !this.isDetailDirty) {
                    this.detailSaved = false
                    return true
                }
                return new Promise(resolve => {
                    this.$bkInfo({
                        title: this.$t('details.longParamUnsavedTitle'),
                        subTitle: this.$t('details.longParamUnsavedTips'),
                        confirmFn: () => {
                            this.draftValue = String(this.value ?? '')
                            resolve(true)
                        },
                        cancelFn: () => resolve(false)
                    })
                })
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
    > .bk-form-textarea {
        flex: 1;
        margin-top: 6px;
        min-height: 0;
        textarea {
            height: 100%;
            resize: none;
            font-family: monospace;
        }
    }
}
.long-param-input-detail-footer {
    padding: 0 24px;
    .bk-button + .bk-button {
        margin-left: 8px;
    }
}
</style>
