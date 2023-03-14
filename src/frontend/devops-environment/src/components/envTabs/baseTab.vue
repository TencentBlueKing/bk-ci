<template>
    <div class="base-message-wrapper">
        <form class="bk-form base-env-form" ref="modifyEnv">
            <div class="bk-form-item">
                <label class="bk-label env-item-label">{{ $t('environment.envInfo.name') }}</label>
                <div class="bk-form-content env-item-content">
                    <div class="edit-content" v-if="isEditingName">
                        <input type="text" class="bk-form-input env-name-input" :placeholder="$t('environment.pleaseEnter')"
                            maxlength="30"
                            name="envName"
                            v-model="editEnvForm.name"
                            v-validate="'required'"
                            :class="{ 'is-danger': errors.has('envName') }">
                    </div>
                    <p v-else class="env-base cur-env-name"><span class="env-name-content">{{ curEnvDetail.name }}</span></p>
                    <div class="handler-btn">
                        <span
                            v-perm="{
                                hasPermission: curEnvDetail.canEdit,
                                disablePermissionApi: true,
                                tooltips: $t('environment.noPermission'),
                                permissionData: {
                                    projectId: projectId,
                                    resourceType: ENV_RESOURCE_TYPE,
                                    resourceCode: envHashId,
                                    action: ENV_RESOURCE_ACTION.EDIT
                                }
                            }"
                        >
                            <i class="devops-icon icon-edit" v-if="!isEditingName" @click="toEditBaseForm('name')"></i>
                        </span>
                        <span class="edit-base" v-if="isEditingName" @click="saveEnvDetail('name')">{{ $t('environment.save') }}</span>
                        <span class="edit-base" v-if="isEditingName" @click="cancelEnvDetail('name')">{{ $t('environment.cancel') }}</span>
                    </div>
                </div>
            </div>
            <div class="bk-form-item">
                <label class="bk-label env-item-label env-desc-label">{{ $t('environment.envInfo.envRemark') }}</label>
                <div class="bk-form-content env-item-content">
                    <div class="edit-content" v-if="isEditingDesc">
                        <textarea class="bk-form-input env-desc-input" :placeholder="$t('environment.pleaseEnter')" name="envDesc" v-if="isEditingDesc"
                            maxlength="100"
                            v-model="editEnvForm.desc">
                                    </textarea>
                    </div>
                    <p v-else class="env-base cur-env-desc">
                        <span v-if="curEnvDetail.desc" class="env-desc-content">{{ curEnvDetail.desc }}</span>
                        <span v-else>--</span>
                    </p>
                    <div class="handler-btn">
                        <span
                            v-perm="{
                                hasPermission: curEnvDetail.canEdit,
                                disablePermissionApi: true,
                                tooltips: $t('environment.noPermission'),
                                permissionData: {
                                    projectId: projectId,
                                    resourceType: ENV_RESOURCE_TYPE,
                                    resourceCode: envHashId,
                                    action: ENV_RESOURCE_ACTION.EDIT
                                }
                            }"
                        >
                            <i class="devops-icon icon-edit" v-if="!isEditingDesc" @click="toEditBaseForm('desc')"></i>
                        </span>
                        <span class="edit-base" v-if="isEditingDesc" @click="saveEnvDetail('desc')">{{ $t('environment.save') }}</span>
                        <span class="edit-base" v-if="isEditingDesc" @click="cancelEnvDetail('desc')">{{ $t('environment.cancel') }}</span>
                    </div>
                </div>
            </div>
            <div class="bk-form-item">
                <label class="bk-label env-item-label env-desc-label">{{ $t('environment.envInfo.envType') }}</label>
                <div class="bk-form-content env-item-content">
                    <div class="edit-content" v-if="isEditingType">
                        <bk-radio-group v-model="editEnvForm.type">
                            <bk-radio :value="'DEV'" class="env-type-radio">{{ $t('environment.envInfo.devEnvType') }}</bk-radio>
                            <bk-radio :value="'PROD'" class="env-type-radio">{{ $t('environment.envInfo.testEnvType') }}</bk-radio>
                        </bk-radio-group>
                    </div>
                    <p class="env-base type-content" v-else>
                        <span>{{ $t(envTypeDesc) }}</span>
                    </p>
                    <div class="handler-btn" v-if="curEnvDetail.envType !== 'BUILD'">
                        <i class="devops-icon icon-edit" v-if="!isEditingType" @click="toEditBaseForm('type')"></i>
                        <span class="edit-base" v-if="isEditingType" @click="saveEnvDetail('type')">{{ $t('environment.save') }}</span>
                        <span class="edit-base" v-if="isEditingType" @click="cancelEnvDetail('type')">{{ $t('environment.cancel') }}</span>
                    </div>
                </div>
            </div>
            <div class="bk-form-item">
                <label class="bk-label env-item-label env-desc-label">{{ $t('environment.envInfo.nodeCount') }}</label>
                <div class="bk-form-content env-item-content">
                    <p class="env-base">{{ curEnvDetail.nodeCount }}</p>
                </div>
            </div>
            <div class="bk-form-item">
                <label class="bk-label env-item-label env-desc-label">{{ $t('environment.envInfo.creationTime') }}</label>
                <div class="bk-form-content env-item-content">
                    <p class="env-base">{{ localConvertTime(curEnvDetail.createdTime) }}</p>
                </div>
            </div>
            <div class="bk-form-item create-user-item">
                <label class="bk-label env-item-label env-desc-label">{{ $t('environment.envInfo.creator') }}</label>
                <div class="bk-form-content env-item-content">
                    <p class="env-base">{{ curEnvDetail.createdUser }}</p>
                </div>
            </div>
        </form>
    </div>
