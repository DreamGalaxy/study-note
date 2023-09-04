# Redis基础

非关系型数据库NoSQL（Not Only SQL）的一种 ，内存存储，并且不记录关系，是关系型数据库的一种补充，MongoDB、HBase都是NoSQL， MongoDB适合记录文档信息，图片信息则适合用分布式系统文件如FastDFS集群存储，搜索关键字适合用ES、solr、Lucene等存储。

## 1、启动

### 1.1、默认启动

```shell
redis-server
```

前台启动，会阻塞整个会话，窗口关闭或 Ctrl + C 可以停止服务，不推荐使用



### 1.2、后台启动

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



## 2、Redis-cli基本操作

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



### 2.1、信息添加

* 功能：设置key，value数据

* 命令 

```Redis
set key value
eg: set name test
```

### 2.2、信息查询

* 功能：根据key查询对应的value，如果不存在，则返回空（nil）

* 命令

```redis
get key
eg: get name
```

### 2.3、清屏

* 功能：清除屏幕中的信息

* 命令

```redis
clear
```

### 2.4、退出

```
quit
exit
<ESC>
```



### 2.5、 查询内存使用

```shell
info memory
```



## 3、Redis数据结构及使用方式

5种基本类型：String、Hash、List、Set、SortedSet

此外还包含多种特殊类型



### 3.1、通用命令

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



### 3.2、key的结构

redis的key允许有多个单词形成的层级结构，多个单词之间用':'隔开，例如：

```
项目名:业务名:类型:id
test:user:1
projct:product:1
```



### 3.3、String类型

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



### 3.4、Hash类型

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



### 3.5、List类型

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



### 3.6、Set类型

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



### 3.7、SortedSet类型

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



## 4、数据结构的选择

在使用redis存储对象时，有使用String存储序列化为json字符串的方式，也有使用hash将对象中的每个字段独立存储两种方式；后者可以针对单个字段CRUD，并且内存占用更小。



## 5、缓存更新策略

|          | 内存淘汰                                                     | 超时剔除                                                     | 主动更新                                     |
| -------- | ------------------------------------------------------------ | ------------------------------------------------------------ | -------------------------------------------- |
| 说明     | 不用自己维护，利用redis的内存淘汰机制，当内存不足时自动淘汰部分数据。下次查询时更新缓存。 | 给缓存数据添加TTL时间，到期后自动删除缓存。下次查询时更新缓存。 | 编写业务逻辑，在修改数据库的同时，更新缓存。 |
| 一致性   | 差                                                           | 一般                                                         | 好                                           |
| 维护成本 | 无                                                           | 低                                                           | 高                                           |

业务场景：

* 低一致性需求：使用内存淘汰机制。例如店铺类型的查询缓存
* 高一致性需求：主动更新，并以超时剔除作为兜底方案。例如店铺详情查询的缓存



### 5.1、主动更新策略

#### 1. cache aside pattern（推荐）

​	由缓存的调用者，在更新数据库的同时更新缓存

#### 2. read/write through pattern

​	缓存与数据库整合为一个服务，由服务来维持一致性。调用者调用该服务，无需关心缓存一致性问题

#### 3. write behind caching pattern

​	调用者只操作缓存，由其他线程异步地将缓存数据持久化数据库，保证最终一致性



### 5.2、最佳实践方案

1. 低一致性需求：使用redis自带的内存淘汰机制
2. 高一致性需求：主动更新，并以超时剔除作为兜底方案
   * 读操作：
     * 缓存命中则直接返回
     * 缓存未命中则查询数据库，并写入缓存，设定超时时间
   * 写操作：
     * 先写数据库，然后删除缓存
     * 要确保数据库与缓存操作的原子性



### 5.3、操作缓存和数据库需要考虑的问题：

1. 删除还是更新缓存：
   * 更新缓存：每次更新数据库都更新缓存，**无效写操作较多**
   * 删除缓存：更新数据库时让缓存失效，**查询时再更新缓存**
2. 如何保证缓存与数据库的操作同时成功或失败：
   * 单体系统：将缓存和数据库操作放在同一个事务里
   * 分布式系统：利用TCC等分布式事务方案
3. 先操作缓存还是先操作数据库：
   * 先删缓存，再操作数据库
   * 先操作数据库，再删除缓存（推荐）



线程安全问题：

* 先删缓存，**不一致情况出现的概率较大**，因为查数据库操作比写数据库操作要快：

<img src="image\image-20230224163510526.png" alt="image-20230224163510526" style="zoom: 50%;" />

* 先操作数据库，**不一致情况出现概率较低**，因为查数据库操作比写数据库操作要快：

<img src="image\image-20230224164140562.png" alt="image-20230224164140562" style="zoom: 50%;" />





## 6、缓存穿透

**缓存穿透**是指客户端请求的数据在缓存和数据库中都不存在，这样缓存永远不会生效，这些请求都会打到数据库。比如用户随意编写不存在的id进行网络攻击。



常见的解决方案有两种：

* 缓存空对象，并设置一个过期时间
  * 优点：实现简单，维护方便
  * 缺点：
    * 额外的内存消耗
    * 可能造成短期的不一致

* 布隆过滤器
  * 优点：内存占用小，没有多余的key
  * 缺点：
    * 实现复杂
    * 存在误判的可能



前两种属于被动解决，主动的解决方式还有：

* 增加id生成规则的复杂度，避免被猜测出规律，并做好相关的校验
* 加强用户权限校验
* 做好热点参数限流



## 7、缓存雪崩

**缓存雪崩**是指在同一时间段大量的缓存key同时失效或者redis服务宕机，导致大量的请求到达数据库，带来巨大的压力。

**解决方案：**

* 给不同的key的TTL添加随机值
* 利用Redis集群提高服务的可用性
* 给缓存业务添加降级限流策略
* 给业务添加多级缓存



秒杀活动刚开始时redis内都没有数据，都会打到数据库上，可以提前将数据导入到redis避免雪崩



## 8、缓存击穿

**缓存击穿**问题也叫**热点key问题**，就是一个**被高并发访问**并且**缓存重建较复杂**的key突然失效了，无数的请求访问会在瞬间给数据库带来巨大的冲击。



常见的解决方案有两种：

* 互斥锁

  缺点是若重建缓存较复杂，将会有大量线程阻塞等待

<img src="image\image-20230227164337759.png" alt="image-20230227164337759" style="zoom:50%;" />



* 逻辑过期

  在存储的数据中增加时间戳相关的信息，由业务去判断是否过期，而缓存在redis中长期存在

<img src="image\image-20230227173457253.png" alt="image-20230227173457253" style="zoom:50%;" />



比较

| 解决方案     | 优点                                         | 缺点                                       |
| ------------ | -------------------------------------------- | ------------------------------------------ |
| **互斥锁**   | 没有额外的内存消耗<br>保证一致性<br>实现简单 | 线程需要等待，性能受影响<br>可能有死锁风险 |
| **逻辑过期** | 线程无需等待，性能较好                       | 不保证一致性<br>有额外内存消耗<br>实现复杂 |



## 9、秒杀业务

### 9.1、全局id

全局id生成器，是一种在分布式系统下用来生成全局唯一ID的工具，一般要满足下列特性：

* 唯一性
* 高可用
* 递增性
* 高性能
* 安全性

​	

全局id生成策略：

* UUID
* Redis自增
* snowflake算法

为了增加ID的安全性，不可以直接使用Redis自增的数值，而是要拼接一些其他信息：



### 9.2、实现秒杀功能

下单时要判断两点：

* 秒杀是否开始或结束，如果尚未开始或已结束则无法下单
* 库存是否充足，不足则无法下单



#### 超卖问题

超卖问题是典型的多线程安全问题，针对这一问题常见的解决方案就是加锁：

* 乐观锁：认为线程安全的问题不一定会发生，因此不加锁，只是在更新数据时去判断有没有其他线程对数据做了修改
  * 如果没有修改则认为是安全的，自己才更新数据
  * 如果已被其他线程修改说明发生了安全问题，此时可以重试或异常

判断数据是否被修改过有两种方式：

* 版本号法
* CAS法（例如在秒杀这个业务中用库存来当版本号，因为该业务不担心发生ABA问题）



* 悲观锁：认为线程安全问题一定会发生，因此在数据操作之前先获取锁，确保线程串行执行
  * 例如Synchronized、Lock都是悲观锁



#### 注意事项：

在单机情况下使用synchronized + @Transactional的时候要注意：

