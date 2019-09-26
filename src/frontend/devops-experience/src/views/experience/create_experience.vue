<template>
    <div style="height: 100%;">
        <content-header>
            <div slot="left">{{ $route.meta.title }}</div>
        </content-header>
        <section class="create-experience-wrapper sub-view-port"
            v-bkloading="{
                isLoading: loading.isLoading,
                title: loading.title
            }">
            <template v-if="!loading.isLoading">
                <bk-form class="experience-form" v-model="createReleaseForm">
                    <template v-if="hasPermission">
                        <div class="version-name">
                            <bk-form-item label="版本名称" :required="true" property="name" :rule="[{ required: true }]">
                                <bk-input
                                    placeholder="请从版本仓库中选择一个ipa或apk文件"
                                    name="releaseName"
                                    disabled
                                    v-model="createReleaseForm.name"
                                />
                            
                            </bk-form-item>
                            <span :class="{ 'prompt-tips': true, 'is-unedit': isEdit }" :disabled="isEdit" @click="toShowPackageList">从版本仓库获取</span>
                        </div>
                        <bk-form-item style="margin-top: 20px" label="版本号">
                            <span class="version-number-info">{{ createReleaseForm.version_no || '--' }}</span>
                        </bk-form-item>
                        <bk-form-item label="版本描述" property="desc">
                            <bk-input
                                type="textarea"
                                placeholder="请填写版本描述"
                                maxlength="128"
                                name="releaseDesc"
                                v-model="createReleaseForm.desc"
                                :class="{ 'is-error': errorFormHandler.nameError }"
                            >
                            </bk-input>
                        </bk-form-item>
                        <bk-form-item label="体验结束日期" :required="true" property="end_date">
                            <bk-date-picker
                                placeholder="请选择体验结束日期"
                                v-model="createReleaseForm.end_date"
                                :start-date="query.beginDate"
                                :quick-select="false"
                            >
                            </bk-date-picker>
                        </bk-form-item>
                        <bk-form-item label="选择体验组" :required="true">
                            <div class="bkdevop-checkbox-group">
                                <bk-checkbox v-for="(col, index) in experienceGroup" :key="index" v-model="col.isChecked" class="exp-group-item">
                                    {{ col.name }}
                                    <bk-popover :delay="500" placement="bottom">
                                        <i class="bk-icon icon-member-list"></i>
                                        <template slot="content">
                                            <p style="max-width: 300px; text-align: left; white-space: normal;word-break: break-all;font-weight: 400;">内部人员名单：
                                                <span v-for="(entry, uIndex) in col.innerUsers" :key="uIndex">{{ entry.replace('"', '') }}<span v-if="index !== (col.innerUsers.length - 1)">,</span></span>
                                            </p>
                                            <p style="max-width: 300px; text-align: left; white-space: normal;word-break: break-all;font-weight: 400;">外部QQ名单：
                                                <span>{{ col.outerUsers }}</span>
                                            </p>
                                        </template>
                                    </bk-popover>
                                </bk-checkbox>
                            </div>
                            <span class="create-group-entry" @click="toCreateGroup">
                                <i class="bk-icon icon-plus-circle" />
                                新增体验组
                            </span>
                        </bk-form-item>
                        <bk-form-item label="附加内部人员" property="internal_list">
                            <bk-member-selector placeholder="仅填写项目组内的人员有效" name="internalList" v-model="createReleaseForm.internal_list"></bk-member-selector>
                        </bk-form-item>
                        <bk-form-item label="附加外部QQ" property="external_list">
                            <bk-input type="textarea"
                                placeholder="请输入QQ号，以逗号或分号分隔"
                                name="externalList"
                                v-model="createReleaseForm.external_list"
                                :onkeyup="textdisplayResult()"
                            >
                            </bk-input>
                        </bk-form-item>
                        <bk-form-item label="通知方式">
                            <div class="bkdevop-checkbox-group">
                                <bk-checkbox v-for="(col, index) in noticeTypeList" :key="index" v-model="col.isChecked">{{ col.name }}</bk-checkbox>
                            </div>
                            <bk-checkbox
                                class="enable-wechat-group"
                                name="enableWechatGroups"
                                :title="groupIdDesc"
                                v-model="createReleaseForm.enableWechatGroups"
                            >
                                启用企业微信群通知
                            </bk-checkbox>
                        </bk-form-item>
                        <group-id-selector class="item-groupid" v-if="createReleaseForm.enableWechatGroups"
                            :handle-change="groupIdChange"
                            :value="createReleaseForm.wechatGroups"
                            placeholder="请输入群ID，多个群ID以分号隔开"
                            icon-class="icon-question-circle"
                            desc-direction="top">
                        </group-id-selector>
                    </template>
                </bk-form>
                <div class="submit-btn-bar">
                    <bk-button theme="primary" @click.prevent="submitFn">{{ submitText }}</bk-button>
                    <bk-button theme="default" @click="cancel">取消</bk-button>
                </div>
            </template>
            <div class="metadata-box" v-if="metaList.length">
                <div class="title">元数据</div>
                <div class="data-head">
                    <div class="key-head">键</div>
                    <div class="value-head">值</div>
                </div>
                <div class="data-row" v-for="(row, index) in metaList" :key="index">
                    <div class="key-item">{{ row.key }}</div>
                    <div class="value-item" :title="row.value">{{ row.value }}</div>
                </div>
            </div>
            <experience-group :node-select-conf="nodeSelectConf"
                :create-group-form="createGroupForm"
                :loading="dialogLoading"
                :on-change="onChange"
                :error-handler="errorHandler"
                :display-result="displayResult"
                @after-submit="afterCreateGroup"
                :cancel-fn="cancelFn"></experience-group>

            <version-package :version-select-conf="versionSelectConf"
                :loading="packageLoading"
                :confirm-fn="confirmSelect"
                :cancel-fn="cancelSelect"></version-package>
        </section>
    </div>
