<template>
    <article class="group-list">
        <section class="loading-wrapper"
            v-bkloading="{
                isLoading: loading.isLoadings,
                title: loading.title
            }">
            <div class="group-list-wrapper">
                <section v-show="showContent" :class="['group-list-content','clearfix',{ 'group-list-center': hasGroup }]">
                  
                    <div class="group-list-hint" v-if="tagGroupList.length > 0">
                        <logo size="12" name="warning-circle" />
                        <span>{{ $t('group.groupNotice') }}</span>
                    </div>
                    <div class="group-list-cards"
                        v-for="(group, groupIndex) in tagGroupList" :key="groupIndex"
                        v-bkloading="{ isLoading: loading.isLoading }">
                        <div class="group-list-title">
                            <div class="title-text">
                                <span>
                                    <b style="margin-right: 8px">{{ groupIndex + 1 }}</b>
                                    <span v-show="groupIndex !== isShowInputIndex">{{ group.name }}</span>
                                </span>
                                <span v-if="groupIndex !== isShowInputIndex">
                                    <span>
                                        <a class="entry-link" @click="showInput(groupIndex, group.name)">{{ $t('rename') }}</a>
                                        <a class="entry-link" @click="deleteGroup(groupIndex)">{{ $t('delete') }}</a>
                                    </span>
                                </span>
                                <span v-else class="group-title-input">
                                    <span class="bk-form-content">
                                        <input
                                            ref="labelInput"
                                            type="text"
                                            class="bk-form-input"
                                            style="width: 95%; margin-right: 10px;"
                                            :placeholder="$t('group.groupInputTips')"
                                            v-model="labelValue"
                                            name="groupName"
                                            @blur="labelInputBlur(groupIndex, group.name)"
                                            @keyup.enter="handleSave(groupIndex)"
                                            maxlength="20"
                                            id="changeGroup" />
                                    </span>
                                    <span>
                                        <a class="entry-link" @click="handleSave(groupIndex)">{{ $t('save') }}</a>
                                        <a class="entry-link" @click="handleCancel(groupIndex, group.name)">{{ $t('cancel') }}</a>
                                    </span>
                                    <div :class="errors.has('groupName') ? 'error-tips' : 'normal-tips'">{{ errors.first("groupName") }}</div>
                                </span>
                            </div>
                        </div>

                        <div :class="['group-card', { 'active': active.isActiveGroup === 's' + groupIndex + tagIndex }, { 'showTips': active.isActiveToops === ('s' + groupIndex + tagIndex) }, { 'group-edit': (active.isGroupEdit === 's' + groupIndex + tagIndex) }]"
                            v-for="(item, tagIndex) in group.labels" :key="tagIndex"
                            @mouseleave="inputMouseLeave">
                            <div class="group-card-title">
                                <span class="tag-text">{{ item.name }}</span>
                                <input
                                    ref="tagInput"
                                    type="text"
                                    class="tag-input"
                                    v-model="tagValue"
                                    v-focus="isFocus(groupIndex, tagIndex)"
                                    maxlength="20"
                                    @blur="tagInputBlur($event, groupIndex, tagIndex)"
                                    @keyup.enter="tagModify(groupIndex, tagIndex)"
                                    :placeholder="$t('group.labelLimitTips')">
                            </div>
                            <div class="group-card-tools">
                                <i class="devops-icon icon-edit2 group-card-icon" v-bk-tooltips="toolTips.rename" @click="tagEdit($event, groupIndex, tagIndex)"></i>
                                <i class="group-card-icon devops-icon icon-delete" v-bk-tooltips="toolTips.delete" @click="tagRemove(groupIndex, tagIndex)"></i>
                            </div>
                            <div v-show="active.isGroupEdit" class="group-card-edit-tools">
                                <i class="devops-icon icon-check-1 group-card-edit-icon" v-bk-tooltips="toolTips.save" @click="tagSave(groupIndex, tagIndex)"></i>
                                <i class="devops-icon icon-close group-card-edit-icon" v-bk-tooltips="toolTips.cancel" @click="tagCancel($event, groupIndex, tagIndex)"></i>
                            </div>
                        </div>

                        <bk-button
                            size="normal"
                            icon="devops-icon icon-plus"
                            class="group-card-add"
                            v-if="group.labels.length < 12"
                            @click="tagAdd($event, groupIndex)"
                            :disabled="btnIsdisable"
                        >
                            {{ $t('group.addLabel') }}
                        </bk-button>
                    </div>

                    <div class="group-list-cards" v-if="isShowGroupBtn()">
                        <bk-button size="large" icon="devops-icon icon-plus" class="group-list-creat"
                            :disabled="btnIsdisable"
                            @click="showDialog"
                        >
                            {{ $t('group.addGroup') }}
                        </bk-button>
                    </div>
                    <div v-show="tagGroupList.length < 1">
                        <empty-tips
                            :title="emptyTipsConfig.title"
                            :desc="emptyTipsConfig.desc"
                        >
                            <div slot="btns">
                                <div class="content">
                                    <p>{{ emptyTipsConfig.contentOne }}</p>
                                    <p>{{ emptyTipsConfig.contentTwo }}</p>
                                </div>
                                <bk-button theme="primary" @click="showDialog">
                                    {{ $t('group.addGroup') }}
                                </bk-button>
                            </div>
                        </empty-tips>
                    </div>
                </section>
                <bk-dialog
                    v-model="groupSetting.isShow"
                    :title="groupSetting.title"
                    :close-icon="false"
                    header-position="left"
                    width="480"
                    @confirm="dialogCommit">
                    <div>
                        <div class="bk-form-item">
                            <label class="bk-label">{{ $t('group.groupName') }}</label>
                            <div class="bk-form-content">
                                <input type="text"
                                    class="bk-form-input"
                                    :placeholder="$t('group.groupInputTips')"
                                    v-model="groupSetting.value"
                                    name="groupName"
                                    v-validate="&quot;required|max:20&quot;"
                                    maxlength="20"
                                    id="newGroup"
                                >
                            </div>
                            <div :class="errors.has('groupName') ? 'error-tips' : 'normal-tips'">{{ errors.first("groupName") }}</div>
                        </div>
                    </div>
                </bk-dialog>
            </div>
        </section>
    </article>
