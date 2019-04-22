@file:Suppress("UnsafeCastFromDynamic")

package com.rarnu.ktnode
import kotlin.js.json

fun main(args: Array<String>) {
    initServer()

    routing("/index") { _, resp ->
        resp.type("text/html")
        loadRes("index.html") {
            println(it)
            resp.send(it)
        }
    }

    routing("/code", "post") { req, resp ->
        println("uuid => " + uuid())
        val c = req.body.c
        val d = req.body.d
        resp.type("text/json")
        resp.send(json("c" to c, "d" to d))
    }

    routing("/api") { req, resp ->
        val name = req.query?.name ?: ""
        resp.type("text/json")
        resp.send(json("result" to 0, "message" to "Hello World: $name"))
    }

    routingFile("/upload") { _, file, resp ->
        loadFile(file.path) { c ->
            saveFile("files/${uuid()}", c) { succ ->
                println(if (succ) "uploaded" else "failed")
            }
        }
        resp.end()
    }

    startListen()
}

