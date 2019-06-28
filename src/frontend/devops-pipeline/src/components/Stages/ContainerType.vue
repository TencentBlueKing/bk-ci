<script>
    import { mapGetters } from 'vuex'
    import { coverTimer, convertMStoString } from '@/utils/util'
    export default {
        name: 'container-type',
        props: {
            container: Object
        },
        computed: {
            ...mapGetters('atom', [
                'isVmContainer',
                'isNormalContainer',
                'isTriggerContainer'
            ]),
            icon () {
                const { container, isVmContainer, isNormalContainer, isTriggerContainer, convertElapsed } = this
                const { vmNames = [], baseOS = '', elements = [] } = container
                let iconProps = {}
                switch (true) {
                    case container.systemElapsed !== undefined || container.elementElapsed !== undefined:
                        const systemElapsed = convertElapsed(container.systemElapsed)
                        const elementElapsed = convertElapsed(container.elementElapsed)
                        const elapsedSum = systemElapsed + elementElapsed
                        const lt1Hour = elapsedSum < 36e5
                    
                        return (
                            <i v-bk-tooltips={{ content: `用户耗时：${convertMStoString(elementElapsed)} + 系统耗时： ${convertMStoString(systemElapsed)}` }}>{lt1Hour ? coverTimer(elapsedSum) : '>1h'}</i>
                        )
                    case container.isError:
                        iconProps = {
                            class: 'bk-icon icon-exclamation-triangle-shape is-danger'
                        }
                        break
                    case isVmContainer(container):
                        iconProps = {
                            class: `bk-icon icon-${baseOS.toLowerCase()}`,
                            title: vmNames.join(',')
                        }
                        break
                    case isNormalContainer(container):
                        iconProps = {
                            class: 'bk-icon icon-none'
                        }
                        break
                    case isTriggerContainer(container):
                        return <i>{elements.length}个</i>
                }
                return <i {...iconProps}></i>
            }
        },
        methods: {
            convertElapsed (val) {
                if (val === undefined) {
                    return 0
                } else {
                    return parseInt(val)
                }
            }
        },
        render (h) {
            return (
                <span class='container-type'>
                    {this.icon}
                </span>
            )
        }
    }
</script>

<style lang="scss">
    @import '../../scss/conf';
    .container-type {
        font-size: 12px;
        margin-right: 12px;
        font-style: normal;
        .bk-icon {
            font-size: 18px;
            &.icon-exclamation-triangle-shape {
                font-size: 14px;
                &.is-danger {
                    color: $dangerColor;
                }
            }
        }
        i {
            font-style: normal;
        }
    }
</style>
