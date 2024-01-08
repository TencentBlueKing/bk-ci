<template>
    <bk-dialog
        :value="isShow"
        :width="700"
        :title="title"
        @value-change="handleChange"
    >
        <bk-form
            ref="applyFrom"
            :model="formData"
            class="apply-form"
            :rules="rules"
            label-width="100"
        >
            <bk-form-item
                :label="$t('userGroupName')"
            >
                <span>{{ groupName }}</span>
            </bk-form-item>
            <bk-form-item
                :label="$t('applicationPeriod')"
                property="expireTime"
                required
                error-display-type="normal"
            >
                <div class="bk-button-group deadline-wrapper">
                    <bk-button
                        v-for="(item, key, index) in timeFilters"
                        :key="index"
                        @click="handleChangeTime(key)"
                        :class="{
                            'is-selected': currentActive === Number(key),
                            'deadline-btn': true
                        }">
                        {{ item }}
                    </bk-button>
                    <bk-button
                        class="deadline-btn"
                        v-show="currentActive !== 'custom'"
                        @click="handleChangCustom"
                    >
                        {{ $t('custom') }}
                    </bk-button>
                    <bk-input
                        v-model="customTime"
                        v-show="currentActive === 'custom'"
                        class="custom-time-select"
                        type="number"
                        :show-controls="false"
                        placeholder="1-365"
                        :min="1"
                        :max="365"
                        @change="handleChangeCustomTime"
                    >
                        <template slot="append">
                            <div class="group-text">
                                {{ $t('day') }}
                            </div>
                        </template>
                    </bk-input>
                </div>
            </bk-form-item>
            <bk-form-item
                v-if="type === 'renewal'"
                :label="$t('expirationTime')"
            >
                <span class="expired">{{ expiredDisplay }}{{ $t('day')}}</span>
                <img class="arrows-icon" src="./svg/arrows-right.svg">
                <span class="new-expired">{{ newExpiredDisplay }}{{ $t('day')}}</span>
            </bk-form-item>
            <bk-form-item
                v-else
                :label="$t('reason')"
                property="reason"
                required
                error-display-type="normal"
            >
                <bk-input
                    v-model="formData.reason"
                    type="textarea"
                    :rows="3"
                    :maxlength="100"
                >
                </bk-input>
            </bk-form-item>
        </bk-form>
        <template slot="footer">
            <bk-button
                class="mr10"
                theme="primary"
                :loading="isLoading"
                @click="handleConfirm"
            >
                {{ $t('confirm') }}
            </bk-button>
            <bk-button
                :loading="isLoading"
                @click="handleCancel"
            >
                {{ $t('cancel') }}
            </bk-button>
        </template>
    </bk-dialog>
