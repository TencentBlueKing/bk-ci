<template>
    <span class="container-type">
        <span v-if="!containerType.showIcon" v-bk-tooltips="containerType.tooltip">
            {{ containerType.content }}
        </span>
        <Logo v-else v-bk-tooltips="containerType.tooltip" v-bind="containerType.iconProps">{{
            containerType.content
        }}</Logo>
    </span>
</template>
<script>
    import { bkTooltips } from 'bk-magic-vue'
    import Logo from './Logo'
    import { localeMixins } from './locale'
    import {
        convertMStoString,
        isNormalContainer,
        isTriggerContainer,
        isVmContainer
    } from './util'

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
                const { container } = this
                const { vmNames = [], baseOS = '', elements = [] } = container
                let iconProps = {}
                let content = ''
                let tooltip = {
                    disabled: true
                }
                let showIcon = true
                switch (true) {
                    case container.timeCost !== undefined: {
                        const { totalCost, executeCost, systemCost } = container.timeCost
                        const lt1Hour = totalCost < 36e5
                        tooltip = {
                            delay: [300, 0],
                            content: `${this.t('userTime')}：${convertMStoString(executeCost)} + ${this.t(
                                'systemTime'
                            )}： ${convertMStoString(systemCost)}`
                        }
                        content = lt1Hour ? convertMStoString(totalCost) : '>1h'
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
        }
    }
</script>
