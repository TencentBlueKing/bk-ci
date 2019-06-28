<template>
    <bk-dialog
        width="800"
        ext-cls="create-view-dialog"
        v-model="showViewCreate"
        :show-footer="viewDialogConfig.hasFooter"
        :close-icon="viewDialogConfig.closeIcon"
        @confirm="confirmHandler"
        @cancel="cancelHandler">
        <template>
            <section class="create-view-content bk-form"
                v-bkloading="{
                    isLoading: viewDialogConfig.loading
                }">
                <div class="create-view-header">
                    <span class="title">{{ title }}</span>
                </div>
                <div class="create-view-form">
                    <div class="bk-form-item is-required">
                        <label class="bk-label view-item-label">类型：</label>
                        <div class="bk-form-content">
                            <bk-radio-group v-model="createViewForm.projected">
                                <bk-radio :value="false" class="view-radio">个人视图</bk-radio>
                                <bk-radio :value="true" class="view-radio" :disabled="!isManagerUser">项目视图<span v-bk-tooltips="viewTypeTips" class="top-start"><i class="bk-icon icon-info-circle"></i></span></bk-radio>
                            </bk-radio-group>
                        </div>
                    </div>
                    <div class="bk-form-item is-required">
                        <label class="bk-label view-item-label">标题：</label>
                        <div class="bk-form-content view-item-content">
                            <input type="text" class="bk-form-input view-name-input" placeholder="请输入标题"
                                maxlength="15"
                                name="viewName"
                                v-model="createViewForm.name"
                                v-validate="{ required: true }"
                                data-vv-validate-on="blur"
                                :class="{ &quot;is-danger&quot;: errors.has(&quot;viewName&quot;) }">
                            <div v-if="errors.has(&quot;viewName&quot;)" class="error-tips">{{ errors.first('viewName') }}</div>
                        </div>
                    </div>
                    <div class="bk-form-item is-required">
                        <label class="bk-label view-item-label">设置视图条件：</label>
                        <div class="bk-form-content">
                            <div class="relationship-content">
                                <label class="view-item-label relationship-label">条件间的关系：</label>
                                <bk-radio-group v-model="createViewForm.logic">
                                    <bk-radio v-for="(entry, key) in conditionList" :key="key" :value="entry.value" class="view-radio">{{ entry.label }}</bk-radio>
                                </bk-radio-group>
                            </div>
                            <table class="bk-table rule-list-table" v-if="createViewForm.filters.length">
                                <thead>
                                    <tr>
                                        <th width="30%">字段</th>
                                        <th>值</th>
                                        <th width="36"></th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr v-for="(row, index) in createViewForm.filters" :key="index">
                                        <td class="indicator-item">
                                            <bk-select
                                                v-model="row.id"
                                                @change="handleChange(row, index)">
                                                <bk-option v-for="(option, oindex) in viewFilterTypeList" :key="oindex" :id="option.id" :name="option.name">
                                                </bk-option>
                                            </bk-select>
                                        </td>
                                        <td class="handler-item">
                                            <section v-if="row.id === 'filterByName'">
                                                <input type="text"
                                                    class="bk-form-input input-text"
                                                    placeholder="支持模糊匹配"
                                                    maxlength="20"
                                                    :name="`item-${index}`" id="pipelineName"
                                                    v-validate="{ required: true }"
                                                    v-model="row.pipelineName"
                                                    data-vv-validate-on="blur"
                                                    :class="{ &quot;is-danger&quot;: errors.has(`item-${index}`) }">
                                                <div v-if="errors.has(`item-${index}`)" class="error-tips">{{ errors.first(`item-${index}`) }}</div>
                                            </section>
                                            <section v-if="row.id === 'filterByCreator'">
                                                <user-input
                                                    :name="'user' + index"
                                                    :value="row.userIds"
                                                    :handle-change="staffHandleChange">
                                                </user-input>
                                                <div v-if="staffHacCheckYet && !row.userIds.length" class="error-tips">创建人不能为空</div>
                                            </section>
                                            <section v-if="row.id !== 'filterByName' && row.id !== 'filterByCreator'">
                                                <bk-select
                                                    v-model="row.labelIds"
                                                    :multiple="true"
                                                >
                                                    <bk-option v-for="(option, oindex) in row.labels" :key="oindex" :id="option.id" :name="option.name">
                                                    </bk-option>
                                                </bk-select>
                                                <div v-if="groupHacCheckYet && !row.labelIds.length" class="error-tips">标签不能为空</div>
                                            </section>
                                        </td>
                                        <td class="delete-handler"><i class="bk-icon icon-minus" @click="reduceFilterItem(index)"></i></td>
                                    </tr>
                                    <tr>
                                        <td colspan="3" class="add-new-item">
                                            <span @click="addFilterItem()"><i class="bk-icon icon-plus-circle" />添加视图条件</span>
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                            <bk-button type="primary" size="small" style="margin-top: 10px;" v-if="!createViewForm.filters.length"
                                @click="addFilterItem()">添加视图条件</bk-button>
                        </div>
                    </div>
                </div>
                <div class="temp-operation-bar">
                    <bk-button theme="primary" @click="confirmHandler()">保存</bk-button>
                    <bk-button @click="cancelHandler()">取消</bk-button>
                </div>
            </section>
        </template>
    </bk-dialog>
