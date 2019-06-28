<template>
    <article class="group-list">
        <section class="loading-wrapper"
            v-bkloading="{
                isLoading: loading.isLoadings,
                title: loading.title
            }">
            <div class="group-list-wrapper">
                <section v-show="showContent" :class="['group-list-content','clearfix',{ 'group-list-center': hasGroup }]">
                    <div class="group-list-cards"
                        v-for="(group, groupIndex) in tagGroupList" :key="groupIndex"
                        v-bkloading="{ isLoading: loading.isLoading }">
                        <div class="group-list-title">
                            <span class="title-text">{{group.name}}</span>
                            <bk-popover placement="bottom" class="group-list-edit" theme="light">
                                <span class="bk-icon icon-more"></span>
                                <div class="group-operate-container" slot="content">
                                    <p class="entry-link" @click="showDialog($event, groupIndex)">重命名</p>
                                    <p class="entry-link" @click="deleteGroup(groupIndex)">删除</p>
                                </div>
                            </bk-popover>
                        </div>

                        <div :class="['group-card', { 'active': active.isActiveGroup === 's' + groupIndex + tagIndex }, { 'showTips': active.isActiveToops === ('s' + groupIndex + tagIndex) }, { 'group-edit': (active.isGroupEdit === 's' + groupIndex + tagIndex) }]"
                            v-for="(item, tagIndex) in group.labels" :key="tagIndex"
                            @mouseleave="inputMouseLeave">
                            <div class="group-card-title">
                                <span class="tag-text">{{item.name}}</span>
                                <input type="text" class="tag-input"
                                    v-model="tagValue"
                                    v-focus="isFocus(groupIndex, tagIndex)"
                                    maxlength="20"
                                    @blur="inputBlur($event, groupIndex, tagIndex)"
                                    @keyup.enter="tagModify(groupIndex, tagIndex)"
                                    placeholder="请输入标签名称,按Enter键确定">
                            </div>
                            <div class="group-card-tools">
                                <i class="bk-icon icon-edit2 group-card-icon" @click="tagEdit($event, groupIndex, tagIndex)"></i>
                                <span class="tools-ele group-card-icon bk-icon icon-close"
                                    @click.stop="toggleTools(groupIndex, tagIndex)">
                                    <div class="tips-content">
                                        <a class="confirm" @click.stop="tagRemove(groupIndex, tagIndex)">确定</a>
                                        <span></span>
                                        <a class="cancel" @click.stop="hideTools">取消</a>
                                    </div>
                                </span>
                            </div>
                        </div>
                        <!-- <div class="group-card" v-if="group.isAdd">
                            <div class="group-card-title group-edit">
                                <input type="text"  class="tag-input"
                                    v-model="tagValue"
                                    @blur="inputBlur($event, groupIndex)"
                                    @keyup.enter="tagModify(groupIndex)"
                                    placeholder="请输入标签名称,按Enter键确定">
                            </div>
                        </div> -->

                        <bk-button
                            size="normal"
                            icon="bk-icon icon-plus"
                            class="group-card-add"
                            v-if="group.labels.length < 10"
                            @click="tagAdd($event, groupIndex)"
                            :disabled="btnIsdisable"
                        >
                            新建标签
                        </bk-button>
                        <bk-button v-else class="group-card-add" size="normal" :disabled="true">
                            一个分组下最多可以添加10个标签
                        </bk-button>
                    </div>

                    <div class="group-list-cards" v-if="isShowGroupBtn()">
                        <h3 class="group-list-title"></h3>
                        <bk-button size="large" icon="bk-icon icon-plus" class="group-list-creat"
                            :disabled="btnIsdisable"
                            @click="showDialog"
                        >
                            新建分组
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
                                    新建分组
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
                            <label class="bk-label">分组名称</label>
                            <div class="bk-form-content">
                                <input type="text"
                                    class="bk-form-input"
                                    placeholder="请输入分组名称"
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
    import imgemptyTips from '@/components/pipelineList/imgEmptyTips'
    import { navConfirm } from '@/utils/util'

    export default {
        directives: {
            focus: {
                inserted: function (el) {
                    el.focus()
                }
            }
        },
        components: {
            'empty-tips': imgemptyTips
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
                    title: '创建一个标签分组',
                    desc: '为流水线分配合理的标签有助于快速定位和管理流水线，请了解以下约束:',
                    contentOne: '1. 一个项目下最多可以添加3个分组',
                    contentTwo: '2. 一个分组下最多可以添加10个标签'
                },
                groupSetting: {
                    hasHeader: true,
                    isShow: false,
                    title: '新建分组',
                    value: '',
                    groupIndex: null,
                    padding: 20
                },
                tagValue: '',
                btnIsdisable: false,
                isAddTagEnter: false,
                active: {
                    isActiveToops: false,
                    oldActiveToops: 'xxx',
                    isGroupEdit: false,
                    isActiveGroup: false
                },
                defaultSettings: {
                    isShow: false,
                    title: '筛选',
                    quickClose: true,
                    width: 360
                }
            }
        },
        computed: {
            ...mapGetters({
                'tagGroupList': 'pipelines/getTagGroupList'
            }),
            hasGroup () {
                return (!this.tagGroupList.length || this.tagGroupList.length < 1)
            },
            projectId () {
                return this.$route.params.projectId
            }
        },
        async created () {
            await this.init()
            this.addClickListenr()
        },
        mounted () {

        },
        methods: {
            isShowGroupBtn () {
                if (this.tagGroupList.length > 0 && this.tagGroupList.length < 3) {
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

            showDialog (event, groupIndex) {
                const setting = {
                    isShow: true
                }
                if (groupIndex || groupIndex === 0) {
                    setting.title = '重命名'
                    setting.groupIndex = groupIndex
                    setting.value = this.tagGroupList[groupIndex].name
                } else {
                    setting.title = '新建分组'
                    setting.groupIndex = null
                    setting.value = ''
                }
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
                if (groups.title === '新建分组') {
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
                const name = this.tagGroupList[groupIndex].name
                const { $store } = this
                const content = `删除${name}标签分组?`
                navConfirm({ title: '确认', content })
                    .then(async () => {
                        try {
                            await $store.dispatch('pipelines/deleteGroup', {
                                groupId: this.tagGroupList[groupIndex].id
                            }).then(res => {
                                if (res) {
                                    $store.commit('pipelines/removeTagGroupById', {
                                        groupId: this.tagGroupList[groupIndex].id
                                    })
                                }
                            })
                        } catch (err) {
                            this.errShowTips(err)
                        }
                    }).catch(() => {})
            },

            async tagRemove (groupIndex, tagIndex) { // 标签删除
                this.REQUEST('deleteTag', {
                    labelId: this.tagGroupList[groupIndex].labels[tagIndex].id
                })
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
            },

            tagEdit (e, groupIndex, tagIndex) {
                const tagList = this.tagGroupList[groupIndex].labels
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
            },

            tagAdd (e, groupIndex) {
                const group = this.tagGroupList[groupIndex]
                this.tagValue = ''
                this.btnIsdisable = true
                this.$store.commit('pipelines/resetTag', {
                    groupIndex: groupIndex,
                    boolean: true
                })
                this.active.isGroupEdit = 's' + groupIndex + (group.labels.length - 1)
            },
            async tagModify (groupIndex, tagIndex) { // 标签input回车
                const { $store } = this
                const group = this.tagGroupList[groupIndex]
                let path, params
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
                    this.REQUEST(path, params, () => {
                        this.reset(groupIndex, tagIndex)
                    })
                    this.btnIsdisable = false
                    return false
                } else {
                    return false
                }
            },
            inputBlur (e, groupIndex, tagIndex) {
                const group = this.tagGroupList[groupIndex]
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
                this.isAddTagEnter = false
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
                const { $store } = this
                try {
                    const res = await $store.dispatch('pipelines/' + method, params)
                    if (res) {
                        this.requestGrouptLists()
                        if (call) {
                            call()
                        }
                    }
                } catch (err) {
                    this.errShowTips(err)
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
        color: $fontLigtherColor;
    }
    .bk-sideslider-wrapper {
        top: 0;
    }
    .group-card {
        width: 314px;
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
                width: 250px;
                height: 60px;
                margin: 0 auto;
                line-height: 20px;
                font-size: 14px;
                text-align: left;
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
                    border: 1px solid $fontLigtherColor;
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
        &-content {
            width: 1002px;
            margin: 0 auto;
            padding-top: 16px;
            transform: translateX(10px)
        }
        &-center {
            transform: translateX(0);
        }
        &-cards {
            float: left;
            width: 314px;
            margin: 0 20px 15px 0;
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
            .group-card-tools {
                display: none;
                position: absolute;
                top: 21px;
                right:16px;
                line-height: 18px;
                font-size: 0;
                .bk-icon {
                    font-size: 10px;
                    color: $fontLigtherColor;
                }
                .group-card-icon {
                    position: relative;
                    display: inline-block;
                    margin-left: 7px;
                    padding: 3px;
                    -webkit-user-select: none;
                    &.active {
                        .tips-content {
                            display: block;
                        }
                    }
                    .tips-content {
                        display: none;
                        position: absolute;
                        bottom: 22px;
                        left: -38px;
                        padding: 0 12px;
                        width: 90px;
                        height: 30px;
                        line-height: 30px;
                        border: none;
                        border-radius: 2px;
                        font-size: 0;
                        color: #fff;
                        background-color: rgba(0,0,0,0.9);
                        z-index: 50;
                        span {
                            margin: 0 7px;
                            font-size: 12px;
                            border: 1px solid rgba(255,255,255,0.1)
                        }
                        a {
                            font-size: 12px;
                            cursor: pointer;
                        }
                        &:after {
                            position: absolute;
                            content: '';
                            width: 6px;
                            height: 6px;
                            border: 1px solid #000;
                            border-bottom: 0;
                            border-right: 0;
                            transform: rotate(45deg);
                            background: rgba(0,0,0,0.9);
                            bottom: -2px;
                            right: 42px;
                        }
                    }
                }
                .bk-icon:hover {
                    color: $iconPrimaryColor;
                    cursor: pointer;
                }
            }
            .group-card {
                position: relative;
                background: #fff;
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
                }
                /** showTips **/
                &.showTips {
                    .group-card-title{
                        padding-right: 56px;
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
                    .group-card-title{
                        border: 1px solid $borderWeightColor;
                    }
                }
            }

            .group-card-add {
                width: 100%;
                height: 54px;
                line-height: 54px;
                border: 1px dashed $borderWeightColor;
                background: $bgHoverColor;
            }
            .group-list-creat {
                width: 100%;
                height: 113px;
                line-height: 113px;
                border: 1px dashed $borderWeightColor;
                background: $bgHoverColor;
            }
        }
        &-title {
            position: relative;
            display: inline-block;
            max-width: 100%;
            padding-right: 20px;
            height: 24px;
            line-height: 24px;
            margin: 0 0 13px;
            color: #333C48;
            font-size: 18px;
            .title-text {
                display: inline-block;
                width: 100%;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
            }
            .bk-tooltip {
                position: absolute;
                top: 0;
                right: 5px;
            }
            .icon-more {
                margin-left: -6px;
                color: $fontLigtherColor;
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
    .group-operate-container {
        .entry-link {
            padding: 5px 3px;
            font-size: 14px;
            cursor: pointer;
            color: $fontWeightColor;
            > a {
                color: $fontWeightColor;
            }
            &:hover {
                color: $primaryColor;
                > a {
                    color: $primaryColor;
                }
            }
        }
    }
</style>
