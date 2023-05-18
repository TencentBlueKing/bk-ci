<template>
    <article class="private-setting">
        <h5 class="private-header">
            <bk-button theme="primary" @click="handleAdd">{{ $t('store.新增配置') }}</bk-button>
        </h5>

        <section v-bkloading="{ isLoading }" class="g-scroll-table">
            <bk-table :data="privateList" :outer-border="false" :header-border="false" :header-cell-style="{ background: '#fff' }" v-if="!isLoading">
                <bk-table-column :label="$t('store.名称')" prop="fieldName" width="180" show-overflow-tooltip></bk-table-column>
                <bk-table-column :label="$t('store.适用范围')" show-overflow-tooltip>
                    <template slot-scope="props">
                        {{ getTypeName(props.row.fieldType) }}
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('store.描述')" prop="fieldDesc" show-overflow-tooltip></bk-table-column>
                <bk-table-column :label="$t('store.修改者')" prop="modifier" width="180" show-overflow-tooltip></bk-table-column>
                <bk-table-column :label="$t('store.修改时间')" prop="updateTime" width="180" show-overflow-tooltip></bk-table-column>
                <bk-table-column :label="$t('store.操作')" width="120" class-name="handler-btn">
                    <template slot-scope="props">
                        <span class="update-btn" @click="handleEdit(props.row)"> {{ $t('store.编辑') }} </span>
                        <span class="update-btn" @click="handleDelete(props.row, props.$index)"> {{ $t('store.删除') }} </span>
                    </template>
                </bk-table-column>
            </bk-table>

            <bk-sideslider :is-show.sync="showAdd" :quick-close="true" :title="$t('store.新增配置')" :width="640" :before-close="closeAddPrivate">
                <bk-form :label-width="120" :model="privateObj" slot="content" class="add-private" ref="privateForm">
                    <bk-form-item :label="$t('store.字段名')" :required="true" :rules="[requireRule($t('store.字段名')), nameRule]" property="fieldName" error-display-type="normal">
                        <bk-input v-model="privateObj.fieldName" :placeholder="$t('store.请输入字段名称，不超过30个字符')" @change="handleChangeForm"></bk-input>
                    </bk-form-item>
                    <bk-form-item :label="$t('store.字段值')" :rules="[requireRule($t('store.字段值'))]" :required="true" property="fieldValue" error-display-type="normal">
                        <bk-input type="textarea" :rows="3" v-model="privateObj.fieldValue" @focus="handlePrivateFocus" :placeholder="$t('store.请输入字段值')" @change="handleChangeForm"></bk-input>
                    </bk-form-item>
                    <bk-form-item :label="$t('store.适用范围')" property="fieldType" :desc="$t('store.适用范围选择为“全部”或“前端”时，字段值将明文返回给插件前端，请谨慎设置')" :desc-type="'icon'">
                        <bk-radio-group v-model="privateObj.fieldType" @change="handleChangeForm" class="radio-group">
                            <bk-radio :value="type.value" v-for="(type, key) in fieldTypeList" :key="key" style="margin-right: 10px;">{{type.label}}</bk-radio>
                        </bk-radio-group>
                    </bk-form-item>
                    <bk-form-item :label="$t('store.描述')" property="fieldDesc">
                        <bk-input type="textarea" :rows="3" v-model="privateObj.fieldDesc" :maxlength="256" :placeholder="$t('store.请输入描述')" @change="handleChangeForm"></bk-input>
                    </bk-form-item>
                    <bk-form-item>
                        <bk-button theme="primary" @click="savePrivate" :loading="isSaving">{{ $t('store.保存') }}</bk-button>
                        <bk-button @click="closeAddPrivate" :disabled="isSaving">{{ $t('store.取消') }}</bk-button>
                    </bk-form-item>
                </bk-form>
            </bk-sideslider>
        </section>

        <bk-dialog v-model="deleteObj.show"
            :loading="deleteObj.loading"
            @confirm="requestDelete"
            @cancel="deleteObj.show = false"
            :title="$t('store.删除')"
        >
            {{`${$t('store.确定删除')}(${deleteObj.name})？`}}
        </bk-dialog>
    </article>
</template>