</template>

<script>
    import { mapGetters } from 'vuex'
    import UserInput from '@/components/atomFormField/UserInput/index.vue'

    export default {
        components: {
            UserInput
        },
        data () {
            return {
                isManagerUser: true,
                title: '',
                multiSelect: true,
                staffHacCheckYet: false,
                groupHacCheckYet: false,
                viewFilterTypeList: [
                    { id: 'filterByName', name: '流水线名称', '@type': 'filterByName' },
                    { id: 'filterByCreator', name: '创建人', '@type': 'filterByCreator' }
                ],
                viewType: [
                    { label: '个人视图', value: false },
                    { label: '项目视图', value: true }
                ],
                conditionList: [
                    { label: '与', value: 'AND' },
                    { label: '或', value: 'OR' }
                ],
                viewDialogConfig: {
                    loading: false,
                    hasFooter: false,
                    closeIcon: false,
                    quickClose: false
                },
                viewTypeTips: {
                    content: '项目视图仅能由项目管理员添加、编辑、删除',
                    placements: ['right']
                }
            }
        },
        computed: {
            ...mapGetters({
                'userInfo': 'pipelines/getUserInfo',
                'tagGroupList': 'pipelines/getTagGroupList',
                'showViewCreate': 'pipelines/getShowViewCreate',
                'createViewForm': 'pipelines/getCreateViewForm'
            }),
            projectId () {
                return this.$route.params.projectId
            }
        },
        watch: {
            tagGroupList (newVal) {
                newVal.map(val => {
                    val['@type'] = 'filterByLabel'
                })
                this.viewFilterTypeList = [...this.viewFilterTypeList, ...newVal]
            },
            showViewCreate (newVal) {
                if (newVal) {
                    if (this.createViewForm.id) {
                        this.title = '编辑视图'
                        this.requestPipelineViewDetail(this.createViewForm.id)
                    } else {
                        this.title = '新建视图'
                        const obj = {
                            projected: false,
                            name: '',
                            logic: 'AND',
                            filters: [
                                { id: 'filterByName', name: '流水线名称', '@type': 'filterByName', pipelineName: '' }
                            ]
                        }
                        this.$store.commit('pipelines/updateViewForm', obj)
                        this.staffHacCheckYet = false
                        this.groupHacCheckYet = false
                    }
                }
            }
        },
        methods: {
            handleChange (item, itemIndex) {
                let temp = {}
                const curItem = this.createViewForm.filters[itemIndex]
                if (curItem.id === 'filterByName' && curItem.name !== '流水线名称') {
                    temp = {
                        id: curItem.id,
                        name: '流水线名称',
                        '@type': curItem.id,
                        pipelineName: ''
                    }
                    this.createViewForm.filters.splice(itemIndex, 1, temp)
                } else if (curItem.id === 'filterByCreator' && curItem.name !== '创建人') {
                    temp = {
                        id: curItem.id,
                        name: '创建人',
                        '@type': curItem.id,
                        userIds: []
                    }
                    this.createViewForm.filters.splice(itemIndex, 1, temp)
                } else if (curItem.id !== 'filterByName' && curItem.id !== 'filterByCreator') {
                    const target = this.viewFilterTypeList.find(val => {
                        return val.id === curItem.id
                    })
                    if ((curItem['@type'] === 'filterByLabel' && curItem.name !== target.name) || curItem['@type'] !== 'filterByLabel') {
                        temp = {
                            id: curItem.id,
                            name: target.name,
                            '@type': 'filterByLabel',
                            groupId: curItem.id,
                            labelIds: [],
                            labels: target.labels
                        }
                        this.createViewForm.filters.splice(itemIndex, 1, temp)
                    }
                }
            },
            staffHandleChange (name, data) {
                const key = parseInt(name.substr(4))
                this.createViewForm.filters.forEach((value, index) => {
                    if (key === index) {
                        value.userIds = data
                    }
                })
                this.createViewForm.filters = [...this.createViewForm.filters]
            },
            formatForm () {
                this.createViewForm.filters.map(value => {
                    if (value['@type'] === 'filterByName') {
                        value.name = '流水线名称'
                        value.id = value['@type']
                    } else if (value['@type'] === 'filterByCreator') {
                        value.name = '创建人'
                        value.id = value['@type']
                    } else if (value['@type'] === 'filterByLabel') {
                        const originItem = this.viewFilterTypeList.find(item => {
                            return item.id === value.groupId
                        })
                        value.id = originItem.id
                        value.name = originItem.name
                        value.labels = originItem.labels
                    }
                })
            },
            async requestPipelineViewDetail (viewId) {
                let message = ''
                let theme = ''

                try {
                    this.viewDialogConfig.loading = true

                    const res = await this.$store.dispatch('pipelines/requestPipelineViewDetail', {
                        projectId: this.projectId,
                        viewId: viewId
                    })
                    this.$store.commit('pipelines/updateViewForm', res)
                    this.formatForm()
                } catch (err) {
                    message = err.message || err
                    theme = 'error'
                } finally {
                    this.viewDialogConfig.loading = false
                    message && this.$showTips({
                        message,
                        theme
                    })
                }
            },
            addFilterItem () {
                const newItem = {
                    id: 'filterByName',
                    name: '流水线名称',
                    '@type': 'filterByName',
                    pipelineName: '',
                    labels: [],
                    labelIds: []
                }
                this.createViewForm.filters.push(newItem)
            },
            reduceFilterItem (index) {
                this.createViewForm.filters.splice(index, 1)
            },
            submitValidate () {
                let errorCount = 0

                if (!this.createViewForm.filters.length) {
                    this.$showTips({
                        message: '视图条件不能为空',
                        theme: 'error'
                    })
                    errorCount++
                } else {
                    this.createViewForm.filters.forEach(item => {
                        if (item['@type'] === 'filterByCreator' && !item.userIds.length) {
                            this.staffHacCheckYet = true
                            errorCount++
                        } else if (item['@type'] === 'filterByLabel' && !item.labelIds.length) {
                            this.groupHacCheckYet = true
                            errorCount++
                        }
                    })
                }

                if (errorCount > 0) {
                    return false
                }

                return true
            },
            getParams () {
                const params = {}
                const targetObj = this.createViewForm

                params.filters = []
                params.projected = targetObj.projected
                params.name = targetObj.name
                params.logic = targetObj.logic

                targetObj.filters.forEach(item => {
                    if (item['@type'] === 'filterByName') {
                        params.filters.push({
                            '@type': item['@type'],
                            condition: 'LIKE',
                            pipelineName: item.pipelineName
                        })
                    } else if (item['@type'] === 'filterByCreator') {
                        params.filters.push({
                            '@type': item['@type'],
                            condition: 'INCLUDE',
                            userIds: item.userIds
                        })
                    } else if (item['@type'] === 'filterByLabel') {
                        params.filters.push({
                            '@type': item['@type'],
                            condition: 'INCLUDE',
                            groupId: item.groupId,
                            labelIds: item.labelIds
                        })
                    }
                })

                return params
            },
            async confirmHandler () {
                const valid = await this.$validator.validate()
                const otherValid = this.submitValidate()
                if (valid && otherValid) {
                    const params = this.getParams()
                    let message = ''
                    let theme = ''

                    try {
                        this.viewDialogConfig.loading = true

                        if (this.createViewForm.id) {
                            await this.$store.dispatch('pipelines/editPipelineView', {
                                projectId: this.projectId,
                                viewId: this.createViewForm.id,
                                params
                            })
                        } else {
                            await this.$store.dispatch('pipelines/createPipelineView', {
                                projectId: this.projectId,
                                params
                            })
                        }
                        message = this.createViewForm.id ? '编辑成功' : '新建成功'
                        theme = 'success'
                        this.cancelHandler()
                        this.$emit('updateViewList', 'flag')
                    } catch (err) {
                        message = err.message || err
                        theme = 'error'
                    } finally {
                        this.viewDialogConfig.loading = false
                        message && this.$showTips({
                            message,
                            theme
                        })
                    }
                }
            },
            cancelHandler () {
                this.$store.commit('pipelines/toggleViewCreateDialog', false)
            }
        }
    }
