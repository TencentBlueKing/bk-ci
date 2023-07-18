<template>
    <section>
        <draggable
            :class="{
                'container-atom-list': true,
                'trigger-container': stageIndex === 0,
                readonly: !reactiveData.editable
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
                :container-group-index="containerGroupIndex"
                :atom-index="index"
                :container-disabled="containerDisabled"
                :is-waiting="isWaiting"
                :is-last-atom="index === atomList.length - 1 && !hasHookAtom"
                :prev-atom="index > 0 ? atomList[index - 1] : null"
                @[COPY_EVENT_NAME]="handleCopy"
                @[DELETE_EVENT_NAME]="handleDelete"
            />

            <span
                v-if="reactiveData.editable"
                :class="{ 'add-atom-entry': true, 'block-add-entry': atomList.length === 0 }"
                @click="editAtom(atomList.length - 1, true)"
            >
                <i class="add-plus-icon" />
                <template v-if="atomList.length === 0">
                    <span class="add-atom-label">{{ t("addAtom") }}</span>
                    <Logo class="atom-invalid-icon" name="exclamation-triangle-shape" />
                </template>
            </span>
            <span
                v-if="hasHookAtom"
                :style="`top: ${hookToggleTop}`"
                :class="{
                    'post-action-arrow': true,
                    [postActionStatus]: true,
                    'post-action-arrow-show': showPostAction
                }"
                @click.stop="togglePostAction"
                v-bk-tooltips="hookToggleTips"
            >
                <logo class="toggle-post-action-icon" size="6" name="angle-down"></logo>
            </span>
        </draggable>
    </section>
</template>

