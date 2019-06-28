<template>
    <draggable class="container-atom-list" :class="{ &quot;trigger-container&quot;: isTriggerContainer(container), &quot;readonly&quot;: !editable }" v-model="atomList" :options="dragOptions">
        <li v-for="(atom, index) in atomList" :key="atom.name" :class="{ &quot;atom-item&quot;: true,
                                                                         [atom.status]: atom.status
        }"
            @click="showPropertyPanel(index)">
            <section class="atom-item atom-section normal-atom"
                :class="{ [atom.status]: atom.status,
                          &quot;is-error&quot;: atom.isError,
                          &quot;template-compare-atom&quot;: atom.templateModify }"
            >
                <status-icon v-if="atom.status && atom.status !== 'SKIP'" type="element" :status="atom.status" />
                <status-icon v-else-if="isWaiting && atom.status !== &quot;SKIP&quot;" type="element" status="WAITING" />
                <img v-else-if="atomMap[atom.atomCode] && atomMap[atom.atomCode].icon" :src="atomMap[atom.atomCode].icon" :class="{ &quot;atom-icon&quot;: true, &quot;skip-icon&quot;: useSkipStyle(atom) }" />
                <logo v-else :class="{ &quot;atom-icon&quot;: true, &quot;skip-icon&quot;: useSkipStyle(atom) }" :name="getAtomIcon(atom.atomCode)" size="18" />
                <span class="atom-name" :title="atom.name" :class="{ &quot;skip-name&quot;: useSkipStyle(atom) }">{{ atom.atomCode ? atom.name : '待选择插件' }}</span>

                <span @click.stop="handleAtomCheck(atom.id, 'PROCESS')" v-if="atom.status === 'REVIEWING' && userInfo && isCurrentUser(atom.reviewUsers)" class="atom-reviewing-tips">继续</span>
                <span @click.stop="handleAtomCheck(atom.id, 'ABORT')" v-if="atom.status === 'REVIEWING' && userInfo && isCurrentUser(atom.reviewUsers)" class="atom-reviewing-tips">驳回</span>
                <bk-popover placement="top" v-if="atom.status === 'REVIEWING' && userInfo && !isCurrentUser(atom.reviewUsers)">
                    <span class="atom-review-diasbled-tips">继续</span>
                    <template slot="content">
                        <p>无权限进行此操作,审核人为{{ atom.reviewUsers.join(',') }}</p>
                    </template>
                </bk-popover>
                <bk-popover placement="top" v-if="atom.status === 'REVIEWING' && userInfo && !isCurrentUser(atom.reviewUsers)">
                    <span class="atom-review-diasbled-tips">驳回</span>
                    <template slot="content">
                        <p>无权限进行此操作,审核人为{{ atom.reviewUsers.join(',') }}</p>
                    </template>
                </bk-popover>
                <bk-popover placement="top" v-if="atom.status === 'REVIEW_ABORT'">
                    <span class="atom-review-diasbled-tips">已驳回</span>
                    <template slot="content">
                        <p>审核未通过，审核人为{{ execDetail.cancelUserId || atom.reviewUsers }}</p>
                    </template>
                </bk-popover>
                <span :class="atom.status === 'SUCCEED' ? 'atom-success-timer' : (atom.status === 'REVIEW_ABORT' ? 'atom-warning-timer' : 'atom-fail-timer')" v-if="atom.elapsed < 36e5 && atom.status !== 'SKIP'">
                    <a href="javascript: void(0);" class="atom-single-retry" v-if="atom.canRetry" @click.stop="singleRetry(atom.id)">重试</a>
                    <template v-else>
                        {{ atom.elapsed !== undefined ? localTime(atom.elapsed) : '' }}
                    </template>
                </span>
                <bk-popover placement="top" v-if="atom.elapsed >= 36e5">
                    <span :class="atom.status === 'SUCCEED' ? 'atom-success-timer' : (atom.status === 'REVIEW_ABORT' ? 'atom-warning-timer' : 'atom-fail-timer')">&gt;1h</span>
                    <template slot="content">
                        <p>{{ atom.elapsed ? localTime(atom.elapsed) : '' }}</p>
                    </template>
                </bk-popover>
                <i v-if="editable" @click.stop="editAtom(index, false)" class="add-plus-icon close" />
                <i v-if="editable && atom.isError" class="bk-icon icon-exclamation-triangle-shape" />
                <bk-checkbox v-if="isPreview && container['@type'].indexOf('trigger') < 0" class="atom-canskip-checkbox" @click.stop v-model="atom.canElementSkip" :disabled="useSkipStyle(atom)" />
            </section>
        </li>
        <span v-if="editable" :class="{ &quot;add-atom-entry&quot;: true, &quot;block-add-entry&quot;: atomList.length === 0 }" @click="editAtom(atomList.length - 1, true)">
            <i class="add-plus-icon" />
            <span v-if="atomList.length === 0">添加插件</span>
        </span>

    </draggable>
