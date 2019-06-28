<template>
    <section class="template-table" v-if="listData.length">
        <table class="bk-table template-list-table">
            <thead>
                <tr>
                    <th width="10%">图标</th>
                    <th width="16%">模板名称</th>
                    <th width="10%">版本</th>
                    <th width="10%">来源</th>
                    <th width="30%">关联代码库</th>
                    <th width="8%">流水线实例</th>
                    <th width="16%">操作</th>
                </tr>
            </thead>
            <tbody>
                <tr v-for="(row, index) of listData" :key="index">
                    <td colspan="7">
                        <table style="border: 0">
                            <tr>
                                <td width="10%" class="icon-item">
                                    <img :src="row.logoUrl" class="pipeline-icon" v-if="row.logoUrl">
                                    <logo size="40" name="pipeline" v-else></logo>
                                </td>
                                <td width="16%" :class="{ 'template-name': isManagerUser }" :title="row.name">
                                    <span @click="editTemplate(row)">{{row.name}}</span>
                                </td>
                                <td width="10%" class="template-version" :title="row.versionName">{{ row.versionName }}</td>
                                <td width="10%">{{row.templateType|templateTypeFilter}}</td>
                                <td width="30%" class="template-code">
                                    <section class="codelib-box" :title="handleFormat(row.associateCodes)">
                                        <div class="codelib-item" v-for="(entry, eIndex) in (row.associateCodes || []).slice(0, 3)" :key="eIndex">{{ entry }}</div>
                                        <div class="codelib-item ellipsis" v-if="row.associateCodes.length > 3">......</div>
                                    </section>
                                </td>
                                <td width="8%">
                                    <div @click="toInstanceList(row)" :class="[canCreatePP ? 'create-permission' : 'not-create-permission', 'pipeline-instance']">{{ row.associatePipelines.length }}</div>
                                </td>
                                <td width="16%" :class="[{ 'not-permission': !isManagerUser }, 'handler-btn']">
                                    <span @click="toInstanceList(row)" :class="canCreatePP ? 'create-permission' : 'not-create-permission'">实例化</span>
                                    <span @click.stop="showTools(row)" :class="[{ 'has-show': row.showMore }, 'show-more']" data-name="btns">
                                        更多
                                        <ul v-show="row.showMore" class="btn-more">
                                            <li @click="copyTemplate(row)" data-name="copy">复制</li>
                                            <template v-if="!['constraint','CONSTRAINT'].includes(row.templateType)">
                                                <li v-if="['customize','CUSTOMIZE'].includes(row.templateType) && row.storeFlag" data-name="stored" class="has-stored">已关联商店</li>
                                                <li @click="toRelativeStore(row)" v-else data-name="store">关联商店</li>
                                            </template>
                                            <li @click="deleteTemplate(row)" data-name="delete">{{['constraint','CONSTRAINT'].includes(row.templateType) ? '卸载' : '删除'}}</li>
                                        </ul>
                                    </span>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
            </tbody>
        </table>

        <bk-pagination
            :paging-config.sync="pagingConfig"
            :limit="pagingConfig.limit"
            :current="pagingConfig.current"
            :count="pagingConfig.count"
            @limit-change="pageCountChange"
            @change="pageChange"
            size="small"
        >
        </bk-pagination>

        <bk-dialog
            width="800"
            v-model="copyTemp.isShow"
            header-position="left"
            ext-cls="pipeline-form-dialog"
            :title="copyTemp.title"
            :close-icon="copyTemp.closeIcon"
            @confirm="copyConfirmHandler"
            @cancel="copyCancelHandler">
            <template>
                <section class="copy-pipeline bk-form">
                    <div class="bk-form-item">
                        <label class="bk-label">模板名称：</label>
                        <div class="bk-form-content">
                            <input type="text" class="bk-form-input" placeholder="请输入模板名称，不能超过30个字符"
                                v-model="copyTemp.templateName"
                                :class="{ 'is-danger': copyTemp.nameHasError }"
                                @input="copyTemp.nameHasError = false"
                                name="copyTemplateName"
                                v-validate="&quot;required|max:30&quot;"
                                maxlength="30"
                            >
                        </div>
                        <p v-if="errors.has('copyTemplateName')" class="error-tips err-name">模板名称不能超过30个字符</p>
                    </div>

                    <div class="bk-form-item">
                        <label class="bk-label tip-bottom">应用设置
                            <span v-bk-tooltips.bottom-end="'选“是”则将流水线设置应用于复制后的模版'" class="bottom-end">
                                <i class="bk-icon icon-info-circle"></i>
                            </span>
                        </label>
                        <div class="bk-form-content">
                            <bk-radio-group v-model="copyTemp.isCopySetting">
                                <bk-radio v-for="(entry, key) in copySettings" :key="key" :value="entry.value" class="form-radio">{{ entry.label }}</bk-radio>
                            </bk-radio-group>
                        </div>
                        <!--<label class="bk-label tip-bottom">应用设置:</label>-->
                        <!--<bk-popover class="form-tips">
                            <span slot="content" style="white-space: normal;">选“是”则将流水线设置应用于复制后的模版</span>
                        </bk-popover>-->
                        <!--<div class="bk-form-content">
                            <label class="bk-form-radio" v-for="(entry, key) in copySettings" :key="key">
                                <input type="radio" :value="entry.value" class="auth-radio" v-model="copyTemp.isCopySetting">
                                <span class="bk-radio-text">{{ entry.label }}</span>
                            </label>
                        </div>-->
                    </div>
                </section>
            </template>
        </bk-dialog>
    </section>
