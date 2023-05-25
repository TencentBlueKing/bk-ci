<template>
    <bk-sideslider class="bkci-property-panel" width="640" :is-show.sync="visible" :quick-close="true">
        <header class="container-panel-header" slot="header">
            <div class="job-name-edit">
                <input v-if="nameEditing" v-bk-focus="1" @blur="toggleEditName(false)" @keydown.enter="toggleEditName(false)" class="bk-form-input" name="name" maxlength="30" v-validate.initial="'required'" @keyup.enter="toggleEditName" @input="handleContainerChange" :placeholder="$t('nameInputTips')" :value="container.name" />
                <p v-if="!nameEditing">{{ container.name }} ({{ stageIndex + 1}}-{{ containerIndex + 1}})</p>
                <i @click="toggleEditName(true)" class="devops-icon icon-edit" :class="nameEditing ? 'editing' : ''" />
            </div>
            <div v-if="showDebugDockerBtn" :class="!editable ? 'control-bar' : 'debug-btn'">
                <bk-button theme="warning" @click="startDebug">{{ $t('editPage.docker.debugConsole') }}</bk-button>
            </div>
        </header>
        <container-content v-bind="$props" slot="content" ref="container"></container-content>
    </bk-sideslider>
</template>

<script>
    import { mapActions, mapState, mapGetters } from 'vuex'
    import ContainerContent from './ContainerContent'

    export default {
        name: 'container-property-panel',
        components: {
            ContainerContent
        },
        props: {
            containerIndex: Number,
            containerGroupIndex: Number,
            stageIndex: Number,
            stages: Array,
            editable: Boolean,
            title: String
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
            ...mapGetters('atom', [
                'checkShowDebugDockerBtn'
            ]),
            container () {
                try {
                    return this.stages[this.stageIndex].containers[this.containerIndex] || {}
                } catch (err) {
                    console.err(err)
                    return {}
                }
            },
            visible: {
                get () {
                    return this.isPropertyPanelVisible
                },
                set (value) {
                    this.togglePropertyPanel({
                        isShow: value
                    })
                }
            },
            showDebugDockerBtn () {
                return this.checkShowDebugDockerBtn(this.container, this.$route.name, this.execDetail)
            }
        },

        methods: {
            ...mapActions('atom', [
                'togglePropertyPanel',
                'updateContainer'
            ]),
            toggleEditName (show) {
                this.nameEditing = show
            },
            handleContainerChange (e) {
                const { value } = e.target
                this.updateContainer({
                    container: this.container,
                    newParam: {
                        name: value
                    }
                })
            },
            startDebug () {
                this.$refs.container.startDebug()
            }
        }
    }
</script>

<style lang="scss">
    @import '../AtomPropertyPanel/propertyPanel';
    .container-panel-header {
        font-size: 14px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        height: 60px;
        .job-name-edit {
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