</template>

<script>
    import StatusIcon from './StatusIcon'
    import { mapActions, mapGetters, mapState } from 'vuex'
    import { coverTimer } from '@/utils/util'
    import draggable from 'vuedraggable'
    import Logo from '@/components/Logo'
    export default {
        name: 'atom-list',
        components: {
            StatusIcon,
            draggable,
            Logo
        },
        props: {
            container: Object,
            stageIndex: Number,
            containerIndex: Number,
            containerStatus: String,
            editable: {
                type: Boolean,
                default: true
            },
            isPreview: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                reviewLoading: false
            }
        },
        computed: {
            ...mapState('atom', [
                'execDetail',
                'atomMap'
            ]),
            ...mapGetters('atom', [
                'isTriggerContainer',
                'getElements',
                'getStage'
            ]),
            isWaiting () {
                return this.containerStatus === 'PREPARE_ENV'
            },
            userInfo () {
                return this.$userInfo
            },
            routerParams () {
                return this.$route.params
            },
            atomList: {
                get () {
                    const atoms = this.getElements(this.container)
                    return atoms
                },
                set (elements) {
                    const { container, updateContainer } = this
                    updateContainer({
                        container,
                        newParam: {
                            elements
                        }
                    })
                }
            },
            dragOptions () {
                return {
                    ghostClass: 'sortable-ghost-atom',
                    chosenClass: 'sortable-chosen-atom',
                    animation: 200,
                    disabled: !this.editable
                }
            }
        },
        methods: {
            ...mapActions('atom', [
                'updateContainer',
                'handleCheckAtom',
                'requestPipelineExecDetail',
                'togglePropertyPanel',
                'addAtom',
                'deleteAtom'
            ]),
            getAtomIcon (atomCode) {
                if (!atomCode) {
                    return 'placeholder'
                }
                return document.getElementById(atomCode) ? atomCode : 'order'
            },
            localTime (time) {
                return coverTimer(time)
            },
            isCurrentUser (users) {
                const targetArr = users.map(user => user.trim())

                return this.userInfo && targetArr.indexOf(this.userInfo.username) > -1
            },
            showPropertyPanel (elementIndex) {
                const { stageIndex, containerIndex } = this
                this.togglePropertyPanel({
                    isShow: true,
                    editingElementPos: {
                        stageIndex,
                        containerIndex,
                        elementIndex
                    }
                })
            },
            editAtom (atomIndex, isAdd) {
                const { stageIndex, containerIndex, container, addAtom, deleteAtom } = this
                const editAction = isAdd ? addAtom : deleteAtom
                editAction({
                    container,
                    atomIndex,
                    stageIndex,
                    containerIndex
                })
            },
            async handleAtomCheck (elementId, action) {
                try {
                    const data = {
                        projectId: this.routerParams.projectId,
                        pipelineId: this.routerParams.pipelineId,
                        buildId: this.routerParams.buildNo,
                        elementId,
                        action
                    }
                    const res = await this.handleCheckAtom(data)
                    if (res === true) {
                        this.$showTips({
                            message: action === 'ABORT' ? '驳回成功' : '审核成功',
                            theme: 'success'
                        })
                        this.requestPipelineExecDetail(this.routerParams)
                    }
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                }
            },
            singleRetry (taskId) {
                if (typeof taskId === 'string') {
                    this.retryPipeline(taskId)
                }
            },
            /**
             * 重试流水线
             */
            async retryPipeline (taskId) {
                let message, theme
                try {
                    // 请求执行构建
                    const res = await this.$store.dispatch('pipelines/requestRetryPipeline', {
                        projectId: this.routerParams.projectId,
                        pipelineId: this.routerParams.pipelineId,
                        buildId: this.routerParams.buildNo,
                        taskId: taskId
                    })
                    if (res.id) {
                        message = '重试成功'
                        theme = 'success'

                        this.$router.push({
                            name: 'pipelinesDetail',
                            params: {
                                buildNo: res.id
                            }
                        })
                        this.requestPipelineExecDetail(this.routerParams)
                    } else {
                        message = '重试失败'
                        theme = 'error'
                    }
                } catch (err) {
                    if (err.code === 403) { // 没有权限执行
                        this.$showAskPermissionDialog({
                            noPermissionList: [{
                                resource: '流水线',
                                option: '执行'
                            }],
                            applyPermissionUrl: `${PERM_URL_PIRFIX}/backend/api/perm/apply/subsystem/?client_id=pipeline&project_code=${this.routerParams.projectId}&service_code=pipeline&role_executor=pipeline:${this.routerParams.pipelineId}`
                        })
                        return
                    } else {
                        message = err.message || err
                        theme = 'error'
                    }
                } finally {
                    message && this.$showTips({
                        message,
                        theme
                    })
                }
            },
            useSkipStyle (atom) {
                return atom && (atom.status === 'SKIP' || (atom.additionalOptions && atom.additionalOptions.enable === false) || (this.container.jobControlOption && this.container.jobControlOption.enable === false))
            }
        }
    }
