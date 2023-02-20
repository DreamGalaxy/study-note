# Zookeeper

## 一、简介

zookeeper是一个分布式的、开源的分布式应用协调服务

功能：

* 配置管理
* 分布式锁
* 集群管理

## 二、基础命令

### 2.1 服务端

```shell
启动：./zkServer.sh start 
停止：./zkServer.sh stop
重启：./zkServer.sh restart
查看状态：./zkServer.sh status
```



### 2.2 客户端

进入客户端命令行

```sh
./zkCli.sh -server ip:端口（默认2181）
```

退出

```sh
quit
```

显示节点

```sh
ls 节点路径
ls -s 节点路径  会展示详细信息
ls /test
```

创建结点

```sh
create [-s][-e] 节点路径 数据   默认是持久节点-e是临时结点，-s是顺序节点 一次创建多级会报错
create /test data
create /test2
```

获取数据

```sh
get 节点路径
get /test
```

删除节点（节点下有子节点时不能删除）

```sh
delete 节点路径  
delete /test
```

删除节点下全部节点

```sh
deleteall 节点路径
deleteall /test
```



## 三、zookeeper数据模型

* zookeeper是一个<font color="red">树形</font>目录服务，其数据模型和Unix的文件系统目录树很类似，拥有一个层次化结构

* 每个节点都被称为ZNode，每个节点上都会保存自己的数据和节点信息
* 节点可以有子节点，同时也允许少量的数据（1MB）存储在该节点下
* 节点可分为4大类：
  * PERSISTENT持久化节点
  * EPHEMERAL临时结点：-e       客户端断开连接时临时结点会被删除
  * PERSISTENT_SEQUENTIAL持久化顺序节点：-s    所有节点会加上顺序编号，并且共用顺序编号
  * EPHEMERAL_SEQUENTIAL临时顺序节点：-es

<img src="image\image-20220128161944832.png" alt="image-20220128161944832" style="zoom: 67%;" />



## 四、Java API操作

### 4.1 Curator介绍

常见的Zookeeper Java API：

* 原生Java API
* ZkClient

* Curator

Curator大大简化了Zookeeper客户端的使用

高版本Curator能兼容低版本zookeeper



### 4.2 Curator API常用操作

#### 4.2.1 建立连接

```java
// 重试策略，有多种可以指定
RetryPolicy retryPolicy = new ExponentialBackoffRetry(3000,10);
// 第一种创建方式，参数分别为服务端地址，会话超时时间ms(默认60s)，连接超时时间ms(默认15s)，重试策略
CuratorFramework client = CuratorFrameworkFactory.newClient("127.0.0.1:2181",60000,15000,retryPolicy);
CuratorFramework client2 = CuratorFrameworkFactory.builder().connectString("127.0.0.1:2181")
.sessionTimeoutMs(60000).connectionTimeoutMs(15000)
.retryPolicy(retryPolicy)
// 是将对应的namespace视为根目录，后续操作默认会加上对应的路径
.namespace("test")
.build();
// 开启连接
client.start();
// 关闭连接
if (client != null) {
    client.close();
}
```

#### 4.2.2 添加节点

临时节点不允许有子节点

```java
try {
    // 1.基本创建，如果没有指定数据，则默认将当前客户端的ip作为数据存储
    String path = client.create().forPath("/app1");
    // 2.带有数据的创建
    client.create().forPath("/app2","test".getBytes());
    // 3.设置节点类型
    client.create().withMode(CreateMode.EPHEMERAL).forPath("/app3");
    // 4.创建多级节点 creatingParentsIfNeeded()如果父节点不存在则创建父节点
    client.create().creatingParentsIfNeeded().forPath("/app4/test");
} catch (Exception e) {
    e.printStackTrace();
}
```

#### 4.2.3 删除节点

```java
// 1.删除单个节点 delete
client.delete().forPath("/test");
// 2.删除带有子节点的节点 deleteall
client.delete().deletingChildrenIfNeeded().forPath("/test");
// 3.必须成功的删除（防止网络抖动等，本质是重复多次）
client.delete().guaranteed().forPath("/test");
// 4.回调
client.delete().inBackground(new BackgroundCallback() {
@Override
public void processResult(CuratorFramework curatorFramework, CuratorEvent curatorEvent) throws Exception {
System.out.println(curatorEvent);
}
}).forPath("/test");
```

#### 4.2.4 修改节点

```java
// 修改数据
client.setData().forPath("/app1","new data".getBytes());

// 先查询出当前版本
Stat stat1 = new Stat();
client.getData().storingStatIn(stat1).forPath("/app1");
int version = stat1.getVersion();
// 修改数据时指定版本，像CAS，版本号相同才能修改
client.setData().withVersion(version).forPath("/app1", "new data".getBytes());
```

