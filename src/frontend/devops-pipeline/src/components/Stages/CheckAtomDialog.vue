<template>
    <bk-dialog
        v-model="isShowCheckDialog"
        ext-cls="check-atom-form"
        :close-icon="false"
        :width="600"
        :position="{ top: '100' }"
        :auto-close="false"
        @confirm="handleAtomCheck"
        @cancel="toggleCheck(false)">
        <div v-bkloading="{ isLoading }" class="pipeline-template">
            <bk-form :label-width="100" form-type="vertical">
                <bk-form-item v-if="data.desc" label="审核描述">
                    <p style="white-space: pre-wrap;">{{data.desc}}</p>
                </bk-form-item>
                <bk-form-item label="审核结果">
                    <bk-radio-group v-model="data.status">
                        <bk-radio class="choose-item" :value="'PROCESS'">同意</bk-radio>
                        <bk-radio class="choose-item" :value="'ABORT'">驳回</bk-radio>
                    </bk-radio-group>
                    <bk-input type="textarea" v-model="data.suggest" placeholder="请输入审核意见" class="check-suggest"></bk-input>
                </bk-form-item>
                <bk-form-item label="自定义变量" v-if="data.status === 'PROCESS' && data.params && data.params.length">
                    <key-value-normal :value="data.params" :edit-value-only="true"></key-value-normal>
                </bk-form-item>
            </bk-form>
        </div>
    </bk-dialog>
</template>

<script>
    import { mapActions } from 'vuex'
    import KeyValueNormal from '@/components/atomFormField/KeyValueNormal'
    export default {
        name: 'check-atom-dialog',
        components: {
            KeyValueNormal
        },
        props: {
            atom: {
                type: Object,
                default: () => ({})
            },
            isShowCheckDialog: {
                type: Boolean,
                default: false
            },
            toggleCheck: {
                type: Function,
                required: true
            }
        },

        data () {
            return {
                isLoading: true,
                data: {
                    status: 'PROCESS',
                    suggest: '',
                    desc: '',
                    params: []
                }
            }
        },
        computed: {
            routerParams () {
                return this.$route.params
            }
        },
        watch: {
            'isShowCheckDialog': function (val) {
                if (val) {
                    this.requestCheckData()
                }
            }
        },
        methods: {
            ...mapActions('atom', [
                'getCheckAtomInfo',
                'handleCheckAtom'
            ]),
            async requestCheckData () {
                try {
                    this.isLoading = true
                    const postData = {
                        projectId: this.routerParams.projectId,
                        pipelineId: this.routerParams.pipelineId,
                        buildId: this.routerParams.buildNo,
                        elementId: this.atom.id
                    }
                    const res = await this.getCheckAtomInfo(postData)
                    this.data = Object.assign(res, { status: 'PROCESS' })
                } catch (err) {
                    this.$showTips({
                        theme: 'error',
                        message: err.message || err
                    })
                } finally {
                    this.isLoading = false
                }
            },
            async handleAtomCheck () {
                try {
                    const data = {
                        projectId: this.routerParams.projectId,
                        pipelineId: this.routerParams.pipelineId,
                        buildId: this.routerParams.buildNo,
                        elementId: this.atom.id,
                        postData: this.data
                    }
                    const res = await this.handleCheckAtom(data)
                    if (res === true) {
                        this.$showTips({
                            message: this.data.status === 'ABORT' ? '驳回成功' : '审核成功',
                            theme: 'success'
                        })
                        // this.requestPipelineExecDetail(this.routerParams)
                    }
                    this.toggleCheck(false)
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                }
            }
        }
    }
</script>

<style lang="scss">
    .check-atom-form {
        .choose-item {
            margin-right: 30px;
        }
        .check-suggest {
            margin-top: 10px;
        }
    }
</style>
