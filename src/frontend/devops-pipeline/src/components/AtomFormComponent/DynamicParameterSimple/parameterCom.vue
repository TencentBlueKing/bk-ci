<template>
    <section class="param-input-home">
        <section class="parameter-input">
            <bk-input
                v-if="type === 'input'"
                class="input-main"
                :clearable="!disabled"
                :disabled="disabled"
                :value="value"
                :placeholder="placeholder"
                @change="(newValue) => $emit('update-value', newValue)"
            />
            <section
                v-else
                class="parameter-select input-main"
            >
                <bk-select
                    :disabled="disabled"
                    v-model="value"
                    :placeholder="placeholder"
                    @change="(newValue) => $emit('update-value', newValue)"
                    ext-cls="select-custom"
                    ext-popover-cls="select-popover-custom"
                    searchable
                >
                    <bk-option
                        v-for="option in options"
                        :key="option.id"
                        :id="option.id"
                        :name="option.name"
                    >
                    </bk-option>
                </bk-select>
            </section>
        </section>
    </section>
</template>

<script>
    import mixins from '../mixins'

    export default {
        mixins: [mixins],

        props: {
            id: {
                type: String
            },
            type: {
                type: String
            },
            value: {
                type: [String, Array]
            },
            disabled: {
                type: Boolean,
                default: false
            },
            options: {
                type: Array
            },
            placeholder: {
                type: String,
                default: ''
            }
        }
    }
</script>

<style lang="scss" scoped>
    .param-input-home {
        display: flex;
        align-items: flex-end;
        flex: 1;
        .param-hyphen {
            margin-right: 11px;
        }
    }
    .parameter-input {
        flex: 1;
        .input-label {
            max-width: 100%;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
        .input-main {
            flex: 1;
        }
    }
    .parameter-select {
        position: relative;
        .parameter-list {
            position: absolute;
            top: 32px;
            left: 0;
            right: 0;
            padding: 6px 0;
            list-style: none;
            border: 1px solid #dcdee5;
            border-radius: 2px;
            line-height: 32px;
            background: #fff;
            color: #63656e;
            overflow: auto;
            max-height: 216px;
            min-width: 100px;
            z-index: 2;
            li {
                padding: 0 16px;
                position: relative;
                &:hover {
                    color: #3a84ff;
                    background-color: #eaf3ff;
                }
            }
            .is-active {
                color: #3a84ff;
                background-color: #f4f6fa;
                &:after {
                    content: '';
                    position: absolute;
                    right: 16px;
                    top: 11px;
                    height: 7px;
                    width: 3px;
                    transform: rotate(45deg);
                    border-bottom: 1px solid #3a84ff;
                    border-right: 1px solid #3a84ff;
                }
            }
        }
    }
</style>
