# Redis

非关系型数据库NoSQL（Not Only SQL）的一种 ，内存存储，并且不记录关系，是关系型数据库的一种补充，MongoDB、HBase都是NoSQL， MongoDB适合记录文档信息，图片信息则适合用分布式系统文件如FastDFS集群存储，搜索关键字适合用ES、solr、Lucene等存储。

## 1 启动

### 1.1 默认启动

```shell
redis-server
```

前台启动，会阻塞整个会话，窗口关闭或 Ctrl + C 可以停止服务，不推荐使用



### 1.2 后台启动

需要修改redis.conf文件：

```properties
# 监听的地址，默认是127.0.0.1，会导致只能本地访问，修改为0.0.0.0则可以在任意IP访问，生产环境不要设置为0.0.0.0
bind 0.0.0.0
# 守护进程，修改为yes后可后台运行
daemonize yes
# 密码，设置后访问redis必须输入密码
requirepass 123456
```

其他常见配置：

```properties
# 监听端口
port 6379
# 工作目录，默认是当前目录，也就是运行redis-server时的目录，日志、持久化等文件会保存在这个目录
dir .
# 数据库数量，设置为1，代表只使用1个库，默认有16个库，编号0~15
databases 1
# 设置redis能够使用的最大内存
maxmemory 512mb
# 日志文件，默认为空，不记录日志，可以指定日志文件名
logfile "redis.log"
```

启动时指定配置文件

```shell
redis-server redis.conf
```



## 2 Redis-cli基本操作

使用方式如下：

```shell
redis-cli [options] [commands]
```

常见的options有：

* `-h 127.0.0.1`：指定要连接的redis节点的地址，默认是127.0.0.1
* `-p 6379`：指定要连接的redis节点的端口，默认是6379
* `-a password`：指定redis的访问密码

其中的commands就是redis的操作命令，如：

* `ping`：与redis服务端做心跳测试，服务端正常会返回`pong`

不指定command时，会进入redis-cli的交互客户端



### 2.1 信息添加

* 功能：设置key，value数据

* 命令 

```Redis
set key value
eg: set name test
```

### 2.2 信息查询

* 功能：根据key查询对应的value，如果不存在，则返回空（nil）

* 命令

```redis
get key
eg: get name
```

### 2.3 清屏

* 功能：清除屏幕中的信息

* 命令

```redis
clear
```

### 2.4 退出

```
quit
exit
<ESC>
```



## 3 Redis数据结构及使用方式

5种基本类型：String、Hash、List、Set、SortedSet

此外还包含多种特殊类型



### 3.1 通用命令

通过`help @generic`可以查看所有通用命令

通过`help [command]`可以查看一个命令的具体用法。



* KEYS：查看符合模板的所有key，<font color="red">不建议在生产环境使用</font>，因为redis是单线程的，keys命令搜索时会阻塞住线程，若为主从模式可以在从节点进行查询

```shell
KEYS *name*
KEYS a??
KEYS *
```

* DEL：删除一个指定的key

```shell
DEL key
DEL k1 k2 k3 k4
```

* EXISTS：判断key是否存在，存在返回1，不存在返回0

* EXPIRE：给一个key设置有效期，有效期到期时该key会被自动删除，单位是秒

* TTL：查看一个KEY的剩余有效期



### 3.2 key的结构

redis的key允许有多个单词形成的层级结构，多个单词之间用':'隔开，例如：

```
项目名:业务名:类型:id
test:user:1
projct:product:1
```



### 3.3 String类型

String类型，也就是字符串类型，是redis中最简单的存储类型。

其中value是字符串，不过根据字符串的格式不同，又可以分为3类：

* String：普通字符串
* int：整数类型，可以做自增、自减操作
* float：浮点类型，可以做自增、自减操作

不管是哪种格式，底层都是字节数组形式存储，只不过编码方式不同。

<font color="red">字符串类型的最大空间不能超过512m</font>。

| KEY   | VALUE       |
| ----- | ----------- |
| msg   | hello world |
| num   | 10          |
| score | 92.5        |



#### 常见命令

* SET：添加或者修改已经存在的一个String类型的键值对
* GET：根据key获取String类型的value
* MSET：批量添加多个String类型的键值对 `MSET k1 v1 k2 v2`
* MGET：根据多个key获取多个String类型的value `MGET k1 k2`
* INCR：让一个整型的key自增1
* INCRBY：让一个整型的key自增并指定步长，例如：incrby num 2，让num值自增2
* INCRYBYFLOAT：让一个浮点类型的数字自增并指定步长
* SETNX：添加一个String类型的键值对，前提是这个key不存在，否则不执行
* SETEX：添加一个String类型的键值对，并指定有效期



### 3.4 Hash类型

Hash类型，也叫散列类型，其value是一个无序字典，类似java中的HashMap

| Key         | value |       |
| ----------- | ----- | ----- |
|             | field | value |
| test:user:1 | name  | jack  |
|             | age   | 26    |
| test:user:2 | name  | rose  |
|             | age   | 22    |

