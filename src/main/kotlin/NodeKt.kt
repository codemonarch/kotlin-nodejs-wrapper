@file:Suppress("HasPlatformType", "UnsafeCastFromDynamic", "ObjectPropertyName", "unused")

package com.rarnu.ktnode

// import from nodejs
external fun require(module: String): dynamic

external val process: dynamic
external val __dirname: dynamic
external val __filename: dynamic
external val Buffer: dynamic

private var path = require("path")
private val express = require("express")
private val exec = require("child_process").exec
private val bodyParser = require("body-parser")
private val fs = require("fs")
private val multer = require("multer")
private val upload = multer(optionOf("dest" to "upload/"))
private val sendSeekable = require("send-seekable")
private val session = require("express-session")
private val req = require("request")
private val mysqldb = require("mysql")
private val MongoClient = require("mongodb").MongoClient
private val nunjucks = require("nunjucks")
private val crypto = require("crypto")

var errorHandler: ErrorHandler? = null
var mysql: Mysql? = null
var mongo: MongoDB? = null

val runPath = "${process.cwd()}"
// app instance
val app = App(express())

fun initServer(staticPath: String = "") {
    process.on("uncaughtException") { err -> errorHandler?.handleException(err) }
    process.on("unhandledRejection") { err, promise -> errorHandler?.handleRejection(err, promise) }
    app.use(bodyParser.urlencoded(optionOf("extended" to false)))
    app.use(express.static(path.join(__dirname, staticPath)))
    app.use(sendSeekable)
    app.set("trust proxy", 1)
    app.use(
        session(
            optionOf(
                "secret" to "keyboard cat",
                "resave" to true,
                "saveUninitialized" to false,
                "cookie" to optionOf("maxAge" to 60000)
            )
        )
    )
    nunjucks.configure("", optionOf("autoescape" to true))
}

fun startListen(port: Int) {
    val pt = process.env.PORT ?: port
    app.listen(pt) {
        println("Listening on port $pt")
    }
}

// request routing
fun routing(path: String, method: String = "get", block: (req: Request, resp: Response) -> Unit) =
    when (method.toLowerCase()) {
        "get" -> app.get(path) { req, resp -> block(req, resp) }
        "post" -> app.post(path) { req, resp -> block(req, resp) }
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
        req,
        File.listOf(req.files),
        resp
    )
}

fun routingSingleFile(
    path: String,
    fileField: String = "file",
    block: (req: Request, file: File, resp: Response) -> Unit
) = app.post(path, upload.single(fileField)) { req, resp -> block(req, File(req.file), resp) }

// partial content
fun routingSeekable(filePath: String, block: (req: Request, stream: dynamic, resp: Response) -> Unit) =
    app.get(filePath) { req, resp ->
        val file = "$runPath/$filePath"
        val stream = fs.createReadStream(file)
        block(req, stream, resp)
    }

// command
fun execute(cmd: String, timeout: Long = 3000, callback: (stdout: String, stderr: String) -> Unit) =
    exec(cmd, optionOf("timeout" to timeout)) { _, stdout, stderr -> callback("$stdout", "$stderr") }

// file operations
fun moveFile(filePath: String, destPath: String, callback: (succ: Boolean) -> Unit) {
    deleteFile(destPath)
    fs.renameSync("$runPath/$filePath", "$runPath/$destPath") { err -> callback(err == null) }
}

fun saveFile(filePath: String, fileContent: String, callback: (succ: Boolean) -> Unit) {
    deleteFile(filePath)
    fs.writeFile("$runPath/$filePath", fileContent) { err -> callback(err == null) }
}

fun deleteFile(filePath: String) {
    if (fileExists(filePath)) {
        fs.unlinkSync("$runPath/$filePath")
    }
}

fun mkdir(path: String) {
    if (!fileExists(path)) {
        fs.mkdirSync("$runPath/$path")
    }
}

fun getFilePath(filePath: String) = "$runPath/$filePath"

fun fileExists(filePath: String): Boolean = fs.existsSync("$runPath/$filePath")

fun loadFile(filePath: String, callback: (content: String) -> Unit) =
    fs.readFile("$runPath/$filePath", "utf8") { _, data -> callback("${data ?: ""}") }

// 网络请求
fun request(
    url: String,
    method: String = "get",
    query: dynamic = optionOf(),
    formData: dynamic = optionOf(),
    callback: (resp: Response, body: String) -> Unit
) {
    val options = optionOf("url" to url, "method" to method, "qs" to query, "form" to formData)
    req(options) { _, resp, body -> callback(Response(resp), "${body ?: ""}") }
}

// database
fun mysqlConnect(
    database: String,
    host: String = "127.0.0.1",
    port: Int = 3306,
    user: String = "root",
    password: String = "root"
): Boolean {
    var ret = false
    val options =
        optionOf("host" to host, "port" to port, "user" to user, "password" to password, "database" to database)
    val conn = mysqldb.createConnection(options)
    if (conn != null) {
        mysql = Mysql(conn)
        mysql?.connect()
        ret = true
    }
    return ret
}

class Mysql(private val base: dynamic) {
    fun connect() = base.connect()
    fun select(sql: String, params: List<Any>, callback: (succ: Boolean, msg: String, result: List<dynamic>) -> Unit) =
        base.query(sql, params.toTypedArray()) { err, result ->
            if (err != null) {
                callback(false, "${err.message}", listOf())
            } else {
                val list = mutableListOf<dynamic>()
                result.forEach { rec -> list.add(rec) }
                callback(true, "", list)
            }
        }

