<template>
    <section class="g-turbo-box basic-info">
        <h3 class="create-title g-turbo-deep-black-font"> {{ $t('turbo.基本信息') }} </h3>
        <bk-form class="g-turbo-form-left" :label-width="138" :model="copyFormData" v-bkloading="{ isLoading: isLoadingEngine }" ref="createTask">
            <bk-form-item :label="$t('turbo.方案ID')" property="openStatus">
                <template v-if="isEdit">
                    <bk-input v-model="copyFormData.planId" class="single-width" :placeholder="$t('turbo.系统自动生成，方案的唯一标识')" disabled></bk-input>
                    <bk-checkbox v-model="copyFormData.openStatus" @change="handleChange" v-bk-tooltips="{ content: $t('turbo.若不开启，配置不生效') }"> {{ $t('turbo.开启方案') }} </bk-checkbox>
                </template>
                <span v-else class="g-turbo-text-break plan-id">
                    <span>{{ copyFormData.planId }}</span>
                    <logo name="copy" @click.native="copyValue(copyFormData.planId)" size="16" class="icon-copy"></logo>
                    <span v-if="copyFormData.openStatus" class="plan-open plan-common" @click="toggleOpen(false)" v-bk-tooltips="{ content: $t('turbo.点击禁用当前方案，禁用后，配置将不再生效') }">
                        <logo name="check" class="plan-icon" size="10"></logo> {{ $t('turbo.已开启') }}
                    </span>
                    <span v-else class="plan-close plan-common" @click="toggleOpen(true)" v-bk-tooltips="{ content: $t('turbo.点击开启方案，可以在构建机或流水线中使用') }">
                        <logo name="suspend" class="plan-icon"></logo> {{ $t('turbo.已禁用') }}
                    </span>
                </span>
            </bk-form-item>
            <bk-form-item :label="$t('turbo.方案名称')" required property="planName" :rules="[requireRule($t('turbo.方案名称')), nameRule]" error-display-type="normal">
                <template v-if="isEdit">
                    <bk-input v-model="copyFormData.planName" @change="handleChange" class="single-width" :placeholder="$t('turbo.以汉字、英文字母、数字、连字符(-)、符号(_+#)组成，不超过30个字')"></bk-input>
                </template>
                <span v-else class="g-turbo-text-break">{{ formData.planName }}</span>
            </bk-form-item>
            <bk-form-item :label="$t('turbo.加速模式')" required property="engineCode" :rules="[requireRule($t('turbo.加速模式'))]" error-display-type="normal">
                <template v-if="isEdit && onlyEdit">
                    <span> {{ $t('turbo.根据你的加速场景选择适用的模式') }} </span>
                    <ul class="turbo-model-list">
                        <li v-for="item in engineList"
                            :key="item"
                            :class="['single-width', 'turbo-model-item', 'g-turbo-text-overflow', { choose: copyFormData.engineCode === item.engineCode }]"
                            @click="chooseMode(item, true)"
                        >
                            <p class="item-title g-turbo-black-font">{{ item.engineName }}<span class="recommend" v-if="item.recommend"> {{ $t('turbo.（荐）') }} <span></span></span></p>
                            <span class="item-desc g-turbo-gray-font g-turbo-text-overflow" v-bk-overflow-tips="{ interactive: true }">{{ item.desc }}</span>
                            <logo name="check" :size="10" class="item-check"></logo>
                        </li>
                    </ul>
                </template>
                <span v-else class="turbo-model-engine">{{ formData.engineName }}</span>
            </bk-form-item>
            <bk-form-item :label="$t('turbo.方案说明')" property="name">
                <template v-if="isEdit">
                    <bk-input v-model="copyFormData.desc" @change="handleChange" type="textarea" class="double-width" :maxlength="200"></bk-input>
                </template>
                <span v-else class="g-turbo-text-break">{{ formData.desc || '-' }}</span>
            </bk-form-item>
        </bk-form>
        <bk-button v-if="isEdit && !onlyEdit" theme="primary" class="g-turbo-bottom-button" @click="save" :loading="isLoading"> {{ $t('turbo.保存') }} </bk-button>
        <bk-button v-if="isEdit && !onlyEdit" class="g-turbo-bottom-button" @click="cancle" :disabled="isLoading"> {{ $t('turbo.取消') }} </bk-button>
        <span class="g-turbo-edit-button" @click="isEdit = true" v-if="!onlyEdit && !isEdit"><logo name="edit" size="16"></logo> {{ $t('turbo.编辑') }} </span>
    </section>
</template>

