<template>
    <bk-dialog
        :value="isShow"
        :width="700"
        :title="$t('applyProject')"
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
                :label="$t('expirationTime')"
            >
                <span>{{ expiredTime }}</span>
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
            expiredTime: {
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
                    expireTime: 0
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
                    ]
                }
            }
        },
        created () {
            this.formData.expireTime = this.formatTimes(2592000)
            if (this.projectCode) {
                this.formData.englishName = this.projectCode
            }
        },
        methods: {
            handleConfirm () {
                if (this.currentActive === 'custom') {
                    const timestamp = this.customTime * 24 * 3600
                    this.formData.expireTime = this.formatTimes(timestamp)
                }
                this.isLoading = true
                this.$store.dispatch('applyToJoinProject', {
                    englishName: this.formData.englishName,
                    ApplicationInfo: this.formData
                }).then(res => {
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
            },
            handleCancel () {
                this.$emit('update:isShow', false)
                this.customTime = 1
                this.formData.expireTime = 0
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
</style>