synchronized 锁字符串（例如用户id、交易码等）时，要通过intern()获取常量池中的对象，否则锁的是个新new出来的String对象没有意义。

如果是在@Transactional的方法内部调用synchronized，实际是会先释放锁再去提交事务的，此时仍可能发生线程安全问题，所以需要将sychronized套在事务的方法外层。



### 9.3、分布式锁

**分布式锁：**满足在分布式系统或集群模式下多进程可见并且互斥的锁。



分布式锁的实现

|        | MySQL                     | Redis                    | Zookeeper                        |
| ------ | ------------------------- | ------------------------ | -------------------------------- |
| 互斥   | 利用mysql本身的互斥锁机制 | 利用setnex这样的互斥命令 | 利用节点的唯一性和有序性实现互斥 |
| 高可用 | 好                        | 好                       | 好                               |
| 高性能 | 一般                      | 好                       | 一般                             |
| 安全性 | 断开连接，自动释放锁      | 利用锁超时时间，到期释放 | 临时结点，断开连接自动释放       |



#### 基于Redis的分布式锁

获取分布式锁需要实现两个基本方法：

* 获取锁：

  * 互斥：确保只能有一个线程获取锁

  * 阻塞：获取到锁后返回，未获取到时循环等待

  * 非阻塞：尝试一次，成功返回true，失败返回false

    ```shell
    # 添加锁，NX是互斥，EX是超时时间
    SET lock thread1 NX EX 10
    ```

* 释放锁：

  * 手动释放

  * 超时释放：获取锁时添加一个超时时间

    ```shell
    # 释放锁，删除即可
    DEL lock
    ```



简易的java实现方式（<font color="red">在释放锁时因为查询和删除操作是两步，所以在极端情况下仍会删错</font>）

```java
@Slf4j
public class SimpleRedisLock {

    private final String name;
    private final StringRedisTemplate redisTemplate;
    private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID().toString().replaceAll("-", "") + "-";

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        // 可以在启动时加载lua脚本，避免频繁的io操作耗时
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        // ClassPathResource可以在springboot的resources目录下寻找
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        // 设置返回值类型
        UNLOCK_SCRIPT.setResultType(Long.class);
    }
    
    public SimpleRedisLock(String name, StringRedisTemplate redisTemplate) {
        this.name = name;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 非阻塞地获取锁
     *
     * @param timeoutSec 锁的超时时间
     * @return 释放获取到锁
     */
    public boolean tryLock(long timeoutSec) {
        // 获取线程标识作为value，避免线程错误释放其他线程的锁，增加UUID作为前缀是为了避免服务间出现相同的线程id
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        // 获取锁
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);
        // 避免返回的是null自动拆箱后出现空指针异常
        return Boolean.TRUE.equals(success);
    }

    /**
     * 查询和删除分两步有问题版本的删除锁
     */
    public void unlock() {
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        // 释放锁的时候要检查是不是自己的线程创建的锁，避免误删其他线程创建的锁
        if ((threadId).equals(redisTemplate.opsForValue().get(KEY_PREFIX + name))) {
            // 判断和释放锁其实是两步，不是原子操作，所以在极端条件下，可能会出现判断完因为gc等情况锁正好超时过期且被重新申请的情况，
            redisTemplate.delete(KEY_PREFIX + name);
        }
    }
    
    /**
     * 使用lua脚本进行原子操作的优化版本删除锁
     */
    public void unlockByLua() {
        // 调用lua脚本
        redisTemplate.execute(UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX + name),
                ID_PREFIX + Thread.currentThread().getId());
    }
}
```

使用

```
SimpleRedisLock lock = new SimpleRedisLock("order:", new StringRedisTemplate());
try {
    if (!lock.tryLock(60)) {
        log.info("获取锁失败");
    }
} finally {
    lock.unlock();
}
```



在线程1因为gc等情况阻塞的时候，锁超时释放，其他线程科能会抢占导致错误删除

![image-20230309155316643](image\image-20230309155316643.png)



要解决多条命令原子性的问题，需要使用redis的lua脚本

Redis提供了Lua脚本功能，在一个脚本中编写多条Redis命令，确保多条命令执行时的原子性。Lua是一种编程语言。



分布式锁查询+删除的参考的Lua

```lua
-- 这里的KEY[1] 就是锁的key，ARGV[1]就是当前线程标识
-- 获取锁中的标识，判断与当前线程标识是否一致
if (redis.call('GET', KEY[1]) == ARGV[1]) then
    -- 一致则删除所锁
    return redis.call('DEL', KEY[1])
end
-- 不一致则直接返回
return 0
```



### 9.4、Redisson

#### 基于Redis分布式锁的优化

存在的问题：

* 不可重入：同一个线程无法多次获取同一把锁
* 不可重试：获取锁只尝试一次就返回false，没有重试机制

* 超时释放：锁超时释放虽然可以避免死锁，但如果是业务执行耗时较长，也会导致锁释放，存在安全隐患
* 主从一致性：Redis提供了主从集群，主从同步存在延迟，当主节点宕机时，如果从节点没有同步主中的数据，则会出现锁失效



Redisson是一个在Redis基础上实现的java驻内存数据网格（In-Memory Data Grid）。它不仅提供了一系列分布式的Java常用对象，还提供了许多分布式服务，其中就包含了各种分布式锁的实现：

* 分布式锁（Lock）和同步器（Synchronized）
  * 可重入锁（Reentrant Lock）
  * 公平锁（Fair Lock）
  * 联锁（MultiLock）（多个可重入锁组成，获取时必须要同时获取到所有节点的锁才可算获取成功）
  * 红锁（RedLock）（类似联锁，只需要成功获取一半以上节点就算获取成功）
  * 读写锁（ReadWriteLock）
  * 信号量（Semaphore）
  * 可过期性信号量（PermitExpirableSemaphore）
  * 闭锁（CountDownLatch）



#### Redisson可重入锁原理

**可重入**核心是<font color="red">**利用hash结构，同时存储线程标识和重入次数**</font>

<img src="image\image-20230313172122808.png" alt="image-20230313172122808" style="zoom:50%;" />



大致lua脚本如下（细节有差异，比如释放删除锁后会publish一个事件通知其他订阅抢锁的线程）：

* 获取锁

```lua
local key = KEYS[1];  -- 锁的KEY
local threadId = ARGV[1];  -- 线程唯一标识
local releaseTime = ARGV[2]; -- 锁的自动释放时间
-- 判断是否存在，这里没有用hexist
if(redis.call('exist',key) == 0) then
    -- 不存在，获取锁
    redis.call('hset', key, threadId, '1');
    -- 设置有效期
    redis.call('expire', key, releaseTime);
    -- 返回结果
    return 1;
end;
-- 锁已经存在，判断threadId是否是自己的
if(redis.call('hexist', key, threadId) == 1) then
    -- 不存在，获取锁，重入次数+1
    redis.call('hincrby', key, threadId, '1');
    -- 设置有效期
    redis.call('expire', key, releaseTime);
    -- 返回结果
    return 1;
end;
-- 代码走到这里说明锁不是自己的，获取锁失败
return 0;
```

* 释放锁

```lua
local key = KEYS[1];  -- 锁的KEY
local threadId = ARGV[1];  -- 线程唯一标识
local releaseTime = ARGV[2]; -- 锁的自动释放时间
-- 判断当前锁是否被自己持有
if(redis.call('hexist',key, threadId) == 0) then
    return nil;
end;
-- 是自己的锁，则重入次数-1
local count = redis.call('hincrby', key ,threadId, -1);
-- 判断是否重入次数已经为0
if(count > 0) then
    -- 大于0说明不能释放锁，重置有效期然后返回
    redis.call('expire', key, releaseTime);
    return nil;
else
    -- 等于0说明可以释放锁，直接删除
    redis.call('del', key);
    return nil;
end;
```



**可重试**是<font color="red">**利用信号量和PubSub功能实现等待、唤醒、获取锁失败的重试机制**</font>



#### Redisson的细节

在抢锁失败的时候，会先计算抢锁花费时间是否超过了等待时间，若超过了会返回，未超过会使用剩余的等待时间去订阅锁的释放事件，若超时了会取消订阅直接返回。后续的剩余时间会循环订阅信号量，直到获取到锁或超时。并且等待使用的是异步的Future方式，对cpu更加友好。



所有锁的实例都会管理在RedissonLock的类的一个ConcurrentHashMap中