</template>

<script>
    import Logo from '@/components/Logo'
    import { navConfirm } from '@/utils/util'

    export default {
        components: {
            Logo
        },

        filters: {
            templateTypeFilter (val) {
                let res = ''
                switch (val) {
                    case 'constraint':
                    case 'CONSTRAINT':
                        res = '研发商店'
                        break
                    default:
                        res = '自定义'
                        break
                }
                return res
            }
        },

        data () {
            return {
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
                    title: '另存为模板',
                    closeIcon: false,
                    quickClose: true,
                    padding: '0 20px',
                    templateId: '',
                    templateName: '',
                    isCopySetting: true
                },
                copySettings: [
                    { label: '是', value: true },
                    { label: '否', value: false }
                ],
                tipsSetting: {
                    content: '选“是”则将流水线设置应用于复制后的模版',
                    placements: ['right']
                }
            }
        },

        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },

        mounted () {
            this.requestHasCreatePermission()
            this.addClickListener()
            this.getListData()
        },

        beforeDestroy () {
            this.removeClickListener()
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
            showTools (row) {
                this.listData.forEach((data) => {
                    if (data.templateId === row.templateId) row.showMore = !row.showMore
                    else data.showMore = false
                })
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

            getListData () {
                this.$emit('getApiData', (res) => {
                    if (res) {
                        // const pageNumber = res.count / this.pagingConfig.limit
                        this.isManagerUser = res.hasPermission
                        this.listData = (res.models || []).map(x => {
                            x.showMore = false
                            return x
                        })
                        // this.pagingConfig.totalPage = Math.ceil(pageNumber || 1)
                        this.pagingConfig.count = res.count
                    }
                })
            },

            handleFormat (codes) {
                let tips = ``
                codes.map(item => {
                    tips += `${item}\n`
                })

                return tips
            },

            toInstanceList (row) {
                if (!this.canCreatePP) return
                this.$router.push({
                    name: 'templateInstance',
                    params: { 'templateId': row.templateId }
                })
            },

            editTemplate (row) {
                if (!this.isManagerUser) return

                this.$router.push({
                    name: 'templateEdit',
                    params: { 'templateId': row.templateId }
                })
            },

            toRelativeStore (row) {
                if (!this.isManagerUser) return

                const href = `${WEB_URL_PIRFIX}/store/atomList/template?projectCode=${this.projectId}&templateId=${row.templateId}`
                window.open(href, '_blank')
            },

            deleteTemplate (row) {
                if (!this.isManagerUser) return
                const content = `删除${row.name}`

                navConfirm({ title: `确认`, content })
                    .then(() => {
                        this.confirmDeleteTemplate(row)
                    }).catch(() => {})
            },

            copyTemplate (row) {
                if (!this.isManagerUser) return

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
                    this.$showTips({ message: '复制模板成功', theme: 'success' })
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
                    this.$showTips({ message: '删除模板成功', theme: 'success' })
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

            addClickListener () {
                document.addEventListener('mouseup', this.closeShowMore)
            },

            removeClickListener () {
                document.removeEventListener('mouseup', this.closeShowMore)
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/scss/conf';

    .template-table {
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
    }

    .template-list-table {
        margin: 20px 0;
        border: none;
        overflow: visible;
        thead {
            font-weight: normal;
            tr {
                background: #ECF0F5;
            }
            th {
                background: #ECF0F5;
                padding-left: 13px;
                height: 42px;
                border: 0;
            }
        }
        tbody {
            border-right: 1px solid #EBF0F5;
            border-left: 1px solid #EBF0F5;
            td {
                padding: 0;
                border: none;
                table {
                    width: 100%;
                    border-bottom: 1px solid #EBF0F5;
                    background: #fff;
                    &:hover {
                        background: #FAFBFD;
                    }
                    td {
                        padding: 8px 13px;
                        overflow: hidden;
                        text-overflow: ellipsis;
                        white-space: nowrap;
                        .create-permission {
                            cursor: pointer;
                        }
                        .not-create-permission {
                            cursor: not-allowed;
                        }
                        &.template-name {
                            max-width: 192px;
                            color: $primaryColor;
                            padding: 0;
                            span {
                                margin: 8px 13px;
                                cursor: pointer;
                            }
                        }
                        &.template-version {
                            max-width: 120px;
                        }
                        &.template-code {
                            max-width: 360px;
                        }
                    }
                }
            }
        }
        .icon-item {
            text-align: center;
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
            .btn-more {
                position: absolute;
                top: 24px;
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
                .has-stored {
                    cursor: not-allowed;
                }
            }
            .show-more {
                position: relative;
                padding-right: 20px;
                &:after {
                    content: '';
                    position: absolute;
                    top: 3px;
                    right: 8px;
                    height: 8px;
                    width: 8px;
                    border-right: 1px solid $fontColor;
                    border-bottom: 1px solid $fontColor;
                    transition: transform 200ms;
                    transform: rotate(45deg);
                    transform-origin: 6px 6px;
                }
            }
            .has-show:after {
                transform: rotate(225deg);
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
