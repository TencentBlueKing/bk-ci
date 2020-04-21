<template>
    <bk-sideslider class="bkci-property-panel" width="640" :quick-close="true" :is-show.sync="visible">
        <header class="property-panel-header" slot="header">
            <div class="atom-name-edit">
                <input v-show="nameEditing" v-bk-focus="1" @blur="toggleEditName(false)" @keydown.enter="toggleEditName(false)" class="bk-form-input" name="name" v-validate.initial="'required|max:30'" @@keyup.enter="toggleEditName" @input="handleEditName" :placeholder="$t('nameInputTips')" :value="element.name" />
                <p v-if="!nameEditing">{{ atomCode ? element.name : this.$t('editPage.pendingAtom') }}</p>
                <i v-if="atomCode && editable" @click="toggleEditName(true)" class="devops-icon icon-edit" :class="nameEditing ? 'editing' : ''" />
            </div>
            <reference-variable :global-envs="globalEnvs" :stages="stages" :container="container" />
        </header>
        <atom-content v-bind="$props" slot="content"></atom-content>
    </bk-sideslider>
</template>

<script>
    import { mapActions, mapState } from 'vuex'
    import ReferenceVariable from './ReferenceVariable'
    import AtomContent from './AtomContent.vue'

    export default {
        name: 'atom-property-panel',
        components: {
            ReferenceVariable,
            AtomContent
        },
        props: {
            elementIndex: Number,
            containerIndex: Number,
            stageIndex: Number,
            stages: Array,
            editable: Boolean,
            isInstanceTemplate: Boolean
        },
        data () {
            return {
                nameEditing: false
            }
        },
        computed: {
            ...mapState('atom', [
                'isPropertyPanelVisible'
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
            }
        },
        methods: {
            ...mapActions('atom', [
                'toggleAtomSelectorPopup',
                'updateAtom',
                'togglePropertyPanel'
            ]),

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
        border-bottom: 1px solid #e6e6e6;

        .atom-name-edit {
            display: flex;
            height: 36px;
            line-height: 36px;
            > p {
                max-width: 450px;
                @include ellipsis();
            }
            > .bk-form-input {
                width: 450px;
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
