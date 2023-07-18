<template>
    <bk-sideslider :is-show.sync="show" :quick-close="true" :before-close="hidden" :width="622" :title="isEdit ? $t('setting.userGroup.editGroup') : $t('setting.userGroup.addGroup')">
        <bk-form :model="formData" ref="groupForm" slot="content" class="group-form" form-type="vertical" :label-width="400">
            <bk-form-item :label="$t('name')" :required="true" :rules="[requireRule('Name'), nameRule]" property="name" error-display-type="normal">
                <bk-input v-model="formData.name" @change="handleChangeForm" placeholder="No more than 10 characters"></bk-input>
            </bk-form-item>
            <bk-form-item :label="$t('setting.userGroup.innerUsers')" property="innerUsers" error-display-type="normal">
                <bk-member-selector
                    v-model="formData.innerUsers"
                    @change="handleChangeForm"
                    class="user-select-item"
                    style="height: 100px"
                    :placeholder="$t('setting.userGroup.innerUsersPlaceholder')"
                >
                </bk-member-selector>
            </bk-form-item>
            <bk-form-item :label="$t('setting.userGroup.outerUsers')" property="outerUsers" :desc="{ content: 'Non-OA users 为外部协作用户，需要在蓝鲸用户管理先注册用户，指引参考：https://iwiki.woa.com/pages/viewpage.action?pageId=1556183163', width: '400px' }">
                <bk-select
                    v-model="formData.outerUsers"
                    @change="handleChangeForm"
                    ext-cls="select-custom"
                    ext-popover-cls="select-popover-custom"
                    placeholder="Please select"
                    multiple
                    searchable>
                    <bk-option v-for="option in outersList"
                        :key="option.id"
                        :id="option.id"
                        :name="option.name">
                    </bk-option>
                </bk-select>
            </bk-form-item>
            <bk-form-item :label="$t('description')" property="remark">
                <bk-input type="textarea" v-model="formData.remark" :placeholder="$t('descriptionPlaceholder')" @change="handleChangeForm"></bk-input>
            </bk-form-item>
            <bk-form-item>
                <bk-button ext-cls="mr5" theme="primary" title="Submit" @click.stop.prevent="submitData" :loading="isLoading">{{$t('submit')}}</bk-button>
                <bk-button ext-cls="mr5" title="Cancel" @click="hidden" :disabled="isLoading">{{$t('cancel')}}</bk-button>
            </bk-form-item>
        </bk-form>
    </bk-sideslider>
</template>

<script>
    import { setting } from '@/http'
    import { mapState } from 'vuex'

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
                }
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
                    message: name + this.$t('isRequired'),
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
                if (window.changeFlag) {
                    this.$bkInfo({
                        title: this.$t('确认离开当前页？'),
                        subHeader: this.$createElement('p', {
                            style: {
                                color: '#63656e',
                                fontSize: '14px',
                                textAlign: 'center'
                            }
                        }, this.$t('离开将会导致未保存信息丢失')),
                        confirmFn: () => {
                            window.changeFlag = false
                            this.$emit('hidden')
                        }
                    })
                } else {
                    this.$emit('hidden')
                }
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
            },

            handleChangeForm () {
                window.changeFlag = true
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
