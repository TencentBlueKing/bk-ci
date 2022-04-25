<template>
    <span class="container-type">
        <span v-if="!containerType.showIcon" v-bk-tooltips="containerType.tooltip">
            {{containerType.content}}
        </span>
        <Logo v-else v-bk-tooltips="containerType.tooltip" v-bind="containerType.iconProps">{{containerType.content}}</Logo>
    </span>
</template>
<script>
    import {
        convertMStoString,
        isVmContainer,
        isTriggerContainer,
        isNormalContainer
    } from './util'
    import Logo from './Logo'
    import { bkTooltips } from 'bk-magic-vue'
    import { localeMixins } from './locale'

    export default {
        name: 'container-type',
        directives: {
            bkTooltips
        },
        components: {
            Logo
        },
        mixins: [localeMixins],
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
                let showIcon = true
                switch (true) {
                    case container.systemElapsed !== undefined || container.elementElapsed !== undefined: {
                        const systemElapsed = convertElapsed(container.systemElapsed)
                        const elementElapsed = convertElapsed(container.elementElapsed)
                        const elapsedSum = systemElapsed + elementElapsed
                        const lt1Hour = elapsedSum < 36e5
                        tooltip = {
                            content: `${this.t('userTime')}：${convertMStoString(elementElapsed)} + ${this.t('systemTime')}： ${convertMStoString(systemElapsed)}`
                        }
                        content = lt1Hour ? convertMStoString(elapsedSum) : '>1h'
                        showIcon = false
                        break
                    }
                    case container.isError:
                        iconProps = {
                            name: 'exclamation-triangle-shape',
                            class: 'is-danger'
                        }
                        break
                    case isVmContainer(container):
                        iconProps = {
                            name: baseOS.toLowerCase(),
                            title: vmNames.join(',')
                        }
                        break
                    case isNormalContainer(container):
                        iconProps = {
                            name: 'none'
                        }
                        break
                    case isTriggerContainer(container):
                        content = `${elements.length} ${this.t('item')}`
                        showIcon = false
                        break
                }
                return {
                    iconProps,
                    tooltip,
                    content,
                    showIcon
                }
            }
        },
        methods: {
            convertElapsed (val) {
                const numVal = parseInt(val, 10)
                return Number.isInteger(numVal) ? numVal : 0
            }
        }
    }

</script>
