<template>
    <bk-dialog
        v-model="isShow"
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
                :label="$t('project')"
                property="englishName"
                required
                error-display-type="normal"
            >
                <span v-if="projectCode">{{ formData.englishName }}</span>
                <bk-select
                    v-else
                    v-model="formData.englishName"
                    searchable
                    enable-scroll-load
                    @scroll-end="getProjectList"
                    :remote-method="handleSearchProject"
                >
                    <bk-option v-for="option in projectList"
                        :key="option.englishName"
                        :id="option.englishName"
                        :name="option.project_name">
                    </bk-option>
                </bk-select>
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
        name: 'ApplyProjectDialog',
        props: {
            projectCode: {
                type: String,
                default: ''
            }
        },
        data () {
            return {
                isShow: false,
                isLoading: false,
                projectList: [],
                hasNext: true, // 项目列表分页是否还有下一页
                pagination: {
                    page: 1,
                    pageSize: 20,
                    projectName: ''
                },
                customTime: 1,
                formData: {
                    expireTime: 0,
                    englishName: '',
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
                    englishName: [
                        {
                            required: true,
                            message: this.$t('pleaseSelectProject'),
                            trigger: 'blur'
                        }
                    ],
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
        watch: {
            isShow (val) {
                if (val) this.getProjectList()
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
                this.$refs.applyFrom.validate().then(async () => {
                    this.isLoading = true
                    await this.$store.dispatch('applyToJoinProject', {
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
                    })
                    this.isLoading = false
                    this.handleCancel()
                })
            },
            handleCancel () {
                this.isShow = false
                this.customTime = 1
                this.formData.expireTime = 0
                this.formData.reason = ''
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
            async getProjectList () {
                if (this.hasNext) {
                    await this.$store.dispatch('fetchWithoutPermissionsProjects', this.pagination).then(res => {
                        this.projectList = [...this.projectList, ...res.records]
                        this.hasNext = res.hasNext
                    })
                    this.pagination.page += 1
                }
            },
            handleSearchProject (search) {
                this.hasNext = true
                this.projectList = []
                this.pagination.page = 1
                this.pagination.projectName = search
                this.getProjectList()
            }
        }
    }
</script>
<style lang="scss" scoped>
    @import '../../assets/scss/conf';
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
