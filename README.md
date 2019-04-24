# kotlin-nodejs-wrapper

Kotlin x Nodejs 包装器

包装器的目的是方便 Kotlin x Nodejs 的开发，在 Kotlin(Javascript) 内无缝的使用各种 Nodejs 的库。

- - -

**如何开始**

1. 在 Idea 内新建 Kotlin(Javascript) 项目后，按本项目的 ```build.gradle``` 所示，对 gradle 进行配置，建议 gradle 版本为 4.4 以上。

2. 按本项目的 ```package.json``` 所示，对 js 库依赖进行配置，并在配置完成后执行 ```npm install``` 安装这些依赖库。

3. 将 ```NodeKt.kt``` 放到你的项目下

4. 开始进行开发

- - -

**开发步骤**

```kotlin
// main 函数必备，从这里开始
fun main(args: Array<String>) {
    // 初始化服务器
    initServer()

    // 路由
    routing("/hello") { req, resp ->
        resp.end("Hello World")
    }

    // 开始监听端口
    startListen(8888)
}
```

在包装器内，目前已经包装了常用的类和方法，主要列表如下:

**内建方法**
| 功能 | 方法 | 参数 | 返回| 描述 |
| :-- | :-- | :-- | :-- | :-- |
| routing | routing | path[, method] | req, resp | 处理一个请求 |
| | routingSingleFile | path[, fileField] | req, file, resp | 处理带有单个文件上传的请求 |
| | routingArrayFile | path[, fileFied, arraySize] | req, files, resp | 处理带有多个文件的上传请求 |
| | routingSeekable | filePath | req, stream, resp | 处理带有 Partial Content 需求的请求，如视频文件等 |
| command | execute | cmd[, timeout] | stdout, stderr | 执行一个位于服务器的命令，并返回执行结果 |
| file | mkdir | path | | 新建一个目录 |
| | moveFile | filePath, destPath | succ | 移动一个文件 |
| | saveFile | filePath, content | succ | 将 content 保存到文件 |
| | loadFile | filePath | content | 读取一个文件的内容 |
| | deleteFile | filePath | | 删除一个文件 |
| | fileExists | filePath | exists | 判断文件是否存在 |
| | getFilePath | filePath | fullPath | 获取文件在磁盘上的完整路径 |
| request | request | url[, method, query, formData] | resp, body | 由服务器内部发起一个请求 |
| mongodb | mongoConnect | databaseName[, host, port] | succ | 连接到 mongodb |
| mysql | mysqlConnect | database[, host, port, user, password] succ | 连接到 mysql |
| render | renderString | str, option | string | 用模板渲染一个字符串 |
| | renderFile | filePath, option | string | 用模板渲染一个文件 |
| utils | uuid | | uuid | 生成一个 uuid |
| | optionOf | pairs ... | dynamic | 生成一个 js 的 option 对象 |

**内建类**
| 类 | 成员 | 数据类型 | 描述 |
| :-- | :-- | :-- | :--|
| Request | get(name) | string | 获取 header 内容 |
| | header(name) | string | 获取 header 内容 |
| | httpVersion | string | |
| | headers | dynamic | header 列表 |
| | url | string | |
| | method | string | |
| | statusCode | int | |
| | statusMessage | string | |
| | baseUrl | string | |
| | originalUrl | string | |
| | params | dynamic | restful 参数 |
| | query | dynamic | query 参数 |
| | body | dynamic | post 参数 |
| | route | dynamic | |
| | file | File | 单一文件上传时的文件参数 |
| | files | List&lt;File&gt; | 多文件上传时的文件参数 |
| | protocol | string | |
| | session | dynamic | 用户会话 |
| | ip | string | |
| | path | string | |
| | hostname | string | |
| | host | string | |
| Response | shouldKeepAlive | boolean | 是否保持连接 |
| | sendDate | boolean | 是否发送日期 |
| | status(code) | | 设置 statusCode |
| | send(body) | | 发送响应内容 |
| | json(obj) | | |
| | jsonp(obj) | | |
| | sendStatus(code) | | 发送 statusCode |
| | sendFile(path) | | 从文件发送响应内容 |
| | sendSeekable(buffer[, options]) | | 发送允许 Partial Content 的内容 |
| | download(path, filename) | | 发送下载内容 |
| | type(contentType) | | 设置 Content-Type |
| | attachment(filename) | | |
| | append(field, value) | | 追加一个 header |
| | get(name) | string | 获取 header 内容 |
| | set(name, value) | | 设置 header 内容 |
| | clearCookie(name) | | 删除指定的 cookie |
| | cookie(name, value) | | 设置指定的 cookie |
| | location(url) | | |
| | redirect(url) | | |
| | statusCode | int | 获取或设置 statusCode |
| | statusMessage | string | 获取或设置 statusMessage |
| | write(content) | | 写入响应数据 |
| | end([data]) | | 结束响应 |
| File | fieldname | string | 请求时传入的字段名 |
| | originalname | string | 请求时传入的原始文件名 |
| | encoding | string | 文件的编码 |
| | mimetype | string | |
| | destination | string | 临时目录，不含文件名 |
| | filename | string | 临时文件名，不含目录 |
| | path | string | 临时文件完整路径，含目录和文件名 |
| | size | long | 文件大小，单位为 byte |
| FormFile | file | dynamic | 用于内部发起请求上传文件的文件对象 |
| Buf | from(c) | dynamic | 通过内容创建一个缓冲 |
| | alloc(len) | dynamic | 通过大小创建一个缓冲 |
| Crypto | getHashes | List&lt;string&gt; | |
| | getCiphers | List&lt;string&gt; | |
| | hash(alg, str) | string | 使用 hash 算法加密 |
| | cipherEncrypt(alg, str, key, iv) | string | |
| | cipherDecrypt(alg, str, key, iv) | string | |
| ErrorHandler | handleException(err) | | |
| | handleRejection(err, promise) | | |
| App | use(c) | | |
| | set(key, value) | | |
| | listen([port]) | completed | |
| | get(path) | req, resp | |
| | post(path) | req, resp | |
| | post(path, option) | req, resp | |
| MongoDB | select(collection, fields, whereOption) | succ, result | 查询数据 |
| | insert(collection, data) | succ | 插入数据 |
| | update(collection, data, whereOption) | succ | 更新数据 |
| | delete(collection, whereOption) | succ | 删除数据 |
| Mysql | select(sql, params) | succ, msg, result | 查询数据 |
| | execute(sql, params) | succ, msg | 执行除查询外的操作 |

**内建实例**

| 名称 | 实例 | 描述 |
| :-- | :-- | :-- |
| app | App | app对象，在程序启动时即可以访问 |
| runPath | string | 程序运行的目录，在程序启动时即可访问 |
| mongo | MongoDB | mongodb 的实例，```mongoConnect()``` 执行成功时可以访问 |
| mysql | Mysql | mysql 的实例，```mysqlConnect()``` 执行成功时可以访问 |
| errorHandler | ErrorHandler | 在程序启动时继承，不继承的情况下程序不做全局异常处理 |

