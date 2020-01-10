<template>
    <bk-dialog
        v-model="makeMirrorConf.isShow"
        :width="'640'"
        :close-icon="false"
        :ext-cls="'make-mirror-dialog'">
        <div
            v-bkloading="{
                isLoading: loading.isLoading,
                title: loading.title
            }">
            <div class="info-header">{{ $t('environment.makeMirror') }}</div>
            <bk-form class="make-mirror-form" v-if="makeMirrorConf.isShow"
                :label-width="100"
                :model="createForm"
                form-type="vertical">
                <devops-form-item :label="$t('environment.mirrorName')" :required="true" :property="'cnName'"
                    :is-error="errors.has('name')"
                    :error-msg="errors.first('name')">
                    <bk-input
                        class="meta-name-input"
                        :placeholder="$t('environment.mirrorNamePlaceholder')"
                        name="name"
                        v-model="createForm.name"
                        v-validate="{
                            required: true,
                            max: 128,
                            mirrorName: true
                        }">
                    </bk-input>
                </devops-form-item>
                <devops-form-item :label="$t('environment.mirrorTag')" :required="true" :property="'cnName'"
                    :is-error="errors.has('mirrorTag')"
                    :error-msg="errors.first('mirrorTag')">
                    <bk-input
                        class="meta-name-input"
                        :placeholder="$t('environment.mirrorTagPlaceholder')"
                        name="mirrorTag"
                        v-model="createForm.tag"
                        v-validate="{
                            required: true,
                            max: 127,
                            mirrorTag: true
                        }">
                    </bk-input>
                </devops-form-item>
                <devops-form-item :label="$t('environment.description')" :required="true" :property="'description'"
                    :is-error="errors.has('mirrorDesc')"
                    :error-msg="errors.first('mirrorDesc')">
                    <bk-input
                        class="meta-name-input"
                        :placeholder="$t('environment.descriptionPlaceholder')"
                        name="mirrorDesc"
                        v-model="createForm.description"
                        v-validate="{
                            required: true
                        }">
                    </bk-input>
                </devops-form-item>
            </bk-form>
        </div>
        <div slot="footer">
            <div class="footer-handler">
                <bk-button theme="primary" :disabled="loading.isLoading" @click="confirmFn">{{ $t('environment.comfirm') }}</bk-button>
                <bk-button theme="default" :disabled="loading.isLoading" @click="cancelFn">{{ $t('environment.cancel') }}</bk-button>
            </div>
        </div>
    </bk-dialog>
</template>

<script>
    export default {
        props: {
            makeMirrorConf: Object,
            currentNode: Object
        },
        data () {
            return {
                createForm: {
                    name: '',
                    tag: '',
                    description: ''
                },
                loading: {
                    title: '',
                    isLoading: false
                },
                mirrorNameRule: {
                    getMessage: field => this.$t('environment.mirrorNameRule'),
                    validate: value => /^[a-z0-9]([a-z0-9_.-]*[a-z0-9])*$/.test(value)
                },
                mirrorTagRule: {
                    getMessage: field => this.$t('environment.mirrorTagRule'),
                    validate: value => /^([a-zA-Z0-9_.])*$/.test(value)
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },
        watch: {
            'makeMirrorConf.isShow' (newVal) {
                this.createForm = {
                    name: '',
                    tag: '',
                    description: ''
                }
            }
        },
        mounted () {
            this.$nextTick(() => {
                this.$validator.extend('mirrorName', this.mirrorNameRule)
                this.$validator.extend('mirrorTag', this.mirrorTagRule)
            })
        },
        methods: {
            cancelFn () {
                this.$emit('cancelMakeMirror')
            },
            async confirmFn () {
                const valid = await this.$validator.validate()
                if (valid) {
                    const params = {
                        name: this.createForm.name,
                        tag: this.createForm.tag,
                        description: this.createForm.description
                    }
                    let message, theme
                    this.loading.isLoading = true
                    try {
                        const res = await this.$store.dispatch('environment/createImage', {
                            projectId: this.projectId,
                            nodeHashId: this.currentNode.nodeHashId,
                            params
                        })

                        if (res) {
                            message = this.$t('environment.successfullySubmited')
                            theme = 'success'
                            this.cancelFn()
                            this.$emit('submitMakeMirror')
                        }
                    } catch (err) {
                        message = err.message ? err.message : err
                        theme = 'error'
                    } finally {
                        this.$bkMessage({
                            message,
                            theme
                        })
                        this.loading.isLoading = false
                    }
                }
            }
        }
    }
</script>

<style lang="scss">
    @import './../../../scss/conf';
    .make-mirror-dialog {
        .bk-dialog-tool {
            display: none;
        }
        .info-header {
            padding: 20px 0 0;
            font-size: 18px;
            color: $fontBoldColor;
        }
        .make-mirror-form {
            padding:  10px 0 20px;
        }
    }
</style>
