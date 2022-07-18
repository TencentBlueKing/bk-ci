<template>
    <bk-sideslider :is-show.sync="show" :quick-close="true" @hidden="hidden" :width="622" :title="$t('setting.agent.addSelfPool')">
        <bk-form :model="formData" ref="poolForm" slot="content" class="pool-form" form-type="vertical">
            <bk-form-item :label="$t('setting.agent.poolName')" :required="true" :rules="[requireRule('Pool name'), nameRule]" property="name" error-display-type="normal">
                <bk-input v-model="formData.name" :placeholder="$t('setting.agent.poolNamePlaceholder')"></bk-input>
            </bk-form-item>
            <bk-form-item :label="$t('description')" property="desc">
                <bk-input type="textarea" v-model="formData.desc" :placeholder="$t('descriptionPlaceholder')"></bk-input>
            </bk-form-item>
            <bk-form-item>
                <bk-button ext-cls="mr5" theme="primary" @click.stop.prevent="submitData" :loading="isSaving">{{$t('submit')}}</bk-button>
                <bk-button ext-cls="mr5" @click="hidden" :disabled="isSaving">{{$t('cancel')}}</bk-button>
            </bk-form-item>
        </bk-form>
    </bk-sideslider>
</template>

<script>
    import { setting } from '@/http'
    import { mapState } from 'vuex'

    export default {
        props: {
            show: Boolean
        },

        data () {
            return {
                formData: {
                    name: '',
                    desc: '',
                    envType: 'BUILD',
                    source: 'EXISTING',
                    nodeHashIds: []
                },
                nameRule: {
                    validator: (val) => (/^[a-zA-Z0-9-]{1,64}$/.test(val)),
                    message: this.$t('setting.agent.poolNamePlaceholder'),
                    trigger: 'blur'
                },
                isSaving: false
            }
        },

        computed: {
            ...mapState(['projectId'])
        },

        watch: {
            show (val) {
                if (val) {
                    this.initData()
                }
            }
        },

        methods: {
            initData () {
                this.formData.name = ''
                this.formData.desc = ''
            },

            requireRule (name) {
                return {
                    required: true,
                    message: name + this.$t('isRequired'),
                    trigger: 'blur'
                }
            },

            submitData () {
                this.$refs.poolForm.validate(() => {
                    this.isSaving = true
                    setting.addEnvironment(this.projectId, this.formData).then(() => {
                        this.hidden()
                        this.$emit('refresh')
                        this.$bkMessage({ theme: 'success', message: 'Added successfully' })
                    }).catch((err) => {
                        this.$bkMessage({ theme: 'error', message: err.message || err })
                    }).finally(() => {
                        this.isSaving = false
                    })
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.content || err })
                })
            },

            hidden () {
                this.$emit('update:show', false)
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .pool-form {
        padding: 20px 30px;
        /deep/ button {
            margin: 8px 10px 0 0;
        }
    }
</style>