    fun execute(sql: String, params: List<Any>, callback: (succ: Boolean, msg: String) -> Unit) =
        base.query(sql, params.toTypedArray()) { err, _ ->
            if (err != null) {
                callback(false, "${err.message}")
            } else {
                callback(true, "")
            }
        }
}

fun mongoConnect(
    databaseName: String,
    host: String = "127.0.0.1",
    port: Int = 27017,
    callback: (succ: Boolean) -> Unit
) =
    MongoClient.connect("mongodb://$host:$port", optionOf("useNewUrlParser" to true)) { err, db ->
        if (err == null) {
            mongo = MongoDB(db, databaseName)
            callback(true)
        } else {
            callback(false)
        }
    }

class MongoDB(private val base: dynamic, private val databaseName: String) {

    fun select(
        collection: String,
        fields: List<String>? = null,
        whereOption: dynamic = optionOf(),
        callback: (succ: Boolean, result: List<dynamic>) -> Unit
    ) {
        val coll = base.db(databaseName).collection(collection)
        val setOption = if (fields != null) {
            val tmpSet: dynamic = object {}
            fields.forEach { f -> tmpSet[f] = 1 }
            tmpSet
        } else {
            null
        }
        coll.find(whereOption, setOption).toArray { err, result ->
            if (err == null) {
                val list = mutableListOf<dynamic>()
                result.forEach { r -> list.add(r) }
                callback(true, list)
            } else {
                callback(false, listOf())
            }
        }
    }

    fun insert(collection: String, data: List<dynamic>, callback: (succ: Boolean) -> Unit) {
        val coll = base.db(databaseName).collection(collection)
        coll.insert(data.toTypedArray()) { err, result ->
            println("$result")
            callback(err == null)
        }
    }

    fun update(
        collection: String,
        data: dynamic,
        whereOption: dynamic = optionOf(),
        callback: (succ: Boolean) -> Unit
    ) {
        val coll = base.db(databaseName).collection(collection)
        val setOption: dynamic = object {}
        setOption["\$set"] = data
        coll.updateMany(whereOption, setOption) { err, result ->
            println("$result")
            callback(err == null)
        }
    }

    fun delete(collection: String, whereOption: dynamic = optionOf(), callback: (succ: Boolean) -> Unit) {
        val coll = base.db(databaseName).collection(collection)
        coll.remove(whereOption) { err, result ->
            println("$result")
            callback(err == null)
        }
    }
}

fun renderString(str: String, option: dynamic): String = nunjucks.renderString(str, option)
fun renderFile(filePath: String, option: dynamic): String = nunjucks.render(filePath, option)

private fun s4() = js("(((1+Math.random())*0x10000)|0).toString(16).substring(1)")
fun uuid() = "${s4()}${s4()}-${s4()}-${s4()}-${s4()}-${s4()}${s4()}${s4()}"

fun optionOf(vararg pairs: Pair<String, Any>): dynamic {
    val opt: dynamic = object {}
    for ((k, v) in pairs) {
        opt[k] = v
    }
    return opt
}

// class enclosure
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
    val file: dynamic get() = base.file
    val files: dynamic get() = base.files
    val protocol: String get() = base.protocol
    val session: dynamic get() = base.session
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
    fun sendSeekable(buffer: dynamic, options: dynamic) = base.sendSeekable(buffer, options)
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

class FormFile(path: String) {
    private var _parsed: dynamic = null

    init {
        _parsed = fs.createReadStream(path)
    }

    fun file() = _parsed
}

object Crypto {
    fun getHashes(): List<String> {
        val list = crypto.getHashes()
        val ret = mutableListOf<String>()
        list.forEach { h -> ret.add(h) }
        return ret
    }

    fun getCiphers(): List<String> {
        val list = crypto.getCiphers()
        val ret = mutableListOf<String>()
        list.forEach { h -> ret.add(h) }
        return ret
    }

    fun hash(alg: String, str: String): String {
        val ch = crypto.createHash(alg)
        ch.update(str)
        return ch.digest("hex")
    }

    fun cipherEncrypt(alg: String, str: String, key: dynamic, iv: dynamic): String {
        val ci = crypto.createCipheriv(alg, key, iv)
        val enc = ci.update(str, "utf8", "hex")
        enc += ci.final("hex")
        return enc
    }

    fun cipherDecrypt(alg: String, str: String, key: dynamic, iv: dynamic): String {
        val di = crypto.createDecipheriv(alg, key, iv)
        val dec = di.update(str, "hex", "utf8")
        dec += di.final("utf8")
        return dec
    }
}

object Buf {
    fun from(c: dynamic): dynamic = Buffer.from(c)
    fun alloc(len: Int): dynamic = Buffer.alloc(len)
}

abstract class ErrorHandler {
    abstract fun handleException(err: String?)
    abstract fun handleRejection(err: String?, promise: dynamic)
}

// app enclosure
class App(val base: dynamic) {
    fun use(c: dynamic) = base.use(c)

    fun set(key: String, value: dynamic) = base.set(key, value)
    fun listen(port: Int = 8888, callback: () -> Unit) = base.listen(port) { callback() }
    fun get(path: String, callback: (req: Request, resp: Response) -> Unit) =
        base.get(path) { req, resp -> callback(Request(req), Response(resp)) }

    fun post(path: String, callback: (req: Request, resp: Response) -> Unit) =
        base.post(path) { req, resp -> callback(Request(req), Response(resp)) }

    fun post(path: String, option: dynamic, callback: (req: Request, resp: Response) -> Unit) =
        base.post(path, option) { req, resp -> callback(Request(req), Response(resp)) }
}