<script>
    import { mapGetters, mapActions } from 'vuex'

    export default {
        data () {
            return {
                privateList: [],
                isLoading: true,
                isSaving: false,
                showAdd: false,
                privateId: '',
                hasClearPrivate: false,
                privateObj: {
                    fieldName: '',
                    fieldValue: '',
                    fieldType: 'BACKEND',
                    fieldDesc: ''
                },
                fieldTypeList: [
                    {
                        label: this.$t('store.后端'),
                        value: 'BACKEND'
                    },
                    {
                        label: this.$t('store.前端'),
                        value: 'FRONTEND'
                    },
                    {
                        label: this.$t('store.全部'),
                        value: 'ALL'
                    }
                ],
                deleteObj: {
                    show: false,
                    loading: false,
                    name: '',
                    id: '',
                    index: ''
                },
                nameRule: {
                    validator: (val) => (/^[a-zA-Z][a-zA-Z0-9-_]{2,29}$/.test(val)),
                    message: this.$t('store.以英文字母开头，由英文字母、数字、连接符(-)或下划线(_)组成，长度大于3小于30个字符'),
                    trigger: 'blur'
                }
            }
        },

        computed: {
            ...mapGetters('store', {
                detail: 'getDetail'
            })
        },

        created () {
            this.initData()
        },

        methods: {
            ...mapActions('store', ['getSensitiveConf', 'deleteSensitiveConf', 'addSensitiveConf', 'modifySensitiveConf']),

            requireRule (name) {
                return {
                    required: true,
                    message: this.$t('store.validateMessage', [name, this.$t('store.必填项')]),
                    trigger: 'blur'
                }
            },

            initData () {
                this.isLoading = true
                this.getSensitiveConf(this.detail.atomCode).then((res) => {
                    this.privateList = res || []
                }).catch(err => this.$bkMessage({ message: err.message || err, theme: 'error' })).finally(() => {
                    this.isLoading = false
                })
            },

            closeAddPrivate () {
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
                        okText: this.$t('离开'),
                        confirmFn: () => {
                            this.privateId = ''
                            this.hasClearPrivate = false
                            this.privateObj = {
                                fieldName: '',
                                fieldValue: '',
                                fieldType: 'BACKEND',
                                fieldDesc: ''
                            }
                            setTimeout(() => {
                                this.showAdd = false
                            })
                            return true
                        }
                    })
                } else {
                    this.showAdd = false
                    this.privateId = ''
                    this.hasClearPrivate = false
                    this.privateObj = {
                        fieldName: '',
                        fieldValue: '',
                        fieldType: 'BACKEND',
                        fieldDesc: ''
                    }
                }
            },

            handlePrivateFocus () {
                if (this.privateId !== '' && !this.hasClearPrivate && this.privateObj.fieldType === 'BACKEND') {
                    this.privateObj.fieldValue = ''
                    this.hasClearPrivate = true
                }
            },

            savePrivate () {
                this.$refs.privateForm.validate().then(() => {
                    const data = {
                        atomCode: this.detail.atomCode,
                        id: this.privateId,
                        postData: this.privateObj
                    }

                    let method = this.addSensitiveConf
                    if (this.privateId !== '') method = this.modifySensitiveConf
                    this.isSaving = true
                    method(data).then(() => {
                        this.initData()
                        this.privateId = ''
                        this.hasClearPrivate = false
                        this.privateObj = {
                            fieldName: '',
                            fieldValue: '',
                            fieldType: 'BACKEND',
                            fieldDesc: ''
                        }
                        setTimeout(() => {
                            this.showAdd = false
                        })
                    }).catch((err) => {
                        this.$bkMessage({ message: (err.message || err), theme: 'error' })
                    }).finally(() => {
                        this.isSaving = false
                    })
                }, (validator) => {
                    this.$bkMessage({ message: validator.content || validator, theme: 'error' })
                })
            },

            handleEdit (row) {
                this.privateId = row.fieldId
                this.privateObj.fieldName = row.fieldName
                this.privateObj.fieldType = row.fieldType || 'BACKEND'
                this.privateObj.fieldValue = row.fieldValue
                this.privateObj.fieldDesc = row.fieldDesc
                this.showAdd = true
            },

            getTypeName (type = 'BACKEND') {
                const item = this.fieldTypeList.find(item => item.value === type)
                return (item && item.label) || this.$t('store.后端')
            },

            handleDelete ({ fieldId, fieldName }, index) {
                this.deleteObj.show = true
                this.deleteObj.name = fieldName
                this.deleteObj.id = fieldId
                this.deleteObj.index = index
            },

            requestDelete () {
                const id = this.deleteObj.id
                this.deleteObj.loading = true
                const data = { atomCode: this.detail.atomCode, id }
                this.deleteSensitiveConf(data).then(() => {
                    this.privateList.splice(this.deleteObj.index, 1)
                    this.$bkMessage({ message: this.$t('store.删除成功'), theme: 'success' })
                }).catch((err) => {
                    this.$bkMessage({ message: (err.message || err), theme: 'error' })
                }).finally(() => {
                    this.deleteObj.loading = false
                    this.deleteObj.show = false
                })
            },

            handleChangeForm () {
                window.changeFlag = true
            },
             
            handleAdd () {
                window.changeFlag = false
                this.showAdd = true
            }
        }
    }
</script>

<style lang="scss" scoped>
    .private-setting {
        background: #fff;
        padding: 3.2vh;
        .private-header {
            margin-bottom: 3.2vh;
            color: #666;
            font-size: 14px;
            font-weight: normal;
            button {
                margin-right: 14px;
            }
        }
        .add-private {
            padding: 32px;
        }
    }
</style>
