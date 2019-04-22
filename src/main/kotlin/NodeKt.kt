@file:Suppress("HasPlatformType", "UnsafeCastFromDynamic", "ObjectPropertyName", "unused")

package com.rarnu.ktnode

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

fun startListen(port: Int) {
    val pt = process.env.PORT ?: port
    app.listen(pt) {
        println("Listening on port $pt")
    }
}

fun routing(path: String, method: String = "get", block: (req: Request, resp: Response) -> Unit) =
    when (method.toLowerCase()) {
        "get" -> app.get(path) { req, resp -> block(Request(req), Response(resp)) }
        "post" -> app.post(path) { req, resp -> block(Request(req), Response(resp)) }
        else -> {
        }
    }

fun routingArrayFile(
    path: String,
    fileField: String = "file",
    arraySize: Int = 0,
    block: (req: Request, file: List<File>, resp: Response) -> Unit
) = app.post(path, upload.array(fileField, arraySize)) { req, resp ->
    block(
        Request(req),
        File.listOf(req.files),
        Response(resp)
    )
}


fun routingSingleFile(
    path: String,
    fileField: String = "file",
    block: (req: Request, file: File, resp: Response) -> Unit
) = app.post(path, upload.single(fileField)) { req, resp -> block(Request(req), File(req.file), Response(resp)) }


fun execute(cmd: String, callback: (stdout: String, stderr: String) -> Unit) =
    exec("\"$cmd\"", js("({timeout: 3000})")) { _, stdout, stderr -> callback("$stdout", "$stderr") }

fun saveFile(filePath: String, fileContent: String, callback: (succ: Boolean) -> Unit) {
    deleteFile(filePath)
    fs.writeFile("$runPath/$filePath", fileContent) { err -> callback(err == null) }
}

fun deleteFile(filePath: String) {
    if (fileExists(filePath)) {
        fs.unlinkSync("$runPath/$filePath")
    }
}

fun fileExists(filePath: String): Boolean = fs.existsSync("$runPath/$filePath")

fun loadFile(filePath: String, callback: (content: String) -> Unit) =
    fs.readFile("$runPath/$filePath", "utf8") { _, data -> callback("${data ?: ""}") }

fun loadRes(filePath: String, callback: (content: String) -> Unit) =
    fs.readFile("$runPath/$filePath", "utf8") { _, data -> callback("${data ?: ""}") }

private fun s4() = js("(((1+Math.random())*0x10000)|0).toString(16).substring(1)")
fun uuid() = "${s4()}${s4()}-${s4()}-${s4()}-${s4()}-${s4()}${s4()}${s4()}"

class Request(private val base: dynamic) {
    fun get(name: String): String = base.get(name)
    fun header(name: String): String = base.header(name)
    val httpVersion: String get() = base.httpVersion
    val headers: dynamic get() = base.headers
    val url: String get() = base.url
    val method: String get() = base.method
    val statusCode: Int
        get() = try {
            base.statusCode
        } catch (e: Throwable) {
            500
        }
    val statusMessage: String
        get() = try {
            base.statusMessage
        } catch (e: Throwable) {
            ""
        }
    val baseUrl: String get() = base.baseUrl
    val originalUrl: String get() = base.originalUrl
    val params: dynamic get() = base.params
    val query: dynamic get() = base.query
    val body: dynamic get() = base.body
    val route: dynamic get() = base.route
    val protocol: String get() = base.protocol
    val ip: String
        get() = try {
            base.ip
        } catch (e: Throwable) {
            ""
        }
    val path: String
        get() = try {
            base.path
        } catch (e: Throwable) {
            ""
        }
    val hostname: String
        get() = try {
            base.hostname
        } catch (e: Throwable) {
            ""
        }
    val host: String get() = base.host
}

class Response(private val base: dynamic) {
    var shouldKeepAlive: Boolean
        get() = base.shouldKeepAlive
        set(value) {
            base.shouldKeepAlive = value
        }
    var sendDate: Boolean
        get() = base.sendDate
        set(value) {
            base.sendDate = value
        }

    fun status(code: Int) = base.status(code)
    fun send(body: dynamic) = base.send(body)
    fun json(obj: dynamic) = base.json(obj)
    fun jsonp(obj: dynamic) = base.jsonp(obj)
    fun sendStatus(code: Int) = base.sendStatus(code)
    fun sendFile(path: String) = base.sendFile(path)
    fun download(path: String, filename: String) = base.download(path, filename)
    fun type(contentType: String) = base.type(contentType)
    fun attachment(filename: String) = base.attachment(filename)
    fun append(field: String, value: String) = base.append(field, value)
    fun get(name: String): String = base.get(name)
    fun set(name: String, value: String) = base.set(name, value)
    fun clearCookie(name: String) = base.clearCookie(name)
    fun cookie(name: String, value: String) = base.cookie(name, value)
    fun location(url: String) = base.location(url)
    fun redirect(url: String) = base.redirect(url)
    var statusCode: Int
        get() = base.statusCode
        set(value) {
            base.statusCode = value
        }
    var statusMessage: String
        get() = base.statusMessage
        set(value) {
            base.statusMessage = value
        }

    fun write(content: dynamic) = base.write(content)
    fun end(data: dynamic = null) = base.end(data)
}

class File(private val base: dynamic) {
    val fieldname: String get() = base.fieldname
    val originalname: String get() = base.originalname
    val encoding: String get() = base.encoding
    val mimetype: String get() = base.mimetype
    val destination: String get() = base.destination
    val filename: String get() = base.filename
    val path: String get() = base.path
    val size: Long get() = base.size

    companion object
}

fun File.Companion.listOf(filelist: dynamic): List<File> {
    val list = mutableListOf<File>()
    filelist.forEach { f -> list.add(File(f)) }
    return list
}
