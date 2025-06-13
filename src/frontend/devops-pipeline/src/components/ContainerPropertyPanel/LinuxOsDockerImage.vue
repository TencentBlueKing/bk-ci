<template>
    <section>
        <form-field>
            <atom-checkbox
                :value="enableDocker"
                :text="$t('editPage.enableDocker')"
                name="enableDocker"
                :handle-change="val => handleeEnableDockerChange(val)"
                :disabled="!editable"
            >
            </atom-checkbox>
            <i
                class="bk-icon icon-info-circle"
                v-bk-tooltips="$t('editPage.enableDockerTips')"
            ></i>
        </form-field>
        <section
            class="linux-docker-image-container"
            v-if="enableDocker"
        >
            <form-field
                :label="$t('editPage.image')"
                :required="true"
                :is-error="errors.has('buildImageVersion') || errors.has('image')"
                :error-msg="$t('editPage.imageErrMgs')"
            >
                <enum-input
                    name="imageType"
                    :list="imageTypeList"
                    :disabled="!editable"
                    :handle-change="changeImageType"
                    :value="buildImageType"
                >
                </enum-input>
                <section
                    v-if="buildImageType === 'BKSTORE'"
                    class="bk-image"
                >
                    <section class="image-name">
                        <span
                            :class="[
                                { disable: !editable },
                                { 'not-recommend': buildImageRecommendFlag === false },
                                'image-named'
                            ]"
                            :title="buildImageRecommendFlag === false
                                ? $t('editPage.notRecomendImage')
                                : buildImageName
                            "
                        >{{ buildImageName || $t("editPage.chooseImage") }}</span>
                        <bk-button
                            theme="primary"
                            @click.stop="chooseImage"
                            :disabled="!editable"
                        >
                            {{
                                buildImageCode ? $t("editPage.reElection") : $t("editPage.select")
                            }}
                        </bk-button>
                    </section>
                    <bk-select
                        @change="changeImageVersion"
                        :value="buildImageVersion"
                        searchable
                        class="image-tag"
                        :loading="isVersionLoading"
                        :disabled="!editable"
                        v-validate.initial="'required'"
                        name="buildImageVersion"
                    >
                        <bk-option
                            v-for="option in versionList"
                            :key="option.versionValue"
                            :id="option.versionValue"
                            :name="option.versionName"
                        >
                        </bk-option>
                    </bk-select>
                </section>
                <bk-input
                    v-else
                    @change="val => changeDockerInfo('image', val)"
                    :value="buildImage"
                    :disabled="!editable"
                    class="bk-image"
                    :placeholder="$t('editPage.thirdImageHolder')"
                    v-validate.initial="'required'"
                    name="image"
                ></bk-input>
            </form-field>

            <form-field
                :label="$t('editPage.imageTicket')"
                v-if="buildImageType === 'THIRD'"
            >
                <select-input
                    v-bind="imageCredentialOption"
                    :disabled="!editable"
                    name="credential"
                    :value="buildImageCredential"
                    :handle-change="changeTicket"
                ></select-input>
            </form-field>

            <form-field
                :label="$t('editPage.imagePullPolicy')"
                required
            >
                <enum-input
                    name="imagePullPolicy"
                    :list="policyList"
                    :disabled="!editable"
                    :handle-change="changeDockerInfo"
                    :value="buildImagePullPolicy"
                >
                </enum-input>
            </form-field>

            <form-field
                label="Volumes"
            >
                <input-parameter-array
                    name="volumes"
                    :value="buildVolumes"
                    :handle-change="changeOptions"
                />
            </form-field>
            <form-field
                label="Mounts"
            >
                <input-parameter-array
                    name="mounts"
                    :value="buildMounts"
                    :handle-change="changeOptions"
                />
            </form-field>

            <form-field
                label="Gpus"
            >
                <bk-input
                    placeholder="all"
                    :value="buildGpus"
                    @change="(val) => changeOptions('gpus', val)"
                />
            </form-field>

            <form-field>
                <atom-checkbox
                    :value="buildPrivileged"
                    :text="$t('editPage.enabledPrivileged')"
                    name="privileged"
                    :handle-change="changeOptions"
                    :disabled="!editable"
                >
                </atom-checkbox>
            </form-field>
        </section>
    </section>
</template>