获取锁成功后且未设置超时时间的续约：会启动一个延时任务**watchDog**，启动延迟为 (锁超时时间 / 3)，默认的锁超时时间为30s，延时任务的内容则是重置锁的超时时间，执行完成后，会**递归调用创建延时任务的方法**。

释放锁时，会从map中取出锁，并取消锁续约的定时任务，并将其从map中移除

<img src="image\image-20230314153538862.png" alt="image-20230314153538862" style="zoom: 50%;" />





#### Redisson分布式锁主从一致性问题

redis主从模式同步存在延迟，所以锁可能会失效，解决方法是不使用主从模式，而是多个独立redis节点（内部可以是主从），获取锁时必须要要向每一个redis节点获取到锁才认为是获取到锁（联锁MultiLock），也可以使用红锁RedLock

![image-20230314160120559](image\image-20230314160120559.png)

代码中需要配置多个redis客户端

```java
RLock lock1 = redissonClient1.getLock("order");
RLock lock2 = redissonClient2.getLock("order");
RLock lock3 = redissonClient3.getLock("order");

// 创建联锁，使用任意一个客户端创建即可
RLock lock = redissionClient1.getMultiLock(lock1, lock2, lock3);
```



联锁中的细节：

如果同时设置了等待超时时间和锁超时时间，在申请锁时使用的锁超时时间会是等待超时时间*2，避免锁超时时间过短，在依次申请锁的中途就超时了，最后成功获取后会将超时时间重置为设置的锁超时时间，这样也能使各个锁的剩余时间相差不大



获取到所有节点的锁才算成功，如果获取失败且设置了重试时间，会释放掉成功获取的锁，重新开始获取（避免死锁）



### 9.5、基于Redis的消息队列（只是种实现，不推荐使用）

#### 9.5.1 基于List数据结构

通过Redis的List结构（双向链表），结合BLPUSH和BRPOP或者BRPUSH和BLPOP，可以实现阻塞消息队列

这种实现方式相较于JVM内的阻塞队列，优缺点如下：

优点：

* 利用Redis存储，不受限于JVM内存上限
* 基于Redis的持久化机制，数据安全性有保证
* 可以满足消息有序性

缺点：

* 无法避免消息丢失
* 只支持单消费者



#### 9.5.2 基于PubSub的消息队列

PubSub（发布订阅）是Redis2.0版本引入的消息传递模型。

消费者可以订阅一个或多个channel，生产者向对应channel发送消息后，所有的订阅者都能收到相关消息。

* SUBCRIBE channel [channel]：订阅一个或多个频道
* PUBLISH channel msg：向一个频道发送消息
* PSUBSCRIBE pattern [patter]：订阅与pattern格式所匹配的所有频道

优点：

* 采用发布订阅模型，支持多生产者，多消费者

缺点：

* 只有发布时订阅的服务会收到消息，后订阅的无法收到

* 不支持数据持久化
* <font color="red">无法避免消息丢失</font>
* 消息堆积有上限，超出时数据丢失



#### 9.5.3 基于Stream的消息队列

Stream是Redis5.0引入的一种新数据类型，可以实现一个功能非常完善的消息队列

```sh
XADD key [NOMKSTREAM] [MAXLEN|MINID [=|~] threshold [LIMIT count]] *|ID field value [field value ...]
```

![image-20230316172404707](image\image-20230316172404707.png)



读取消息的方式之一：XREAD

```shell
XREAD [COUNT count] [BLOCK milliseconds] [STREAMS key [key ...]] ID [ID ...]
```

![image-20230316172503502](image\image-20230316172503502.png)

注意：当使用$指定获取最新消息时，如果在处理一条消息的过程中，又有超过1条以上的消息到达消息队列，则下次也只能获取到最新的1条，会出现<font color="red">漏读消息</font>的问题。



特点：

STREAM类型消息队列的XREAD命令特点：

* 消息可回溯
* 一个消息可以被多个消费者读取
* 可以阻塞读取
* 有消息漏读的风险



#### 9.5.4 基于Stream的消息队列-消费者组

消费者组（Consumer Group）：将多个消费者划分到一个组中，监听同一个队列。具备以下特点：

* 消息分流：队列中的消息会分流给组内的不同消费者，从而加快消息处理的速度

* 消息标识：消费者会维护一个标识，记录最后一个被处理的消息，哪怕消费者宕机，还会从标识之后读取消息。确保每一个消息都会被消费。

* 消息确认：消费者获取到消息后，消息出于一个pending状态，并存入一个pending-list。当处理完成需要通过XACK来确认消息，标记消息为已处理，才会从pending-list移除。



消费者组相关命令

```shell
XGROUP CREATE key groupname ID [MKSTREAM]
```

* key：队列名称
* groupName：消费者组名称
* ID：起始ID标识，$代表队列中的最后一个消息，0代表队列中的第一个消息
* MKSTREAM：队列不存在时自动创建队列

其他常见命令：

```shell
# 删除指定消费者组
XGROUP DESTORY key groupname

# 给指定消费者组添加消费者
XGROUP CREATECONSUMER key groupname consumername

# 删除消费者组中的指定消费者
XGROUP DELCONSUMER key groupname consumername
```



从消费者组读取消息：

```shell
XREADGROUP GROUP group consumer [COUNT count] [BLOCK milliseconds] [NOACK] STREAMS key [key ...] ID [ID ...]
```

* group：消费者组名称
* consumer：消费者名称，如果消费者不存在，会自动创建一个消费者
* count：本次查询的最大数量
* BLOCK milliseconds：当没有消息时的最长等待时间
* NOACK：无需手动ACK，获取到消息后自动确认
* STREAMS key：指定队列名称
* ID：获取消息的起始ID：
  * ">"：从下一个未消费的消息开始
  * 其他：根据指定id从pending-list中获取已消费但未确认的消息，例如0，是从pending-list中的第一个消息开始



STRAM类型消息队列的XREADGROUP命令特点：

* 消息可回溯
* 可以多消费者争抢消息，加快消费速度
* 可以阻塞读取
* 没有消息漏读的风险
* 有消息确认机制，保证消息至少被消费一次



## 10、用户点赞

### 10.1、普通点赞

因为对于同一篇博客，一个用户最多点赞1次，同时为了在集合中区分用户，**可以使用Set数据结构**：以该博客的id+部分字符串为key，用户id的集合为value。

点赞前通过`SISMEMBER`查询用户是否在集合中（是否点过赞），未点过`SADD`通过添加用户id标记点赞，点过则通过`SREM`移除用户id取消点赞。



### 10.2、点赞同时记录点赞顺序

对于类似朋友圈点赞记录顺序的情况，**可以使用SortedSet数据结构**，以时间戳为score，其余和普通点赞相同。

点赞前通过`ZSCORE`查询用户是否在集合中（是否点过赞），未点过`ZADD`通过添加用户id标记点赞，点过则通过`ZREM `移除用户id取消点赞。

如果要查询点赞的前n位用户，可以通过`ZRANGE key 0 n`查询出对应的用户id列表。



## 11、好友关注

### 11.1、共同关注

可以从数据库中分别查出两人的关注列表，存入set集合中（也可以从业务层面直接将关注列表存在redis的set集合中），再通过`SINTER`求集合的交集。



### 11.2、关注推送

关注推送也叫做Feed流，直译为投喂，为用户提供“沉浸式”的体验，通过无限下拉刷新获取新的信息。

Feed流常见的两种模式：

* Timeline：不做内容筛选，简单的按照内容发布时间排序，常用于好友或关注。例如朋友圈
  * 优点：信息全面，不会有确实。并且实现也相对简单
  * 缺点：信息噪音较多，用户不一定感兴趣，内容获取效率低
* 智能排序：利用智能算法屏蔽掉违规的、用户不感兴趣的内容。推送用户感兴趣信息来吸引用户
  * 优点：投喂用户感兴趣信息，用户粘度很高，容易沉迷
  * 缺点：如果算法不精准，可能起到反作用



Timeline的实现方案有三种：

* 拉模式：由用户主动去获取

<img src="image\image-20230412113250960.png" alt="image-20230412113250960" style="zoom: 50%;" />

* 推模式：当内容发布后主动推送给用户

<img src="image\image-20230412113511882.png" alt="image-20230412113511882" style="zoom:50%;" />

* 推拉结合：兼具推拉两种模式的优点

<img src="image\image-20230412113731652.png" alt="image-20230412113731652" style="zoom: 50%;" />

## 11、用户签到表

