<template>
    <portal to="atom-selector-popup">
        <transition name="selector-slide">
            <div v-if="showAtomSelectorPopup" class="atom-selector-popup">
                <bk-tab :active.sync="activeTab" class="atom-tab" type="unborder-card">
                    <bk-tab-panel
                        v-for="(panel, index) in atomPanels"
                        v-bind="panel"
                        :key="index">
                        <atom-search-input
                            :tab-name="panel.name"
                            :container="container"
                            :active-tab="activeTab" />
                        <atom-card-list
                            :tab-name="panel.name"
                            :container="container"
                            :active-tab="activeTab"
                            :element-index="elementIndex"
                            :delete-reasons="deleteReasons"
                            :atom-code="atomCode" />
                    </bk-tab-panel>
                </bk-tab>
            </div>
        </transition>
    </portal>
</template>

<script>
    import { mapGetters, mapState, mapActions } from 'vuex'
    import atomSearchInput from './atomSearchInput'
    import atomCardList from './atomCardList'

    export default {
        components: {
            atomSearchInput,
            atomCardList
        },
        props: {
            container: {
                type: Object,
                default: () => ({})
            },
            element: {
                type: Object,
                default: () => ({})
            },
            elementIndex: Number
        },
        data () {
            return {
                activeTab: 'projectAtom',
                deleteReasons: [],
                atomPanels: [
                    { name: 'projectAtom', label: this.$t('editPage.projectAtom') },
                    { name: 'storeAtom', label: this.$t('editPage.store') }
                ]
            }
        },
        computed: {
            ...mapGetters('atom', [
                'getAtomTree',
                'getAtomCodeListByCategory',
                'classifyCodeListByCategory',
                'isTriggerContainer'
            ]),
            ...mapState('atom', [
                'fetchingAtomList',
                'showAtomSelectorPopup',
                'isPropertyPanelVisible',
                'atomClassifyMap',
                'atomCodeList',
                'storeAtomData',
                'atomClassifyCodeList',
                'atomMap',
                'atomModalMap'
            ]),
            atomCode () {
                if (this.element) {
                    const isThird = this.element.atomCode && this.element['@type'] !== this.element.atomCode
                    if (isThird) {
                        return this.element.atomCode
                    } else {
                        return this.element['@type']
                    }
                }
                return null
            }
        },
        created () {
            this.initData()
        },
        methods: {
            ...mapActions('atom', [
                'getDeleteReasons'
            ]),
            initData () {
                this.getDeleteReasons().then(({ data: res }) => {
                    this.deleteReasons = res || []
                })
            }
        }
    }
</script>

<style lang="scss">
    @import '../../scss/conf';
    .atom-selector-popup {
        right: 660px;
        position: absolute;
        width: 600px;
        height: calc(100% - 20px);
        background: white;
        z-index: 10000;
        border: 1px solid $borderColor;
        border-radius: 5px;
        top: 0;
        margin: 10px 0;
        .atom-tab {
            height: 100%;
            .bk-tab-label-wrapper {
                text-align: center;
            }
        }
        .bk-tab-section {
            padding: 0;
            height: calc(100% - 50px);
            .bk-tab-content {
                height: 100%;
                .atom-item-main {
                    height: calc(100% - 62px);
                }
            }
        }
    }
</style>
