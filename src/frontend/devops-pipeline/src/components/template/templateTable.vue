<template>
    <section
        class="template-list-table-wrapper"
        v-if="listData.length"
    >
        <bk-table
            class="template-list-table"
            height="100%"
            :data="listData"
            :pagination="pagingConfig"
            @page-limit-change="pageCountChange"
            @page-change="pageChange"
            @sort-change="handleSortChange"
        >
            <bk-table-column
                :label="$t('icon')"
                prop="logoUrl"
                width="140"
            >
                <template slot-scope="{ row }">
                    <div class="icon-item">
                        <img
                            :src="row.logoUrl"
                            class="pipeline-icon"
                            v-if="row.logoUrl"
                        >
                        <logo
                            size="40"
                            name="pipeline"
                            v-else
                        ></logo>
                    </div>
                </template>
            </bk-table-column>
            <bk-table-column
                :label="$t('template.name')"
                sortable
                prop="name"
            >
                <template slot-scope="{ row }">
                    <div
                        :class="{
                            'template-name': true,
                            'manager-user': isEnabledPermission ? row.canView : row.canEdit
                        }"
                        :title="row.name"
                    >
                        <span
                            v-if="isEnabledPermission"
                            @click="editTemplate(row)"
                            v-perm="{
                                hasPermission: row.canView,
                                disablePermissionApi: true,
                                permissionData: {
                                    projectId: projectId,
                                    resourceType: 'pipeline_template',
                                    resourceCode: row.templateId,
                                    action: TEMPLATE_RESOURCE_ACTION.VIEW
                                }
                            }"
                        >
                            {{ row.name }}
                        </span>
                        <span
                            v-else
                            @click="editTemplate(row)"
                        >{{ row.name }}</span>
                    </div>
                </template>
            </bk-table-column>
            <bk-table-column
                :label="$t('version')"
                width="120"
                prop="versionName"
            ></bk-table-column>
            <bk-table-column
                :label="$t('template.source')"
                width="120"
                prop="ip"
            >
                <template slot-scope="{ row }">
                    {{ templateTypeFilter(row.templateType) }}
                </template>
            </bk-table-column>
            <bk-table-column
                :label="$t('template.relatedCodelib')"
                prop="associateCodes"
            >
                <template slot-scope="{ row }">
                    <div
                        class="template-code"
                    >
                        <section
                            class="codelib-box"
                            :title="handleFormat(row.associateCodes)"
                        >
                            <div
                                class="codelib-item"
                                v-for="(entry, eIndex) in (row.associateCodes || []).slice(0, 3)"
                                :key="eIndex"
                            >
                                {{ entry }}
                            </div>
                            <div
                                class="codelib-item ellipsis"
                                v-if="row.associateCodes.length > 3"
                            >
                                ......
                            </div>
                        </section>
                    </div>
                </template>
            </bk-table-column>
            <bk-table-column
                :label="$t('template.pipelineInstance')"
                prop="associatePipelines"
            >
                <template slot-scope="{ row }">
                    <div
                        v-if="isEnabledPermission"
                        class="pipeline-instance"
                        @click="toInstanceList(row)"
                        v-perm="{
                            hasPermission: row.canView,
                            disablePermissionApi: true,
                            permissionData: {
                                projectId: projectId,
                                resourceType: 'pipeline_template',
                                resourceCode: row.templateId,
                                action: TEMPLATE_RESOURCE_ACTION.VIEW
                            }
                        }"
                    >
                        {{ row.associatePipelines.length }}
                    </div>
                    <div
                        v-else
                        @click="toInstanceList(row)"
                        :class="[canCreatePP ? 'create-permission' : 'not-create-permission', 'pipeline-instance']"
                    >
                        {{ row.associatePipelines.length }}
                    </div>
                </template>
            </bk-table-column>

            <bk-table-column
                :label="$t('template.lastModifiedBy')"
                sortable
                width="180"
                prop="creator"
            >
            </bk-table-column>
            <bk-table-column
                :label="$t('template.lastModifiedDate')"
                sortable
                prop="updateTime"
            >
            </bk-table-column>
            <bk-table-column
                :label="$t('operate')"
                prop="operate"
            >
                <template slot-scope="{ row }">
                    <div
                        :class="[{ 'not-permission': !isManagerUser && !isEnabledPermission }, 'handler-btn']"
                    >
                        <span
                            v-if="isEnabledPermission"
                            @click="toInstanceList(row)"
                            :key="row.templateId"
                            v-perm="{
                                permissionData: {
                                    projectId: projectId,
                                    resourceType: row.canView ? 'pipeline' : 'pipeline_template',
                                    resourceCode: row.canView ? projectId : row.templateId,
                                    action: row.canView ? RESOURCE_ACTION.CREATE : TEMPLATE_RESOURCE_ACTION.VIEW
                                }
                            }"
                        >
                            {{ $t('template.instantiate') }}
                        </span>
                        <span
                            v-else
                            @click="toInstanceList(row)"
                            :class="canCreatePP ? 'create-permission' : 'not-create-permission'"
                        >
                            {{ $t('template.instantiate') }}
                        </span>
                        <ext-menu
                            :data="row"
                            :config="row.templateActions"
                        />
                    </div>
                </template>
            </bk-table-column>
        </bk-table>

        <bk-dialog
            width="800"
            v-model="copyTemp.isShow"
            header-position="left"
            ext-cls="pipeline-form-dialog"
            :title="copyTemp.title"
            :close-icon="copyTemp.closeIcon"
            @confirm="copyConfirmHandler"
            @cancel="copyCancelHandler"
        >
            <template>
                <section class="copy-pipeline bk-form">
                    <div class="bk-form-item">
                        <label class="bk-label">{{ $t('template.name') }}：</label>
                        <div class="bk-form-content">
                            <input
                                type="text"
                                class="bk-form-input"
                                :placeholder="$t('template.nameInputTips')"
                                v-model="copyTemp.templateName"
                                :class="{ 'is-danger': copyTemp.nameHasError }"
                                @input="copyTemp.nameHasError = false"
                                name="copyTemplateName"
                                v-validate="'required|max:30'"
                                maxlength="30"
                            >
                        </div>
                        <p
                            v-if="errors.has('copyTemplateName')"
                            class="error-tips err-name"
                        >
                            {{ $t('template.nameErrTips') }}
                        </p>
                    </div>

                    <div class="bk-form-item">
                        <label class="bk-label tip-bottom">{{ $t('template.applySetting') }}
                            <span
                                v-bk-tooltips.bottom-end="'选“是”则将流水线设置应用于复制后的模版'"
                                class="bottom-end"
                            >
                                <i class="bk-icon icon-info-circle"></i>
                            </span>
                        </label>
                        <div class="bk-form-content">
                            <bk-radio-group v-model="copyTemp.isCopySetting">
                                <bk-radio
                                    v-for="(entry, key) in copySettings"
                                    :key="key"
                                    :value="entry.value"
                                    class="form-radio"
                                >
                                    {{ entry.label }}
                                </bk-radio>
                            </bk-radio-group>
                        </div>
                    </div>
                </section>
            </template>
        </bk-dialog>
    </section>