**可以使用BitMap数据结构**来存储用户每个月的签到记录，每1位代表一天的签到情况

Redis中是利用String类型数据结构实现BitMap的，因此最大上限是512MB，转换为bit则是2^32个bit位。

BitMap基本操作：

SETBIT：向指定位置（offset）存入一个0或1

GETBIT：获取指定位置（offset）的bit值

BITCOUNT：统计BitMap中值为1的bit位的数量

BITFIELD：操作（查询、修改、自增）BitMap中bit数组中的指定位置（offset）的值

BITFIELD_RO：获取BitMap中bit数组，并以十进制形式返回

BITOP：将多个BitMap的结果做位运算（与、或、异或）

BITPOS：查找bit数组中指定范围内第一个0或1出现的位置



## 12、页面UV统计

UV：全称Unique Visitor，也叫独立访客量，是指通过互联网访问、浏览这个网页的自然人。1天内一个用户多次访问该网站，只记录1次。



如果网站访问量极大，将用户信息缓存至redis，内存一定是不够用的，**可以使用HyperLogLog数据结构**

HyperLogLog（HLL）是从LogLog算法派生的概率算法，用于确定非常大的集合的基数，而不需要存储其所有值。相关算法原理可以参考：https://juejin.cn/post/6844903785744056333



Redis中HLL是基于String结构实现的，单个HLL的内存永远小于16KB，内存占用极低，但测量结果存在小于0.81%的误差，不过对于极大访问量的UV统计来说，这个误差可以忽略。

HLL基本操作：

PFADD：添加统计对象，例如用户id

PFCOUNT：获得近似的统计基数

PFMERGE：合并多个不同的HLL结果为一个



# 分布式缓存-Redis集群

单点Redis的问题及分布式解决方案：

* 数据丢失问题：实现Redis数据持久化
* 并发能力问题：搭建主从集群，实现读写分离
* 故障恢复问题：搭建分片集群，利用插槽机制实现动态扩容
* 存储能力问题：利用Redis哨兵，实现健康检查和自动恢复



## 1、Redis持久化

### 1.1、RDB

RDB全称Redis Database Backup file（Redis数据备份文件），也被叫做Redis数据快照。简单来说就是把内存中的所有数据都记录到磁盘中。当Redis实现故障重启后，从磁盘读取快照文件，恢复数据。

快照文件称为RDB文件，默认是保存在当前运行目录。

手动持久化命令：

```shell
redis-cli
save #由Redis的主进程来执行RDB，会阻塞所有命令
bgsave #开启子进程执行RDB，避免主进程受到影响
```

Redis正常停机时会执行一次RDB



#### 1.1.1 配置

Redis内部有触发RDB的机制，可以在redis.conf文件中找到，格式如下：

```shell
# 900秒内，如果有至少1个key被修改，则执行bgsave，如果是save ""则表示禁用RDB
save 900 1
save 300 10
save 60 10000
```

RDB的其他配置也可以在redis.conf中设置：

```shell
# 是否压缩，建议不开启，压缩也会消耗cpu，cpu资源紧张时不推荐压缩
rdbcompression yes

# RDB文件名称
dbfilename dump.rdb

# 文件保存的路径目录
dir ./
```



#### 1.1.2 bgsave原理

bgsave开始时会fork主进程得到子进程，子进程共享主进程的内存数据。完成fork后读取内存数据并写入RDB文件。

fork采用的是copy-on-write技术：

* 当主进程执行读操作时，访问共享内存；
* 当主进程执行写操作时，则会拷贝一份数据，执行写操作。
  * 优点是可以使子进程不需要阻塞主进程拷贝页表即可快速开始持久化
  * 缺点是如果拷贝过程中大量的数据被替换，则会占用大量额外内存，当数据全部被替换时则占用双倍

![image-20230404173714722](image\image-20230404173714722.png)



#### 1.1.3 缺点

RDB执行间隔时间长，两次RDB之间写入数据有丢失的风险

fork子进程、压缩、写出RDB文件都比较耗时



### 1.2、AOF

AOF全称为Append Only File（追加文件）。Redis处理的每一个写命令都会记录在AOF文件，可以看做是命令日志文件。

#### 1.2.1 配置

AOF默认是关闭的，需要修改redis.conf配置文件来开启AOF，同时注意通过save ""关掉RDB

```shell
# 是否开启AOF功能，默认是no
appendonly yes
# AOF文件的名称
appendfilename "appendonly.aof"
```

AOF命令记录的频率也可以通过redis.conf文件配置:

```shell
# 表示每执行一次写命令，立即记录到AOF文件
appendfsync always
# 写命令执行完先放入AOF缓冲区，然后表示每隔1秒将缓冲区数据写入到AOF文件，是默认方案
appendfsync everysec
# 写命令执行完先放入AOF缓冲区，由操作系统决定何时将缓冲区内容写回磁盘
appendfsync no
```

比较

| 配置项   | 刷盘时机     | 优点                   | 缺点                         |
| -------- | ------------ | ---------------------- | ---------------------------- |
| always   | 同步刷盘     | 可靠性高，几乎不丢数据 | 性能影响大                   |
| everysec | 每秒刷盘     | 性能适中               | 最多丢失1秒数据              |
| no       | 操作系统控制 | 性能良好               | 可靠性差，可能会丢失大量数据 |



#### 1.2.2 AOF的压缩

因为是记录命令，AOF文件会比RDB文件大得多。而且AOF会记录对同一个key的多次写操作，但只有最后一次写操作才有意义。通过执行bgrewriteaof命令，可以让AOF文件执行重写功能，用最少的命令达到相同的效果。



redis也会在达到阈值的时候自动去重写AOF文件。阈值也可以在redis.conf中配置：

```shell
# AOF文件比上次文件增长超过多少百分比则触发重写
auto-aof-rewrite-percentage 100
# AOF文件体积最小多大以上才触发重写
auto-aof-rewrite-min-size 64mb
```



### 1.3 RDB与AOF对比

 RDB和AOF各有自己的优缺点，如果对数据安全性要求较高，在实际开发中往往会<font color="red">**结合**</font>两者使用。

|                | RDB                                          | AOF                                                        |
| -------------- | -------------------------------------------- | ---------------------------------------------------------- |
| 持久化方式     | 定时对整个内存做快照                         | 记录每一次执行的命令                                       |
| 数据完整性     | 不完整，两次备份之间会丢失                   | 相对完整，取决于刷盘策略                                   |
| 文件大小       | 会有压缩，文件体积小                         | 记录命令，文件体积很大                                     |
| 宕机恢复速度   | 很快                                         | 慢                                                         |
| 数据恢复优先级 | 低，因为数据完整性不如AOF高                  | 高，因为数据完整性更高                                     |
| 资源占用       | 高，大量CPU和内存消耗                        | 低，主要是磁盘IO资源，但是AOF重写时会占用大量CPU和内存资源 |
| 使用场景       | 可以容忍数分钟的数据丢失，追求更快的启动速度 | 对数据安全性要求较高常见                                   |



## 2、Redis主从

单节点Redis的并发能力是有上限的，要进一步提高Redis的并发能力，就需要搭建主从集群，实现读写分离。



### 2.1、配置

配置有临时和永久两种模式：

* 修改配置文件（永久生效）
  * 在redis.conf中添加一行配置：salveof \<masterip> \<masterport>
* 使用redis-cli客户端<font color="red">连接到要做从节点的redis服务</font>，执行salveof命令（重启后失效）：

```shell
salveof <masterip> <masterport>
```

<font color="red">注意</font>：在5.0以后新增命令replicaof，与salveof效果一致



### 2.2、数据同步原理

主从第一次同步是<font color="red">全量同步</font>，但如果salve重启后同步，则执行<font color="red">增量同步</font>

<img src="image\image-20230509151351355.png" alt="image-20230509151351355" style="zoom:50%;" />



用于同步的概念

* Replication Id：简称replid，是数据集的标记，id一致则说明是同一数据集。每一个master都有唯一replid，salve则会继承master节点的replid
* offset：偏移量，随着记录在repl_baklog中的数据增多而逐渐增大。salve完成同步时也会记录当前同步的offset。如果salve的offset小于master的offset，说明salve数据落后于master，需要更新

salve做数据同步，必须向master声明自己的replication id和offset，master才能判断到底需要同步哪些数据



replication id不一致则全量，后续根据offset同步增量

<img src="image\image-20230509154651904.png" alt="image-20230509154651904" style="zoom:50%;" />

