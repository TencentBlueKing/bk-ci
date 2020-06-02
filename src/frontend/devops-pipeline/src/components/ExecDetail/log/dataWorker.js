import ansiParse from './ansiParse'
let dataPort
let tempListData = []
let curId
let allRepeatLineNum = {}
let allListLength = {}
let allMainWidth = {}
let allMainWordNum = {}

let getTextWidth
let englishLength = 8
if (self.OffscreenCanvas) {
    const canvas = new OffscreenCanvas(100, 1)
    const context = canvas.getContext('2d')
    context.font = 'normal 12px Consolas, "Courier New", monospace'
    englishLength = context.measureText('a').width
    const wordLength = context.measureText('我').width
    getTextWidth = (text) => {
        const wordNum = text.match(/[\u4e00-\u9fa5]/g) ? text.match(/[\u4e00-\u9fa5]/g).length : 0
        const res = wordNum * wordLength + (text.length - wordNum) * englishLength
        return res + 20
    }
} else {
    // 兼容safari
    getTextWidth = (text) => {
        const wordNum = text.match(/[\u4e00-\u9fa5]/g) ? text.match(/[\u4e00-\u9fa5]/g).length : 0
        const eNum = text.length - wordNum
        const res = wordNum * 12 + eNum * 8 - eNum + 1
        return res + 20
    }
}

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
    }
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
            case 'resetData':
                resetData()
                break
        }
    }
}

function resetData () {
    allRepeatLineNum = {}
    allListLength = {}
    allMainWidth = {}
    allMainWordNum = {}
}

function addListData ({ list, mainWidth }) {
    allMainWidth[curId] = mainWidth - 90
    allMainWordNum[curId] = Math.floor(allMainWidth[curId] / englishLength)
    tempListData = []
    if (allListLength[curId] === undefined) {
        allListLength[curId] = 0
        allRepeatLineNum[curId] = -1
    }

    list.forEach((item) => {
        const newItemArr = (item.message || '').split(/\r\n|\n/)
        newItemArr.forEach((val) => {
            const { message, color } = handleColor(val || '')
            const regex = /<a[^>]+?href=["']?([^"']+)["']?[^>]*>([^<]+)<\/a>/gi
            const aList = []
            let tempA = null
            while ((tempA = regex.exec(message)) != null) {
                aList.push({
                    content: tempA[0],
                    href: tempA[1],
                    text: tempA[2],
                    startIndex: tempA.index
                })
            }
            const msg = aList.length ? message.replace(regex, '$2') : message
            handleMessage(msg, tempListData, aList, 0, color, item.timestamp, 0)
        })
    })
    dataPort.postMessage({ type: 'complateHandleData', list: tempListData.splice(0, tempListData.length), curId })
}

function handleMessage (message, tempListData, aList, currentMsgIndex, color, timestamp, msgIndex) {
    let tempMes = ''
    let time = 0
    do {
        tempMes = splitByChar(message)
        message = message.slice(tempMes.length)
        // a标签单独处理
        aList.forEach((x) => {
            if (x.startIndex <= currentMsgIndex + tempMes.length && x.startIndex >= currentMsgIndex) {
                const curStartIndex = x.startIndex - currentMsgIndex
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

        const currentIndex = allListLength[curId]
        const newItem = {
            message: tempMes,
            color,
            isNewLine: msgIndex > 0 ? (allRepeatLineNum[curId]++, true) : false,
            showIndex: currentIndex - allRepeatLineNum[curId],
            realIndex: currentIndex,
            timestamp
        }

        tempListData.push(newItem)
        allListLength[curId]++
        if (tempListData.length > 20000) {
            dataPort.postMessage({ type: 'complateHandleData', list: tempListData.splice(0, 20000), curId })
        }

        currentMsgIndex += tempMes.length
        time++
        msgIndex++
    } while (time < 10000 && message.length > 0)
    if (message.length > 0) handleMessage(message, tempListData, aList, currentMsgIndex, color, timestamp, msgIndex)
}

function splitByChar (message) {
    let tempMes = message.slice(0, allMainWordNum[curId])
    message = message.slice(allMainWordNum[curId])
    let tempWidth = getTextWidth(tempMes)
    while (tempWidth > allMainWidth[curId] || (allMainWidth[curId] - tempWidth > 15 && message !== '')) {
        if (tempWidth > allMainWidth[curId]) {
            message = `${tempMes.slice(-1)}${message}`
            tempMes = tempMes.slice(0, -1)
            tempWidth = getTextWidth(tempMes)
        } else {
            tempMes += message.slice(0, 1)
            message = message.slice(1)
            tempWidth = getTextWidth(tempMes)
        }
    }
    return tempMes
}
