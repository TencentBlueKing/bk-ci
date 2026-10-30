<template>
    <span class="container-type">
        <span v-if="!containerType.showIcon" v-bk-tooltips="containerType.tooltip">
            {{ containerType.content }}
        </span>
        <Logo v-else v-bk-tooltips="containerType.tooltip" v-bind="containerType.iconProps">{{ containerType.content }}
        </Logo>
    </span>
</template>
<script>

import Logo from './Logo';
import { localeMixins } from './locale';
import {
    convertMStoString,
    formatRangeTime,
    isNormalContainer,
    isTriggerContainer,
    isVmContainer,
} from "./util";

export default {
    name: 'container-type',
    components: {
        Logo,
    },
    mixins: [localeMixins],
    inject: ["reactiveData"],
    props: {
        container: Object,
    },
    computed: {
        containerType() {
            const { container } = this
            const { vmNames = [], baseOS = "", elements = [] } = container
            let iconProps = {}
            let content = ""
            let tooltip = {
                disabled: true,
            }
            let showIcon = true
            switch (true) {
                case container.timeCost !== undefined: {
                    const { totalCost, executeCost, systemCost } = container.timeCost
                    const timeRange = formatRangeTime(container.startTime, container.endTime, this.reactiveData?.buildStartTime)
                    const runningLabel = container.endTime ? "" : ` ${this.t("started")}`
                    const costStr = `${this.t("userTime")}：${convertMStoString(
                        executeCost
                    )} + ${this.t("systemTime")}： ${convertMStoString(systemCost)}`
                    tooltip = {
                        delay: [300, 0],
                        allowHtml: true,
                        content: timeRange
                            ? `<div class="container-time-range-tips">${this.t("startEndTime")}：${timeRange}${runningLabel}<br>${costStr}</div>`
                            : costStr,
                    }
                    content = convertMStoString(totalCost)
                    showIcon = false
                    break
                }
                case container.isError:
                    iconProps = {
                        name: "exclamation-triangle-shape",
                        class: "is-danger",
                    }
                    break
                case isVmContainer(container):
                    iconProps = {
                        name: baseOS.toLowerCase() || 'WINDOWS',
                        title: vmNames.join(","),
                    }
                    break
                case isNormalContainer(container):
                    iconProps = {
                        name: "none",
                    }
                    break
                case isTriggerContainer(container):
                    content = `${elements.length} ${this.t("item")}`
                    showIcon = false
                    break
            }
            return {
                iconProps,
                tooltip,
                content,
                showIcon,
            }
        },
    },
}
</script>

<style lang="scss">
.is-danger {
    color: #ff5656 !important;
}

.container-time-range-tips {
    line-height: 20px;
}
</style>