<script>
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import EnumInput from '@/components/atomFormField/EnumInput'
    import SelectInput from '@/components/AtomFormComponent/SelectInput'
    import AtomCheckbox from '@/components/atomFormField/AtomCheckbox'
    import InputParameterArray from '@/components/AtomFormComponent/InputParameterArray'
    export default {
        name: 'LinuxOsDockerImage',
        components: {
            FormField,
            EnumInput,
            SelectInput,
            AtomCheckbox,
            InputParameterArray
        },
        props: {
            editable: {
                type: Boolean,
                default: false
            },
            container: {
                type: Object,
                default: () => {}
            },
            imageTypeList: {
                type: Array,
                default: () => []
            },
            versionList: {
                type: Array,
                default: () => []
            },
            isVersionLoading: {
                type: Boolean,
                default: false
            },
            chooseImage: {
                type: Function,
                required: true
            },
            handleContainerChange: {
                type: Function,
                required: true
            }
        },
        data () {
            return {
                enableDocker: false
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            buildImageCode () {
                return this.container.dispatchType?.dockerInfo?.storeImage?.imageCode
            },
            buildImageVersion () {
                return this.container.dispatchType?.dockerInfo?.storeImage?.imageVersion
            },
            buildImageName () {
                return this.container.dispatchType?.dockerInfo?.storeImage?.imageName
            },
            dockerInfo () {
                return this.container.dispatchType?.dockerInfo ?? {}
            },
            buildImageType () {
                return this.container.dispatchType?.dockerInfo?.imageType ?? 'THIRD'
            },
            buildImageCredential () {
                return this.container.dispatchType?.dockerInfo?.credential?.credentialId ?? ''
            },
            buildImagePullPolicy () {
                return this.container.dispatchType?.dockerInfo?.imagePullPolicy ?? 'always'
            },
            buildImage () {
                return this.container.dispatchType?.dockerInfo?.image ?? ''
            },
            buildVolumes () {
                return this.container.dispatchType?.dockerInfo?.options?.volumes ?? []
            },
            buildMounts () {
                return this.container.dispatchType?.dockerInfo?.options?.mounts ?? []
            },
            buildGpus () {
                return this.container.dispatchType?.dockerInfo?.options?.gpus ?? ''
            },
            buildPrivileged () {
                return this.container.dispatchType?.dockerInfo?.options?.privileged ?? false
            },
            buildImageRecommendFlag () {
                return this.container.dispatchType && this.container.dispatchType.recommendFlag
            },
            imageCredentialOption () {
                return {
                    optionsConf: {
                        paramId: 'credentialId',
                        paramName: 'credentialId',
                        url: `/ticket/api/user/credentials/${this.projectId}/hasPermissionList?permission=USE&page=1&pageSize=1000&credentialTypes=USERNAME_PASSWORD`,
                        hasAddItem: true,
                        itemText: this.$t('editPage.addCredentials'),
                        itemTargetUrl: `/ticket/${this.projectId}/createCredential/USERNAME_PASSWORD/true`
                    }
                }
            },
            policyList () {
                return [
                    { label: this.$t('editPage.alwaysPull'), value: 'always' },
                    { label: this.$t('editPage.ifNotPresent'), value: 'if-not-present' }
                ]
            }
        },
        created () {
            if (Object.keys(this.dockerInfo).length) this.enableDocker = true
        },
        methods: {
            handleeEnableDockerChange () {
                this.enableDocker = !this.enableDocker
                let { dispatchType } = this.container
                dispatchType = this.enableDocker
                    ? {
                        ...dispatchType,
                        dockerInfo: {
                            ...dispatchType.dockerInfo,
                            imageType: 'THIRD'
                        }
                    }
                    : {
                        ...dispatchType,
                        dockerInfo: undefined
                    }
                this.handleContainerChange(
                    'dispatchType',
                    Object.assign({
                        ...dispatchType
                    })
                )
            },

            changeImageType (name, value) {
                const dockerInfo = {
                    ...this.container.dispatchType?.dockerInfo,
                    [name]: value
                }

                if (value !== 'THIRD') {
                    dockerInfo.image = ''
                    dockerInfo.credential = ''
                } else {
                    dockerInfo.storeImage = undefined
                }

                this.handleContainerChange(
                    'dispatchType',
                    Object.assign({
                        ...this.container.dispatchType,
                        dockerInfo: {
                            ...dockerInfo
                        }
                    })
                )
            },

            changeDockerInfo (name, value) {
                this.handleContainerChange(
                    'dispatchType',
                    Object.assign({
                        ...this.container.dispatchType,
                        dockerInfo: {
                            ...this.container.dispatchType.dockerInfo,
                            [name]: value
                        }
                    })
                )
            },

            changeTicket (name, value) {
                this.handleContainerChange(
                    'dispatchType',
                    Object.assign({
                        ...this.container.dispatchType,
                        dockerInfo: {
                            ...this.container.dispatchType.dockerInfo,
                            [name]: {
                                credentialId: value
                            }
                        }
                    })
                )
            },

            changeOptions (name, value) {
                this.handleContainerChange(
                    'dispatchType',
                    Object.assign({
                        ...this.container.dispatchType,
                        dockerInfo: {
                            ...this.container.dispatchType.dockerInfo,
                            options: {
                                ...this.container.dispatchType.dockerInfo.options,
                                [name]: value
                            }
                        }
                    })
                )
            },

            changeImageVersion (value) {
                this.handleContainerChange(
                    'dispatchType',
                    Object.assign({
                        ...this.container.dispatchType,
                        dockerInfo: {
                            ...this.container.dispatchType.dockerInfo,
                            storeImage: {
                                ...this.container.dispatchType.dockerInfo.storeImage,
                                imageVersion: value
                            }
                        }
                    })
                )
            }
        }
    }
</script>

<style lang="scss">
    .linux-docker-image-container {
        padding: 5px 20px 15px;
        margin-top: 10px;
        background-color: #f5f7fa;
    }
</style>
