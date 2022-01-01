<template>
    <section>
        <draggable
            :class="{
                'container-atom-list': true,
                'trigger-container': stageIndex === 0,
                'readonly': !editable
            }"
            :data-baseos="container.baseOS || container.classType"
            v-model="atomList"
            v-bind="dragOptions"
            :move="checkMove"
        >
            <atom
                v-for="(atom, index) in atomList"
                :key="atom.id"
                :stage="stage"
                :container="container"
                :atom="atom"
                :stage-index="stageIndex"
                :container-index="containerIndex"
                :atom-index="index"
                :container-disabled="containerDisabled"
                :is-waiting="isWaiting"
                :user-name="userName"
                :editable="editable"
                :can-skip-element="canSkipElement"
                :is-last-atom="index === atomList.length - 1"
                :prev-atom="index > 0 ? atomList[index - 1] : null"
                :match-rules="matchRules"
                @[COPY_EVENT_NAME]="handleCopy"
                @[DELETE_EVENT_NAME]="handleDelete"
            />
            
            <span
                v-if="editable"
                :class="{ 'add-atom-entry': true, 'block-add-entry': atomList.length === 0 }"
                @click="editAtom(atomList.length - 1, true)"
            >
                <i class="add-plus-icon" />
                <span v-if="atomList.length === 0">{{ $t('editPage.addAtom') }}</span>
            </span>
        </draggable>
    </section>
</template>

<script>
    
    import draggable from 'vuedraggable'
    import Atom from './Atom'
    import { eventBus } from './util'
    import {
        DELETE_EVENT_NAME,
        COPY_EVENT_NAME,
        ATOM_ADD_EVENT_NAME
    } from './constants'
    export default {
        name: 'atom-list',
        components: {
            draggable,
            Atom
        },
        props: {
            stage: Object,
            container: Object,
            stageIndex: Number,
            containerIndex: Number,
            containerGroupIndex: Number,
            containerStatus: String,
            containerDisabled: Boolean,
            editable: {
                type: Boolean,
                default: true
            },
            isPreview: {
                type: Boolean,
                default: false
            },
            canSkipElement: {
                type: Boolean,
                default: false
            },
            handleChange: {
                type: Function,
                required: true
            },
            userName: {
                type: String,
                default: 'unknow'
            },
            matchRules: {
                type: Array,
                default: []
            }
        },
        data () {
            return {
                atomMap: {},
                DELETE_EVENT_NAME,
                COPY_EVENT_NAME
            }
        },
        computed: {
            isWaiting () {
                return this.containerStatus === 'PREPARE_ENV'
            },
            isInstanceEditable () {
                return !this.editable && this.pipeline && this.pipeline.instanceFromTemplate
            },
            atomList: {
                get () {
                    return this.container.elements.map(atom => {
                        atom.isReviewing = atom.status === 'REVIEWING'
                        if (atom.isReviewing) {
                            const atomReviewer = this.getReviewUser(atom)
                            atom.computedReviewers = atomReviewer
                        }

                        return atom
                    })
                },
                set (elements) {
                    this.handleChange(this.container, { elements })
                }
            },
            dragOptions () {
                return {
                    group: 'pipeline-atom',
                    ghostClass: 'sortable-ghost-atom',
                    chosenClass: 'sortable-chosen-atom',
                    animation: 130,
                    disabled: !this.editable
                }
            }
        },
        methods: {
            handleCopy ({ elementIndex, element }) {
                this.container.elements.splice(elementIndex + 1, 0, element)
            },
            handleDelete ({ elementIndex }) {
                this.container.elements.splice(elementIndex, 1)
            },
            checkMove (event) {
                const dragContext = event.draggedContext || {}
                const element = dragContext.element || {}
                const atomCode = element.atomCode || ''
                const os = element.os || []
                const isTriggerAtom = element.category === 'TRIGGER'

                const to = event.to || {}
                const dataSet = to.dataset || {}
                const baseOS = dataSet.baseos || ''

                const isJobTypeOk = os.includes(baseOS) || (os.length <= 0 && (!baseOS || baseOS === 'normal'))
                return !!atomCode && (
                    (isTriggerAtom && baseOS === 'trigger')
                    || (!isTriggerAtom && isJobTypeOk)
                    || (!isTriggerAtom && baseOS !== 'trigger' && os.length <= 0 && element.buildLessRunFlag)
                )
            },

            getReviewUser (atom) {
                try {
                    const list = atom.reviewUsers || (atom.data && atom.data.input && atom.data.input.reviewers)
                    const reviewUsers = list.map(user => user.split(';').map(val => val.trim())).reduce((prev, curr) => {
                        return prev.concat(curr)
                    }, [])
                    return reviewUsers
                } catch (error) {
                    console.error(error)
                    return []
                }
            },
            editAtom (atomIndex, isAdd) {
                const { stageIndex, containerIndex, container } = this
                const editAction = isAdd ? ATOM_ADD_EVENT_NAME : DELETE_EVENT_NAME
                eventBus.$emit(editAction, {
                    container,
                    atomIndex,
                    stageIndex,
                    containerIndex
                })
            }
            
        }
    }
</script>

