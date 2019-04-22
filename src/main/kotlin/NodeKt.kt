@file:Suppress("HasPlatformType", "UnsafeCastFromDynamic", "ObjectPropertyName")

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
val multer = require("multer")
val upload = multer(js("({ dest: 'upload/'})"))

var app: dynamic = null

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

fun routingFile(
    path: String,
    fileField: String = "file",
    isArray: Boolean = false,
    arraySize: Int = 0,
    block: (req: dynamic, file: dynamic, resp: dynamic) -> Unit
) = if (isArray) {
    app.post(path, upload.array(fileField, arraySize)) { req, resp -> block(req, req.files, resp) }
} else {
    app.post(path, upload.single(fileField)) { req, resp -> block(req, req.file, resp) }
}

fun execute(cmd: String, callback: (stdout: String, stderr: String) -> Unit) =
    exec("\"$cmd\"", js("({timeout: 3000})")) { _, stdout, stderr -> callback("$stdout", "$stderr") }

fun saveFile(filePath: String, fileContent: String, callback: (succ: Boolean) -> Unit) {
    val fpath = "$runPath/$filePath"
    if (fs.existsSync(fpath)) {
        fs.unlinkSync(fpath)
    }
    fs.writeFile(fpath, fileContent) { err -> callback(err == null) }
}

fun loadFile(filePath: String, callback: (content: String) -> Unit) =
    fs.readFile("$runPath/$filePath", "utf8") { _, data -> callback("${data ?: ""}") }

fun loadRes(filePath: String, callback: (content: String) -> Unit) =
    fs.readFile("$runPath/$filePath", "utf8") { _, data -> callback("${data ?: ""}") }

private fun s4() = js("(((1+Math.random())*0x10000)|0).toString(16).substring(1)")
fun uuid() = "${s4()}${s4()}-${s4()}-${s4()}-${s4()}-${s4()}${s4()}${s4()}"