</template>

<script>
    import { convertTime } from '@/utils/util'
    import { ENV_RESOURCE_ACTION, ENV_RESOURCE_TYPE } from '@/utils/permission'
    export default {
        name: 'base-tab',
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
                ENV_RESOURCE_ACTION,
                ENV_RESOURCE_TYPE,
                isEditingName: false,
                isEditingDesc: false,
                isEditingType: false,
                editEnvForm: {
                    name: '',
                    desc: '',
                    type: ''
                }
            }
        },
        computed: {
            configList () {
                return this.curEnvDetail.envVars
            },
            envTypeDesc () {
                const { envType } = this.curEnvDetail
                const descMap = {
                    DEV: 'devEnvType',
                    PROD: 'testEnvType',
                    BUILD: 'buildEnvType'
                }
                return `environment.envInfo.${descMap[envType]}`
            }
        },
        methods: {
            toEditBaseForm (type) {
                if (type === 'name') {
                    this.isEditingName = true
                    this.editEnvForm.name = this.curEnvDetail.name
                } else if (type === 'desc') {
                    this.isEditingDesc = true
                    this.editEnvForm.desc = this.curEnvDetail.desc
                } else {
                    this.isEditingType = true
                    this.editEnvForm.type = this.curEnvDetail.envType
                }
            },
            async saveEnvDetail (type) {
                const valid = await this.$validator.validate()
                if ((type === 'name' && valid) || type !== 'name') {
                    let message, theme
                    const modifyEenv = {
                        envVars: []
                    }

                    this.configList.forEach(item => {
                        const temp = {}
                        temp.name = item.name
                        temp.value = item.value
                        temp.secure = item.isSecure !== 'plaintext'
                        modifyEenv.envVars.push(temp)
                    })

                    try {
                        if (type === 'name') {
                            if (this.editEnvForm.name) {
                                modifyEenv.name = this.editEnvForm.name
                                modifyEenv.desc = this.curEnvDetail.desc
                                modifyEenv.envType = this.curEnvDetail.envType

                                await this.$store.dispatch('environment/toModifyEnv', {
                                    projectId: this.projectId,
                                    envHashId: this.envHashId,
                                    params: modifyEenv
                                })

                                message = this.$t('environment.successfullySaved')
                                theme = 'success'
                            }
                        } else if (type === 'desc') {
                            modifyEenv.name = this.curEnvDetail.name
                            modifyEenv.desc = this.editEnvForm.desc
                            modifyEenv.envType = this.curEnvDetail.envType

                            await this.$store.dispatch('environment/toModifyEnv', {
                                projectId: this.projectId,
                                envHashId: this.envHashId,
                                params: modifyEenv
                            })

                            message = this.$t('environment.successfullySaved')
                            theme = 'success'
                        } else {
                            modifyEenv.name = this.curEnvDetail.name
                            modifyEenv.desc = this.curEnvDetail.desc
                            modifyEenv.envType = this.editEnvForm.type

                            await this.$store.dispatch('environment/toModifyEnv', {
                                projectId: this.projectId,
                                envHashId: this.envHashId,
                                params: modifyEenv
                            })

                            message = this.$t('environment.successfullySaved')
                            theme = 'success'
                        }
                    } catch (e) {
                        this.handleError(
                            e,
                            {
                                projectId: this.projectId,
                                resourceType: ENV_RESOURCE_TYPE,
                                resourceCode: this.envHashId,
                                action: ENV_RESOURCE_ACTION.EDIT
                            }
                        )
                    } finally {
                        if (theme === 'success') {
                            this.$bkMessage({
                                message,
                                theme
                            })
                            this.requestEnvDetail()
                            if (type === 'name') {
                                this.curEnvDetail.name = modifyEenv.name
                                this.isEditingName = false
                            } else if (type === 'desc') {
                                this.curEnvDetail.desc = modifyEenv.desc
                                this.isEditingDesc = false
                            } else {
                                this.curEnvDetail.envType = modifyEenv.envType
                                this.isEditingType = false
                            }
                        }
                    }
                }
            },
            cancelEnvDetail (type) {
                if (type === 'name') {
                    this.isEditingName = false
                } else if (type === 'desc') {
                    this.isEditingDesc = false
                } else {
                    this.isEditingType = false
                }
            },
            /**
             * 处理时间格式
             */
            localConvertTime (timestamp) {
                return convertTime(timestamp * 1000)
            }
        }
    }
</script>
