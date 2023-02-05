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

<img src="image\image-20230205204251740.png" alt="image-20230205204251740" style="zoom:67%;" />

#### 常见命令：

* HSET key field value：添加或者修改hash类型key的field的值
* HGET key field：获取一个hash类型key的field的值
* HMSET：批量添加多个hash类型key的field的值
* HMGET：批量获取多个hash类型key的field的值
* HGETALL：获取一个hash类型的key中所有的field和value
* HKEYS：获取一个hash类型的key中的所有field
* HVALS：获取一个hash类型的key中的所有value
* HINCRBY：让一个hash类型key的字段值自增并指定步长
* HSETNX：添加一个hash类型的key的field，前提是这个field不存在，否则不执行



### 3.5 List类型

Redis中的List类型与java中的LinkedList类似，可以看做是一个双向链表结构。既可以支持正向检索也可以支持反向检索。

特征也与LinkedList类似：

* 有序
* 元素可以重复
* 插入和删除快
* 查询速度一般

常用来存储一个有序数据，例如：朋友圈点赞列表，评论列表等。



#### 常见命令：

* LPUSH key element ...：向列表左侧插入一个或多个元素
* LPOP key：移除并返回列表左侧的第一个元素，没有则返回nil
* RPUSH key element...：向列表右侧插入一个或多个元素
* RPOP key：移除并返回列表右侧的第一个元素
* LRANGE key star end：返回一段角标范围内的所有元素
* BLPOP和BRPOP：与LPOP和RPOP类似，只不过在没有元素时等待指定时间，而不是直接返回nil



### 3.6 Set类型

Redis的Set结构与Java中的HashSet类似，可以看做是一个value为null的HashMap（HashSet的value实际是同一个object对象）。因为也是一个hash表，因此具备与HashSet类似的特征：

* 无序
* 元素不可重复
* 查找快
* 支持交集、并集、差集等功能



#### 常见命令：

* SADD key member ...：向set中添加一个或多个元素
* SREM key member ...：移除set中的指定元素
* SCARD key：返回set中元素的个数
* SISMEMBER key member：判断一个元素是否存在于set中
* SMEMBERS：获取set中的所有元素

* SINTER key1 key2 ...：求key1与key2的交集

* SDIFFER key1 key2 ...：求key1与key2的差集
* SUNION key1 key2 ...：求key1和key2的并集

<img src="image\image-20230205222405619.png" alt="image-20230205222405619" style="zoom:50%;" />



### SortedSet类型

Redis的SortedSet是一个可排序的set集合，与java中的TreeSet有些类似，但底层数据结构却差别很大。SortedSet中的每一个元素都带有一个score属性，可以基于sorce属性对元素排序，底层的实现是一个跳表（SkipList）加hash表。

SortedSet具备以下特性：

* 可排序
* 元素不重复
* 查询速度快

因为Sorted的可排序性，经常被用来实现排行榜这样的功能。



#### 常见命令

* ZADD key score member：添加一个或多个元素到sorted set，如果已经存在则更新其score值

* ZREM key member：删除sorted set中的一个指定元素
* ZSCORE key member：获取sorted set中的指定元素的score值
* ZRANK key member：获取sorted set中的指定元素的排名
* ZCARD key：获取sorted set中的元素个数
* ZCOUNT key min max：统计score值在给定范围内的所有元素的个数
* ZINCRBY key increment member：让sorted set中的指定元素自增，步长为指定的increment值
* ZRANGE key min max：按照score排序后，获取指定排名范围内的元素
* ZRANGEBYSCORE key min max：按照score排序后，获取指定score范围内的元素
* ZDIFF、ZINTER、ZUNION：求差集、交集、并集