</template>

<script>
    import { mapGetters } from 'vuex'
    import Logo from '@/components/Logo'
    import imgemptyTips from '@/components/pipelineList/imgEmptyTips'
    // import { navConfirm } from '@/utils/util'

    export default {
        directives: {
            focus: {
                inserted: function (el) {
                    el.focus()
                }
            }
        },
        components: {
            'empty-tips': imgemptyTips,
            logo: Logo
        },
        data () {
            return {
                loading: {
                    isLoadings: true,
                    isLoading: false,
                    title: ''
                },
                showContent: false,
                isShowGroup: true,
                emptyTipsConfig: {
                    title: this.$t('group.emptyTips.title'),
                    desc: this.$t('group.emptyTips.desc'),
                    contentOne: this.$t('group.emptyTips.contentOne'),
                    contentTwo: this.$t('group.emptyTips.contentTwo')
                },
                groupSetting: {
                    hasHeader: true,
                    isShow: false,
                    title: this.$t('group.addGroup'),
                    value: '',
                    groupIndex: null,
                    padding: 20
                },
                tagValue: '',
                tagOriginalValue: '',
                labelValue: '',
                btnIsdisable: false,
                isAddTagEnter: false,
                isShowInputIndex: -1,
                active: {
                    isActiveToops: false,
                    oldActiveToops: 'xxx',
                    isGroupEdit: false,
                    isActiveGroup: false
                },
                addTagGroupIndex: null,
                addTagIndex: null,
                toolTips: {
                    rename: {
                        content: '重命名'
                    },
                    delete: {
                        content: '删除'
                    },
                    save: {
                        content: '保存'
                    },
                    cancel: {
                        content: '取消'
                    }
                }
            }
        },
        computed: {
            ...mapGetters({
                tagGroupList: 'pipelines/getTagGroupList'
            }),
            hasGroup () {
                return (!this.tagGroupList.length || this.tagGroupList.length < 1)
            },
            projectId () {
                return this.$route.params.projectId
            },
            toolsConfigInstance () {
                return this.$refs.toolsConfigRef && this.$refs.toolsConfigRef[0] && this.$refs.toolsConfigRef[0].instance ? this.$refs.toolsConfigRef[0].instance : null
            }
        },
        async created () {
            await this.init()
            this.addClickListenr()
        },
        methods: {
            isShowGroupBtn () {
                if (this.tagGroupList.length > 0 && this.tagGroupList.length < 10) {
                    let boolean = true
                    this.tagGroupList.forEach((item, index) => {
                        if (!item.labels.length) {
                            boolean = false
                            return false
                        }
                    })
                    return boolean
                } else {
                    return false
                }
            },
            async init () {
                this.loading.isLoadings = true
                await this.requestGrouptLists()
                this.showContent = true
                this.loading.isLoadings = false
            },
            isFocus (groupIndex, tagIndex) {
                return this.active.isGroupEdit === 's' + groupIndex + tagIndex
            },
            clickHandler (event) {
                const classArr = event.target.className.split(' ')
                if (classArr[0] === 'tools-ele') {
                    return
                }
                this.active.isActiveToops = false
            },
            addClickListenr () {
                document.addEventListener('mouseup', this.clickHandler)
            },
            labelInputBlur (groupIndex, val) {
                if (this.$refs.labelInput.length) {
                    this.$refs.labelInput[0].focus()
                }
            },
            handleCancel (groupIndex, val) {
                // this.resetTag()
                this.btnIsdisable = false
                this.labelValue = val
                this.isShowInputIndex = -1
            },
            async handleSave (groupIndex) {
                this.btnIsdisable = false

                const { $store } = this
                const params = {
                    projectId: this.projectId,
                    name: this.labelValue
                }
                
                let theme, message

                if (!params.name.length) {
                    const valid = await this.$validator.validate()
                    
                    if (!valid) {
                        return false
                    }
                }
                try {
                    params.id = this.tagGroupList[groupIndex].id
                    await $store.dispatch('pipelines/modifyGroup', params).then(res => {
                        if (res) $store.commit('pipelines/modifyTagGroupById', params)
                        message = res ? '标签组名称保存成功。' : '标签组名称保存失败。'
                        theme = res ? 'success' : 'error'
                    })
                    this.$bkMessage({
                        message,
                        theme
                    })
                } catch (err) {
                    this.errShowTips(err)
                }
                this.isShowInputIndex = -1
            },

            showInput (index, val) {
                this.resetTag()
                this.active.isGroupEdit = false
                this.labelValue = val
                this.isShowInputIndex = index
                this.btnIsdisable = true
                this.$nextTick(function () {
                    document.getElementById('changeGroup').focus()
                })
            },

            showDialog (event, groupIndex) {
                const setting = {
                    isShow: true
                }
                // if (groupIndex || groupIndex === 0) {
                //     setting.title = this.$t('rename')
                //     setting.groupIndex = groupIndex
                //     setting.value = this.tagGroupList[groupIndex].name
                // } else {
                setting.title = this.$t('group.addGroup')
                setting.groupIndex = null
                setting.value = ''
                // }
                this.groupSetting = Object.assign({}, this.groupSetting, setting)

                this.$nextTick(function () {
                    document.getElementById('newGroup').focus()
                })
            },
            async dialogCommit () {
                const groups = this.groupSetting
                const { $store } = this
                const params = {
                    projectId: this.projectId,
                    name: groups.value
                }
                const valid = await this.$validator.validate()

                if (!valid) {
                    return false
                }
                if (groups.title === this.$t('group.addGroup')) {
                    this.REQUEST('addGroup', {
                        name: groups.value,
                        projectId: this.projectId
                    })
                } else {
                    try {
                        params.id = this.tagGroupList[groups.groupIndex].id
                        await $store.dispatch('pipelines/modifyGroup', params).then(res => {
                            if (res) $store.commit('pipelines/modifyTagGroupById', params)
                        })
                    } catch (err) {
                        this.errShowTips(err)
                    }
                }
                groups.isShow = false
            },
            deleteGroup (groupIndex) {
                // const name = this.tagGroupList[groupIndex].name
                // const content = this.$t('deleteReason')
                // navConfirm({ type: 'warning', content })
                //     .then(async () => {
                //         try {
                //             await $store.dispatch('pipelines/deleteGroup', {
                //                 groupId: this.tagGroupList[groupIndex].id
                //             }).then(res => {
                //                 if (res) {
                //                     $store.commit('pipelines/removeTagGroupById', {
                //                         groupId: this.tagGroupList[groupIndex].id
                //                     })
                //                 }
                //             })
                //         } catch (err) {
                //             this.errShowTips(err)
                //         }
                //     }).catch(() => {})

                this.addTagIndex = null
                this.addTagGroupIndex = null
                this.btnIsdisable = false
                this.resetTag()
                const { $store } = this

                let theme, message

                this.$bkInfo({
                    title: '确认删除该标签组?',
                    confirmFn: async () => {
                        try {
                            await $store.dispatch('pipelines/deleteGroup', {
                                projectId: this.projectId,
                                groupId: this.tagGroupList[groupIndex].id
                            }).then(res => {
                                theme = res ? 'success' : 'error'
                                message = res ? '删除标签组成功。' : '删除标签组失败。'
                                if (res) {
                                    $store.commit('pipelines/removeTagGroupById', {
                                        groupId: this.tagGroupList[groupIndex].id
                                    })
                                }
                                this.$bkMessage({
                                    message,
                                    theme
                                })
                            })
                        } catch (err) {
                            this.errShowTips(err)
                        }
                    }
                })
            },

            tagRemove (groupIndex, tagIndex) { // 标签删除
                this.resetTag()
                this.active.isGroupEdit = false
                this.addTagGroupIndex = null
                this.addTagIndex = null

                this.$bkInfo({
                    title: '确认删除该标签?',
                    confirmFn: () => {
                        this.REQUEST('deleteTag', {
                            projectId: this.projectId,
                            labelId: this.tagGroupList[groupIndex].labels[tagIndex].id
                        })
                    }
                })
                // this.REQUEST('deleteTag', {
                //     labelId: this.tagGroupList[groupIndex].labels[tagIndex].id
                // })
                // if (this.toolsConfigInstance) {
                //     this.toolsConfigInstance.hide()
                // }
            },
            toggleTools (groupIndex, tagIndex) { // 删除提示toggle
                const active = this.active
                if (active.oldActiveToops === 's' + groupIndex + tagIndex) {
                    if (active.isActiveToops) {
                        active.isActiveToops = false
                    } else {
                        active.oldActiveToops = active.isActiveToops = 's' + groupIndex + tagIndex
                    }
                } else {
                    active.oldActiveToops = active.isActiveToops = 's' + groupIndex + tagIndex
                }
            },
            hideTools () { // 删除提示隐藏
                this.active.isActiveToops = false
                if (this.toolsConfigInstance) {
                    this.toolsConfigInstance.hide()
                }
            },

            tagEdit (e, groupIndex, tagIndex) {
                this.resetTag()
                this.handleCancel()
                const tagList = this.tagGroupList[groupIndex].labels
                this.tagOriginalValue = tagList[tagIndex].name
                this.tagOriginalGroupIndex = groupIndex
                this.tagOriginalTagIndex = tagIndex
                if ((groupIndex || groupIndex === 0) && (tagIndex || tagIndex === 0)) {
                    this.tagValue = tagList[tagIndex].name
                }
                this.active.isGroupEdit = 's' + groupIndex + tagIndex
                let el = e.target || e
                this.$nextTick(function () {
                    el = el.parentNode.previousElementSibling
                    el.lastElementChild.focus()
                    this.btnIsdisable = true
                })
                this.addTagGroupIndex = null
                this.addTagIndex = null
            },

            tagAdd (e, groupIndex) {
                this.resetTag()
                this.isShowInputIndex = -1
                this.tagOriginalGroupIndex = null
                this.tagOriginalTagIndex = null
                const group = this.tagGroupList[groupIndex]
                this.addTagGroupIndex = groupIndex
                this.addTagIndex = group.labels.length
                this.tagValue = ''
                this.btnIsdisable = true
                this.$store.commit('pipelines/resetTag', {
                    groupIndex: groupIndex,
                    boolean: true
                })
                this.active.isGroupEdit = 's' + groupIndex + (group.labels.length - 1)
            },
            tagSave (groupIndex, tagIndex) {
                this.addTagGroupIndex = groupIndex
                this.addTagIndex = tagIndex
                this.tagModify(groupIndex, tagIndex)
            },
            tagCancel (e, groupIndex, tagIndex) {
                const group = this.tagGroupList[groupIndex]
                if (this.tagOriginalValue) group.labels[tagIndex].name = this.tagOriginalValue
                this.btnIsdisable = false
                this.active.isGroupEdit = false
                if (!group.labels[tagIndex].id) {
                    if (!this.isAddTagEnter) {
                        this.$store.commit('pipelines/resetTag', {
                            groupIndex: groupIndex,
                            boolean: false
                        })
                    }
                }
                this.addTagGroupIndex = null
                this.addTagIndex = null
                this.active.isGroupEdit = false
                this.isAddTagEnter = false
                this.requestGrouptLists()
            },
            async tagModify (groupIndex, tagIndex) { // 标签input回车
                const { $store } = this
                const group = this.tagGroupList[groupIndex]
                let path, params
                this.tagOriginalValue = group.labels[tagIndex].name
                this.isAddTagEnter = false
                if (this.tagValue) {
                    // ajax提交数据操作
                    if (tagIndex !== undefined && group.labels[tagIndex].id) {
                        $store.commit('pipelines/modifyTag', { groupIndex, tagIndex, name: this.tagValue })
                        path = 'modifyTag'
                        params = {
                            id: group.labels[tagIndex].id,
                            groupId: group.labels[tagIndex].groupId,
                            name: this.tagValue
                        }
                    } else {
                        $store.commit('pipelines/modifyTag', { groupIndex, tagIndex: group.labels.length - 1, name: this.tagValue })
                        path = 'addTag'
                        params = {
                            groupId: group.id,
                            name: this.tagValue
                        }
                        this.isAddTagEnter = true
                    }
                    this.REQUEST(path, {
                        ...params,
                        projectId: this.projectId
                    }, () => {
                        this.reset(groupIndex, tagIndex)
                    })
                    return false
                } else {
                    return false
                }
            },
            resetTag () {
                if (typeof this.addTagGroupIndex === 'number' && this.addTagGroupIndex !== null && this.addTagIndex !== null) {
                    const group = this.tagGroupList[this.addTagGroupIndex]
                    this.btnIsdisable = false
                    this.active.isGroupEdit = false
                    if (!Object.prototype.hasOwnProperty.call(group.labels[this.addTagIndex], 'groupId')) {
                        this.$store.commit('pipelines/resetTag', {
                            groupIndex: this.addTagGroupIndex,
                            boolean: false
                        })
                    }
                    this.isAddTagEnter = false
                }
                this.addTagGroupIndex = null
                this.addTagIndex = null
            },
            tagInputBlur (e, groupIndex, tagIndex) {
                let newTagIndex = tagIndex
                if (groupIndex) {
                    for (let index = 0; index < groupIndex; index++) {
                        newTagIndex += this.tagGroupList[index].labels.length
                    }
                }
                if (this.$refs.tagInput[newTagIndex]) {
                    this.$refs.tagInput[newTagIndex].focus()
                }
            },
            inputMouseLeave () {
                this.active.isActiveGroup = false
            },
            reset (groupIndex, tagIndex) {
                this.tagValue = ''
                this.active.isActiveGroup = 's' + groupIndex + (tagIndex || this.tagGroupList[groupIndex].labels.length - 1)
                this.active.isGroupEdit = false
            },

            async requestGrouptLists () {
                const { $store } = this
                let res
                try {
                    res = await $store.dispatch('pipelines/requestGetGroupLists', {
                        projectId: this.projectId
                    })
                    $store.commit('pipelines/updateGroupLists', res)
                } catch (err) {
                    this.errShowTips(err)
                }
            },
            errShowTips (err) {
                this.$showTips({
                    message: err.message || err,
                    theme: 'error'
                })
            },
            async REQUEST (method, params, call) {
                // method
                // modifyTag 修改标签
                // deleteTag 删除标签
                // addTag 新增标签
                // addGroup 新增标签组
                const { $store } = this
                let theme, message

                try {
                    const res = await $store.dispatch('pipelines/' + method, params)
                    if (res) {
                        this.requestGrouptLists()
                        theme = res ? 'success' : 'error'
                        if (method === 'modifyTag') message = res ? '标签名称保存成功。' : '标签名称保存失败。'
                        if (method === 'deleteTag') message = res ? '删除标签成功。' : '删除标签失败。'
                        if (method === 'addTag') message = res ? '新增标签成功。' : '新增标签失败。'
                        if (method === 'addGroup') message = res ? '新增标签组成功。' : '新增标签组失败。'
                        if (call) {
                            call()
                        }
                    }
                    this.btnIsdisable = false
                    if (!this.btnIsdisable) {
                        this.$bkMessage({
                            theme,
                            message
                        })
                    }
                } catch (err) {
                    this.errShowTips(err)
                    if (this.tagOriginalGroupIndex && this.tagOriginalTagIndex) {
                        this.tagGroupList[this.tagOriginalGroupIndex].labels[this.tagOriginalTagIndex].name = this.tagOriginalValue
                    }
                }
            }
        }
    }
