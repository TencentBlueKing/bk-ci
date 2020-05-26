import ansiParse from './assets/ansiParse'
let dataPort
let tempListData = []
let curId
let allRepeatLineNum = {}
let allListLength = {}
let allMainWidth = {}
let allMainWordNum = {}

const colorList = [
    { key: '##[command]', color: 'rgba(146,166,202,1)' },
    { key: '##[info]', color: 'rgba(127,202,84,1)' },
    { key: '##[warning]', color: 'rgba(246,222,84,1)' },
    { key: '##[error]', color: 'rgba(247,49,49,1)' },
    { key: '##[debug]', color: 'rgba(99,176,106,1)' }
]

const reg = (() => {
    const colors = colorList.map((color) => {
        let key = color.key
        key = key.replace(/\[|\]/gi, '\\$&')
        return `(${key})`
    })
    return new RegExp(`${colors.join('|')}`, 'gi')
})()

function handleColor (val) {
    const parseRes = ansiParse(val) || [{ message: '' }]
    const res = { message: '' }
    parseRes.forEach((item) => {
        res.message += item.message
        if (!res.color && item.color) res.color = item.color
    })
    
    const currentColor = colorList.find(color => String(val).startsWith(color.key))
    if (currentColor) {
        res.color = currentColor.color
        res.message = String(res.message).replace(reg, '')
    }
    if (res.color) res.fontWeight = 600
    return res
}

onmessage = (e) => {
    const data = e.data
    const type = data.type

    switch (type) {
        case 'init':
            initWorker(data)
            break
        case 'resetData':
            resetData()
            break
    }
}

function resetData () {
    allRepeatLineNum = {}
    allListLength = {}
    allMainWidth = {}
    allMainWordNum = {}
}

function initWorker (data) {
    dataPort = data.dataPort

    dataPort.onmessage = (e) => {
        const data = e.data
        const type = data.type
        curId = data.curId

        switch (type) {
            case 'addListData':
                addListData(data)
                break
        }
    }
}

function addListData ({ list, mainWidth }) {
    allMainWidth[curId] = mainWidth - 90
    allMainWordNum[curId] = Math.floor(allMainWidth[curId] / 6.8)
    tempListData = []
    if (allListLength[curId] === undefined) {
        allListLength[curId] = 0
        allRepeatLineNum[curId] = -1
    }

    list.forEach((item) => {
        const newItemArr = (item.message || '').split(/\r\n|\n/)
        newItemArr.forEach((val) => {
            const { message, color } = handleColor(val || '')
            const splitTextArr = splitText(message)
            splitTextArr.forEach((message, i) => {
                const currentIndex = allListLength[curId]
                const newItem = {
                    message,
                    color,
                    isNewLine: i > 0 ? (allRepeatLineNum[curId]++, true) : false,
                    showIndex: currentIndex - allRepeatLineNum[curId],
                    realIndex: currentIndex,
                    timestamp: item.timestamp
                }

                tempListData.push(newItem)
                allListLength[curId]++
                if (tempListData.length > 20000) {
                    dataPort.postMessage({ type: 'complateHandleData', list: tempListData.splice(0, 20000), curId })
                }
            })
        })
    })
    dataPort.postMessage({ type: 'complateHandleData', list: tempListData, curId })
}

function splitText (message) {
    let tempMes = ''
    const totalWidth = getTextWidth(message)
    const mesRes = []
    if (totalWidth < allMainWidth[curId]) {
        mesRes.push(message)
    } else {
        const regex = /<a[^>]+?href=["']?([^"']+)["']?[^>]*>([^<]+)<\/a>/gi
        const aList = []
        let tempA = null
        let currentIndex = 0

        while ((tempA = regex.exec(message)) != null) {
            aList.push({
                content: tempA[0],
                href: tempA[1],
                text: tempA[2],
                startIndex: tempA.index
            })
        }
        if (aList.length) message = message.replace(regex, '$2')

        while (message !== '') {
            [tempMes, message] = splitByChar(message)
            // a标签单独处理
            aList.forEach((x) => {
                if (x.startIndex <= currentIndex + tempMes.length && x.startIndex >= currentIndex) {
                    const curStartIndex = x.startIndex - currentIndex
                    const curLength = x.text.length
                    const diffDis = curStartIndex + curLength - tempMes.length
                    if (diffDis > 0) {
                        message = tempMes.slice(curStartIndex) + message
                        tempMes = tempMes.slice(0, curStartIndex)
                    } else {
                        tempMes = (tempMes.slice(0, curStartIndex) + x.content + tempMes.slice(curStartIndex + curLength))
                    }
                }
            })

            currentIndex += tempMes.length
            mesRes.push(tempMes)
        }
    }
    return mesRes
}

function splitByChar (message) {
    let tempMes = message.slice(0, allMainWordNum[curId])
    message = message.slice(allMainWordNum[curId])
    let tempWidth = getTextWidth(tempMes)
    while (tempWidth > allMainWidth[curId] || (allMainWidth[curId] - tempWidth > 15 && message !== '')) {
        if (tempWidth > allMainWidth[curId]) {
            message = tempMes.slice(-1) + message
            tempMes = tempMes.slice(0, -1)
            tempWidth = getTextWidth(tempMes)
        } else {
            tempMes += message.slice(0, 1)
            message = message.slice(1)
            tempWidth = getTextWidth(tempMes)
        }
    }
    return [tempMes, message]
}

let getTextWidth
if (self.OffscreenCanvas) {
    const canvas = new OffscreenCanvas(100, 1)
    const context = canvas.getContext('2d')
    context.font = 'normal 12px Consolas, "Courier New", monospace'
    getTextWidth = (text) => {
        const metrics = context.measureText(text)
        return metrics.width
    }
} else {
    // 兼容safari
    getTextWidth = (text) => {
        let res = 0
        let disNum = 0
        for (let i = 0, len = text.length; i < len; i++) {
            if (/[\u4e00-\u9fa5]/.test(text[i])) {
                res += 12
            } else {
                res += 8
                disNum++
            }
        }
        res -= (disNum - 1)
        return res + 20
    }
}
