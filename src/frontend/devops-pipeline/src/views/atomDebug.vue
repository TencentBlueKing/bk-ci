<template>
    <section class="preview-atom">
        <div class="preview-header"><p class="page-title">研发商店插件前端task.json在线调试</p></div>
        <main class="main-container">
            <div class="init-json">
                <textarea class="json-input" placeholder="请在此输入正确的task.json内容..." v-model="initJsonStr"></textarea>
            </div>
            <div class="transform-json">
                <json-viewer
                    :value="initJson"
                    :expand-depth="5"
                ></json-viewer>
            </div>
            <div class="preview-atom">
                <preview-atom
                    :atom-value="atomInputValue"
                    :output-value="outputValue"
                    :atom-props-model="initJson"
                    :handle-update-preview-input="handleChangePreviewInput"
                    class="atom-content">
                </preview-atom>
            </div>
        </main>
    </section>
</template>

<script>
    import Vue from 'vue'
    import JsonViewer from 'vue-json-viewer'
    import PreviewAtom from '@/components/AtomPropertyPanel/PreviewAtom'
    import { getAtomOutputObj, getAtomDefaultValue } from '@/store/modules/atom/atomUtil'
    Vue.use(JsonViewer)

    export default {
        components: {
            PreviewAtom
        },
        data () {
            return {
                initJsonStr: '',
                atomInputValue: {},
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
                            return `error: cannot transform ${this.initJsonStr} into json`
                        }
                    } catch (e) {
                        return 'error: ' + e
                    }
                }
                return {}
            }
            // initElement () {
            //     return {
            //         data: {
            //             input: getAtomDefaultValue(this.initJson.input),
            //             output: getAtomOutputObj(this.initJson.output)
            //         }
            //     }
            // },
            // atomInputValue () {
            //     return getAtomDefaultValue(this.initJson.input)
            // },
            // outputValue () {
            //     return getAtomOutputObj(this.initJson.outputValue)
            // }
        },
        watch: {
            // initElement: {
            //     handler (val) {
            //         this.dataValue = val
            //     },
            //     immediate: true
            // }
            initJson: {
                handler (val) {
                    this.atomInputValue = getAtomDefaultValue(this.initJson.input)
                    this.outputValue = getAtomOutputObj(this.initJson.outputValue)
                },
                immediate: true
            }
        },
        methods: {
            handleChangePreviewInput (name, value) {
                Vue.set(this.atomInputValue, name, value)
            },
            handleChangePreviewOutput () {

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