</script>

<style lang='scss'>
    @import './../../scss/conf';

    body {
        position: relative;
    }
    ::-webkit-input-placeholder{
        color: $fontLighterColor;
    }
    .bk-sideslider-wrapper {
        top: 0;
    }
    .group-card {
        width: 389px;
        height: 54px;
        line-height: 54px;
        margin-bottom: 5px;
        font-size: 14px;
    }
    .group-list {
        height: 100%;
        padding-bottom: 20px;
        .loading-wrapper {
            min-height: calc(100% - 60px);
        }
        .group-header {
            width: 100%;
            height: 60px;
            background: #63656E;
        }
        &-wrapper {
            padding: 20px 0 0;
            .devops-empty-tips .title {
                margin-bottom: 6px;
            }
            .devops-empty-tips .desc {
                margin-bottom: 10px;
            }
            .devops-empty-tips .content {
                height: 60px;
                line-height: 20px;
                font-size: 14px;
                text-align: center;
            }
            .bk-dialog-body .form-group{
                label {
                    display: inline-block;
                    margin-bottom: 8px;
                }
                .form-control {
                    display: inline-block;
                    width: 100%;
                    height: 36px;
                    padding: 0 12px;
                    line-height: 36px;
                    border: 1px solid $fontLighterColor;
                    border-radius: 2px;
                    outline: none;
                    font-size: 14px;
                    color: #666;
                    box-shadow: none;
                    transition: border linear .2s;
                    cursor: text;
                    &:hover,
                    &:focus {
                        border-color: #0082ff
                    }
                }
            }
        }
        &-hint {
            font-size: 12px;
            color: #979BA5;
        }
        &-content {
            width: 1700px;
            // display: flex;
            // flex-wrap: wrap;
            margin: 0 auto;
            padding-top: 16px;
            transform: translateX(10px)
        }
        &-center {
            transform: translateX(0);
        }
        &-cards {
            display: flex;
            flex-wrap: wrap;
            width: 100%;
            background-color: #FFF;
            padding: 24px;
            margin: 10px 0 20px;
            .bk-button {
                &:hover {
                    border: 1px solid $iconPrimaryColor
                }
                &:disabled:hover {
                    border: 1px dashed $iconPrimaryColor
                }
                &:before {
                    font-size: 12px
                }
            }
            .group-card-title {
                padding: 0 16px 0 16px;
                height: 54px;
                border: 1px solid $borderWeightColor;
                border-radius: 2px;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
                .tag-text {
                    display: inline
                }
                .tag-input {
                    display: none;
                    width: 100%;
                    padding-right: 16px;
                    outline: none;
                    border: none;
                    &:focus {
                        border: none;
                    }
                }
            }
            .group-card-tools,
            .group-card-edit-tools {
                display: none;
                position: absolute;
                top: 21px;
                right:16px;
                line-height: 18px;
                font-size: 0;
                .devops-icon {
                    font-size: 10px;
                    color: $fontLighterColor;
                }
                .group-card-icon,
                .group-card-edit-icon {
                    position: relative;
                    display: inline-block;
                    margin-left: 10px;
                    padding: 3px;
                    -webkit-user-select: none;
                    &.active {
                        .tips-content {
                            display: block;
                        }
                    }
                }
                .devops-icon:hover {
                    color: $iconPrimaryColor;
                    cursor: pointer;
                }
            }
            .group-card {
                position: relative;
                background: #fff;
                margin-right: 24px;
                cursor: default;
                /* normal */
                &:hover {
                    .group-card-tools {
                        display: inline-block;
                    }
                    .group-card-title{
                        padding-right: 56px;
                        border: 1px solid $iconPrimaryColor;
                    }
                }
                /** edit **/
                &.group-edit {
                    .group-card-title {
                        padding-right: 16px;
                        border: 1px solid $iconPrimaryColor;
                    }
                    .tag-text {
                        display: none;
                    }
                    .tag-input {
                        display: inline;
                        padding-right: 0;
                    }
                    .group-card-tools {
                        display: none;
                    }
                    .group-card-edit-tools {
                        display: inline;
                    }
                }
                /** showTips **/
                &.showTips {
                    .group-card-title{
                        padding-right: 56px;
                    }
                    .group-card-edit-tools {
                        display: none;
                    }
                    .group-card-tools {
                        display: inline;
                        .tips-content {
                            display: block;
                        }
                    }
                }
                &.active:hover {
                    .group-card-tools {
                        display: none;
                    }
                    .group-card-edit-tools {
                        display: inline;
                        .tips-content {
                            display: block;
                        }
                    }
                    .group-card-title{
                        border: 1px solid $borderWeightColor;
                    }
                }
            }

            .group-card-add {
                width: 389px;
                height: 54px;
                line-height: 54px;
                border: 1px dashed $borderWeightColor;
                background: $bgHoverColor;
            }
            .group-list-creat {
                width: 100%;
                height: 54px;
                line-height: 54px;
                border: 1px dashed $borderWeightColor;
                background: $bgHoverColor;
            }
        }
        &-title {
            position: relative;
            display: inline-block;
            width: 100%;
            padding-right: 20px;
            line-height: 32px;
            margin: 0 0 10px;
            color: #333C48;
            font-size: 14px;
            .title-text {
                display: flex;
                justify-content: space-between;
                width: 100%;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
            }
            .group-title-input {
                flex: 1;
                margin-left: 5px;
            }
            .bk-tooltip {
                position: absolute;
                top: 0;
                right: 5px;
            }
            .icon-more {
                margin-left: -6px;
                color: $fontLighterColor;
                vertical-align: middle;
            }
        }
        &-tools {
            position: absolute;
            top: 2px;
            right: 15px;
            display: block;
            font-size: 14px;
            .group-list-icon {
                margin-left: 10px;
                &:hover {
                    color: $iconPrimaryColor;
                    cursor: pointer;
                }
            }
        }
    }
    .entry-link {
        color: #699DF4;
        font-size: 14px;
        cursor: pointer;
    }

    .tools-config-tooltip {
        span {
            margin: 0 7px;
            font-size: 12px;
            border: 1px solid rgba(255,255,255,0.1)
        }
        a {
            font-size: 12px;
            cursor: pointer;
            &:hover {
                color: $primaryColor;
            }
        }
    }
</style>