repl_baklog大小有上限，写满后会覆盖最早的数据。如果salve断开时间过久，导致尚未备份的数据被覆盖，则无法基于log做增量同步，只能再次全量同步。



### 2.3、数据同步优化

* 在master中配置repl-dickless-sync yes 启用无磁盘复制，避免全量同步时的磁盘IO。（适用于磁盘较慢但网络较快的场景）
* Redis单节点上的内存占用不要太大，减少RDB导致的过多磁盘IO。
* 适当提高repl_backlog的大小，发现salve宕机时尽快实现故障恢复，尽可能避免全量同步
* 限制一个master上的salve节点数量，如果有太多salve，可以采用主-从-从链式结构，减少master压力



## 3、Redis哨兵

### 3.1、哨兵的作用和原理

Redis提供了哨兵（Sentinel）机制来实现主从集群的自动故障恢复。哨兵的结构和作用如下：

* **监控：**Sentinel会不断地检查master和salve是否按预期工作
* **自动故障恢复：**如果master故障，Sentinel会将一个slave提升为master。当故障实例恢复后也以新的master为主
* **通知：**Sentinel充当Redis客户端的服务发现来源，当集群发生故障转移时，会将最新信息推送给Redis的客户端

<img src="image\image-20230531113851056.png" alt="image-20230531113851056" style="zoom:50%;" />



#### 服务状态监控

Sentinel基于心跳机制监测服务状态，每隔1秒向集群的每个实例发送ping命令：

* 主观下线：如果某sentinel节点发现某实例未在规定时间响应，则应认为该实例**主观下线**
* 客观下线：若超过指定数量（quorum）的sentinel都认为该实例主观下线，则该实例**客观下线**。quorum值最好超过sentinel实例数量的一半



#### 选举新的master

一旦发现master故障，sentinel需要在salve中选择一个作为新的master，选择依据是这样的：

* 首先会判断slave节点与master节点断开时间长短，如果超过指定值（down-after-milliseconds * 10）则会排除该slave节点
* 然后判断slave节点的slave-priority，值越小优先级越高，如果是0则永不参与选举
* 如果salve-priority一样，则判断slave节点的offset值，越大说明数据越新，优先级越高
* 最后是判断slave节点的运行id大小，越小优先级越高



#### 实现故障转移

当选了其中一个slave为新的master后，故障的转移步骤如下：

* sentinel给备选的slave节点发送slave of no one命令，让该节点成为master
* sentinel给所有其他slave发送`salveof ip 端口`命令，让这些slave成为新的master的从节点，开始从新的master上同步数据
* 最后，sentinel将故障节点标记为slave，当故障节点恢复后会自动成为新的master的slave节点



### 3.2、RedisTemplate的哨兵模式

配置主从读写分离

```java
@Bean
public LettuceClientConfigurationBuilderCustomizer configurationBuilderCustomizer(){
    return configBuilder -> configBuilder.readFrom(ReadFrom.REPLICA_PREFERRED);
}
```

ReadFrom是配置Redis的读取策略，是一个枚举，包括下面的选项：

* MASTER：从主节点读取
* MASTER_PREFERRED：优先从主节点读取，master不可用才读取replica
* REPLICA：从slave（replica）节点读取
* REPLICA_PREFERRED：优先从slave（replica）节点读取，所有的slave都不用才读取master



当master节点故障时，lettuce会自动订阅到最新的master节点



## 4、Redis分片集群

主从和哨兵可以解决高可用、高并发读的问题。但是依然有两个问题没有解决：

* 海量数据存储问题
* 高并发写的问题

使用分片集群可以解决上述问题，分片集群特征：

* 集群中有多个master，每个master保存不同数据
* 每个master都可以有多个slave节点
* master之间通过ping互相检测彼此健康状态
* 客户端请求可以访问集群任意节点，最终都会被转发到正确节点





<img src="image\image-20230531153020064.png" alt="image-20230531153020064" style="zoom:50%;" />



### 4.1、散列插槽

Redis会把每一个master节点映射到0~16383共16384个插槽（hash slot）上，查看集群信息时就能看到

数据key不是与节点绑定，而是与插槽绑定（当节点数量变化时，每个节点负责的插槽也会变）。redis会根据key的有效部分计算插槽值，分两种情况：

* key中包含“{}”，且“{}”中至少包含1个字符，“{}”中的部分是有效部分
* key中不包含“{}”，整个key都是有效部分

例如：key是num，那么就根据num计算，如果是{test}num，则根据test计算。计算方式是利用CRC16算法得到一个hash值，然后对16384取余，得到的结果就是slot值。



如果需要将某一类数据保存在同一个redis实例中，可以让这一类数据都使用相同的有效部分，例如key都以{typeId}为前缀



### 4.2、故障转移

当某个实例与其他实例失去连接后，会被判断疑似宕机，当确定服务下线后，会自动从该集群中提升一个slave为新的master



### 4.3、数据迁移

利用cluster failover命令可以手动让集群中的某个master宕机，切换到执行cluster failover命令的这个slave节点，实现无感知的数据迁移

手动的failover支持三种不同模式：

* 缺省：默认的流程，如图1~6步

* force：省略了对offset的一致性校验

* takeover：直接执行第5步，忽略数据一致性、忽略master状态和其他master的意见

<img src="image\image-20230531162205498.png" alt="image-20230531162205498" style="zoom:50%;" />



# Redis最佳实践

## 1、Redis键值设计

### 1.1、优雅的key结构

Redis的key最好遵循以下约定：

* 遵循基本格式：[][][业务名称]:[数据名]:[id]
* 长度不超过44字节
* 不包含特殊字符

例如登陆用户的key可以是login:user:10



优点：

1. 可读性强

2. 避免key冲突

3. 方便管理

4. 更节省内存：key是string类型，底层编码包含int、embstr和raw三种。embstr在小于44字节（低于4.0版本是39字节）时使用，采用连续内存空间，内存占用更小

   ```shell
   # 通过如下命令可以查看key类型
   object encoding key
   ```

   

### 1.2、避免BigKey

BigKey通常以Key的大小和Key中的成员的数量来综合判定，例如：

* Key本身的数据量过大：一个String类型的Key，它的值为5MB
* Key中的成员数量过多：一个ZSET类型的Key，它的成员数量为10000个
* Key中成员的数据量过大：一个Hash类型的Key，它的成员数量虽然只有1000个，但这些成员的Value总大小为100MB



推荐值：

* 单个Key的value小于10KB
* 对于集合类型的Key，建议元素数量小于1000



```shell
# 通过如下命令可以查看key的大小，但比较耗费cpu
MEMORY USAGE KEY
# 查看string类型value的长度
STRLEN KEY
```



#### BigKey的危害

* 网络阻塞

  对BigKey执行读请求，少量的QPS就可能导致带宽使用率被占满，导致Redis实例，乃至所在物理机变慢

* 数据倾斜

  BigKey所在的Redis实例内存使用率远超其他实例，无法使数据分片的内存资源达到平衡

* Redis阻塞

  对元素较多的hash、list、zset等做运算会耗时较久，使主线程被阻塞

* CPU压力

  对BigKey的数据序列化和反序列化会导致CPU的使用率飙升，影响Redis实例和本机其他应用



#### 发现BigKey

* redis-cli --bigkeys

  利用redis-cli提供的 --bigkeys参数，可以遍历分析所有key，并返回Key的整体统计信息与每个数据的Top1的big key

* scan扫描

  自己编程，利用scan命令扫描Redis中的所有key，利用strlen、hlen等命令判断key的长度（不建议使用MEMORY USAGE）

  SCAN命令扫描全部，HSCAN扫描Hash，SSCAN扫描Set，ZSCAN扫描Sorted Set

* 第三方工具

  利用第三方工具，如Redis-Rdb-Tools分析RDB快照文件，全面分析内存使用情况

* 网络监控

  自定义工具，监控进出Redis的网络数据，超出预警值时主动告警



#### 删除BigKey

BigKey占用内存较多，即便删除这样的key也要耗费很长时间，导致Redis主线程阻塞，引发一系列问题

* redis3.0版本以下

  如果是集合类型，则遍历BigKey的元素，先逐个删除子元素，最后删除BigKey

* redis4.0版本以后

  Redis在4.0版本以后提供了异步删除命令：unlink



### 1.3、恰当的数据结构

例1：存储一个复杂对象

* 方式一：json字符串
  * 优点：实现简单
  * 缺点：数据耦合，不够灵活
