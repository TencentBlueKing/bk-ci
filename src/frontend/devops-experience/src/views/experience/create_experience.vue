<template>
    <div style="height: 100%;">
        <content-header>
            <div slot="left">{{ $t(`experience.${$route.meta.title}`) }}</div>
        </content-header>
        <section
            class="create-experience-wrapper sub-view-port"
            v-bkloading="loading"
        >
            <template v-if="!loading.isLoading">
                <bk-form
                    class="experience-form"
                    ref="form"
                    :model="createReleaseForm"
                    label-width="190"
                    :rules="rules"
                >
                    <template v-if="hasPermission">
                        <bk-form-item
                            :label="$t('experience.platform')"
                            property="platform"
                            required
                        >
                            <bk-radio-group
                                class="exp-platform-enum"
                                v-model="createReleaseForm.platform"
                            >
                                <bk-radio
                                    v-for="platform in platformList"
                                    :key="platform.id"
                                    :value="platform.id"
                                >
                                    {{ platform.name }}
                                </bk-radio>
                            </bk-radio-group>
                        </bk-form-item>
                        <div class="version-name">
                            <bk-form-item
                                :label="$t('experience.package_type')"
                                :required="true"
                                property="name"
                            >
                                <bk-input
                                    ref="releaseName"
                                    :placeholder="$t('experience.select_package_placeholder')"
                                    name="releaseName"
                                    disabled
                                    v-model="createReleaseForm.name"
                                />
                            </bk-form-item>
                            <span
                                :class="{ 'prompt-tips': true, 'is-unedit': isEdit }"
                                :disabled="isEdit"
                                @click="toShowPackageList"
                            >{{ $t('experience.get_from_repository') }}</span>
                        </div>
                        <bk-form-item
                            style="margin-top: 20px"
                            :label="$t('experience.app_name')"
                            desc-type="icon"
                            desc-icon="icon-info-circle"
                            :desc="$t('experience.app_name_tip')"
                            property="experienceName"
                            :required="appNameRequired"
                        >
                            <bk-input
                                v-model="createReleaseForm.experienceName"
                                :placeholder="$t('experience.enter_app_name')"
                                maxlength="20"
                            />
                        </bk-form-item>
                        <bk-form-item
                            :label="$t('experience.bundleIdentifier')"
                            required
                            property="bundleIdentifier"
                        >
                            <bk-input
                                :disabled="!isWindowsPlatform"
                                :placeholder="$t('experience.bundleIdentifierPlaceholder')"
                                v-model="createReleaseForm.bundleIdentifier"
                            />
                        </bk-form-item>
                        <bk-form-item
                            :label="$t('experience.version_title')"
                            required
                            desc-type="icon"
                            desc-icon="icon-info-circle"
                            :desc="$t('experience.version_title_tip')"
                            property="versionTitle"
                        >
                            <bk-input
                                v-model="createReleaseForm.versionTitle"
                                :placeholder="$t('experience.enter_version_title')"
                                :rule="[{ required: true }]"
                                maxlength="100"
                            />
                        </bk-form-item>
                        <bk-form-item
                            :label="$t('experience.version_number')"
                            required
                            property="version_no"
                        >
                            <bk-input
                                :disabled="!isWindowsPlatform"
                                v-model="createReleaseForm.version_no"
                            />
                        </bk-form-item>
                        <bk-form-item
                            style="margin-top: 20px"
                            :label="$t('experience.group_identifier')"
                            desc-type="icon"
                            desc-icon="icon-info-circle"
                            :desc="$t('experience.group_identifier_tip')"
                            property="classify"
                        >
                            <bk-input
                                v-model="createReleaseForm.classify"
                                :placeholder="$t('experience.enter_group_identifier')"
                                maxlength="20"
                            />
                        </bk-form-item>
                        <bk-form-item
                            :label="$t('experience.version_description')"
                            property="desc"
                            :required="true"
                        >
                            <bk-input
                                type="textarea"
                                :placeholder="$t('experience.enter_version_description')"
                                maxlength="2000"
                                name="releaseDesc"
                                v-model="createReleaseForm.desc"
                            >
                            </bk-input>
                        </bk-form-item>
                        <bk-form-item
                            :label="$t('experience.product_category')"
                            :required="true"
                            property="categoryId"
                        >
                            <bk-select v-model="createReleaseForm.categoryId">
                                <bk-option
                                    v-for="option in categoryList"
                                    :key="option.id"
                                    :id="option.id"
                                    :name="option.name"
                                >
                                </bk-option>
                            </bk-select>
                        </bk-form-item>
                        <bk-form-item
                            :label="$t('experience.product_owner')"
                            :required="true"
                            property="productOwner"
                        >
                            <bk-member-selector
                                v-model="createReleaseForm.productOwner"
                                :rule="[{ required: true }]"
                                :placeholder="$t('experience.enter_product_owner')"
                            />
                        </bk-form-item>
                        <bk-form-item
                            :label="$t('experience.experience_end_time')"
                            :required="true"
                            property="end_date"
                        >
                            <bk-date-picker
                                :placeholder="$t('experience.select_experience_end_time')"
                                v-model="createReleaseForm.end_date"
                                :start-date="query.beginDate"
                                :quick-select="false"
                            >
                            </bk-date-picker>
                        </bk-form-item>
                        <bk-form-item
                            :label="$t('experience.experience_scope')"
                            :required="true"
                        >
                            <bk-radio-group v-model="experienceRange">
                                <bk-radio
                                    value="internals"
                                    class="mr20"
                                >
                                    {{ $t('experience.internal_experience_group') }}
                                </bk-radio>
                                <bk-radio value="public">{{ $t('experience.public_experience') }}</bk-radio>
                            </bk-radio-group>
                        </bk-form-item>
                        <bk-form-item
                            v-show="isInerExp"
                            :label="$t('experience.experience_group')"
                            required
                            property="experienceGroups"
                        >
                            <div class="bkdevop-checkbox-group">
                                <bk-checkbox
                                    class="exp-group-item"
                                    v-for="(col, index) in experienceGroup"
                                    :key="index"
                                    v-model="col.isChecked"
                                    @change="handleGroupChange"
                                >
                                    <span class="exp-group-item-content">
                                        <span class="exp-group-item-name">{{ col.name }}</span>
                                        <bk-popover
                                            :delay="[300, 0]"
                                            max-width="600"
                                            placement="bottom"
                                        >
                                            <i class="devops-icon icon-member-list"></i>
                                            <div
                                                class="exp-group-popup-box"
                                                slot="content"
                                            >
                                                <p
                                                    v-for="item in expGroupPopupConf"
                                                    :key="item.key"
                                                    class="exp-group-popup-item"
                                                >
                                                    <span>{{ item.typeLabel }}：</span>
                                                    <span>{{ col[item.key].join(', ') }}</span>
                                                </p>
                                            </div>
                                        </bk-popover>
                                    </span>
                                </bk-checkbox>
                            </div>
                            <span
                                class="create-group-entry"
                                @click="toCreateGroup"
                            >
                                <i class="devops-icon icon-plus-circle" />
                                {{ $t('experience.add_experience_group') }}
                            </span>
                        </bk-form-item>
                        <bk-form-item
                            v-show="isInerExp"
                            :label="$t('experience.temp_experience_personnel_internal')"
                            desc-type="icon"
                            desc-icon="icon-info-circle"
                            :desc="$t('experience.temp_experience_personnel_internal_tip')"
                            property="internal_list"
                        >
                            <bk-member-selector
                                :placeholder="$t('experience.enter_english_name')"
                                name="internalList"
                                v-model="createReleaseForm.internal_list"
                            ></bk-member-selector>
                        </bk-form-item>
                        <bk-form-item
                            v-show="isInerExp"
                            :label="$t('experience.temp_experience_personnel_external')"
                            property="external_list"
                        >
                            <bk-select
                                :disabled="false"
                                v-model="createReleaseForm.external_list"
                                ext-cls="select-custom"
                                ext-popover-cls="select-popover-custom"
                                multiple
                                searchable
                            >
                                <bk-option
                                    v-for="option in outersList"
                                    :key="option.id"
                                    :id="option.id"
                                    :name="option.name"
                                >
                                </bk-option>
                            </bk-select>
                        </bk-form-item>
                        <bk-form-item
                            :label="$t('experience.notification_method')"
                            v-bind="notifyDesc"
                        >
                            <div
                                v-if="isInerExp"
                                class="bkdevop-checkbox-group notify-group"
                            >
                                <bk-checkbox
                                    v-for="(col, index) in noticeTypeList"
                                    :key="index"
                                    v-model="col.isChecked"
                                >
                                    {{ col.name }}
                                    <i
                                        v-if="col.placeholder"
                                        v-bk-tooltips="col.placeholder"
                                        class="bk-icon icon-info-circle"
                                    />
                                </bk-checkbox>
                            </div>
                            <bk-checkbox
                                class="enable-wechat-group"
                                name="enableWechatGroups"
                                v-model="createReleaseForm.enableWechatGroups"
                            >
                                {{ $t('experience.enable_enterprise_wechat_notification') }}
                                <span
                                    v-bk-tooltips="groupIdDesc"
                                    class="top-start"
                                >
                                    <i class="bk-icon icon-info-circle" />
                                </span>
                            </bk-checkbox>
                        </bk-form-item>
                        <group-id-selector
                            class="item-groupid"
                            v-if="createReleaseForm.enableWechatGroups"
                            :handle-change="groupIdChange"
                            :value="createReleaseForm.wechatGroups"
                            :placeholder="$t('experience.enter_group_id')"
                            icon-class="icon-question-circle"
                            desc-direction="top"
                        >
                        </group-id-selector>
                    </template>
                </bk-form>
                <div class="submit-btn-bar">
                    <bk-button
                        v-perm="{
                            tooltips: '没有权限',
                            permissionData: {
                                projectId: projectId,
                                resourceType: EXPERIENCE_TASK_RESOURCE_TYPE,
                                resourceCode: projectId,
                                action: EXPERIENCE_TASK_RESOURCE_ACTION.CREATE
                            }
                        }"
                        theme="primary"
                        key="submitBtn"
                        @click.prevent="beforeSubmit"
                    >
                        {{ submitText }}
                    </bk-button>
                    <bk-button
                        theme="default"
                        @click="cancel"
                    >
                        {{ $t('experience.cancel') }}
                    </bk-button>
                </div>
            </template>
            <div
                class="metadata-box"
                v-if="metaList.length"
            >
                <div class="title">{{ $t('experience.metadata') }}</div>
                <div class="data-head">
                    <div class="key-head">{{ $t('experience.key') }}</div>
                    <div class="value-head">{{ $t('experience.value') }}</div>
                </div>
                <div
                    class="data-row"
                    v-for="(row, index) in metaList"
                    :key="index"
                >
                    <div class="key-item">{{ row.key }}</div>
                    <div
                        class="value-item"
                        :title="row.value"
                    >
                        {{ row.value }}
                    </div>
                </div>
            </div>
            <experience-group
                v-bind="groupSideslider"
                :create-group-form="createGroupForm"
                :handle-group-field-change="handleGroupFieldChange"
                :error-handler="errorHandler"
                @after-submit="afterCreateGroup"
                :cancel-fn="cancelFn"
            >
            </experience-group>

            <version-package
                :platform="createReleaseForm.platform"
                :version-select-conf="versionSelectConf"
                :loading="packageLoading"
                :confirm-fn="confirmSelect"
                :cancel-fn="cancelSelect"
            />
        </section>
    </div>