</template>

<script>
    import Logo from '@/components/Logo'
    import {
        RESOURCE_ACTION,
        TEMPLATE_RESOURCE_ACTION
    } from '@/utils/permission'
    import { navConfirm } from '@/utils/util'
    import dayjs from 'dayjs'
    import ExtMenu from './extMenu'

    export default {
        components: {
            Logo,
            ExtMenu
        },
        data () {
            return {
                hasCreatePermission: true,
                isEnabledPermission: false,
                pagingConfig: {
                    current: 1,
                    limit: 10,
                    count: 0
                },
                canCreatePP: true,
                isManagerUser: true,
                listData: [],
                copyTemp: {
                    isShow: false,
                    title: this.$t('template.saveAsTemplate'),
                    closeIcon: false,
                    quickClose: true,
                    padding: '0 20px',
                    templateId: '',
                    templateName: '',
                    isCopySetting: true
                },
                copySettings: [
                    { label: this.$t('true'), value: true },
                    { label: this.$t('false'), value: false }
                ],
                tipsSetting: {
                    content: this.$t('template.tipsSetting'),
                    placements: ['right']
                },
                sortByMap: {
                    name: 'NAME',
                    creator: 'CREATOR',
                    updateTime: 'CREATE_TIME',
                    null: ''
                },
                sortTypeMap: {
                    ascending: 'ASC',
                    descending: 'DESC',
                    null: ''
                }
            }
        },

        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            TEMPLATE_RESOURCE_ACTION () {
                return TEMPLATE_RESOURCE_ACTION
            },
            RESOURCE_ACTION () {
                return RESOURCE_ACTION
            }
        },

        mounted () {
            this.requestHasCreatePermission()
            this.getListData()
        },

        methods: {
            pageCountChange (limit) {
                this.pagingConfig.current = 1
                this.pagingConfig.limit = limit
                this.getListData()
            },
            pageChange (current) {
                this.pagingConfig.current = current
                this.getListData()
            },
            async requestHasCreatePermission () {
                try {
                    this.canCreatePP = await this.$store.dispatch('pipelines/requestHasCreatePermission', {
                        projectId: this.projectId
                    })
                } catch (err) {
                    this.$showTips({ message: err.message || err, theme: 'error' })
                }
            },

            getListData (params = {}) {
                this.$emit('getApiData', params, (res) => {
                    if (res) {
                        this.hasCreatePermission = res.hasCreatePermission
                        this.isEnabledPermission = res.enableTemplatePermissionManage
                        this.isManagerUser = res.hasPermission
                        this.listData = (res.models || []).map(x => {
                            x.updateTime = dayjs(x.updateTime).format('YYYY-MM-DD HH:mm:ss')
                            x.templateActions = [
                                {
                                    text: this.$t('clone'),
                                    handler: this.copyTemplate,
                                    hasPermission: this.hasCreatePermission,
                                    disablePermissionApi: true,
                                    permissionData: {
                                        projectId: this.projectId,
                                        resourceType: 'pipeline_template',
                                        resourceCode: this.projectId,
                                        action: this.RESOURCE_ACTION.CREATE
                                    }
                                },
                                ...(
                                    !['constraint', 'CONSTRAINT'].includes(x.templateType)
                                        ? [
                                            ['customize', 'CUSTOMIZE'].includes(x.templateType) && x.storeFlag
                                                ? {
                                                    text: this.$t('template.alreadyToStore'),
                                                    disable: true
                                                }
                                                : {
                                                    text: this.$t('template.toStore'),
                                                    handler: this.toRelativeStore,
                                                    hasPermission: x.canEdit,
                                                    disablePermissionApi: true,
                                                    permissionData: {
                                                        projectId: this.projectId,
                                                        resourceType: 'pipeline_template',
                                                        resourceCode: x.templateId,
                                                        action: this.TEMPLATE_RESOURCE_ACTION.EDIT
                                                    }
                                                }
                                        ]
                                        : []
                                ),
                                {
                                    text: ['constraint', 'CONSTRAINT'].includes(x.templateType) ? this.$t('uninstall') : this.$t('delete'),
                                    handler: this.deleteTemplate,
                                    hasPermission: x.canEdit,
                                    disablePermissionApi: true,
                                    permissionData: {
                                        projectId: this.projectId,
                                        resourceType: 'pipeline_template',
                                        resourceCode: x.templateId,
                                        action: this.TEMPLATE_RESOURCE_ACTION.EDIT
                                    }
                                }
                            ]
                            return x
                        })
                        this.pagingConfig.count = res.count
                    }
                })
            },

            handleFormat (codes) {
                let tips = ''
                codes.forEach(item => {
                    tips += `${item}\n`
                })

                return tips
            },

            templateTypeFilter (val) {
                switch (val) {
                    case 'constraint':
                    case 'CONSTRAINT':
                        return this.$t('store')
                    default:
                        return this.$t('template.customize')
                }
            },

            toInstanceList (row) {
                if (!this.isEnabledPermission && !row.canView) return
                this.$router.push({
                    name: 'templateInstance',
                    params: { templateId: row.templateId }
                })
            },

            editTemplate (row) {
                if (!this.isEnabledPermission && !row.canEdit) return

                this.$router.push({
                    name: 'templateEdit',
                    params: { templateId: row.templateId }
                })
            },

            toRelativeStore (row) {
                if (!this.isEnabledPermission && !row.canEdit) return

                const href = `${WEB_URL_PREFIX}/store/workList/template?projectCode=${this.projectId}&templateId=${row.templateId}`
                window.open(href, '_blank')
            },

            deleteTemplate (row) {
                if (!this.isEnabledPermission && !row.canEdit) return
                const content = `${this.$t('template.deleteTemplateTips', [row.name])}`

                navConfirm({ type: 'warning', content })
                    .then((val) => {
                        val && this.confirmDeleteTemplate(row)
                    }).catch(() => {})
            },

            copyTemplate (row) {
                if (!this.hasCreatePermission) return

                this.copyTemp.templateName = `${row.name}_copy`
                this.copyTemp.isShow = true
                this.copyTemp.templateId = row.templateId
            },

            async copyConfirmHandler (row) {
                const valid = await this.$validator.validate()
                if (!valid) return

                this.$parent.isLoading = true
                const templateName = this.copyTemp.templateName || ''
                if (!templateName.trim()) {
                    this.copyTemp.nameHasError = true; return
                }

                const postData = {
                    projectId: this.projectId,
                    templateId: this.copyTemp.templateId,
                    params: {
                        templateName: this.copyTemp.templateName,
                        isCopySetting: this.copyTemp.isCopySetting
                    }
                }
                this.$store.dispatch('pipelines/copyTemplate', postData).then(({ id: templateId }) => {
                    this.copyCancelHandler()
                    this.$showTips({ message: this.$t('template.copySuc'), theme: 'success' })
                    this.$router.push({
                        name: 'templateEdit',
                        params: { templateId }
                    })
                }).catch((err) => {
                    const message = err.message || err
                    this.$showTips({ message, theme: 'error' })
                }).finally(() => {
                    this.$parent.isLoading = false
                })
            },

            copyCancelHandler () {
                this.copyTemp.isShow = false
                this.copyTemp.templateName = ''
                this.copyTemp.pipelineId = ''
                this.copyTemp.nameHasError = false
                this.copyTemp.isCopySetting = true
            },

            async confirmDeleteTemplate (row) {
                this.$parent.isLoading = true
                try {
                    await this.$store.dispatch('pipelines/deleteTemplate', {
                        projectId: this.projectId,
                        templateId: row.templateId
                    })

                    this.getListData()
                    this.$showTips({ message: this.$t('template.deleteSuc'), theme: 'success' })
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                } finally {
                    this.$parent.isLoading = false
                }
            },

            closeShowMore (event) {
                const btns = ['copy', 'stored', 'store', 'delete', 'btns']
                const dataSet = event.target.dataset
                const targetName = dataSet.name
                if (!btns.includes(targetName)) {
                    this.listData.forEach(x => {
                        x.showMore = false
                    })
                }
            },

            handleSortChange ({ prop, order }) {
                this.getListData({
                    orderBy: this.sortByMap[prop],
                    sort: this.sortTypeMap[order]
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/scss/conf';
    .template-list-table-wrapper {
        height: 100%;
        overflow: hidden;

        .template-list-table {
            &:after {
                content: '';
                clear: both;
                display: table;
            }
            .bk-label.tip-bottom {
                border-bottom: 1px dotted #63656E;
                padding: 0;
                margin: 10px 20px 10px 60px;
                width: 60px;
            }
            .form-tips {
                position: absolute;
                width: 40px;
                left: 50px;
                bottom: 5px;
                box-sizing: content-box;
                padding-left: 40px;
            }
            .create-permission {
                cursor: pointer;
            }
            .not-create-permission {
                cursor: not-allowed;
            }
            .template-name {
                max-width: 192px;
                padding: 0;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
                span {
                    cursor: pointer;
                    margin: 8px 13px;
                }
            }
            .manager-user {
                color: $primaryColor;
                cursor: pointer;
            }
            .is-disabled {
                cursor: not-allowed !important;
            }
            .icon-item {
                margin-top: 10px;
            }
            .pipeline-icon {
                width: 40px;
                height: 40px;
            }
            .template-name p {
                margin-top: 10px;
                font-size: 12px;
                color: #C3CDD7;
                span {
                    color: #63656E;
                }
            }
            .codelib-item {
                white-space: nowrap;
                overflow: hidden;
                max-width: 406px;
                text-overflow: ellipsis;
                font-size: 12px;
                color: #C3CDD7;
            }
            .pipeline-instance {
                color: $primaryColor;
                cursor: pointer;
            }
            .handler-btn {
                overflow: visible;
                position: relative;
                display: flex;
                .btn-more {
                    position: absolute;
                    top: 50px;
                    right: -33px;
                    width: 91px;
                    max-height: 250px;
                    background: #fff;
                    padding: 0;
                    margin: 0;
                    z-index: 99;
                    overflow: auto;
                    border-radius: 2px;
                    border: 1px solid #c3cdd7;
                    transition: all 200ms;
                    box-shadow: 0 2px 6px rgba(51, 60, 72, 0.1);
                    li {
                        cursor: pointer;
                        line-height: 40px;
                        text-align: center;
                        color: $fontColor;
                        &:hover {
                            color: $primaryColor;
                            background: $primaryLightColor;
                        }
                    }
                }
                span {
                    display: inline-block;
                    margin-left: 5px;
                    color: $fontColor;
                    cursor: pointer;
                    &:hover {
                        color: $primaryColor;
                    }
                }
            }
            .not-permission {
                span, .btn-more li {
                    cursor: not-allowed;
                }
            }
            ::v-deep .cell {
                height: 60px;
                line-height: 60px;
            }
        }
    }

    .template-nav {
        margin: 30px 0 0;
    }

    .pipeline-form-dialog {
        .err-name {
            text-align: left;
            margin-left: 150px;
            margin-bottom: -21px;
        }
        .form-radio {
            margin-right: 30px;
            margin-top: 5px;
        }
    }

</style>
