const { src, dest, parallel, series, task } = require('gulp')
const svgSprite = require('gulp-svg-sprite')
const rename = require('gulp-rename')
const Ora = require('ora')
const yargs = require('yargs')
const argv = yargs.alias({
    'dist': 'd',
    'env': 'e',
    'lsVersion': 'l',
    'type': 't',
    'scope': 's'
}).default({
    'dist': 'frontend',
    'env': 'master',
    'lsVersion': 'dev',
    'type': 'tencent'
}).describe({
    'dist': 'build output dist directory',
    'env': 'environment [dev, test, master, external]',
    'lsVersion': 'localStorage version',
    'type': 'bkdevops version 【ee | tencent】'
}).argv
const { dist, env, lsVersion, type, scope } = argv


const svgSpriteConfig = {
    mode: {
        symbol: true
    }
};

console.log(scope)
function taskGenerator(type) {
    return () => {
        return src(`./svg-sprites/${type}/*.svg`)
        .pipe(svgSprite(svgSpriteConfig))
        .pipe(dest(`./svg-sprites/dist/${type}`))
    }
}

function renameSvg (type) {
    return () => {
        return src(`./svg-sprites/dist/${type}/symbol/svg/sprite.symbol.svg`)
            .pipe(rename(`${type}_sprite.svg`))
            .pipe(dest(`${dist}/svg-sprites/`))
    }
}

task('devops', series([taskGenerator('devops'), renameSvg('devops')]))
task('pipeline', series([taskGenerator('pipeline'), renameSvg('pipeline')]))
task('copy', () => src(['common-lib/**'], { 'base': '.' }).pipe(dest(`${dist}/`)))
task('build', cb => {
    const spinner = new Ora('building bk-ci frontend project').start()
    require('child_process').exec(`lerna run public:${env} --scope=devops-{${scope}} -- --env.dist=${dist} --env.version=${type} --env.lsVersion=${lsVersion}`, {
        maxBuffer: 5000 * 1024
    }, (err, res) => {
        if (err) {
            console.log(err)
            process.exit(1)
        }
        spinner.succeed(`Finished building bk-ci frontend project`)
        cb()
    })
})
  
exports.default = parallel('devops', 'pipeline', 'copy', 'build')