* 方式二：字段打散，将每个属性作为一个字符串
  * 优点：可以灵活访问对象任意字段
  * 缺点：占用空间大、没法做统一控制
* 方式三：hash（推荐）
  * 优点：底层使用ziplist，占用空间小，可以灵活访问对象的任意字段
  * 缺点：代码相对复杂



例2：hash类型的key，其中有100万对field和value，field是自增的id，这个key存在什么问题，如何优化

存在问题：hash的entry数量超过500时，会使用哈希表而不是ziplist，内存占用较多

* 方案一：可以通过hash-max-ziplist-entries配置entry上限，但如果entry过多就会导致BigKey问题

```shell
# 查看
config get hash-max-ziplist-entries
# 修改
config set hash-max-ziplist-entries 数量
```

* 方案二：拆分为string类型：
  * 问题：
    * string结构底层没有太多的内存优化，内存占用较多
    * 想要批量获取这些数据比较麻烦

* 方案三（推荐）：拆分为小的hash，将id/100作为key，将id % 100作为field，这样每100个元素作为一个hash



## 2、批处理

因为Redis处理命令非常快，可以一次将一批命令发送至redis执行并返回，通过减少网络请求次数来加快执行速度

但不要在一次批处理中传输太多命令，否则单次命令占用带宽过多，会导致网络阻塞



### 2.1、pipeline

MSET虽然可以批处理，但是却只能操作部分数据类型，因此如果有对复杂数据类型的批处理需求，建议使用pipeline功能

**注意：**pipeline多个命令间不具备原子性，中间redis可能会执行其他客户端的命令



### 2.2、集群下的批处理

如MSET或Pipeline这样的批处理命令需要在一次请求中携带多条命令，而此时如果redis是一个集群，那批处理命令的多个key必须落在一个插槽中，否则会导致执行失败

|          | 串行命令                      | 串行slot                                                     | 并行slot（推荐）                                             | hash_tag                                               |
| -------- | ----------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------ |
| 实现思路 | for循环遍历，依次执行每个命令 | 在客户端计算每个key的slot，将slot一致分为一个组，每组都利用pipeline进行批处理，<font color="red">串行</font>执行各组命令 | 在客户端计算每个key的slot，将slot一致分为一个组，每组都利用pipeline进行批处理，<font color="red">并行</font>执行各组命令 | 将所有的key设置相同的hash_tag，则所有key的slot一定相同 |
| 耗时     | N次网络耗时 + N次命令耗时     | m次网络耗时 + N次命令耗时<br>m=key的slot个数                 | 1次网络耗时 + N次命令执行耗时                                | 1次网络耗时 + N次命令执行耗时                          |
| 优点     | 实现简单                      | 耗时较短                                                     | 耗时非常短                                                   | 耗时非常短、实现简单                                   |
| 缺点     | 耗时非常久                    | 实现稍复杂，slot越多，耗时越久                               | 实现复杂                                                     | 容易出现数据倾斜                                       |

计算slot并分组代码样例

```java
Map<Integer, List<Map.Entry<String, String>>> result = map.entrySet()
    .stream()
    .collect(Collectors.groupingBy(
        entry -> CluserSlotHashUtil.calculateSlot(entry.getKey())
    ));
```



**Spring的Redis客户端内部以并行slot解决了这个问题（Lettuce）**

建议直接使用Spring提供的StringRedisTemplate去进行集群下的批处理，而不是自己实现（Jedis）



## 3、服务端优化

### 3.1、持久化配置

Redis的持久化虽然可以保证数据安全，但也会带来很多额外的开销，因此持久化一般遵循下列建议：

1. 用来做缓存的Redis实例尽量不要开启持久化功能
2. 建议使用RDB和AOF的混合持久化模式
3. 利用脚本定期在salve结点做RDB，实现数据备份
4. 设置合理的rewrite阈值，避免频繁地bgwrite
5. 配置no-appendfsync-on-rewrite = yes，禁止在rewrite期间做aof，避免因AOF引起的阻塞（但也要权衡这期间若宕机丢失数据的问题）



部署相关建议：

1. Redis实例的物理机要预留足够的内存，对应fork和rewrite
2. 单个Redis实例内存上限不要太大，例如4G或8G。可以加快fork的速度，从而减少主从同步、数据迁移的压力
3. 不要与CPU密集型的应用部署在一起
4. 不要与高硬盘负载应用一起部署，例如：数据库、消息队列



### 3.2 慢查询

在Redis执行时耗时超过某个阈值的命令，称为慢查询



慢查询阈值配置：

* slowlog-log-slower-than：慢查询阈值，单位是微秒。默认是10000（10毫秒），建议1000

慢查询会被放入慢查询日志中，日志的长度有上限，可以通过配置指定：

* slowlog-max-len：慢查询日志（本质是一个队列）的长度。默认128，建议1000



动态修改这两个配置可以通过config set命令（永久修改需要修改配置文件）：

```shell
# 修改
config set slowlog-log-slower-than 1000
# 查询
config get slowlog-log-slower-than
```



查看慢查询列表：

* slowlog len：查询慢查询日志长度
* slowlog get [n]：读取n条慢查询日志
* slowlog reset：清空慢查询列表

<img src="image\image-20230716183244342.png" alt="image-20230716183244342" style="zoom:50%;" />



## 4、命令及安全配置

Redis会绑定在0.0.0.0:6379，这样会将redis暴露在公网上，而Redis如果没有做身份认证，会出现严重的安全漏洞。漏洞重现方式：https://cloud.tencent.com/developer/article/1039000



为了避免这样的漏洞，这里给出一些建议：

1. Redis一定要设置密码
2. 禁止线上使用下面的命令：keys、flushall、flushdb、config set等命令。可以利用rename-command禁用
3. bind：限制网卡、禁止外网网卡访问
4. 开启防火墙
5. 不要使用Root账号启动Redis
6. 尽量不使用默认端口



## 5、内存配置

当Redis内存不足时，可能导致Key频繁被删除、响应时间变长、QPS不稳等问题。当内存使用率达到90%以上时就需要我们警惕，并快速定位到内存占用的原因

| 内存占用   | 说明                                                         |
| ---------- | ------------------------------------------------------------ |
| 数据内存   | 是Redis最主要的部分，存储Redis的键值信息。主要问题是BigKey问题、内存碎片问题 |
| 进程内存   | Redis主进程本身运行肯定需要占用内存，如代码、常量池等等；这部分内存大约占几MB，在大多数生产环境中与Redis数据占用的内存相比可以忽略。 |
| 缓冲区内存 | 一般包括客户端缓冲区、AOF缓冲区、复制缓冲区等。客户端缓冲区又包括输入缓冲区和输出缓冲区两种。这部分内存占用波动较大，不当使用BigKey，可能会导致内存溢出。 |

Redis提供了一些命令，可以查看Redis目前的内存分配状态：

````shell
# info会展示cpu、memory、客户端信息等
# info memory只展示内存、info clients只展示客户端连接情况
info memory
````



### 内存缓冲区配置

内存缓冲区常见的有三种：

* 复制缓冲区：主从复制的repl_backlog_buf，如果太小可能导致频繁的全量复制，影响性能。通过repl-backlog-size来设置，默认1mb
* AOF缓冲区：AOF刷盘之前的缓存区域，AOF执行rewrite的缓冲区。无法设置容量上限
* 客户端缓冲区：分为输入输出缓冲区，输入缓冲区最大1G且不能设置。输出缓冲区可以设置

![image-20230716190618006](image\image-20230716190618006.png)



```shell
# 查看客户端连接信息
client list
```



## 6、集群最佳实践

单体Redis（主从Redis）已经能达到万级别的QPS，并且也具备很强的高可用性。如果主从能满足业务需求的情况下，尽量不搭建Redis集群



在redis的默认配置中，如果发现一个插槽不可用，则整个集群都会对外服务：

为保证高可用特性，这里建议将cluster-require-full-coverage配置为false



### 集群带宽问题

集群节点之间会不断地互相Ping来确定集群中其他节点的状态。每次Ping携带的信息至少包括：

* 插槽信息
* 集群状态信息

集群中节点越多，集群状态信息数据量也越大，10个节点的相关信息可能达到1kb，此时每次集群互通需要的带宽会非常高



解决途径：

1. 避免大集群，集群节点数不要太多，最好少于1000，如果业务庞大，则建立多个集群
2. 避免在单个物理机中运行太多的Redis实例
3. 配置合适的cluster-node-timeout值



