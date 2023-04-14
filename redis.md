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



### 3.7 SortedSet类型

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



## 4. 数据结构的选择

在使用redis存储对象时，有使用String存储序列化为json字符串的方式，也有使用hash将对象中的每个字段独立存储两种方式；后者可以针对单个字段CRUD，并且内存占用更小。





## 5. 缓存更新策略

|          | 内存淘汰                                                     | 超时剔除                                                     | 主动更新                                     |
| -------- | ------------------------------------------------------------ | ------------------------------------------------------------ | -------------------------------------------- |
| 说明     | 不用自己维护，利用redis的内存淘汰机制，当内存不足时自动淘汰部分数据。下次查询时更新缓存。 | 给缓存数据添加TTL时间，到期后自动删除缓存。下次查询时更新缓存。 | 编写业务逻辑，在修改数据库的同时，更新缓存。 |
| 一致性   | 差                                                           | 一般                                                         | 好                                           |
| 维护成本 | 无                                                           | 低                                                           | 高                                           |

业务场景：

* 低一致性需求：使用内存淘汰机制。例如店铺类型的查询缓存
* 高一致性需求：主动更新，并以超时剔除作为兜底方案。例如店铺详情查询的缓存



### 5.1 主动更新策略

#### 1. cache aside pattern（推荐）

​	由缓存的调用者，在更新数据库的同时更新缓存

#### 2. read/write through pattern

​	缓存与数据库整合为一个服务，由服务来维持一致性。调用者调用该服务，无需关心缓存一致性问题

#### 3. write behind caching pattern

​	调用者只操作缓存，由其他线程异步地将缓存数据持久化数据库，保证最终一致性



### 5.2 最佳实践方案

1. 低一致性需求：使用redis自带的内存淘汰机制
2. 高一致性需求：主动更新，并以超时剔除作为兜底方案
   * 读操作：
     * 缓存命中则直接返回
     * 缓存未命中则查询数据库，并写入缓存，设定超时时间
   * 写操作：
     * 先写数据库，然后删除缓存
     * 要确保数据库与缓存操作的原子性



### 5.3 操作缓存和数据库需要考虑的问题：

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





## 6 缓存穿透

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



## 7 缓存雪崩

**缓存雪崩**是指在同一时间段大量的缓存key同时失效或者redis服务宕机，导致大量的请求到达数据库，带来巨大的压力。

**解决方案：**

* 给不同的key的TTL添加随机值
* 利用Redis集群提高服务的可用性
* 给缓存业务添加降级限流策略
* 给业务添加多级缓存



秒杀活动刚开始时redis内都没有数据，都会打到数据库上，可以提前将数据导入到redis避免雪崩



## 8 缓存击穿

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



## 9 秒杀业务

### 9.1 全局id

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



### 9.2 实现秒杀功能

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



### 9.3 分布式锁

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



### 9.4 Redisson

#### 基于Redis分布式锁的优化

存在的问题：

* 不可重入：同一个线程无法多次获取通一把锁
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



### 9.5 基于Redis的消息队列（只是种实现，不推荐使用）

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



## 10 用户点赞

### 10.1 普通点赞

因为对于同一篇博客，一个用户最多点赞1次，同时为了在集合中区分用户，**可以使用Set数据结构**：以该博客的id+部分字符串为key，用户id的集合为value。

点赞前通过`SISMEMBER`查询用户是否在集合中（是否点过赞），未点过`SADD`通过添加用户id标记点赞，点过则通过`SREM`移除用户id取消点赞。



### 10.2 点赞同时记录点赞顺序

对于类似朋友圈点赞记录顺序的情况，**可以使用SortedSet数据结构**，以时间戳为score，其余和普通点赞相同。

点赞前通过`ZSCORE`查询用户是否在集合中（是否点过赞），未点过`ZADD`通过添加用户id标记点赞，点过则通过`ZREM `移除用户id取消点赞。

如果要查询点赞的前n位用户，可以通过`ZRANGE key 0 n`查询出对应的用户id列表。



## 11 好友关注

### 11.1 共同关注

可以从数据库中分别查出两人的关注列表，存入set集合中（也可以从业务层面直接将关注列表存在redis的set集合中），再通过`SINTER`求集合的交集。



### 11.2 关注推送

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

## 11 用户签到表

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



## 12 页面UV统计

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



## 1. Redis持久化

### 1.1 RDB

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



### 1.2 AOF

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

|              | RDB  | AOF  |
| ------------ | ---- | ---- |
| 持久化方式   |      |      |
| 数据完整性   |      |      |
| 文件大小     |      |      |
| 宕机恢复速度 |      |      |
|              |      |      |
|              |      |      |
|              |      |      |

