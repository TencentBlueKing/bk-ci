<template>
    <article class="manage-environment">
        <header class="environment-head">
            <bk-button theme="primary" @click="addEnv">{{ $t('store.新增环境变量') }}</bk-button>
            <bk-select v-model="envScope" class="head-item">
                <bk-option v-for="option in scopesList"
                    :key="option.id"
                    :id="option.id"
                    :name="option.name">
                </bk-option>
            </bk-select>
            <section :class="[{ error: searchError }, 'head-item']">
                <bk-input class="head-input"
                    v-model="envName"
                    right-icon="bk-icon icon-search"
                    :clearable="true"
                    :placeholder="$t('store.请输入名称，按回车搜索')"
                ></bk-input>
                <span class="err-info">{{ $t('store.以大写字母开头，包含大写字母、下划线或数字') }}</span>
            </section>
        </header>

        <main v-bkloading="{ isLoading }" class="g-scroll-table">
            <bk-table v-if="!isLoading"
                :data="envList"
                :outer-border="false"
                :header-border="false"
                :header-cell-style="{ background: '#fff' }"
            >
                <bk-table-column :label="$t('store.变量名')" prop="varName"></bk-table-column>
                <bk-table-column :label="$t('store.变量值')" prop="varValue"></bk-table-column>
                <bk-table-column :label="$t('store.备注')" prop="varDesc"></bk-table-column>
                <bk-table-column :label="$t('store.生效范围')" prop="scope" :formatter="convertScope"></bk-table-column>
                <bk-table-column :label="$t('store.是否加密')" prop="encryptFlag" :formatter="convertEncryptFlag"></bk-table-column>
                <bk-table-column :label="$t('store.操作')" width="220" class-name="handler-btn">
                    <template slot-scope="props">
                        <span class="environment-btn" @click="editEnv(props.row)"> {{ $t('store.编辑') }} </span>
                        <span class="environment-btn" @click="deleteEnv(props.row)"> {{ $t('store.删除') }} </span>
                        <span class="environment-btn" @click="showHistory(props.row)"> {{ $t('store.变更历史') }} </span>
                    </template>
                </bk-table-column>
            </bk-table>
        </main>

        <bk-sideslider :is-show.sync="addEnvObj.show" :quick-close="true" :title="addEnvObj.title" :width="640" @hidden="closeAddEnv">
            <bk-form :label-width="100" :model="addEnvObj.form" slot="content" class="add-env" ref="envForm">
                <bk-form-item :label="$t('store.变量名')" :required="true" :rules="[requireRule($t('store.变量名')), numMax(20), nameRule]" property="varName" error-display-type="normal">
                    <bk-input v-model="addEnvObj.form.varName" :placeholder="$t('store.以大写字母开头，包含大写字母、下划线或数字')"></bk-input>
                </bk-form-item>
                <bk-form-item :label="$t('store.变量值')" :rules="[requireRule($t('store.变量值'))]" :required="true" property="varValue" error-display-type="normal">
                    <bk-input type="textarea" :rows="3" v-model="addEnvObj.form.varValue" :placeholder="$t('store.请输入变量值')"></bk-input>
                </bk-form-item>
                <bk-form-item :label="$t('store.生效范围')" property="varValue">
                    <bk-select v-model="addEnvObj.form.scope" :clearable="false">
                        <bk-option v-for="option in scopesList"
                            :key="option.id"
                            :id="option.id"
                            :name="option.name">
                        </bk-option>
                    </bk-select>
                </bk-form-item>
                <bk-form-item :label="$t('store.备注')" property="varDesc">
                    <bk-input type="textarea" :rows="3" v-model="addEnvObj.form.varDesc" :placeholder="$t('store.请输入备注')"></bk-input>
                </bk-form-item>
                <bk-form-item :label="$t('store.是否加密')" property="encryptFlag">
                    <bk-select v-model="addEnvObj.form.encryptFlag" :clearable="false">
                        <bk-option v-for="option in encryptList"
                            :key="option.id"
                            :id="option.id"
                            :name="option.name">
                        </bk-option>
                    </bk-select>
                </bk-form-item>
                <bk-form-item>
                    <bk-button theme="primary" @click="saveEnv" :loading="isSaving">{{ $t('store.确定') }}</bk-button>
                    <bk-button @click="closeAddEnv" :disabled="isSaving">{{ $t('store.取消') }}</bk-button>
                </bk-form-item>
            </bk-form>
        </bk-sideslider>

        <bk-sideslider :is-show.sync="envHistory.show" :quick-close="true" :title="`${$t('store.变更历史')}（${envHistory.name}）`" :width="640">
            <bk-table slot="content"
                class="environment-history"
                :data="envHistory.list"
                v-bkloading="{ isLoading: envHistory.isLoading }"
                :outer-border="false"
                :header-border="false"
                :header-cell-style="{ background: '#fff' }"
            >
                <bk-table-column :label="$t('store.变更前')" prop="beforeVarValue"></bk-table-column>
                <bk-table-column :label="$t('store.变更后')" prop="afterVarValue"></bk-table-column>
                <bk-table-column :label="$t('store.变更人')" prop="modifier"></bk-table-column>
                <bk-table-column :label="$t('store.变更时间')" prop="updateTime" :formatter="convertTime" width="210"></bk-table-column>
            </bk-table>
        </bk-sideslider>

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
    import { mapGetters } from 'vuex'
    import { convertTime } from '@/utils/index'
    import api from '@/api'

    export default {
        data () {
            return {
                envScope: '',
                envName: '',
                envList: [],
                isLoading: false,
                isSaving: false,
                searchError: false,
                scopesList: [
                    { id: 'ALL', name: this.$t('store.所有') },
                    { id: 'TEST', name: this.$t('store.测试') },
                    { id: 'PRD', name: this.$t('store.正式') }
                ],
                encryptList: [
                    { id: true, name: this.$t('store.是') },
                    { id: false, name: this.$t('store.否') }
                ],
                addEnvObj: {
                    show: false,
                    form: {
                        varName: '',
                        varValue: '',
                        scope: 'ALL',
                        varDesc: '',
                        encryptFlag: false
                    }
                },
                nameRule: {
                    validator: (val) => (/^[A-Z][A-Z0-9_]*$/.test(val)),
                    message: this.$t('store.以大写字母开头，包含大写字母、下划线或数字'),
                    trigger: 'blur'
                },
                deleteObj: {
                    show: false,
                    loading: false,
                    name: ''
                },
                envHistory: {
                    show: false,
                    isLoading: false,
                    list: []
                }
            }
        },

        computed: {
            ...mapGetters('store', {
                detail: 'getDetail'
            }),

            storeType () {
                const typeMap = {
                    atom: 'ATOM',
                    image: 'IMAGE',
                    service: 'SERVICE'
                }
                const type = this.$route.params.type
                return typeMap[type]
            },

            storeCode () {
                const keyMap = {
                    atom: 'atomCode',
                    image: 'imageCode',
                    service: 'serviceCode'
                }
                const type = this.$route.params.type
                const key = keyMap[type]
                return this.detail[key]
            },

            postData () {
                return {
                    storeType: this.storeType,
                    storeCode: this.storeCode,
                    scope: this.envScope,
                    varName: String(this.envName)
                }
            }
        },

        watch: {
            postData: {
                handler () {
                    this.getAllEnvList()
                },
                immediate: true
            }
        },

        methods: {
            editEnv (form) {
                Object.assign(form, {
                    option: 'update',
                    variableId: form.id
                })
                form = JSON.parse(JSON.stringify(form))
                Object.assign(this.addEnvObj, {
                    show: true,
                    title: this.$t('store.修改环境变量'),
                    form
                })
            },

            addEnv () {
                Object.assign(this.addEnvObj, {
                    show: true,
                    title: this.$t('store.新增环境变量'),
                    form: {
                        varName: '',
                        varValue: '',
                        scope: 'ALL',
                        varDesc: '',
                        option: 'create',
                        encryptFlag: false
                    }
                })
            },

            deleteEnv (row) {
                this.deleteObj.show = true
                this.deleteObj.name = row.varName
                this.deleteObj.scope = row.scope
            },

            requestDelete () {
                this.deleteObj.loading = true
                const data = {
                    storeType: this.storeType,
                    storeCode: this.storeCode,
                    varNames: this.deleteObj.name,
                    scope: this.deleteObj.scope
                }
                api.deleteEnv(data).then((res) => {
                    this.getAllEnvList()
                }).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => {
                    this.deleteObj.show = false
                    this.deleteObj.loading = false
                })
            },

            showHistory (row) {
                this.envHistory.show = true
                this.envHistory.isLoading = true
                this.envHistory.name = row.varName
                const data = {
                    storeType: this.storeType,
                    storeCode: this.storeCode,
                    varName: row.varName,
                    scope: row.scope
                }
                api.getEnvChangeList(data).then((res) => {
                    this.envHistory.list = res || []
                }).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => {
                    this.envHistory.isLoading = false
                })
            },

            convertScope (row, column, cellValue, index) {
                const currentScope = this.scopesList.find(x => x.id === cellValue)
                return currentScope.name
            },

            convertEncryptFlag (row, column, cellValue, index) {
                const currentEncrypt = this.encryptList.find(x => x.id === cellValue)
                return currentEncrypt.name
            },

            convertTime (row, column, cellValue, index) {
                return convertTime(cellValue)
            },

            saveEnv () {
                this.$refs.envForm.validate().then(() => {
                    this.isSaving = true
                    const postData = Object.assign({
                        storeCode: this.storeCode,
                        storeType: this.storeType
                    }, this.addEnvObj.form)
                    api.addEnv(postData).then((res) => {
                        this.getAllEnvList()
                        this.closeAddEnv()
                    }).catch((err) => {
                        this.$bkMessage({ message: err.message || err, theme: 'error' })
                    }).finally(() => {
                        this.isSaving = false
                    })
                }, (validator) => {
                    this.$bkMessage({ message: validator.content || validator, theme: 'error' })
                })
            },

            closeAddEnv () {
                this.addEnvObj.show = false
                this.addEnvObj.form.varName = ''
                this.addEnvObj.form.varValue = ''
                this.addEnvObj.form.scope = 'ALL'
                this.addEnvObj.form.varDesc = ''
                this.addEnvObj.form.encryptFlag = false
            },

            getAllEnvList () {
                this.searchError = this.envName && !/^[A-Z][A-Z0-9_]*$/.test(this.envName)
                if (this.searchError) {
                    this.envList = []
                    return
                }

                this.isLoading = true
                api.getAllEnv(this.postData).then((res) => {
                    this.envList = res || []
                }).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => {
                    this.isLoading = false
                })
            },

            requireRule (name) {
                return {
                    required: true,
                    message: this.$t('store.validateMessage', [name, this.$t('store.必填项')]),
                    trigger: 'blur'
                }
            },

            numMax (num) {
                return {
                    validator: (val = '') => (val.length <= num),
                    message: this.$t('store.validateNum', [num]),
                    trigger: 'blur'
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    .manage-environment {
        height: 100%;
        padding: 3.2vh 3.2vh 1.7vh;
        background: #fff;
        .environment-head {
            margin-bottom: 3.2vh;
            display: flex;
            .head-item {
                width: 220px;
                margin-left: 16px;
                position: relative;
                &.error {
                    ::v-deep .bk-input-text input {
                        border-color: #ff5656;
                    }
                    .err-info {
                        display: inline-block;
                    }
                }
                .err-info {
                    display: none;
                    position: absolute;
                    left: 0;
                    top: 34px;
                    width: 300px;
                    font-size: 12px;
                    color: #ea3636;
                }
            }
        }
        .add-env {
            padding: 32px;
        }
        .environment-history {
            padding: 32px;
            height: 100%;
        }
        ::v-deep .bk-sideslider-content {
            height: calc(100% - 60px);
            .bk-table-body-wrapper {
                max-height: calc(100% - 43px);
                overflow-y: auto;
            }
        }
    }
</style>
