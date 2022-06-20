<template>
    <bk-sideslider :is-show.sync="show" :quick-close="true" @hidden="hidden" :width="622" :title="isEdit ? 'Edit experience group' : 'Add experience group'">
        <bk-form :model="formData" ref="groupForm" slot="content" class="group-form" form-type="vertical" :label-width="400">
            <bk-form-item label="Name" :required="true" :rules="[requireRule('Name'), nameRule]" property="name" error-display-type="normal">
                <bk-input v-model="formData.name" placeholder="No more than 10 characters"></bk-input>
            </bk-form-item>
            <bk-form-item label="users" property="innerUsers" error-display-type="normal">
                <bk-input
                    v-model="formData.innerUsers"
                    class="user-select-item"
                    style="height: 100px"
                    placeholder="Please enter users"
                ></bk-input>
            </bk-form-item>
            <bk-form-item label="Description" property="remark">
                <bk-input type="textarea" v-model="formData.remark" placeholder="Please enter a group description"></bk-input>
            </bk-form-item>
            <bk-form-item>
                <bk-button ext-cls="mr5" theme="primary" title="Submit" @click.stop.prevent="submitData" :loading="isLoading">Submit</bk-button>
                <bk-button ext-cls="mr5" title="Cancel" @click="hidden" :disabled="isLoading">Cancel</bk-button>
            </bk-form-item>
        </bk-form>
    </bk-sideslider>
</template>

<script>
    import { setting } from '@/http'
    import { mapState } from 'vuex'
    import LINK_CONFIG from '@/conf/link-config.js'

    export default {
        props: {
            show: Boolean,
            form: Object
        },
        data () {
            return {
                outersList: [],
                formData: {
                    name: 'PASSWORD',
                    innerUsers: '',
                    outerUsers: '',
                    remark: ''
                },
                isLoading: false,
                nameRule: {
                    validator: (val) => val.length <= 10,
                    message: 'no more than 10 words',
                    trigger: 'blur'
                },
                LINK_CONFIG
            }
        },

        computed: {
            ...mapState(['projectId']),

            isEdit () {
                return this.form.permissions
            }
        },

        watch: {
            show (val) {
                if (val) {
                    this.initData()
                    this.getOuterUserList()
                }
            }
        },

        methods: {
            initData () {
                const defaultForm = {
                    name: '',
                    innerUsers: [],
                    outerUsers: [],
                    remark: ''
                }
                this.formData = Object.assign(defaultForm, this.form)
            },

            requireRule (name) {
                return {
                    required: true,
                    message: name + ' is required',
                    trigger: 'blur'
                }
            },

            submitData () {
                this.$refs.groupForm.validate(() => {
                    let method = setting.createExpGroup
                    const params = [this.projectId, this.formData]
                    if (this.isEdit) {
                        method = setting.modifyExpGroup
                        params.push(this.formData.groupHashId)
                    }
                    this.isLoading = true
                    method(...params).then(() => {
                        const message = this.isEdit ? 'Edit successfully' : 'Added successfully'
                        this.$bkMessage({ theme: 'success', message })
                        this.$emit('success')
                    }).catch((err) => {
                        this.$bkMessage({ theme: 'error', message: err.message || err })
                    }).finally(() => {
                        this.isLoading = false
                    })
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.content || err })
                })
            },

            hidden () {
                this.$emit('hidden')
            },

            async getOuterUserList () {
                setting.getOuterUserList(this.projectId).then((res) => {
                    (res || []).forEach(item => {
                        this.outersList.push({
                            id: item.username,
                            name: item.username
                        })
                    })
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                })
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .group-form {
        padding: 20px 30px;
        /deep/ button {
            margin: 8px 10px 0 0;
        }
        .user-select-item {
            width: 100%;
        }
    }
</style>
