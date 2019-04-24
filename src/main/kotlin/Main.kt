@file:Suppress("UnsafeCastFromDynamic")

package com.rarnu.ktnode
import kotlin.js.json

fun main(args: Array<String>) {
    errorHandler = object : ErrorHandler() {
        override fun handleRejection(err: String?, promise: dynamic) {
            println(err)
        }

        override fun handleException(err: String?) {
            println(err)
        }
    }

    initServer()
    mongoConnect("rarnu") { succ ->
        if (succ) {
            println("mongo => $mongo")
        } else {
            println("mongo => error")
        }
    }
    val key = Buf.from("12345678")
    val iv = Buf.alloc(0)
    val enc = Crypto.cipherEncrypt("des-ecb", "hello", key, iv)
    println(enc)
    val dec = Crypto.cipherDecrypt("des-ecb", enc, key, iv)
    println(dec)

    val opt = optionOf("a" to 1, "b" to 2)
    js("for (var prop in opt) { console.log(prop); }")

    mkdir("files")

    routing("/index") { req, resp ->
        if (req.session.views) {
            req.session.views += 1
        } else {
            req.session.views = 1
        }
        println("view => ${req.session.views}")
        val name = req.query.name
        resp.type("text/html")
        val html = renderFile("index.html", optionOf("username" to name))
        resp.send(html)
    }

    routing("/code", "post") { req, resp ->
        // _ = req.body.language
        val code = req.body.code
        val fpath = "files/${uuid()}"
        saveFile(fpath, code) { succ ->
            if (succ) {
                execute("/usr/local/bin/node ${getFilePath(fpath)}") { stdout, stderr ->
                    resp.type("text/json")
                    resp.send(json("result" to 0, "output" to stdout, "error" to stderr))
                }
            }
        }
    }

    routing("/api") { req, resp ->
        val name = req.query?.name ?: ""
        resp.type("text/json")
        resp.send(json("result" to 0, "message" to "Hello World: $name"))
    }

    routing("/callapi") { _, resp ->
        request("http://localhost:8888/api", query = optionOf("name" to "rarnu")) { _, body ->
            println("body => $body")
            resp.end()
        }
    }

    routingSingleFile("/upload") { _, file, resp ->
        moveFile(file.path, "files/${uuid()}") { succ ->
            println(if (succ) "uploaded" else "failed")
        }
        resp.end("0")
    }

    routing("/sql") { _, resp ->
        mongo?.select("user", listOf("name", "age"), optionOf("name" to "yyy")) { succ, result ->
            if (succ) {
                result.forEach {
                    println("data => ${it.name}, ${it.age}")
                }
            }
            resp.end()
        }
    }

    routing("/insert") { _, resp ->
        mongo?.insert("user", listOf(optionOf("id" to 6, "name" to "rarnu", "age" to 30), optionOf("id" to 7, "name" to "rarnu666", "age" to 35))) { succ ->
            println(if (succ) "insert succ" else "insert failed")
            resp.end()
        }

    }

    routing("/update") { _, resp ->
        mongo?.update("user", optionOf("age" to 12345), optionOf("age" to optionOf("\$eq" to 60))) { succ ->
            println(if (succ) "update succ" else "update failed")
            resp.end()
        }
    }

    routing("/delete") { _, resp ->
        mongo?.delete("user", optionOf("age" to optionOf("\$eq" to 12))) { succ ->
            println(if (succ) "delete succ" else "delete failed")
            resp.end()
        }
    }

    routing("/user/:id") { req, resp ->
        val id = req.params.id
        println("userid => $id")
        resp.end()
    }

    startListen(8888)
}