</script>

<style lang='scss'>
    @import './../../scss/conf';
    .create-view-dialog {
        .bk-dialog-tool {
            display: none;
        }
        .bk-dialog-body {
            margin: 0px;
        }
        .create-view-header {
            padding-left: 20px;
            height: 54px;
            line-height: 54px;
            border-bottom: 1px solid $borderWeightColor;
            font-weight: bold;
        }
        .create-view-form {
            padding: 20px 60px 20px 0;
            height: 470px;
            overflow: auto;
            .relationship-label {
                margin-right: 10px;
            }
            .icon-info-circle {
                position: relative;
                top: 2px;
                left: 2px;
                color: $fontLigtherColor;
            }
            .bk-tooltip-inner {
                max-width: 268px;
            }
            .rule-list-table {
                margin-top: 10px;
                border: 1px solid $borderWeightColor;
                tbody {
                    background: #fff;
                }
                th, td {
                    height: 42px;
                    color: #333C48;
                    font-weight: normal;
                    padding: 10px;
                }
                td {
                    vertical-align: top;
                }
                .delete-handler {
                    padding: 0 4px;
                    i {
                        top: 18px;
                        position: relative;
                        cursor: pointer;
                    }
                }
            }
            .add-new-item {
                position: relative;
                border: none;
                span {
                    color: $primaryColor;
                    cursor: pointer;
                }
                .bk-icon {
                    margin-right: 4px;
                    position: relative;
                    top: 1px;
                }
            }
            .bk-selector-node .text {
                color: $fontColor;
            }
            .view-radio {
                margin-right: 30px;
                margin-top: 5px;
            }
        }
        .temp-operation-bar {
            margin-bottom: 14px;
            padding-right: 20px;
            text-align: right;
            border-top: 1px solid $borderWeightColor;
            .bk-button {
                margin-top: 14px;
            }
        }
    }
</style>