</template>
<script>
    export default {
        props: {
            isShow: {
                type: Boolean
            },
            groupName: {
                type: String
            },
            groupId: {
                type: String
            },
            expiredDisplay: {
                type: String
            },
            title: {
                type: String
            },
            type: {
                type: String,
                default: 'apply'
            },
            resourceType: {
                type: String
            }
        },
        data () {
            return {
                isLoading: false,
                pagination: {
                    page: 1,
                    pageSize: 20,
                    projectName: ''
                },
                customTime: 1,
                formData: {
                    expireTime: 0,
                    reason: ''
                },
                currentActive: 2592000,
                timeFilters: {
                    2592000: this.$t('oneMonth'),
                    7776000: this.$t('threeMonth'),
                    15552000: this.$t('sixMonth'),
                    31104000: this.$t('twelveMonth')
                },
                rules: {
                    expireTime: [
                        {
                            validator: () => {
                                if (this.currentActive === 'custom' && this.customTime) {
                                    return true
                                }
                                return this.currentActive !== 'custom'
                            },
                            message: this.$t('selectPeriod'),
                            trigger: 'blur'
                        }
                    ],
                    reason: [
                        {
                            required: true,
                            message: this.$t('fillReason'),
                            trigger: 'blur'
                        }
                    ]
                }
            }
        },
        computed: {
            userName () {
                return this.$userInfo && this.$userInfo.username ? this.$userInfo.username : ''
            },
            projectId () {
                return this.$route.params.projectId
            },
            newExpiredDisplay () {
                const timeMap = {
                    2592000: 30,
                    7776000: 90,
                    15552000: 180,
                    31104000: 360
                }
                if (this.currentActive === 'custom') {
                    return Number(this.expiredDisplay) + Number(this.customTime)
                }
                return Number(this.expiredDisplay) + timeMap[this.currentActive]
            }
        },
        created () {
            this.formData.expireTime = this.formatTimes(2592000)
            if (this.projectCode) {
                this.formData.englishName = this.projectCode
            }
            if (this.type === 'apply') {
                this.formData.reason = ''
            }
        },
        methods: {
            handleConfirm () {
                if (this.currentActive === 'custom') {
                    const timestamp = this.customTime * 24 * 3600
                    this.formData.expireTime = this.formatTimes(timestamp)
                }
                if (this.type === 'renewal') {
                    const timestamp = this.newExpiredDisplay * 24 * 3600
                    const expiredDisplayTime = this.formatTimes(timestamp)
                    this.formData.expireTime = expiredDisplayTime
                    this.handleRenewalGroup()
                } else {
                    this.handleApplyGroup()
                }
            },
            handleCancel () {
                this.$emit('update:isShow', false)
                this.customTime = 1
                this.formData.expireTime = this.formatTimes(2592000)
                this.formData.reason = ''
                this.currentActive = 2592000
                setTimeout(() => {
                    this.$refs.applyFrom.clearError()
                }, 500)
            },
            handleChangeCustomTime (value) {
                if (!/^[0-9]*$/.test(value)) {
                    this.$nextTick(() => {
                        this.customTime = 1
                    })
                } else if (this.customTime > 365) {
                    this.$nextTick(() => {
                        this.customTime = 365
                    })
                }
            },
            handleChangeTime (value) {
                this.$refs.applyFrom.clearError()
                this.currentActive = Number(value)
                this.formData.expireTime = this.formatTimes(value)
            },
            handleChangCustom () {
                this.currentActive = 'custom'
            },
            formatTimes (value) {
                const nowTimestamp = +new Date() / 1000
                const tempArr = String(nowTimestamp).split('')
                const dotIndex = tempArr.findIndex(i => i === '.')
                const nowSecond = parseInt(tempArr.splice(0, dotIndex).join(''), 10)
                return Number(value) + nowSecond
            },
            handleChange (val) {
                if (!val) this.handleCancel()
            },
            handleApplyGroup () {
                this.$refs.applyFrom.validate().then(() => {
                    this.isLoading = true
                    this.$ajax
                        .post('/auth/api/user/auth/apply/applyToJoinGroup', {
                            groupIds: [this.groupId],
                            expiredAt: this.formData.expireTime,
                            reason: this.formData.reason,
                            applicant: this.userName
                        })
                        .then(res => {
                            this.$bkMessage({
                                theme: 'success',
                                message: this.$t('applySuccess')
                            })
                        }).catch((err) => {
                            this.$bkMessage({
                                theme: 'error',
                                message: err.message
                            })
                        }).finally(() => {
                            this.isLoading = false
                            this.handleCancel()
                        })
                })
            },
            handleRenewalGroup () {
                this.isLoading = true
                this.$ajax
                    .put(`/auth/api/user/auth/resource/group/${this.projectCode}/${this.resourceType}/${this.groupId}/member/renewal`, {
                        expiredAt: this.formData.expireTime,
                        projectId: this.projectId,
                        resourceType: this.resourceType
                    })
                    .then(res => {
                        this.$bkMessage({
                            theme: 'success',
                            message: this.$t('applySuccess')
                        })
                    }).catch((err) => {
                        this.$bkMessage({
                            theme: 'error',
                            message: err.message
                        })
                    }).finally(() => {
                        this.isLoading = false
                        this.handleCancel()
                    })
            }
        }
    }
</script>
<style lang="scss" scoped>
    .apply-form {
        width: 98%;
    }
    ::v-deep .bk-dialog-header {
        text-align: left !important;
    }
    .deadline-wrapper {
        display: flex;
    }
    .deadline-btn {
        min-width: 100px;
    }
    .custom-time-select {
        width: 110px;
    }
    .expired {
        padding-right: 10px;
    }
    .new-expired {
        padding-left: 10px;
    }
    .arrows-icon {
        width: 12px;
        height: 12px;
    }
</style>
