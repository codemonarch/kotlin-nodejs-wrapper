@file:Suppress("HasPlatformType", "UnsafeCastFromDynamic")

package com.rarnu.ktnode

const val DEFAULT_PORT = 8888

external fun require(module: String): dynamic
external val process: dynamic
external val __dirname: dynamic

var path = require("path")
val express = require("express")
val exec = require("child_process").exec
val bodyParser = require("body-parser")
val fs = require("fs")
val runPath = "${process.cwd()}"

private var app: dynamic = null

fun initServer(staticPath: String = "") {
    app = express()
    app.use(bodyParser.urlencoded(extended = false))
    app.use(express.static(path.join(__dirname, staticPath)))
}

fun startListen() {
    val port = process.env.PORT ?: DEFAULT_PORT
    app.listen(port) {
        println("Listening on port $port")
    }
}

fun routing(path: String, method: String = "get", block: (req: dynamic, resp: dynamic) -> Unit) =
    when (method.toLowerCase()) {
        "get" -> app.get(path) { req, resp -> block(req, resp) }
        "post" -> app.post(path) { req, resp -> block(req, resp) }
        else -> {
        }
    }

fun execute(cmd: String, callback: (stdout: String, stderr: String) -> Unit) =
    exec(cmd) { _, stdout, stderr -> callback("$stdout", "$stderr") }

fun saveFile(filePath: String, fileContent: String, callback: (succ: Boolean) -> Unit) {
    val path = "$runPath/files/"
    if (!fs.existsSync(path)) {
        fs.mkdirSync(path)
    }
    val fpath = "$path$filePath"
    if (fs.existsSync(fpath)) {
        fs.unlinkSync(fpath)
    }
    println(fpath)
    fs.writeFile(fpath, fileContent) { err -> callback(err == null) }
}

fun loadFile(filePath: String, callback: (content: String) -> Unit) =
    fs.readFile("$runPath/files/$filePath", "utf8") { _, data -> callback("${data ?: ""}") }

fun loadRes(filePath: String, callback: (content: String) -> Unit) =
    fs.readFile("$runPath/$filePath", "utf8") { _, data -> callback("${data ?: ""}") }


private fun s4() = js("(((1+Math.random())*0x10000)|0).toString(16).substring(1)")
fun uuid() = "${s4()}${s4()}-${s4()}-${s4()}-${s4()}-${s4()}${s4()}${s4()}"
