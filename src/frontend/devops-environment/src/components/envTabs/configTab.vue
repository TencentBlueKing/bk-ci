<template>
    <div class="config-content-wrapper">
        <div class="config-content-header">
            <bk-button theme="primary" :disabled="lastselectConfIndex > -1"
                @click="createConfigItem">{{ $t('environment.addConfItem') }}
            </bk-button>
        </div>
        <div class="config-table" v-if="configList.length">
            <div class="table-head config-head">
                <div class="table-config-item config-item-key">{{ $t('environment.envInfo.key') }}</div>
                <div class="table-config-item config-item-value">{{ $t('environment.envInfo.value') }}</div>
                <div class="table-config-item config-item-type">{{ $t('environment.envInfo.type') }}</div>
                <div class="table-config-item config-item-handler">{{ $t('environment.operation') }}</div>
            </div>
            <div class="table-config-body">
                <div class="table-row config-row" v-for="(row, index) of configList" :key="index">
                    <div class="table-config-item config-item-key">
                        <input type="text" class="bk-form-input config-input config-key-input" :placeholder="$t('environment.pleaseEnter')"
                            v-if="row.isCreateItem || row.isEditItem"
                            v-model="row.name"
                            name="confName"
                            @input="errorHandler.nameError = false"
                            :class="{ 'is-danger': errorHandler.nameError }">
                        <span class="config-name" v-else>{{ row.name }}</span>
                    </div>
                    <div class="table-config-item config-item-value">
                        <input type="password" class="bk-form-input config-input config-value-input" :placeholder="$t('environment.pleaseEnter')"
                            v-if="(!curIsPlaintext && (row.isCreateItem || row.isEditItem) && (row.isSecure === 'ciphertext'))"
                            v-model="row.value"
                            name="confvalue"
                            @input="errorHandler.valueError = false"
                            :class="{ 'is-danger': errorHandler.valueError }">
                        <input type="text" class="bk-form-input config-input config-value-input" :placeholder="$t('environment.pleaseEnter')"
                            v-if="(curIsPlaintext || row.isSecure === 'plaintext') && (row.isCreateItem || row.isEditItem)"
                            v-model="row.value"
                            name="confvalue"
                            @input="errorHandler.valueError = false"
                            :class="{ 'is-danger': errorHandler.valueError }">
                        <i class="devops-icon" :class="curIsPlaintext ? 'icon-eye' : 'icon-hide'"
                            v-if="(row.isCreateItem || row.isEditItem) && row.isSecure === 'ciphertext'"
                            @click="curIsPlaintext = !curIsPlaintext"></i>
                        <span class="config-name"
                            v-if="(!row.isCreateItem && !row.isEditItem)">{{ row.secure ? '******' : row.value }}</span>
                    </div>
                    <div class="table-config-item config-item-type">
                        <bk-select v-if="row.isCreateItem"
                            class="config-text-type"
                            popover-min-width="120"
                            v-model="row.isSecure"
                            @item-selected="secureSelected">
                            <bk-option v-for="(option, cindex) in confTextType"
                                :key="cindex"
                                :id="option.label"
                                :name="option.name">
                            </bk-option>
                        </bk-select>
                        <span class="config-type" v-else>{{ row.secure ? $t('environment.envInfo.cipherText') : $t('environment.envInfo.clearText') }}</span>
                    </div>
                    <div class="table-config-item config-item-handler">
                        <div class="editing-handler" v-if="(row.isCreateItem || row.isEditItem)">
                            <span class="config-edit" @click="saveEditConfig(row, index)">{{ $t('environment.save') }}</span>
                            <span class="text-type" @click="cancelEdit(row, index)">{{ $t('environment.cancel') }}</span>
                        </div>
                        <div class="preview-handler" v-else>
                            <span class="config-edit" @click="changeConfig(row, index)">{{ $t('environment.edit') }}</span>
                            <span class="config-edit" @click="deleteConfig(row, index)">{{ $t('environment.delete') }}</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <bk-exception
            v-else
            class="exception-wrap-item exception-part" type="empty" scene="part"
        />
    </div>
</template>