* 查询节点

```java
// 1.查询数据get
byte[] data = client.getData().forPath("/app1");
new String(data);
// 2.查询子节点（这里虽然写的是/，但其实是找的命名空间下的）
List<String> paths = client.getChildren().forPath("/");
// 3.查询节点状态信息 ls -s
Stat stat = new Stat();
byte[] data2 = client.getData().storingStatIn(stat).forPath("/app1");
```

#### 4.2.5 Watch事件监听

* Zookeeper允许用户在指定节点上注册一些Watcher，在一些特定事件触发时，zookeeper服务端会将事件通知到感兴趣的客户端上去，该机制是zookeeper实现分布式协调服务的重要特性
* Zookeeper中引入了Wathcer机制来实现发布/订阅功能，能够让多个订阅者同时监听某一对象，当一个对象自身状态变化时，会通知所有订阅者
* Zookeeper原生支持通过注册Watcher来进行事务监听，但是其使用不是特别方便，需要开发人员自己反复注册Wathcer<font color="red">（因为Wathcer是一次性的，想要实现持久订阅需要在事件触发后重新发起）</font>，比较繁琐
* Curator引入了Cache来实现对Zookeeper服务端事件的监听
* 旧版本Curator提供了3种Watcher：
  * NodeCache：只是监听某一个特定结点
  * PathChildrenCache：监控一个ZNode的子节点
  * TreeCache：可以监控整个树上所有的节点，类似于PathChildrenCache和NodeCache的组合
* 新版本Curator提供了一种Watcher
  * CuratorCache：默认监听节点及其子树节点，build时通过指定CuratorCache.Options.SINGLE_NODE_CACHE可以只监听单个节点
  * 新版本也兼容了旧版本，在设置监听的builder()后可以通过forNodeCache()、forPathChildrenCache()和forTreeCache()对旧版本的进行桥接

```java
// 1.创建CuratorCache对象，SINGLE_NODE_CACHE表示只监听单个节点，默认是监听子树，Options还能指定是否压缩数据、client关闭后cache数据是否清除
CuratorCache curatorCache = CuratorCache.build(client, "/test", CuratorCache.Options.SINGLE_NODE_CACHE);
// 2.创建listener并设置监听事件
CuratorCacheListener curatorCacheListener = CuratorCacheListener.builder()
    // 也可以同时设置多种监听的类型
    .forChanges((oldNode, node) -> {
        System.out.println(oldNode);
        System.out.println(node);
    })
    .forAll((type, oldData, data) -> {
        System.out.println(type);
        System.out.println(oldData);
        System.out.println(data);
    })
    .build();
// 3.注册监听
curatorCache.listenable().addListener(curatorCacheListener);
// 4.开启监听
curatorCache.start();
```

#### 4.2.6 分布式锁实现

在Curator中有五种锁的实现方案：

* InterProcessSemaphoreMutex：分布式排它锁（非可重入锁）
* InterProcessMutex：分布式可重入排它锁
* InterProcessReadWriteLock：分布式读写锁
* InterProcessMultiLock：将多个锁作为单个实体管理的容器
* InterProcessSemaphoreV2：共享信号量



## 五、Zookeeper分布式锁原理

### 5.1 分布式锁概念

* 在单机应用开发中，涉及并发的同时往往采用synchronized或Lock的方式来解决多线程间的代码同步问题，这时多线程的运行都是在同一个JVM下，没有任何问题
* 但在分布式集群的工作环境下，属于多个JVM的工作环境，跨JVM之间已经无法通过多线程的锁解决同步问题
* 那么就需要一种更加高级的锁机制，来处理跨机器的进程之间的数据同步问题——这就是分布式锁

分布式锁的实现：

* 基于缓存实现分布式锁：例如redis、memcache
* zookeeper实现分布式锁：curator
* 数据库层面实现分布式锁：悲观锁、乐观锁

redis实现分布式锁性能高但可靠性相对较低（master还没同步锁信息就挂了，可能导致产生多把锁）

zookeeper实现分布式锁可靠性高但性能相对较低



### 5.2 实现原理

<font color="red">核心思想</font>：当客户端要获取锁，则创建节点，使用完锁，则删除该节点