# Redis底层原理

## 1、底层数据结构

### 1.1、动态字符串SDS

Redis构建了一种新的字符串结构，简称为动态字符串（**S**imple **D**ynamic **S**tring），简称为SDS

源码如下：

```c
#define SDS_TYPE_5	0
#define SDS_TYPE_8	1
#define SDS_TYPE_16	2
#define SDS_TYPE_32	3
#define SDS_TYPE_64	4

struct __attribute__ ((__packed__)) sdshdr8 {
    uint8_t len; /* buf已保存的字符串字节数，不包含结束标识*/
    uint8_t alloc; /* buf申请的总的字节数，不包含结束标识*/
    unsigned char flags; /* 不同SDS的头类型，用来控制SDS的头大小*/
    char buf[];
};
```

<img src="image\image-20230718114420657.png" alt="image-20230718114420657" style="zoom:50%;" />



#### SDS动态扩容

* 新的字符串小于1M：新空间为扩展后字符串长度的二倍 + 1
* 新的字符串大于1M：新空间为扩展后字符串长度 + 1M + 1。称为内存预分配

多申请一部分的原因是向操作系统申请内存需要切换到内核态，这是个耗时操作，频繁切换会导致性能下降，所以预分配来避免频繁向操作系统申请来提高性能。

<img src="image\image-20230718115648362.png" alt="image-20230718115648362" style="zoom:50%;" />

优点：

1. 获取字符串长度的时间复杂度变为O(1)
2. 支持动态扩容
3. 减少内存分配次数
4. 二进制安全



### 1.2、IntSet

IntSet是Redis集合中Set的一种实现方式，基于整数数组来实现，并且具备长度可变、有序等特点。

源码如下：

```c
/* encodeing包含三种模式，表示存储的整数大小不同*/
# define INTSET_ENC_INT16 (sizeof(int16_t)) /* 2字节整数，范围类似java的short*/
# define INTSET_ENC_INT32 (sizeof(int32_t)) /* 4字节整数，范围类似java的int*/
# define INTSET_ENC_INT64 (sizeof(int64_t)) /* 8字节整数，范围类似java的long*/

typedef struct intset {
    uint32_t encoding; /* 编码方式，支持存放16位、32位、64位整数*/
    uint32_t length; /* 元素个数*/
    int8_t contents[]; /* 整数数组，保存集合数据*/
} intset;
```

<img src="image\image-20230718143304782.png" alt="image-20230718143304782" style="zoom:50%;" />



#### IntSet升级

当添加的数字超过了对应编码格式表示的范围，IntSet会**自动升级**编码方式到合适的大小：

1. 升级编码模式至合适的大小，并按照新的编码方式及元素个数扩容数组
2. 倒序依次将数组中的元素拷贝到扩容后的正确位置（因为正序会直接覆盖多个后续字节，丢失原有数据）
3. 将待添加元素放入数组末尾
4. 最后，将IntSet的encoding属性改为对应的编码格式，并将length+1

<img src="image\image-20230718145800277.png" alt="image-20230718145800277" style="zoom:50%;" />



**特点：**

1. Redis会确保IntSet中的元素唯一、有序
2. 具备类型升级机制（通过使用尽可能小的编码格式来），可以节省内存空间
3. 底层采用二分查找方式来查询



### 1.3 Dict

Redis是一个键值型的数据库，而键与值的映射正式通过Dict来实现的

Dict由三部分组成，分别是：哈希表（DictHashTable）、哈希节点（DictEntry）、字典（Dict）

```c
typedef struct dictht {
	// entry数组
    // 数组中保存的是指向entry的指针
    dictEntry **table;
    // 哈希表大小，是2的n次幂
    unsigned long size;
    // 哈希表大小的掩码，等于总size - 1
    unsigned long sizemask;
    // entry个数
    unsigned long used;
} dictht;

typedef struct dictEntry {
    void *key; //键
    union {
        void *val;
        uint64_t u64;
        int64_t s64;
        double d;
    } v; // 值
    // 下一个Entry的指针
    struct dictEntry *next;
} dictEntry;
```

当向Dict添加键值对时，Redis现根据key计算出hash值（h），然后利用 h & sizemask来计算元素应该存储到数组的哪个索引位置

<img src="image\image-20230718163346582.png" alt="image-20230718163346582" style="zoom:50%;" />



```c
typedef struct dict {
	dictType *type; // dict类型，内置不同的hash函数
    void *privdata; // 私有数据，在做特殊hash运算时使用
    dictht ht[2]; // 一个Dict包含两个哈希表，其中一个是当前数据，另一个一般是空，rehash时使用
    long rehashidx; // rehash进度，-1表示未进行
    int16_t pauserehash; // rehash是否暂停，1则暂停，0则继续
} dict;
```

<img src="image\image-20230718163957696.png" alt="image-20230718163957696" style="zoom: 33%;" />



#### Dict的扩容

Dict中的HashTable就是数组结合的单向链表的实现，当集合中元素较多时，必然导致哈希冲突增多，链表过长，则查询效率会大大降低。

Dict在每次新增时都会检查**负载因子**（LoadFactor=use/size），满足以下两种情况的时候会触发**哈希扩容：**

* 哈希表的LoadFactor >= 1，并且服务没有执行bgsave或者bgrewriteaof等后台进程；
* 哈希表的LoadFactor >  5;



#### Dict的缩容

除了扩容外，每次删除元素时，也会对负载因子做检查，当size> 4 且 LoadFactor < 0.1时，会做哈希表收缩。



#### Dict的rehash

不管是扩容还是缩容，必定会创建新的哈希表，导致哈希表的size和sizemask变化，而key的查询与sizemask有关。因此必须对哈希表中的每一个key重新计算索引，插入新的哈希表，这个过程称为rehash。

但Dict的rehash并不是一次性完成的。若Dict中包含大量数据，一次rehash完成极有可能导致主线程阻塞，因此Dict的rehash是分多次、渐进式地完成的，因此称为**渐进式rehash**。过程如下：

1. 重新计算hash的realSize，值取决于当前是要做扩容还是缩容：
   * 扩容：新size为第一个大于等于dict.ht[0].used + 1的2^n^
   * 缩容：新的size为第一个大于等于dict.ht[0].used的2^n^（不小于4）
2. 按照新的realSize申请内存空间，创建dictht，并赋值给dict.ht[1]
3. 设置dict.rehashidex=0，标识开始rehash

​	~~4. 将dict.ht[0]中的每一个dictEntry都rehash到dict.ht[1]~~

4. 每次执行新增、查询、修改、删除操作时，都检查一下dict.rehashidx是否大于-1，如果是则将dict.ht[0].table[rehashidx]的entry链表rehash到dict.ht[1]，并且将rehashidx++。直至dict.ht[0]的所有数据都rehash到dict.ht[1]
4. 将dict.ht[1]赋值给dict.ht[0]，给dict.ht[1]初始化为空哈希表，释放原来dict.ht[0]的内存
4. 将rehashidx赋值为-1，代表rehash结束
4. 在rehash过程中，新增操作，则直接写入dict.ht[1]，查询、修改和删除则会在dict.ht[0]和dict.ht[1]依次查找并执行。这样可以确保dict.ht[0]的数据只减不增，随着rehash最终为空。



### 1.4 ZipList

ZipList是一种特殊的“双端链表”，由一系列特殊编码的连续内存块组成。可以在任意一端进行压入/弹出操作，并且该操作的时间复杂度为O(1)。

<img src="image\image-20230821230557222.png" alt="image-20230821230557222" style="zoom:50%;" />

|  属性   |   类型    | 长度  | 用途                                                         |
| :-----: | :-------: | :---: | ------------------------------------------------------------ |
| zlbytes | uint_32_t | 4字节 | 记录整个压缩列表占用的内存字节数                             |
| zltail  | uint32_t  | 4字节 | 记录压缩列表表尾节点距离压缩列表的起始地址有多少字节，通过这个偏移量，可以确定表位节点的地址 |
|  zllen  | uint16_t  | 2字节 | 记录了压缩列表包含的节点数量。最大值为UINT16_MAX（65534），如果超过这个值，这里会记录为65535，但节点的真实数量需要遍历整个压缩列表才能计算得出 |
|  entry  | 列表节点  | 不定  | 压缩列表包含的各个节点，节点的长度由节点保存的内容决定       |
|  zlend  |  uint8_t  | 1字节 | 特殊值0xFF（十进制255），用于标记压缩列表的末端              |