<script>
    export default {
        name: 'config-tab',
        props: {
            projectId: {
                type: String,
                required: true
            },
            envHashId: {
                type: String,
                required: true
            },
            curEnvDetail: {
                type: Object,
                default: () => ({})
            },
            requestEnvDetail: {
                type: Function,
                required: true
            }
        },
        data () {
            return {
                curIsPlaintext: false, // 明文/密文
                lastselectConfIndex: -1, // 最后选中的配置项索引
                lastSelectConfig: {},
                configList: this.curEnvDetail.envVars || [],
                errorHandler: {
                    nameError: false,
                    valueError: false
                } // 最后选中配置型obj
            }
        },
        
        computed: {
            confTextType () {
                return [
                    { label: 'plaintext', name: this.$t('environment.envInfo.clearText') },
                    { label: 'ciphertext', name: this.$t('environment.envInfo.cipherText') }
                ]
            }
        },

        watch: {
            'curEnvDetail.envVars': {
                handler (v) {
                    console.log(v)
                    this.configList = [...v]
                }
            }
        },
        methods: {
            /**
             * 编辑配置项
             */
            changeConfig (row, index) {
                if (this.lastselectConfIndex === -1) {
                    this.lastselectConfIndex = index
                    this.lastSelectConfig = row

                    this.configList.forEach((item, index) => {
                        if (item.name === row.name) {
                            this.curIsPlaintext = !item.secure
                            item.isEditItem = true
                        }
                    })

                    this.lastSelectConfig = JSON.parse(JSON.stringify(row))
                    this.configList = [
                        ...this.configList
                    ]
                }
            },
            /**
             * 取消编辑配置项
             */
            cancelEdit (row, index) {
                const target = this.lastSelectConfig

                if (target.isEditItem) {
                    target.isEditItem = false
                    this.configList.splice(index, 1, target)
                } else {
                    this.configList.shift()
                }

                this.errorHandler.nameError = false
                this.errorHandler.valueError = false
                this.lastselectConfIndex = -1
            },
            /**
             * 新增配置项
             */
            createConfigItem () {
                const newItem = {
                    name: '',
                    value: '',
                    isSecure: 'plaintext',
                    secure: false,
                    isCreateItem: true,
                    isEditItem: false
                }

                this.lastselectConfIndex = 0
                this.lastSelectConfig = newItem
                this.configList.unshift(newItem)
            },
            validate (row) {
                let errorCount = 0
                if (!row.name) {
                    this.errorHandler.nameError = true
                    errorCount++
                }
                if (!row.value) {
                    this.errorHandler.valueError = true
                    errorCount++
                }
                if (errorCount > 0) {
                    return false
                }
                return true
            },
            /**
             * 保存编辑的配置项
             */
            async saveEditConfig (row) {
                const isValid = this.validate(row)

                if (!isValid) {
                    return
                }

                let message, theme

                try {
                    await this.$store.dispatch('environment/toModifyEnv', {
                        projectId: this.projectId,
                        envHashId: this.envHashId,
                        params: {
                            ...this.curEnvDetail,
                            envVars: this.configList.map(item => {
                                return {
                                    name: item.name,
                                    value: item.value,
                                    secure: item.isSecure !== 'plaintext'
                                }
                            })
                        }
                    })

                    message = this.$t('environment.successfullySaved')
                    theme = 'success'
                } catch (err) {
                    message = err.message ? err.message : err
                    theme = 'error'
                } finally {
                    this.$bkMessage({
                        message,
                        theme
                    })

                    this.lastselectConfIndex = -1
                    this.requestEnvDetail()
                }
            },
            /**
             * 删除环境变量
             */
            async deleteConfig (row, index) {
                if (this.lastselectConfIndex === -1) {
                    const h = this.$createElement
                    const content = h('p', {
                        style: {
                            textAlign: 'center'
                        }
                    }, `${this.$t('environment.deleteConfigItem')}?`)

                    this.$bkInfo({
                        title: this.$t('environment.delete'),
                        subHeader: content,
                        confirmFn: async () => {
                            let message, theme
                            const modifyEenv = {
                                name: this.curEnvDetail.name,
                                desc: this.curEnvDetail.desc,
                                envType: this.curEnvDetail.envType,
                                envVars: this.configList.filter((item, i) => index !== i).map(item => ({
                                    name: item.name,
                                    value: item.value,
                                    secure: item.isSecure !== 'plaintext'
                                }))
                            }

                            try {
                                await this.$store.dispatch('environment/toModifyEnv', {
                                    projectId: this.projectId,
                                    envHashId: this.envHashId,
                                    params: modifyEenv
                                })

                                message = this.$t('environment.successfullyDeleted')
                                theme = 'success'
                            } catch (err) {
                                message = err.message ? err.message : err
                                theme = 'error'
                            } finally {
                                this.$bkMessage({
                                    message,
                                    theme
                                })

                                this.lastselectConfIndex = -1
                                this.requestEnvDetail()
                            }
                        }
                    })
                }
            },
            /**
             * 明文/密文切换
             */
            secureSelected (val) {
                this.curIsPlaintext = val === 'plaintext'
            }
        }
    }
</script>
