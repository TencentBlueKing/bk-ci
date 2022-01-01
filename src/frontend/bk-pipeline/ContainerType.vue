<template>
    <span class="container-type">
        <i v-bk-tooltips="containerType.tooltip" v-bind="containerType.iconProps">{{containerType.content}}</i>
    </span>
</template>
<script>
    import {
        convertMStoString,
        isVmContainer,
        isTriggerContainer,
        isNormalContainer
    } from './util'
    import { bkTooltips } from 'bk-magic-vue'

    export default {
        name: 'container-type',
        directives: [bkTooltips],
        props: {
            container: Object
        },
        computed: {
            containerType () {
                const { container, convertElapsed } = this
                const { vmNames = [], baseOS = '', elements = [] } = container
                let iconProps = {}
                let content = ''
                let tooltip = {
                    disabled: true
                }
                switch (true) {
                    case container.systemElapsed !== undefined || container.elementElapsed !== undefined: {
                        const systemElapsed = convertElapsed(container.systemElapsed)
                        const elementElapsed = convertElapsed(container.elementElapsed)
                        const elapsedSum = systemElapsed + elementElapsed
                        const lt1Hour = elapsedSum < 36e5

                        tooltip = {
                            content: `${this.$t('editPage.userTime')}：${convertMStoString(elementElapsed)} + ${this.$t('editPage.systemTime')}： ${convertMStoString(systemElapsed)}`
                        }
                        content = lt1Hour ? convertMStoString(elapsedSum) : '>1h'
                        break
                    }
                    case container.isError:
                        iconProps = {
                            class: 'devops-icon icon-exclamation-triangle-shape is-danger'
                        }
                        break
                    case isVmContainer(container):
                        iconProps = {
                            class: `devops-icon icon-${baseOS.toLowerCase()}`,
                            title: vmNames.join(',')
                        }
                        break
                    case isNormalContainer(container):
                        iconProps = {
                            class: 'devops-icon icon-none'
                        }
                        break
                    case isTriggerContainer(container):
                        content = `${elements.length} ${this.$t('settings.item')}`
                        break
                }
                return {
                    iconProps,
                    tooltip,
                    content
                }
            }
        },
        methods: {
            convertElapsed (val) {
                try {
                    return parseInt(val, 10)
                } catch (error) {
                    return 0
                }
            }
        }
    }

</script>