1. 客户端获取锁时，在某个节点例如叫lock下创建<font color="red">临时 顺序</font>节点（非临时的服务挂了节点就无法释放了）
2. 然后获取lock下面的所有子节点，客户端获取到所有子节点后，如果发现自己创建的子节点<font color="red">序号最小</font>，那么就认为该客户端获取到了锁。使用完锁后将该节点删除
3. 如果发现自己创建的节点非lock所有节点中最小的那个，说明自己还没有获取到锁，此时客户端需要找到比自己小的节点（小一的那个），同时对其注册事件监听器，监听删除事件。
4. 如果发现比自己小的那个节点被删除，则客户端的Watcher会受到相应的通知，此时再次判断自己创建的节点是否是lock子节点中序号最小的（因为可能是上一个节点的服务挂了，但还没轮到它），如果是则获取到了锁，如果不是则重复上述，继续获取到比自己小的一个节点并注册监听

<img src="image\image-20220129162618524.png" alt="image-20220129162618524" style="zoom: 50%;" />



## 六、Zookeeper集群搭建

### 6.1 Leader选举

* ServiceId：服务器ID

  比如有三台服务器，编号分别为1,2,3。编号越大在选择算法中权重越大

* Zxid：数据ID

  服务器中存放的最大数据ID。值越大说明数据越新，在选举算法中数据越新权重越大

* 在leader选举中，如果某台zookeeper获得了超过半数的选票，则此zookeeper就可以成为leader



leader选举，要求 可用节点数量 <font color="red">></font> 总节点数量/2，不包含等于

3个节点的集群，2个服务都挂掉，主服务器也无法运行。因为可运行的机器没有超过集群总数的一半



脑裂：

在zookeeper发生长时间gc或网络通信不佳的情况下，其他节点会重新选举leader导致同时有多个leader存在，分裂成了不同的小集群，这就是脑裂



要求集群数量为奇数，并且要求可用节点数量大于总节点数的一半，可以保证发生脑裂产生两个小集群时，一定有一个节点数小于半数，另一个大于半数，小于半数的集群将不可用，而大于半数的集群则仍能正常提供服务

要求奇数还有一个好处，就是在提供相同容错能力的情况下（集群数的一半相同），奇数能节省资源



### 6.2 Curator实现的选举

#### 6.2.1 LeaderLatch

原理：向同一路径下创建**临时顺序节点**，节点编号最小的为leader

特点：

```java
public static void main(String[] args) {
    CuratorFramework client = CuratorFrameworkFactory.builder()
            .connectString("127.0.0.1:2181")
            .connectionTimeoutMs(10000)
            .sessionTimeoutMs(60000)
            .retryPolicy(new ExponentialBackoffRetry(1000, 3))
            .namespace("testLeader").build();
    client.start();
    // Latch关闭策略，SILENT-关闭时不触发监听器回调，NOTIFY_LEADER-关闭时触发监听器回调方法，默认不触发
    LeaderLatch leaderLatch = new LeaderLatch(client, "/leader","", LeaderLatch.CloseMode.NOTIFY_LEADER);
    leaderLatch.addListener(new LeaderLatchListener() {
        @Override
        public void isLeader() {
            log.info("currently run as leader");
        }

        @Override
        public void notLeader() {
            // 状态从hasLeadership = true到hasLeadership = false时触发，比如通过leaderLatch.close()
            log.info("lose leader");
        }
    });
    try {
        leaderLatch.start();
    } catch (Exception e) {
        log.error("leaderLatch启动失败", e);
    }

}
```





#### 6.2.2 LeaderSelector

原理：利用Curator中InterProcessMutex分布式锁进行抢主，抢到锁的即为Leader

特点：执行完`takeLeadership()`回调方法后释放leader权限，设置了autoRequeue后会重新加入抢leader权限的队列

`LeaderSelector`有`start()`和`autoRequeue()`两个方法，第一个为启动，第二个是自动抢主

`LeaderSelectorListener`是LeaderSelector客户端节点成为leader后回调的一个监听器，在`takeLeadership()`回调方法中编写获得Leader权力后的业务处理逻辑

`LeaderSelectorListenerAdapter`是实现了`LeaderSelectorListener`接口的一个抽象类，封装了客户端与zk服务器连接起来挂起或断开时的逻辑处理（抛出抢主失败CancleLeadershipException），一般监听器推荐实现该类

```java
public class CuratorLeaderTest extends LeaderSelectorListenerAdapter {

    public static void main(String[] args) {
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString("127.0.0.1:2181")
                .connectionTimeoutMs(10000)
                .sessionTimeoutMs(60000)
                .retryPolicy(new ExponentialBackoffRetry(1000,3))
                .namespace("testLeader").build();
        client.start();
        LeaderSelector leaderSelector = new LeaderSelector(client,"/leader",new CuratorLeaderTest());
        leaderSelector.autoRequeue();
        leaderSelector.start();
    }

    @Override
    public void takeLeadership(CuratorFramework client) throws Exception {
        System.out.println("11111");
    }
}
```