<script>
    import draggable from 'vuedraggable'
    import Atom from './Atom'
    import Logo from './Logo'
    import { eventBus } from './util'
    import { localeMixins } from './locale'
    import {
        DELETE_EVENT_NAME,
        COPY_EVENT_NAME,
        ATOM_ADD_EVENT_NAME,
        STATUS_MAP,
        QUALITY_IN_ATOM_CODE,
        QUALITY_OUT_ATOM_CODE
    } from './constants'
    export default {
        name: 'atom-list',
        components: {
            draggable,
            Logo,
            Atom
        },
        inject: ['reactiveData'],
        mixins: [localeMixins],
        props: {
            stage: {
                type: Object,
                required: true
            },
            container: {
                type: Object,
                required: true
            },
            stageIndex: {
                type: Number,
                required: true
            },
            containerIndex: {
                type: Number,
                required: true
            },
            containerGroupIndex: Number,
            containerStatus: String,
            containerDisabled: Boolean,
            handleChange: {
                type: Function,
                required: true
            }
        },
        data () {
            return {
                atomMap: {},
                showPostAction: false,
                DELETE_EVENT_NAME,
                COPY_EVENT_NAME
            }
        },
        computed: {
            isWaiting () {
                return this.containerStatus === STATUS_MAP.PREPARE_ENV
            },
            isInstanceEditable () {
                return (
                    !this.reactiveData.editable && this.pipeline && this.pipeline.instanceFromTemplate
                )
            },
            hasHookAtom () {
                return this.container.elements.some(this.isHookAtom)
            },
            hookToggleTips () {
                return this.t(`${this.showPostAction ? 'fold' : 'open'}POST`)
            },
            hookToggleTop () {
                const firstHookIndex = this.container.elements.findIndex(this.isHookAtom)
                if (firstHookIndex > -1) {
                    let top = 0
                    const hookToggleSize = 7
                    this.container.elements.forEach((atom, index) => {
                        if (index < firstHookIndex) {
                            top += this.isQualityGate(atom) ? 35 : 53
                        }
                    })
                    console.log(top, hookToggleSize)
                    // TODO: more elegant
                    return `${top - hookToggleSize}px`
                }
                return 0
            },
            atomList: {
                get () {
                    return this.container.elements
                        .filter((atom) => !this.isHookAtom(atom) || this.showPostAction)
                        .map((atom) => {
                            atom.isReviewing = atom.status === STATUS_MAP.REVIEWING
                            if (atom.isReviewing) {
                                const atomReviewer = this.getReviewUser(atom)
                                atom.computedReviewers = atomReviewer
                            }
                            if (!atom.atomCode) {
                                atom.atomCode = atom['@type']
                            }
                            return atom
                        })
                },
                set (elements) {
                    this.handleChange(this.container, { elements })
                }
            },
            postActionStatus () {
                if (this.hasHookAtom) {
                    const postAtoms = this.container.elements.filter(this.isHookAtom)
                    for (let i = 0; i < postAtoms.length; i++) {
                        const atom = postAtoms[i]
                        switch (atom.status) {
                            case STATUS_MAP.FAILED:
                            case STATUS_MAP.CANCELED:
                                return atom.status
                            case STATUS_MAP.SUCCEED:
                                if (i === postAtoms.length - 1) {
                                    return atom.status
                                }
                                break
                            case STATUS_MAP.RUNNING:
                                return atom.status
                        }
                    }
                }
                return ''
            },
            dragOptions () {
                return {
                    group: 'pipeline-atom',
                    ghostClass: 'sortable-ghost-atom',
                    chosenClass: 'sortable-chosen-atom',
                    animation: 130,
                    disabled: !this.reactiveData.editable
                }
            }
        },
        methods: {
            isHookAtom (atom) {
                try {
                    return !!atom.additionalOptions?.elementPostInfo
                } catch (error) {
                    return false
                }
            },
            isQualityGate (atom) {
                try {
                    return [QUALITY_IN_ATOM_CODE, QUALITY_OUT_ATOM_CODE].includes(atom.atomCode)
                } catch (error) {
                    return false
                }
            },
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
                const isJobTypeOk
                    = os.includes(baseOS) || (os.length <= 0 && (!baseOS || baseOS === 'normal'))
                return (
                    !!atomCode
                    && ((isTriggerAtom && baseOS === 'trigger')
                        || (!isTriggerAtom && isJobTypeOk)
                        || (!isTriggerAtom
                            && baseOS !== 'trigger'
                            && os.length <= 0
                            && element.buildLessRunFlag))
                )
            },

            getReviewUser (atom) {
                try {
                    const list
                        = atom.reviewUsers || (atom.data && atom.data.input && atom.data.input.reviewers)
                    const reviewUsers = list
                        .map((user) => user.split(';').map((val) => val.trim()))
                        .reduce((prev, curr) => {
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
            },
            togglePostAction () {
                this.showPostAction = !this.showPostAction
            },
            expandPostAction () {
                this.showPostAction = true
            }
        }
    }
</script>

<style lang="scss">
@import "./conf";
.container-atom-list {
  position: relative;
  z-index: 3;

  .sortable-ghost-atom {
    opacity: 0.5;
  }
  .sortable-chosen-atom {
    transform: scale(1);
  }
  .add-atom-entry {
    position: absolute;
    bottom: -10px;
    left: 111px;
    background-color: white;
    cursor: pointer;
    z-index: 3;
    .add-plus-icon {
      @include add-plus-icon($fontLighterColor, $fontLighterColor, white, 18px, true);
      @include add-plus-icon-hover($primaryColor, $primaryColor, white);
    }
    &.block-add-entry {
      display: flex;
      flex-direction: row;
      align-items: center;
      height: $itemHeight;
      margin: 0 0 11px 0;
      background-color: #fff;
      border-radius: 2px;
      font-size: 14px;
      transition: all 0.4s ease-in-out;
      z-index: 2;
      position: static;
      padding-right: 12px;
      border-style: dashed;
      color: $dangerColor;
      border-color: $dangerColor;
      border-width: 1px;
      .add-atom-label {
        flex: 1;
        color: $borderWeightColor;
      }
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
  .post-action-arrow {
    position: relativecd;
    display: flex;
    align-items: center;
    justify-content: center;
    position: absolute;
    height: 14px;
    width: 14px;
    border: 1px solid $unexecColor;
    color: $unexecColor;
    border-radius: 50%;
    background: white !important;
    top: -7px;
    left: 17px;
    z-index: 3;
    font-weight: bold;
    .toggle-post-action-icon {
      display: block;
      transition: all 0.5s ease;
    }
    &.post-action-arrow-show {
      .toggle-post-action-icon {
        transform: rotate(180deg);
      }
    }
    &::after {
      content: "";
      position: absolute;
      width: 2px;
      height: 6px;
      background-color: $unexecColor;
      left: 5px;
      top: -6px;
    }

    &.FAILED {
      border-color: $dangerColor;
      color: $dangerColor;
      &::after {
        background-color: $dangerColor;
      }
    }
    &.CANCELED {
      border-color: $warningColor;
      color: $warningColor;
      &::after {
        background-color: $warningColor;
      }
    }

    &.SUCCEED {
      border-color: $successColor;
      color: $successColor;
      &::after {
        background-color: $successColor;
      }
    }
    &.RUNNING {
      border-color: $primaryColor;
      color: $primaryColor;
      &::after {
        background-color: $primaryColor;
      }
    }
  }
}
</style>