#### ZipListEntry

ZipList中的Entry并不像普通链表那样记录前后节点的指针，因为记录两个指针需要占用16字节，浪费内存。而是采用了下面的结构：

| previous_entry_length | encoding | content |
| --------------------- | -------- | ------- |

* previous_entry_length：前一节点的长度，占1个或5个字节
  * 如果前一节点的长度小于254字节，则采用1个字节来保存这个长度值
  * 如果前一节点的长度大于254字节，则采用5个字节来保存这个长度值，第一个字节为0xFE，后面4个字节才是真实的长度数据
* encoding：编码属性，记录content的数据类型（字符串还是整数）及长度，占用1个、2个或5个字节
* content：负责保存节点的内容，可以是字符串或整数



**注意：**ZipList中所有存储长度的数值均采用小端字节序，即低位字节在前，高位字节在后。例如：数值0x1234，采用小端字节序后实际存储值为：0x3412



#### Endoding编码

ZipListEntry中的encoding编码分为字符串和整数两种：

* 字符串：如果encoding是以“00”、“01”或者“10”开头，则证明content是字符串(pqrst等字母是想用8个字符表示一个字节)

  |                         编码                         | 编码长度 |     字符串大小      |
  | :--------------------------------------------------: | :------: | :-----------------: |
  |                     \|00pppppp\|                     |  1 byte  |     <= 63 bytes     |
  |                \|01pppppp\|qqqqqqqq\|                | 2 bytes  |   <= 16383 bytes    |
  | \|10000000\|qqqqqqqq\|rrrrrrrr\|ssssssss\|tttttttt\| | 5 bytes  | <= 4294967295 bytes |



![image-20230821233321967](image\image-20230821233321967.png)



* 整数：如果encoding是以“11”开始，则证明content是整数，且encoding固定只占用1个字节

  |   编码   | 编码长度 |                           整数类型                           |
  | :------: | :------: | :----------------------------------------------------------: |
  | 11000000 |    1     |                      int16_t（2 bytes）                      |
  | 11010000 |    1     |                      int32_t（4 bytes）                      |
  | 11100000 |    1     |                      int64_t（8 bytes）                      |
  | 11110000 |    1     |                  24位有符号整数（3 bytes）                   |
  | 11111110 |    1     |                   8位有符号整数（1 byte）                    |
  | 1111xxxx |    1     | 直接在xxxx位置保存数值，范围从0001~1101（因为0000和1110是前面的编码，1111是结束符号，都被占用了），减1后结果为实际值 |

![image-20230821234552573](image\image-20230821234552573.png)



#### ZipList的连锁更新问题

ZipList的每个Entry都包含previous_entry_length来记录上一个节点的大小，长度是1个或5个字节：

* 如果前一节点的长度小于254字节，则采用1个字节来保存

* 如果前一节点的长度大于254字节，则采用5个字节来保存这个长度值，第1个字节为0xFE，后面4个字节才是真实的长度数据



假设ZipList中存储了N个连续的、长度为250-253字节之间的Entry，每个Entry的previous_entry_length都占用1个字节

如果此时在头结点插入一个大于254字节的的Entry，原来第1个Entry会因为使用5个字节记录上一个节点的长度，导致Entry自身的长度超过254字节，然后会连锁导致后续所有Entry的长度都超过254字节，内存数据的调整将会耗费大量资源。



ZipList在这种特殊情况下产生的连续多次空间扩展操作称之为**连锁更新（Cascade Update）**。新增、删除都有可能导致连锁更新的发生。但因为要求连续N个previous_entry_length相同的Entry且字节在临界值，所以实际发生的概率相对较低。



新增、删除都有可能导致

![image-20230821235504584](image\image-20230821235504584.png)



### 1.5、QuickList

Redis在3.2版本引入了新的数据结构QuickList，它是一个双端链表，只不过链表中的每个节点都是一个ZipList。

解决的问题：

1. 限制了每个ZipList的长度和entry的大小，避免了内存占用较多、申请内存效率低下的问题
2. 创建多个ZipList来分片存储数据，避免存储大量数据时超出ZipList的最佳上限
3. 统一管理避免数据拆分后比较分散，不方便管理和查找



![image-20230904230215538](image\image-20230904230215538.png)

为了避免QuickList中的每个ZipList中entry过多，Redis提供了一个配置项：<font color="red">list-max-ziplist-size</font>来限制，可通过`config get/set list-max-ziplist-size`查看设置

* 如果值为正，则代表ZipList的允许的entry个数的最大值
* 如果值为负，则代表ZipList的最大内存大小，分5种情况：
  * -1：每个ZipList的内存占用不能超过4kb
  * **-2（默认值）：每个ZipList的内存占用不能超过8kb**
  * -3：每个ZipList的内存占用不能超过16kb
  * -4：每个ZipList的内存占用不能超过32kb
  * -5：每个ZipList的内存占用不能超过64kb



QuickList还可以对节点的ZipList做压缩。通过配置项 list-compress-depth 来控制。因为链表一般从首尾访问较多，所以首尾是不压缩的，这个参数是控制首尾不压缩的节点个数：

* 0：默认值，代表不压缩
* 正整数n：标识QuickList的首尾各有n个节点不压缩，中间节点压缩



QuickList和QuickListNode的源码：

```c
typedef struct quicklist {
    // 头节点指针
    quicklistNode *head;
    // 尾节点指针
    quicklistNode *tail;
    // 所有ziplist的entry的数量
    unsigned long count;
    // ziplist总数量
    unsigned long len;
    // ziplist的entry上限，默认值 -2
    int fill : QL_FILL_BITS;
    // 首尾不压缩的节点数量
    unsigned int compress : QL_COMP_BITS;
    // 内存重分配时的书签数量级数组，一般用不到
    unsigned int bookmark_count : QL_BM_BITS;
    quicklistBookmark bookmarks[];
} quicklist;
```



```c
typedef struct quicklistNode {
	// 前一个节点指针
    struct quicklistNode *prev;
 	// 下一个节点指针
    struct quicklistNode *next;
    // 当前节点的ZipList指针
    unsigned char *zl;
    // 当前节点的ZipList的字节大小
    unsigned int sz;
    // 当前节点的ZipList的entry个数
    unsigned int count : 16;
    // 编码方式： 1：ZipList；2：lzf压缩模式
    unsigned int encoding : 2;
    // 数据容器类型（预留）：1：其他；2：ZipList
    unsigned int container : 2;
    // 是否被解压缩。1：则说明被解压缩了，将来要重新压缩
    unsigned int recompress : 1;
    // 测试用
    unsigned int attempted_compress : 1;
    // 预留字段
    unsigned int extra : 10;
} quicklistNode;
```

<img src="image\image-20230904233246967.png" alt="image-20230904233246967" style="zoom:50%;" />



### 1.6、SkipList

SkipList（跳表）是链表但与传统链表相比有几点差异：

* 元素按照升序排列存储
* 节点可能包含多个指针，指针跨度不同

<img src="image\image-20230904233852560.png" alt="image-20230904233852560" style="zoom:50%;" />

```c
typedef struct zskiplist {
	// 头尾节点指针
    struct zskiplistNode *header, *tail;
    // 节点数量
    unsigned long length;
    // 最大的索引层级，默认是1
    int level;
} zskiplist;
```

```c
typedef struct zskiplistNode {
    // 节点存储的值
	sds ele;
    // 节点分数，排序、查找用
    double score;
    // 前一个节点指针
    struct zskiplistNode *backward;
    // 多级索引数组
    struct zskipListLevel {
        // 下一个节点指针
        struct zskiplistNode *forward;
        // 索引跨度
        unsigned long span;
    } level[];
} zskiplistNode;
```

![image-20230904235643424](image\image-20230904235643424.png)



#### 特点：

* 跳表是一个双向链表，每个节点包含score和ele值
* 节点按照score值排序，score值一样则按照ele字典排序
* 每个节点都可以包含多层指针，层数是1到32之间的随机数
* 不同层指针到下一个节点的跨度不同，层级越高，跨度越大
* 增删改查效率与红黑树基本一致，实现却更简单






# Redis使用中遇到的问题

## 1、Redisson在k8s环境中，配置哨兵的服务发现地址后，日志中不断打印连接不上日志

实际是能连接到哨兵集群的，但会额外连接到服务发现对应的ip，解决方式是在redisson的配置文件中添加`dnsMonitoringInterval:-1`来关闭dns检测
