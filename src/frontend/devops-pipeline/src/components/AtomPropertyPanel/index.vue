<template>
    <bk-sideslider
        class="bkci-property-panel"
        :class="{ 'with-variable-open': showVariable }"
        :is-show.sync="visible"
        :width="640"
        :quick-close="true"
        :before-close="handleBeforeClose"
        :z-index="2016"
        show-mask
    >
        <header class="property-panel-header" slot="header">
            <div class="atom-name-edit">
                <input
                    v-if="nameEditing"
                    :maxlength="30"
                    v-bk-focus="1"
                    @blur="toggleEditName(false)"
                    @keydown.enter="toggleEditName(false)"
                    class="bk-form-input"
                    name="name"
                    v-validate.initial="'required|max:30'"
                    @@keyup.enter="toggleEditName"
                    @input="handleEditName"
                    :placeholder="$t('nameInputTips')"
                    :value="element.name"
                />
                <p v-if="!nameEditing">{{ atomCode ? element.name : this.$t('editPage.pendingAtom') }}</p>
                <i v-if="atomCode && editable" @click="toggleEditName(true)" class="devops-icon icon-edit" :class="nameEditing ? 'editing' : ''" />
            </div>
        </header>
        <atom-content v-bind="$props" slot="content" :handle-update-atom="handleUpdateAtom">
            <template slot="footer">
                <slot name="footer"></slot>
            </template>
        </atom-content>
    </bk-sideslider>
</template>

<script>
    import { navConfirm } from '@/utils/util'
    import { mapActions, mapGetters, mapState } from 'vuex'
    import AtomContent from './AtomContent.vue'

    export default {
        name: 'atom-property-panel',
        components: {
            AtomContent
        },
        props: {
            elementIndex: Number,
            containerIndex: Number,
            containerGroupIndex: Number,
            stageIndex: Number,
            stages: Array,
            editable: Boolean,
            isInstanceTemplate: Boolean,
            closeConfirm: Boolean,
            beforeClose: Function,
            afterHidden: {
                type: Function,
                default: () => () => {
                }
            }
        },
        data () {
            return {
                nameEditing: false
            }
        },
        computed: {
            ...mapState('atom', [
                'showVariable',
                'globalEnvs',
                'isPropertyPanelVisible',
                'isElementModified'
            ]),
            ...mapGetters('atom', [
                'getElement',
                'getContainer',
                'getContainers',
                'getStage'
            ]),
            visible: {
                get () {
                    return this.isPropertyPanelVisible
                },
                set (value) {
                    this.toggleAtomSelectorPopup(value)
                    this.togglePropertyPanel({
                        isShow: value
                    })
                }
            },
            stage () {
                const { stageIndex, getStage, stages } = this
                return getStage(stages, stageIndex)
            },
            containers () {
                const { stage, getContainers } = this
                return getContainers(stage)
            },
            container () {
                const { containerIndex, containerGroupIndex, containers, getContainer } = this
                return getContainer(containers, containerIndex, containerGroupIndex)
            },
            element () {
                const { container, elementIndex, getElement } = this
                const element = getElement(container, elementIndex)
                return element
            },
            atomCode () {
                if (this.element) {
                    const isThrid = this.element.atomCode && this.element['@type'] !== this.element.atomCode
                    if (isThrid) {
                        return this.element.atomCode
                    } else {
                        return this.element['@type']
                    }
                }
                return ''
            }
        },
        methods: {
            ...mapActions('atom', [
                'toggleAtomSelectorPopup',
                'updateAtom',
                'setAtomEditing',
                'togglePropertyPanel'
            ]),
            async handleBeforeClose () {
                console.log('handleBeforeClose', this.closeConfirm, this.isElementModified)
                if (!this.closeConfirm || !this.isElementModified) {
                    return true
                }
                const res = await navConfirm({
                    title: this.$t('leaveConfirmTitle'),
                    content: this.$t('leaveConfirmTips')
                })
                if (res && typeof this.beforeClose === 'function') {
                    await this.beforeClose()
                }
                this.setAtomEditing(false)
                return res
            },

            toggleEditName (show) {
                this.nameEditing = show
            },

            handleEditName (e) {
                const { value } = e.target
                this.handleUpdateAtom('name', value)
            },

            handleUpdateAtom (name, val) {
                this.updateAtom({
                    element: this.element,
                    newParam: {
                        [name]: val
                    }
                })
            }
        }
    }
</script>

<style lang="scss">
    @import './propertyPanel';

    .property-panel-header {
        font-size: 14px;
        font-weight:normal;
        display: flex;
        justify-content: space-between;
        align-items: center;
        height: 60px;
        width: calc(100% - 30px);

        .atom-name-edit {
            display: flex;
            height: 36px;
            line-height: 36px;
            > p {
                max-width: 450px;
                @include ellipsis();
            }
            > .bk-form-input {
                width: 420px;
            }
            .icon-edit {
                cursor: pointer;
                margin-left: 12px;
                line-height: 36px;
                &.editing {
                    display: none;
                }
            }
        }
    }
</style>