<script>
    import { mapActions } from 'vuex'
    import { getEngineList, modifyTaskBasic } from '@/api'
    import logo from '@/components/logo'
    import { copyText } from '@/assets/js/util'

    export default {
        components: {
            logo
        },

        props: {
            onlyEdit: {
                type: Boolean,
                default: true
            },
            formData: {
                type: Object
            }
        },

        data () {
            return {
                isEdit: this.onlyEdit,
                copyFormData: {},
                engineList: [],
                isLoading: false,
                isLoadingEngine: false,
                nameRule: {
                    validator: (val) => (/^[\u4e00-\u9fa5a-zA-Z0-9-_+#]+$/.test(val) && val.length <= 30),
                    message: this.$t('turbo.以汉字、英文字母、数字、连字符(-)、符号(_+#)组成，不超过30个字'),
                    trigger: 'blur'
                }
            }
        },

        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },

        watch: {
            projectId: {
                handler () {
                    this.getEngineList()
                },
                immediate: true
            }
        },

        created () {
            this.copyFormData = JSON.parse(JSON.stringify(this.formData))
        },

        methods: {
            ...mapActions('turbo', ['setParamConfig']),

            toggleOpen (isOpen) {
                this.copyFormData.openStatus = isOpen
                this.save()
            },

            copyValue (planId) {
                copyText(planId, this.$t.bind(this))
            },

            requireRule (name) {
                return {
                    required: true,
                    message: this.$t('turbo.validateMessage', [name, this.$t('turbo.必填项')]),
                    trigger: 'blur'
                }
            },

            save () {
                this.$refs.createTask.validate().then(() => {
                    this.isLoading = true
                    const postData = {
                        ...this.copyFormData,
                        projectId: this.projectId
                    }
                    modifyTaskBasic(postData).then(() => {
                        this.$bkMessage({ theme: 'success', message: this.$t('turbo.修改成功') })
                        this.$emit('update:formData', this.copyFormData)
                        this.isEdit = false
                    }).catch((err) => {
                        this.$bkMessage({ theme: 'error', message: err.message || err })
                    }).finally(() => {
                        this.isLoading = false
                    })
                }, (validator) => {
                    this.$bkMessage({ message: validator.content || validator, theme: 'error' })
                })
            },

            cancle () {
                this.copyFormData = JSON.parse(JSON.stringify(this.formData))
                this.isEdit = false
            },

            handleChange () {
                window.changeFlag = true
            },

            chooseMode (item, changeFlag) {
                window.changeFlag = changeFlag
                const formData = {
                    ...JSON.parse(JSON.stringify(this.copyFormData)),
                    paramConfig: item.paramConfig,
                    userManual: item.userManual,
                    engineCode: item.engineCode
                }
                if (JSON.stringify(formData) === JSON.stringify(this.copyFormData)) return
                this.copyFormData = JSON.parse(JSON.stringify(formData))
                this.setParamConfig(item.paramConfig)
                this.$emit('update:formData', formData)
            },

            getEngineList () {
                this.isLoadingEngine = true
                const projectId = this.projectId
                getEngineList(projectId).then((res = []) => {
                    this.engineList = res
                    const engineCode = this.copyFormData.engineCode || this.$route.query.engineCode
                    const curEngine = res.find((item) => (engineCode && item.engineCode === engineCode)) || {}
                    this.chooseMode(curEngine, false)
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isLoadingEngine = false
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '../../assets/scss/var.scss';

    .basic-info {
        position: relative;
    }

    .plan-id {
        display: flex;
        align-items: center;
        .icon-copy {
            margin: 0 16px 0 4px;
            cursor: pointer;
        }
        .plan-common {
            display: flex;
            align-items: center;
            line-height: 18px;
            padding: 0 5px;
            font-size: 12px;
            cursor: pointer;
        }
        .plan-open {
            border: 1px solid #3a84ff;
            border-radius: 2px;
            color: #3a84ff;
        }
        .plan-close {
            border: 1px solid #c4c6cc;
            border-radius: 2px;
            color: #c4c6cc;
        }
        .plan-icon {
            margin-right: 3px;
        }
    }

    .turbo-model-engine {
        background: #e1ecff;
        border-radius: 2px;
        font-size: 12px;
        line-height: 20px;
        color: #3a84ff;
        padding: 0 9px;
        display: inline-block;
    }

    .turbo-model-list {
        margin-top: -5px;
        &::after {
            content: '';
            display: table;
            clear: both;
        }
        .turbo-model-item {
            position: relative;
            float: left;
            height: round(60px * $designToPx);
            border: 1px solid #C4C6CC;
            border-radius: 2px;
            margin: 10px 10px 0 0;
            padding: 0 11px;
            display: flex;
            flex-direction: column;
            align-items: flex-start;
            justify-content: center;
            cursor: pointer;
            .recommend {
                color: #3a84ff;
            }
            .item-check {
                display: none;
                position: absolute;
                right: 1px;
                top: 1px;
                color: #fff;
            }
            .item-title {
                line-height: 22px;
            }
            .item-desc {
                font-size: 12px;
                line-height: 20px;
                max-width: 100%;
            }
            &:hover {
                border-color: #3a84ff;
                box-shadow: 0 0 0 2px #e1ecff;
            }
            &.choose {
                border-color: #3a84ff;
                &:before {
                    content: '';
                    position: absolute;
                    right: -15px;
                    top: -15px;
                    width: 30px;
                    height: 30px;
                    background: #3a84ff;
                    transform: rotate(45deg);
                }
                .item-check {
                    display: block;
                }
            }
        }
    }
    .g-turbo-box {
        margin-bottom: 20px;
        padding: 26px 32px;
        .create-title {
            font-size: 14px;
            line-height: 22px;
            margin-bottom: 17px;
        }
    }
    .single-width {
        width: 3.5rem;
        margin-right: 10px;
    }
    .double-width {
        width: 7.1rem;
    }
</style>
