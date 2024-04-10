function ansiparse (str) {
    let matchingControl = null
    let matchingData = null
    let matchingText = ''
    let ansiState = []
    const result = []
    let state = {}

    const eraseChar = function () {
        let index
        let message
        if (matchingText.length) {
            matchingText = matchingText.substr(0, matchingText.length - 1)
        } else if (result.length) {
            index = result.length - 1
            message = result[index].message
            if (message.length === 1) result.pop()
            else result[index].message = message.substr(0, message.length - 1)
        }
    }

    for (let i = 0; i < str.length; i++) {
        if (matchingControl !== null) {
            if (matchingControl === '\u001b' && str[i] === '\[') {
                if (matchingText) {
                    state.message = matchingText
                    result.push(state)
                    state = {}
                    matchingText = ''
                }

                matchingControl = null
                matchingData = ''
            } else {
                matchingText += matchingControl + str[i]
                matchingControl = null
            }
            continue
        } else if (matchingData !== null) {
            if (str[i] === ';') {
                ansiState.push(matchingData)
                matchingData = ''
            } else if (str[i] === 'm') {
                ansiState.push(matchingData)
                matchingData = null
                matchingText = ''
                ansiState.forEach(function (ansiCode) {
                    if (ansiparse.foregroundColors[ansiCode]) {
                        state.color = ansiparse.foregroundColors[ansiCode]
                    } else if (ansiparse.backgroundColors[ansiCode]) {
                        state.backgroundColor = ansiparse.backgroundColors[ansiCode]
                    } else if (ansiCode === 39) {
                        delete state.color
                    } else if (ansiCode === 49) {
                        delete state.backgroundColor
                    } else if (ansiparse.styles[ansiCode]) {
                        state[ansiparse.styles[ansiCode]] = true
                    } else if (ansiCode === 22) {
                        state.bold = false
                    } else if (ansiCode === 23) {
                        state.italic = false
                    } else if (ansiCode === 24) {
                        state.underline = false
                    }
                })
                ansiState = []
            } else {
                matchingData += str[i]
            }
            continue
        }

        if (str[i] === '\u001b') {
            matchingControl = str[i]
        } else if (str[i] === '\u0008') {
            eraseChar()
        } else {
            matchingText += str[i]
        }
    }

    if (matchingText) {
        state.message = matchingText + (matchingControl || '')
        result.push(state)
    }
    return result
}

ansiparse.foregroundColors = {
    30: 'rgba(0,0,0,1)',
    31: 'rgba(247,49,49,1)',
    32: 'rgba(127,202,84,1)',
    33: 'rgba(246,222,84,1)',
    34: 'rgba(0,0,255,1)',
    35: 'rgba(255,0,255,1)',
    36: 'rgba(0,255,255,1)',
    37: 'rgba(255,255,255,1)',
    90: 'rgba(128,128,128,1)'
}

ansiparse.backgroundColors = {
    40: 'rgba(0,0,0,1)',
    41: 'rgba(247,49,49,1)',
    42: 'rgba(127,202,84,1)',
    43: 'rgba(246,222,84,1)',
    44: 'rgba(0,0,255,1)',
    45: 'rgba(255,0,255,1)',
    46: 'rgba(0,255,255,1)',
    47: 'rgba(255,255,255,1)'
}

ansiparse.styles = {
    1: 'bold',
    3: 'italic',
    4: 'underline'
}

export default ansiparse