</template>

<script>
    import GroupIdSelector from '@/components/common/groupIdSelector'
    import {
        EXPERIENCE_TASK_RESOURCE_ACTION,
        EXPERIENCE_TASK_RESOURCE_TYPE,
        PIPELINE_RESOURCE_ACTION,
        PIPELINE_RESOURCE_TYPE
    } from '@/utils/permission'
    import { convertTime, platformMap } from '@/utils/util'
    import { mapGetters } from 'vuex'
    import experienceGroup from './create_group'
    import versionPackage from './version_package'

    export default {
        components: {
            experienceGroup,
            versionPackage,
            GroupIdSelector
        },
        data () {
            return {
                hasPermission: true,
                curPipelineId: '',
                curPipelineName: '',
                experienceGroup: [],
                groupIdStorage: [],
                metaList: [],
                outersList: [],
                loading: {
                    isLoading: false,
                    title: ''
                },
                query: {},
                experienceRange: 'internals',
                createReleaseForm: {
                    platform: platformMap.ANDROID,
                    name: '',
                    version_no: '',
                    versionTitle: '',
                    desc: '',
                    experienceName: '',
                    categoryId: null,
                    productOwner: [],
                    end_date: new Date(),
                    internal_list: [],
                    external_list: [],
                    notice_list: '',
                    wechatGroups: '',
                    enableWechatGroups: false,
                    experienceGroups: [],
                    classify: ''
                },
                packageLoading: {
                    isLoading: false,
                    title: ''
                },
                tipLoading: {
                    isLoading: true,
                    title: ''
                },
                versionSelectConf: {
                    isShow: false,
                    closeIcon: false,
                    hasHeader: false,
                    quickClose: false,
                    confirmText: this.$t('experience.confirm')
                },
                groupSideslider: {
                    title: '',
                    visible: false,
                    isLoading: false
                },
                createGroupForm: {
                    name: '',
                    members: [],
                    remark: ''
                  
                },
                errorHandler: {
                    nameError: false
                },
                PIPELINE_RESOURCE_ACTION,
                PIPELINE_RESOURCE_TYPE,
                EXPERIENCE_TASK_RESOURCE_TYPE,
                EXPERIENCE_TASK_RESOURCE_ACTION
            }
        },
        computed: {
            ...mapGetters('experience', [
                'getSelectedFile',
                'getUserGroup'
            ]),
            rules () {
                return {
                    name: [
                        {
                            required: true,
                            message: (field) => this.$t('experience.noEmptyTips', [field]),
                            trigger: 'blur'
                        }
                    ],
                    experienceName: [
                        {
                            required: true,
                            message: (field) => this.$t('experience.noEmptyTips', [field]),
                            trigger: 'blur'
                        }
                    ],
                    bundleIdentifier: [
                        {
                            required: this.isWindowsPlatform,
                            message: (field) => this.$t('experience.noEmptyTips', [field]),
                            trigger: 'blur'
                        }
                    ],
                    versionTitle: [{
                        required: this.appNameRequired,
                        message: (field) => this.$t('experience.noEmptyTips', [field]),
                        trigger: 'blur'
                    }
                    ],
                    desc: [
                        {
                            required: true,
                            message: (field) => this.$t('experience.noEmptyTips', [field]),
                            trigger: 'blur'
                        }
                    ],
                    version_no: [
                        {
                            required: this.isWindowsPlatform,
                            message: (field) => this.$t('experience.noEmptyTips', [field]),
                            trigger: 'blur'
                        }
                    ],
                    end_date: [
                        {
                            required: true,
                            message: (field) => this.$t('experience.noEmptyTips', [field]),
                            trigger: 'blur'
                        }
                    ],
                    categoryId: [
                        {
                            required: true,
                            message: (field) => this.$t('experience.noEmptyTips', [field]),
                            trigger: 'blur'
                        }
                    ],
                    productOwner: [
                        {
                            required: true,
                            message: (field) => this.$t('experience.noEmptyTips', [field]),
                            trigger: 'blur'
                        }
                    ],
                    experienceGroups: [
                        {
                            required: true,
                            validator: (value) => {
                                return Array.isArray(value) && value.length > 0
                            },
                            message: field => this.$t('experience.noEmptyTips', [field]),
                            trigger: 'blur'
                        }
                    ]
                }
            },
            isWindowsPlatform () {
                return this.createReleaseForm?.platform === platformMap.WINDOWS
            },
            appNameRequired () {
                return this.isWindowsPlatform
            },
            groupIdDesc () {
                return {
                    content: this.$t('experience.enable_group_tips'),
                    maxWidth: 300
                }
            },
            platformList () {
                return Object.keys(platformMap).map(key =>({
                    id: key,
                    name: this.$t(`experience.platform_labels.${key}`)
                }))
            },
            categoryList () {
                return [
                    { id: 1, name: this.$t('experience.game') },
                    { id: 2, name: this.$t('experience.tool') },
                    { id: 3, name: this.$t('experience.live') },
                    { id: 4, name: this.$t('experience.social') }
                ]
            },
            noticeTypeList () {
                return [
                    ...(this.isWindowsPlatform ? [] : [{
                        name: this.$t('experience.app_push'),
                        value: 'PUSH',
                        isChecked: true,
                        placeholder: this.$t('experience.app_push_placeholder')
                    }]),
                    { name: this.$t('experience.rtx'), value: 'RTX', isChecked: false },
                    { name: this.$t('experience.mail'), value: 'EMAIL', isChecked: false }
                ]
            },
            pathName () {
                return this.$route.name
            },
            isEdit () {
                return this.pathName === 'editExperience'
            },
            projectId () {
                return this.$route.params.projectId
            },
            experienceHashId () {
                return this.$route.params.experienceId
            },
            innerTitle () {
                return this.$t(`experience.${this.isEdit ? 'editExp' : 'addExp'}`)
            },
            submitText () {
                return this.$t(`experience.${this.isEdit ? 'updateExp' : 'toExp'}`)
            },
            isInerExp () {
                return this.experienceRange === 'internals'
            },
            isPublicExp () {
                return this.experienceRange === 'public'
            },
            expGroupPopupConf () {
                return [{
                    typeLabel: this.$t('experience.innerMember'),
                    key: 'innerUsers'
                }, {
                    typeLabel: this.$t('experience.innerOrgs'),
                    key: 'depts'
                }, {
                    typeLabel: this.$t('experience.outerMember'),
                    key: 'outerUsers'
                }]
            },
            isAlphaApk () {
                return !!this.metaList.find(item => item.key === 'BK-CI-APP-STAGE' && item.value === 'Alpha')
            },
            
            createInnerApkExpTips () {
                return this.$t(`experience.${this.isPublicExp ? 'publicExpTips' : 'innerExpTips'}`, [this.createReleaseForm.name])
            },
            notifyDesc () {
                return this.isPublicExp
                    ? {}
                    : {
                        desc: this.$t('experience.over2000Tips'),
                        descType: 'icon',
                        descIcon: 'icon-info-circle'
                    }
            }
        },
        watch: {
            '$route' (path) {
                if (!this.isEdit) {
                    this.requestGroupList()
                    this.noticeTypeList.forEach(item => {
                        item.isChecked = item.value === 'PUSH'
                    })
                    this.metaList = []
                    this.query.initDate = ''
                    this.createReleaseForm = {
                        name: '',
                        version_no: '',
                        desc: '',
                        end_date: '',
                        internal_list: [],
                        external_list: [],
                        notice_list: '',
                        versionTitle: '',
                        experienceName: '',
                        categoryId: null,
                        productOwner: [],
                        experienceGroups: []
                    }
                }
            },
            projectId (val) {
                this.toExperienceList()
            }
        },
        async created () {
            await this.requestGroupList()
            this.query.beginDate = new Date()
            if (this.isEdit) {
                this.requestExperienceDetail()
            }
        },
        mounted () {
            this.groupIdStorage = localStorage.getItem('groupIdStr') ? localStorage.getItem('groupIdStr').split(';').filter(item => item) : []
        },
        methods: {
            /**
             * 获取体验组列表
             */
            async requestGroupList (repect) {
                if (!repect) {
                    this.loading.isLoading = true
                }

                try {
                    const res = await this.$store.dispatch('experience/requestGroupList', {
                        projectId: this.projectId,
                        params: {
                            returnPublic: false
                        }
                    })
                    this.experienceGroup.splice(0, this.experienceGroup.length)
                    res.records.forEach(item => {
                        item.isChecked = false
                        item.isHover = false
                        this.experienceGroup.push(item)
                    })
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    this.loading.isLoading = false
                }
            },
            handleGroupChange (val) {
                const newExperienceGroups = []
                this.experienceGroup.forEach(item => {
                    if (item.isChecked) {
                        newExperienceGroups.push(item.groupHashId)
                    }
                })
                this.createReleaseForm.experienceGroups = newExperienceGroups
            },

            /**
             * 获取体验详情
             */
            async requestExperienceDetail () {
                this.loading.isLoading = true

                try {
                    const res = await this.$store.dispatch('experience/requestExperienceDetail', {
                        projectId: this.projectId,
                        experienceHashId: this.experienceHashId
                    })
                    this.createReleaseForm.platform = res.platform
                    this.createReleaseForm.name = res.name
                    this.createReleaseForm.path = res.path
                    this.createReleaseForm.artifactoryType = res.artifactoryType
                    this.createReleaseForm.version_no = res.version
                    this.createReleaseForm.end_date = convertTime(res.expireDate * 1000).split(' ')[0]
                    this.query.initDate = this.createReleaseForm.end_date
                    this.createReleaseForm.desc = res.remark
                    this.createReleaseForm.versionTitle = res.versionTitle
                    this.createReleaseForm.experienceName = res.experienceName
                    this.createReleaseForm.categoryId = res.categoryId
                    this.createReleaseForm.productOwner = res.productOwner
                    this.createReleaseForm.external_list = res.outerUsers
                    this.createReleaseForm.internal_list = res.innerUsers
                    this.createReleaseForm.enableWechatGroups = res.enableWechatGroups
                    this.createReleaseForm.experienceGroups = res.experienceGroups
                    this.createReleaseForm.classify = res.classify
                    // 体验组如果为kygplomw,选中公开体验
                    const publicGroup = this.createReleaseForm.experienceGroups.find(item => item.groupHashId === 'kygplomw')
                    this.experienceRange = publicGroup ? 'public' : 'internals'
                    if (res.enableWechatGroups) {
                        this.createReleaseForm.wechatGroups = res.wechatGroups
                    }

                    this.getFileInfo(res.path, res.artifactoryType)

                    res.experienceGroups.forEach(vv => {
                        this.experienceGroup.forEach(kk => {
                            if (vv.groupHashId === kk.groupHashId) {
                                kk.isChecked = true
                            }
                        })
                    })

                    res.notifyTypes.forEach(vv => {
                        this.noticeTypeList.forEach(kk => {
                            if (vv === kk.value) {
                                kk.isChecked = true
                            }
                        })
                    })
                } catch (e) {
                    this.handleError(
                        e,
                        {
                            projectId: this.projectId,
                            resourceType: EXPERIENCE_TASK_RESOURCE_TYPE,
                            resourceCode: this.experienceHashId,
                            action: EXPERIENCE_TASK_RESOURCE_ACTION.EDIT
                        }
                    )
                } finally {
                    this.loading.isLoading = false
                }
            },
            async getFileInfo (path, type) {
                try {
                    const res = await this.$store.dispatch('experience/requestMetaList', {
                        projectId: this.projectId,
                        artifactoryType: type,
                        path: path
                    })

                    this.metaList.splice(0, this.metaList.length)
                    res.forEach(item => {
                        this.metaList.push(item)
                    })

                    for (let i = this.metaList.length - 1; i >= 0; i--) {
                        if ((this.metaList[i].key === 'buildId') || (this.metaList[i].key === 'pipelineId')
                            || (this.metaList[i].key === 'projectId') || (this.metaList[i].key === 'source')) {
                            this.metaList.splice(i, 1)
                        }
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            cancel () {
                this.$bkInfo({
                    title: this.$t('experience.dialog.leave_warning'),
                    type: 'warning',
                    theme: 'warning',
                    confirmFn: () => {
                        this.toExperienceList()
                    }
                })
            },
            selectGroups (col) {
                this.experienceGroup.forEach(item => {
                    if (col.groupHashId === item.groupHashId) {
                        item.isChecked = !item.isChecked
                    }
                })
            },
            selectedNotice (col) {
                this.noticeTypeList.forEach(item => {
                    if (col.name === item.name) {
                        item.isChecked = !item.isChecked
                    }
                })
            },
            toCreateGroup () {
                this.createGroupForm = {
                    name: '',
                    members: [],
                    remark: ''
                }
                this.groupSideslider.title = this.$t('experience.add_experience_group')
                this.groupSideslider.visible = true
            },
            handleGroupFieldChange (name, value) {
                this.createGroupForm[name] = value
            },
            
            productResult () {
                this.createReleaseForm.productOwner = this.createReleaseForm.productOwner.split(',')
            },
            afterCreateGroup () {
                this.requestGroupList()
                this.groupSideslider.visible = false
            },
            cancelFn () {
                if (!this.groupSideslider.isLoading) {
                    this.groupSideslider.visible = false
                }
            },
            toShowPackageList () {
                if (!this.isEdit) {
                    this.versionSelectConf.isShow = true
                }
            },
            async confirmSelect () {
                if (!this.packageLoading.isLoading) {
                    if (this.getSelectedFile.name) {
                        this.packageLoading.isLoading = true
                        this.versionSelectConf.confirmText = this.$t('experience.submiting')

                        const obj = this.getSelectedFile
                        
                        try {
                            const res = await this.$store.dispatch('experience/requestMetaList', {
                                projectId: this.projectId,
                                artifactoryType: obj.artifactoryType,
                                path: obj.fullPath
                            })
                            
                            this.metaList = res.map(item => {
                                if (item.key === 'appVersion') {
                                    this.createReleaseForm.version_no = item.value
                                }
                                if (item.key === 'pipelineId') {
                                    this.curPipelineId = item.value
                                }
                                if (item.key === 'pipelineName') {
                                    this.curPipelineName = item.value
                                }
                                if (item.key === 'bundleIdentifier' && !!item.value) {
                                    this.createReleaseForm.bundleIdentifier = item.value
                                }
                                return item
                            })

                            for (let i = this.metaList.length - 1; i >= 0; i--) {
                                if ((this.metaList[i].key === 'buildId') || (this.metaList[i].key === 'pipelineId')
                                    || (this.metaList[i].key === 'projectId') || (this.metaList[i].key === 'source')) {
                                    this.metaList.splice(i, 1)
                                }
                            }

                            Object.assign(this.createReleaseForm, {
                                name: obj.name,
                                path: obj.fullPath,
                                artifactoryType: obj.artifactoryType
                            })
                            
                        } catch (err) {
                            const message = err.message ? err.message : err
                            const theme = 'error'

                            this.$bkMessage({
                                message,
                                theme
                            })
                        } finally {
                            this.versionSelectConf.isShow = false
                            this.packageLoading.isLoading = false
                            this.versionSelectConf.confirmText = this.$t('experience.confirm')
                        }
                    }
                }
            },
            cancelSelect () {
                if (!this.packageLoading.isLoading) {
                    this.versionSelectConf.isShow = false
                    this.packageLoading.isLoading = false
                    this.versionSelectConf.confirmText = this.$t('experience.confirm')
                }
            },
            groupIdChange (name, value) {
                this.createReleaseForm.wechatGroups = value
            },
            toggleEnable (name, value) {
                this.createReleaseForm.enableWechatGroups = value
            },
            // 补全末尾分号
            wechatGroupCompletion () {
                const wechatGroup = this.createReleaseForm.wechatGroups
                let targetGro = ''
                if (wechatGroup && wechatGroup.charAt(wechatGroup.length - 1) !== ';') {
                    this.createReleaseForm.wechatGroups += ';'
                    targetGro = this.createReleaseForm.wechatGroups
                } else {
                    targetGro = this.createReleaseForm.wechatGroups
                }

                return targetGro
            },
            setGroupidStorage (data) {
                if (!this.createReleaseForm.enableWechatGroups) {
                    return false
                }
                data.split(';').filter(item => item).forEach(item => {
                    if (!this.groupIdStorage.includes(item)) {
                        this.groupIdStorage.push(item)
                    }
                })
                localStorage.setItem('groupIdStr', this.groupIdStorage.sort().join(';'))
            },
            beforeSubmit () {
                if (!this.isAlphaApk) {
                    this.submitFn()
                    return
                }
                this.$bkInfo({
                    subTitle: this.createInnerApkExpTips,
                    type: 'warning',
                    confirmFn: () => {
                        this.submitFn()
                    }
                })
            },
            async submitFn () {
                if (this.isPublicExp) {
                    this.createReleaseForm.experienceGroups = ['kygplomw']
                    this.createReleaseForm.internal_list = []
                    this.createReleaseForm.external_list = []
                }
                const validate = await this.$refs.form.validate()
                if (!validate) {
                    return
                }
                if (this.isInerExp) {
                    // 如果为内部体验组，取选中体验组id
                    const newExperienceGroups = []
    
                    this.experienceGroup.forEach(item => {
                        if (item.isChecked) {
                            newExperienceGroups.push(item.groupHashId)
                        }
                    })
                    this.createReleaseForm.experienceGroups = newExperienceGroups
                }
                const params = {
                    name: this.createReleaseForm.name,
                    outerUsers: this.createReleaseForm.external_list,
                    innerUsers: this.createReleaseForm.internal_list,
                    version: this.createReleaseForm.version_no,
                    notifyTypes: [],
                    expireDate: Date.parse(this.createReleaseForm.end_date) / 1000,
                    remark: this.createReleaseForm.desc || undefined,
                    ...this.createReleaseForm
                }
                if (this.createReleaseForm.enableWechatGroups) {
                    params.wechatGroups = this.wechatGroupCompletion()
                } else {
                    params.wechatGroups = ''
                }

                this.noticeTypeList.forEach(item => {
                    if (item.isChecked) {
                        params.notifyTypes.push(item.value)
                    }
                })
            
                let message, theme
            
                this.loading.isLoading = true
                try {
                    if (this.$route.params.experienceId) {
                        await this.$store.dispatch('experience/editExperience', {
                            projectId: this.projectId,
                            experienceHashId: this.experienceHashId,
                            params
                        })
            
                        message = this.$t('experience.editExpSuccess')
                        theme = 'success'
                    } else {
                        const payload = {
                            path: params.path,
                            artifactoryType: params.artifactoryType
                        }
            
                        const result = await this.$store.dispatch('experience/requestHasPermission', {
                            projectId: this.projectId,
                            payload
                        })
            
                        if (result) {
                            await this.$store.dispatch('experience/createExperience', {
                                projectId: this.projectId,
                                params
                            })
            
                            message = this.$t('experience.addExpSuccess')
                            theme = 'success'
                        } else {
                            this.handleNoPermission({
                                projectId: this.projectId,
                                resourceType: PIPELINE_RESOURCE_TYPE,
                                resourceCode: this.curPipelineId,
                                action: PIPELINE_RESOURCE_ACTION.EXECUTE
                            })
                        }
                    }
                } catch (err) {
                    message = err.message ? err.message : err
                    theme = 'error'
                } finally {
                    if (message) {
                        this.$bkMessage({
                            message,
                            theme
                        })
                    }
                            
                    this.loading.isLoading = false
            
                    if (theme === 'success') {
                        if (this.createReleaseForm.enableWechatGroups) {
                            this.setGroupidStorage(params.wechatGroups)
                        }
                        this.toExperienceList()
                    }
                }
                
            },
            
            toExperienceList () {
                this.$router.push({
                    name: 'experienceList',
                    params: {
                        projectId: this.projectId
                    }
                })
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';
    @import '@/scss/mixins/ellipsis';
    .create-experience-wrapper {
        .experience-form {
            width: 800px;
            .bkdevop-checkbox-group {
                &.notify-group {
                    display: grid;
                    grid-gap: 20px;
                    grid-auto-flow: column;
                    height: 32px;
                    align-items: center;
                    grid-template-columns: repeat(3, 1fr);
                    margin-bottom: 10px;
                    .bk-form-checkbox {
                        display: flex;
                        .bk-checkbox-text {
                            flex: 1;
                            @include ellipsis();
                        }
                    }
                }
                
                .exp-group-item {
                    .icon-member-list {
                        display: none;
                    }
                    .exp-group-item-content {
                        display: flex;
                        align-items: center;
                        width: 170px;
                        margin-right: 10px;
                        .exp-group-item-name {
                            flex: 1;
                            @include ellipsis();
                        }
                    }
                    &:hover {
                        .icon-member-list {
                            display: inline-block;
                        }
                    }
                }
            }
            .create-group-entry {
                cursor: pointer;
                color: $primaryColor;
            }
            .exp-platform-enum {
                display: flex;
                align-items: center;
                gap: 20px;
                height: 32px;
    }
            .version-name {
                margin-top: 20px;
                display: flex;
                align-items: center;
                .bk-form-item {
                    flex: 1;
                    margin-right: 10px;
                }
                .prompt-tips {
                    cursor: pointer;
                    color: $primaryColor;
                    &.is-unedit {
                        color: $fontLighterColor;
                    }
                }
            }
            .item-groupid {
                margin-top: 12px;
                margin-left: 40px;
            }
        }
        
        .submit-btn-bar {
            width: 800px;
            text-align: center;
            padding-top: 20px;
        }

        .metadata-box {
            position: absolute;
            top: 38px;
            left: 840px;
            width: 420px;
            min-width: 420px;
            height: min-content;
            border: 1px solid $borderWeightColor;
            border-radius: 2px;

            &:before {
                content: '';
                position: absolute;
                width: 8px;
                height: 8px;
                font-size: 0;
                line-height: 0;
                overflow: hidden;
                border-width: 1px;
                border-style: solid solid solid solid;
                border-color: $borderWeightColor transparent transparent $borderWeightColor;
                background-color: #f5f7fa;
                top: 12px;
                left: -5px;
                transform: rotate(45deg);
                -webkit-transform: rotate(-45deg);
            }

            .title {
                padding-left: 16px;
                line-height: 42px;
                border-bottom: 1px solid $borderWeightColor;
                font-weight: bold;
                color: #333C48;
            }

            .data-head,
            .data-row {
                display: flex;
                line-height: 42px;
                background-color: $bgHoverColor;
            }

            .data-head {
                color: #333C48;
            }

            .key-head,
            .value-head,
            .key-item,
            .value-item {
                flex: 2;
                padding-left: 16px;
            }

            .value-item {
                width: 200px;
                padding-right: 10px;
                overflow: hidden;
                text-overflow: ellipsis;
            }

            .key-head {
                border-right: 1px solid $borderWeightColor;
            }

            .data-row {
                background-color: #fff;
                border-top: 1px solid $borderWeightColor;
                font-size: 12px;
            }
        }
    }
    .exp-group-popup-box {
        display: grid;
        grid-template-rows: auto;
        grid-gap: 10px;

        .exp-group-popup-item {
            display: flex;
            align-items: flex-start;
            word-break: break-all;
            > span:first-child {
                flex-shrink: 0;
            }
            font-weight: 400;
        }
    }
</style>
