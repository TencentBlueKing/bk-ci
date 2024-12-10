<template>
    <section class="preview-atom">
        <div class="preview-header"><p class="page-title">{{ $t('atomDebug.title') }}</p></div>
        <main class="main-container">
            <div class="init-json">
                <textarea
                    class="json-input"
                    :placeholder="$t('atomDebug.inputTips')"
                    v-model="initJsonStr"
                ></textarea>
            </div>
            <div class="transform-json">
                <json-viewer
                    :value="initJson"
                    :expand-depth="5"
                ></json-viewer>
            </div>
            <div
                class="preview-atom"
            >
                <preview-atom
                    v-if="initJson"
                    :atom-props-model="initJson"
                    :element="element"
                    :error-tips="errorTips"
                    class="atom-content"
                >
                </preview-atom>
            </div>
        </main>
    </section>
</template>

<script>
    import Vue from 'vue'
    import JsonViewer from 'vue-json-viewer'
    import PreviewAtom from '@/components/AtomPropertyPanel/PreviewAtom'
    import { getAtomDefaultValue } from '@/store/modules/atom/atomUtil'

    export default {
        components: {
            PreviewAtom,
            JsonViewer
        },
        data () {
            return {
                initJsonStr: '',
                element: {
                    data: {
                        input: {}
                    }
                },
                errorTips: this.$t('atomDebug.taskJsonEmpty'),
                outputValue: {}
            }
        },
        computed: {
            initJson () {
                if (this.initJsonStr) {
                    try {
                        const obj = JSON.parse(this.initJsonStr)
                        if (typeof obj === 'object' && obj) {
                            return obj
                        } else {
                            this.errorTips = `error: cannot transform ${this.initJsonStr} into json`
                            return {}
                        }
                    } catch (e) {
                        this.errorTips = 'error: ' + e
                        return {}
                    }
                }
                return {}
            }
        },
        watch: {
            '$route.params.projectId' (val) {
                this.$nextTick(this.resetValue)
            },
            initJson: {
                handler (val) {
                    this.$nextTick(this.resetValue)
                },
                immediate: true
            }
        },
        methods: {
            resetValue () {
                Vue.set(this.element, 'data', {
                    input: getAtomDefaultValue(this.initJson.input)
                })
            }
        }
    }
</script>

<style lang='scss'>
    @import './../scss/conf';
    .preview-atom {
        width: 100%;
        height: calc(100% - 55px);
        background-color: #fff;
        .preview-header {
            height: 50px;
            border: solid 1px #E5EBEE;
            .page-title {
                font-weight: bold;
                line-height: 50px;
                margin-left: 15px;
            }
        }
        .main-container {
            width: 98%;
            height: 100%;
            display:flex;
            overflow: hidden;
            margin: 0 auto;
            > div {
                // border-top: solid 1px #E5EBEE;
                border-right: solid 1px #E5EBEE;
                overflow: auto;
            }
            .init-json {
                flex: 3;
                overflow: hidden;
                margin-top: 10px;
                .json-input {
                    border: 0px;
                    // margin: 10px;
                    width: 100%;
                    height: 100%;
                    min-height: 600px;
                }
            }
            .transform-json {
                flex: 3;
            }
            .preview-atom {
                flex: 4;
                padding: 20px;
                border-right: 0px;
                .atom-content {
                    margin-bottom: 20px;
                    .empty-tips {
                        text-align: center;
                        font-size: 14px;
                        a {
                            color: $primaryColor;
                        }
                    }
                }
            }
        }
    }
</style>