<style lang="scss">
    @import "./index";
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
            height: $itemHeight;
            margin: 0 0 11px 0;
            background-color: white;
            border-radius: 2px;
            font-size: 14px;
            transition: all .4s ease-in-out;
            z-index: 2;
            .atom-icon {
                text-align: center;
                margin: 0 14.5px;
                font-size: 18px;
                width: 18px;
                fill: currentColor;
            }
            .atom-icon.skip-icon {
                color: #c4cdd6;
            }
            .atom-name span.skip-name {
                text-decoration: line-through;
                color: #c4cdd6;
                &:hover {
                    color: #c4cdd6;
                }
            }
            .pause-button {
                margin-right: 8px;
                color: $primaryColor;
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
                .atom-icon.skip-icon {
                    color: #c4cdd6;
                }
                .atom-icon {
                    color: $primaryColor;
                }
                .add-plus-icon.close, .copy {
                    cursor: pointer;
                    color: #c3cdd7;
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
                @include add-plus-icon(#fff, #fff, #c4c6cd, 16px, true);
                @include add-plus-icon-hover($dangerColor, $dangerColor, white);
                display: none;
                margin-right: 10px;
                border: none;
                transform: rotate(45deg);
                &:before, &:after {
                    left: 7px;
                    top: 4px;
                }
            }

            .copy {
                display: none;
                margin-right: 10px;
                fill: #c4c6cd;
                &:hover {
                    fill: $primaryColor;
                }
            }

            > .atom-name {
                flex: 1;
                color: $fontWeightColor;
                @include ellipsis();
                max-width: 188px;
                span:hover {
                    color: $primaryColor;
                }
            }
            .disabled {
                cursor: not-allowed;
                color: #c4cdd6;
            }

            .executing-job {
                cursor: default;
                &:before {
                    display: inline-block;
                    animation: rotating infinite .6s ease-in-out;
                }
            }

            .atom-operate-area {
                margin: 0 8px 0 2px;
                color: $primaryColor;
            }

            .atom-reviewing-tips {
                
                &[disabled] {
                    cursor: not-allowed;
                    color: #c3cdd7;
                }
            }
            
            .atom-canskip-checkbox {
                margin-right: 6px;
            }
        }

        .quality-item {
            height: 24px;
            line-height: 20px;
            text-align: center;
            background: transparent;
            font-size: 12px;
            &:before {
                height: 40px;
                z-index: 8;
            }
            &:after {
                display: none;
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

        .is-intercept {
            border-color: $warningColor;
            &:hover {
                border-color: $warningColor;
            }
        }

        .last-quality-item {
            &:before {
                height: 22px;
            }
        }

        .quality-atom {
            margin-left: 84px;
            width: 55px;
            border-radius: 12px;
            z-index: 9;
            .atom-title {
                font-weight: bold;
                &:before,
                &:after {
                    content: '';
                    position: absolute;
                    left: 0;
                    top: 10px;
                    height: 1px;
                    width: 84px;
                    border-top: 2px dashed $fontLigtherColor;
                }
                &:before {
                    left: 21.5px;
                    width: 62px;
                }
                &:after {
                    left: 138px;
                    width: 100px;
                }
            }
            &.is-success {
                border-color: $successColor;
                .atom-title {
                    color: $successColor;
                    &:before,
                    &:after {
                        border-color: $successColor;
                    }
                }
            }
            &.is-review {
                border-color: $warningColor;
                .atom-title {
                    color: $warningColor;
                    &:before {
                        border-color: $warningColor;
                    }
                    &:after {
                        display: none;
                    }
                }
            }
            &.is-fail {
                border-color: $dangerColor;
                .atom-title {
                    color: $dangerColor;
                    &:before,
                    &:after {
                        border-top: 2px solid $dangerColor;
                    }
                }
            }
            .handler-list {
                position: absolute;
                right: 0;
                span {
                    color: $primaryColor;
                    font-size: 12px;
                    &:first-child {
                        margin-right: 5px;
                    }
                }
            }
            .executing-job {
                position: absolute;
                top: 6px;
                right: 42px;
                &:before {
                    display: inline-block;
                    animation: rotating infinite .6s ease-in-out;
                }
            }
            .disabled-review span {
                color: #c4cdd6;
                cursor: default;
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
                cursor: pointer;
                .atom-name:hover {
                    span {
                        color: #63656E;
                    }
                    .skip-name {
                        text-decoration: line-through;
                        color: #c4cdd6;
                    }
                }
                &:hover {
                    border-color: $fontLigtherColor;
                    .atom-icon {
                        color: #63656E;
                    }
                    .skip-icon {
                        color: #c4cdd6;
                    }
                }
                &.CANCELED,
                &.SKIP,
                &.REVIEWING {
                    border-color: $warningColor;
                    .atom-icon,
                    .atom-execute-time {
                        color: $warningColor;
                    }
                }
                &.FAILED,
                &.QUALITY_CHECK_FAIL,
                &.HEARTBEAT_TIMEOUT,
                &.QUEUE_TIMEOUT,
                &.EXEC_TIMEOUT {
                    border-color: $dangerColor;
                    .atom-icon,
                    .atom-execute-time {
                        color: $dangerColor;
                    }
                }
                &.SUCCEED,
                &.REVIEW_PROCESSED {
                    border-color: $successColor;
                    
                    .atom-icon,
                    .atom-execute-time {
                        color: $successColor;
                    }
                }
                &.PAUSE {
                    border-color: $pauseColor;
                    
                    .atom-icon,
                    .atom-execute-time {
                        color: $pauseColor;
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
            .quality-prev-atom {
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
            .is-review,
            .is-intercept {
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
        transform: scale(1.0);
    }
</style>