</script>

<style lang="scss">
    @import "./Stage";
    .container-atom-list {
        position: relative;
        z-index: 3;

        &.trigger-container .atom-item {
            &:before,
            &:after {
                display: none;
            }
        }
        .atom-item {
            position: relative;
            display: flex;
            flex-direction: row;
            align-items: center;
            width: 240px;
            height: $itemHeight;
            margin: 0 0 11px 0;
            background-color: white;
            border-radius: 2px;
            font-size: 14px;
            cursor: pointer;
            transition: all .4s ease-in-out;
            z-index: 2;
            .atom-icon {
                text-align: center;
                margin: 0 14.5px;
                font-size: 18px;
                width: 18px;
                fill: currentColor;
            }
            .skip-icon {
                color: #c4cdd6;
            }
            .skip-name {
                text-decoration: line-through;
                color: #c4cdd6;
            }

            &.is-error {
                border-color: $dangerColor;
                color: $dangerColor;
                &:hover {
                   .icon-exclamation-triangle-shape {
                        display: none;
                    }
                }
                .icon-exclamation-triangle-shape {
                    margin: 0 12px;
                }
            }

            &:hover{
                border-color: $primaryColor;
                color: $primaryColor;
                .add-plus-icon.close {
                    display: block;
                }
            }
            &:first-child {
                &:before {
                    top: -16px;
                }
            }
            &:before {
                content: '';
                position: absolute;
                height: 14px;
                width: 2px;
                background: $fontLigtherColor;
                top: -12px;
                left: 21.5px;
                z-index: 1;
            }

            &:after {
                content: '';
                position: absolute;
                height: 4px;
                width: 4px;
                border: 2px solid $fontLigtherColor;
                border-radius: 50%;
                background: white;
                top: -5px;
                left: 18.5px;
                z-index: 2;
            }

            .add-plus-icon.close {
                @include add-plus-icon($fontLigtherColor, $fontLigtherColor, white, 18px, true);
                @include add-plus-icon-hover($dangerColor, $dangerColor, white);
                margin: 0 12px;
                display: none;
                transform: rotate(45deg);
            }

            > .atom-name {
                flex: 1;
                color: $fontWeightColor;
                @include ellipsis();
            }

            .atom-review-diasbled-tips {
                cursor: default;
                color: #c3cdd7;
                margin: 0 8px 0 2px;
            }
            .atom-reviewing-tips {
                margin: 0 8px 0 2px;
                color: $primaryColor;
            }
            .atom-success-timer {
                margin: 0 8px 0 2px;
                color: $successColor;
            }
            .atom-fail-timer {
                margin: 0 8px 0 2px;
                color: $failColor;
            }
            .atom-warning-timer {
                margin: 0 8px 0 2px;
                color: $warningColor;
            }
            .atom-single-retry {
                color: $primaryColor;
            }
            .atom-canskip-checkbox {
                margin-right: 6px;
            }
        }

        .atom-section {
            margin: 0;
            width: 100%;
            height: 100%;
            border: 1px solid $fontLigtherColor;
            &:before,
            &:after {
                display: none;
            }
        }

        .add-atom-entry {
            position: absolute;
            bottom: -10px;
            left: 111px;
            background-color: white;
            cursor: pointer;
            z-index: 3;
            .add-plus-icon {
                @include add-plus-icon($fontLigtherColor, $fontLigtherColor, white, 18px, true);
                @include add-plus-icon-hover($primaryColor, $primaryColor, white);
            }
            &.block-add-entry {
                @extend .atom-item;
                position: static;
                border-style: dashed;
                color: $borderWeightColor;
                border-color: $borderWeightColor;
                border-width: 1px;
                .add-plus-icon {
                    margin: 12px 13px;
                }
                &:before,
                &:after {
                    display: none;
                }
            }

            &:hover {
                border-color: $primaryColor;
                color: $primaryColor;
            }
        }

        &.readonly {
            .atom-item {
                &:hover {
                    color: $fontColor;
                    border-color: $fontLigtherColor;
                }
                &.CANCELED, &.REVIEWING {
                    border-color: $warningColor;
                    // &:before {
                    //     background: $warningColor;
                    // }

                    // &:after {
                    //     border: 2px solid $warningColor;
                    //     background: white;
                    // }
                    .atom-icon {
                        color: $warningColor;
                    }
                }
                &.FAILED, &.HEARTBEAT_TIMEOUT, &.QUEUE_TIMEOUT, .EXEC_TIMEOUT {
                    border-color: $dangerColor;
                    // &:before {
                    //     background: $dangerColor;
                    // }

                    // &:after {
                    //     border: 2px solid $dangerColor;
                    //     background: white;
                    // }
                    .atom-icon {
                        color: $dangerColor;
                    }
                }
                &.SUCCEED, &.REVIEW_PROCESSED {
                    border-color: $successColor;
                    // &:before {
                    //     background: $successColor;
                    // }

                    // &:after {
                    //     border: 2px solid $successColor;
                    //     background: white;
                    // }
                    .atom-icon {
                        color: $successColor;
                    }
                }
            }
            .arrival-atom {
                &:before {
                    background: $successColor;
                }
                &:after {
                    border: 2px solid $successColor;
                    background: white;
                }
            }
            .qualitt-next-atom {
                &:before {
                    height: 24px;
                    top: -23px;
                }
            }
            .template-compare-atom {
                // border-color: $warningColor;
                border-color: #ff6e00;
                &:hover {
                   border-color: $warningColor;
                }
            }
            .is-fail {
                &:hover {
                   border-color: $dangerColor;
                }
            }
            .is-review {
                &:hover {
                   border-color: $warningColor;
                }
            }
            .is-success,
            .is-process {
                &:hover {
                   border-color: $successColor;
                }
                &:before {
                    background: $successColor;
                }
            }
        }
    }

    .sortable-ghost-atom {
        opacity: 0.5;
    }
    .sortable-chosen-atom {
        transform: scale(1.1);
    }
</style>