</template>

<script>
    import { mapGetters } from 'vuex'
    import experienceGroup from './create_group'
    import versionPackage from './version_package'
    import { convertTime } from '@/utils/util'
    import GroupIdSelector from '@/components/common/groupIdSelector'

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
                defaultDate: '2018-05-04',
                groupIdDesc: '可发通知至企业微信群。群ID获取方法：将"DevOpsRobot(蓝盾机器人)" 拉进群，手动@DevOpsRobot(蓝盾机器人)并输入关键字"会话ID",发送后即可获取群ID',
                experienceGroup: [],
                groupIdStorage: [],
                noticeTypeList: [
                    { name: '企业微信', value: 'RTX', isChecked: false },
                    { name: '邮件', value: 'EMAIL', isChecked: false },
                    { name: '微信', value: 'WECHAT', isChecked: false }
                ],
                metaList: [],
                loading: {
                    isLoading: false,
                    title: ''
                },
                query: {},
                createReleaseForm: {
                    name: '',
                    version_no: '',
                    desc: '',
                    end_date: new Date(),
                    internal_list: [],
                    external_list: '',
                    notice_list: '',
                    wechatGroups: '',
                    enableWechatGroups: false
                },
                dialogLoading: {
                    isLoading: false,
                    title: ''
                },
                packageLoading: {
                    isLoading: false,
                    title: ''
                },
                tipLoading: {
                    isLoading: true,
                    title: ''
                },
                nodeSelectConf: {
                    title: '',
                    isShow: false,
                    closeIcon: false,
                    hasHeader: false,
                    quickClose: false
                },
                versionSelectConf: {
                    isShow: false,
                    closeIcon: false,
                    hasHeader: false,
                    quickClose: false,
                    confirmText: '确定'
                },
                createGroupForm: {
                    idEdit: false,
                    name: '',
                    internal_list: [],
                    external_list: '',
                    desc: ''
                },
                errorHandler: {
                    nameError: false
                },
                errorFormHandler: {
                    nameError: false,
                    dateError: false
                }
            }
        },
        computed: {
            ...mapGetters('experience', [
                'getSelectedFile',
                'getUserGroup'
            ]),
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
                return this.isEdit ? '编辑体验' : '新增体验'
            },
            submitText () {
                return this.isEdit ? '更新体验' : '转体验'
            }
        },
        watch: {
            '$route' (path) {
                if (!this.isEdit) {
                    this.requestGroupList()
                    this.noticeTypeList.forEach(item => {
                        item.isChecked = false
                    })
                    this.metaList = []
                    this.query.initDate = ''
                    this.createReleaseForm = {
                        name: '',
                        version_no: '',
                        desc: '',
                        end_date: '',
                        internal_list: [],
                        external_list: '',
                        notice_list: ''
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
                        projectId: this.projectId
                    })

                    this.experienceGroup.splice(0, this.experienceGroup.length)
                    res.records.map(item => {
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

                    this.createReleaseForm.name = res.name
                    this.createReleaseForm.path = res.path
                    this.createReleaseForm.artifactoryType = res.artifactoryType
                    this.createReleaseForm.version_no = res.version
                    this.createReleaseForm.end_date = this.localConvertTime(res.expireDate).split(' ')[0]
                    this.query.initDate = this.createReleaseForm.end_date
                    this.createReleaseForm.desc = res.remark
                    this.createReleaseForm.external_list = res.outerUsers
                    this.createReleaseForm.internal_list = res.innerUsers
                    this.createReleaseForm.enableWechatGroups = res.enableWechatGroups
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
            async getFileInfo (path, type) {
                try {
                    const res = await this.$store.dispatch('experience/requestMetaList', {
                        projectId: this.projectId,
                        artifactoryType: type,
                        path: path
                    })

                    this.metaList.splice(0, this.metaList.length)
                    res.map(item => {
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
                this.toExperienceList()
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
                    isEdit: false,
                    groupHashId: '',
                    name: '',
                    internal_list: [],
                    external_list: '',
                    desc: ''
                }
                this.nodeSelectConf.title = '新增体验组'
                this.nodeSelectConf.isShow = true
            },
            onChange (tags) {
                this.createGroupForm.internal_list = tags
            },
            displayResult () {
                if (this.nodeSelectConf.isShow) {
                    this.createGroupForm.external_list = this.createGroupForm.external_list.replace(/[^\d;,]/g, '')
                }
            },
            textdisplayResult () {
                this.createReleaseForm.external_list = this.createReleaseForm.external_list.replace(/[^\d;,]/g, '')
            },
            validate () {
                let errorCount = 0
                if (!this.createGroupForm.name) {
                    this.errorHandler.nameError = true
                    errorCount++
                }

                if (errorCount > 0) {
                    return false
                }

                return true
            },
            afterCreateGroup () {
                this.requestGroupList()
                this.nodeSelectConf.isShow = false
            },
            cancelFn () {
                if (!this.dialogLoading.isLoading) {
                    this.nodeSelectConf.isShow = false
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
                        this.versionSelectConf.confirmText = '提交中...'

                        const obj = this.getSelectedFile
                        console.log(obj, 'obj')
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
                                return item
                            })

                            for (let i = this.metaList.length - 1; i >= 0; i--) {
                                if ((this.metaList[i].key === 'buildId') || (this.metaList[i].key === 'pipelineId')
                                    || (this.metaList[i].key === 'projectId') || (this.metaList[i].key === 'source')) {
                                    this.metaList.splice(i, 1)
                                }
                            }

                            this.createReleaseForm.name = obj.name
                            this.createReleaseForm.path = obj.fullPath
                            this.createReleaseForm.artifactoryType = obj.artifactoryType
                            this.errorFormHandler.nameError = false
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
                            this.versionSelectConf.confirmText = '确定'
                        }
                    }
                }
            },
            cancelSelect () {
                if (!this.packageLoading.isLoading) {
                    this.versionSelectConf.isShow = false
                    this.packageLoading.isLoading = false
                    this.versionSelectConf.confirmText = '确定'
                }
            },
            submitValidate () {
                let errorCount = 0
                if (!this.createReleaseForm.name) {
                    this.errorFormHandler.nameError = true
                    errorCount++
                }

                if (!this.createReleaseForm.end_date) {
                    this.errorFormHandler.dateError = true
                    errorCount++
                }

                if (errorCount > 0) {
                    return false
                }

                return true
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
            async submitFn () {
                const isValid = this.submitValidate()

                if (isValid) {
                    let message
                    const theme = 'error'
                   
                    if (!this.createReleaseForm.desc) {
                        message = '请填写版本描述'

                        this.$bkMessage({
                            message,
                            theme
                        })
                    } else {
                        const params = {
                            name: this.createReleaseForm.name,
                            path: this.createReleaseForm.path,
                            artifactoryType: this.createReleaseForm.artifactoryType,
                            version: this.createReleaseForm.version_no,
                            expireDate: Date.parse(this.createReleaseForm.end_date) / 1000,
                            remark: this.createReleaseForm.desc || undefined,
                            outerUsers: this.createReleaseForm.external_list,
                            innerUsers: this.createReleaseForm.internal_list,
                            enableWechatGroups: this.createReleaseForm.enableWechatGroups,
                            experienceGroups: [],
                            notifyTypes: []
                        }

                        if (this.createReleaseForm.enableWechatGroups) {
                            params.wechatGroups = this.wechatGroupCompletion()
                        } else {
                            params.wechatGroups = ''
                        }

                        this.experienceGroup.forEach(item => {
                            if (item.isChecked) {
                                params.experienceGroups.push(item.groupHashId)
                            }
                        })

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

                                message = '编辑体验成功'
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

                                    message = '新增体验成功'
                                    theme = 'success'
                                } else {
                                    const params = {
                                        noPermissionList: [
                                            { resource: '流水线', option: '执行' }
                                        ],
                                        applyPermissionUrl: `/backend/api/perm/apply/subsystem/?client_id=code&project_code=${this.projectId}&service_code=pipeline&role_executor=pipeline:${this.curPipelineId}`
                                    }

                                    this.$showAskPermissionDialog(params)
                                }
                            }
                        } catch (err) {
                            message = err.message ? err.message : err
                            theme = 'error'
                        } finally {
                            this.$bkMessage({
                                message,
                                theme
                            })

                            this.loading.isLoading = false

                            if (theme === 'success') {
                                if (this.createReleaseForm.enableWechatGroups) {
                                    this.setGroupidStorage(params.wechatGroups)
                                }
                                this.toExperienceList()
                            }
                        }
                    }
                }
            },
            /**
             * 处理时间格式
             */
            localConvertTime (timestamp) {
                return convertTime(timestamp * 1000)
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

    .create-experience-wrapper {
        .experience-form {
            width: 800px;
            .bkdevop-checkbox-group {
                .bk-form-checkbox {
                    width: 180px;
                    line-height: 30px;
                    margin-bottom: 10px;
                }
                .exp-group-item {
                    .icon-member-list {
                        display: none;
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
                        color: $fontLigtherColor;
                    }
                }
            }
            .enable-wechat-group {
                width: 250px;
            }
            .version-number-info {
                line-height: 30px;
            }
            .item-groupid {
                margin-top: 12px;
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
                background-color: #fff;
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
</style>
