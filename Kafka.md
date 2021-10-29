# Kafka

## 1.消息队列的两种模式

### 1.1点对点模式

消息发送者生产消息发送到消息队列中，然后消息接收者从消息队列中取出并消费消息。消息被消费以后，消息队列中不再有存储，所以消息接收者不可能消费到已经被消费的消息。

特点：

* 每个消息只有一个接收者（Consumer）（一旦被消费，消息就不在消息队列中）
* 发送者和接收者间没有依赖性，发送者发送消息后，不管有没有接收者在运行，都不会影响到发送者下次发送消息
* 接收者在成功接收消息后需要向队列应答成功，以便消息队列删除当前接收的消息



### 1.2发布订阅模式

特点：

* 每个消息可以有多个订阅者
* 发布者和订阅者之间没有时间上的依赖性，针对某个主题（topic）的订阅者，它必须创建一个订阅者之后，才能消费发布者的消息
* 为了消费消息，订阅者需要提前订阅该角色的主题，并保持在线运行



启动命令

./bin/kafka-server-start.sh config/server.properties

测试是否启动成功

bin/kafka-topics.sh --bootstrap-server kafka地址:端口 --list



### 分布式流平台包含的关键能力：

1.发布和订阅数据流，类似于消息队列或企业消息传递系统

2.以容错的持久化方式存储流数据

3.处理流数据



## 2.基础操作

### 2.1 启动

#### 2.1.1启动zookeeper

单机或自测启动kafka自带的zk

```shell
./bin/zookeeper-server-start.bat config/zookeeper.properties
```

集群时单独部署启动zk

```shell
sh zkServer.sh start
```

#### 2.1.2启动kafka

```shell
./bin/kafka-server-start config/server.properties
```

后台启动

```shell
./bin/kafka-server-start -daemon config/server.properties
```



### 2.2创建topic

创建一个主题(topic)。kafka中所有消息都是保存在主题中，要生产消息到Kafka，首先必须要有一个确定的主题。

```shell
# 创建名为test的主题
bin/kafka-topics.sh --create --bootstrap-server kafkaip:端口 --topic test
# 查看目前kafka中的主题
bin/kafka-topics.sh --list --bootstrap-server kafkaip:端口 
```



### 2.3生产消息到Kafka

```shell
bin/kafka-console producer.sh --broker-list kafkaip:端口 --topic test
```



### 2.4从Kafka消费消息

```shell
bin/kafka-console-consumer.sh --bootstrap-server kafkaip:端口 --topic test --from-beginning
```



### 2.5修改分区数量

```shell
# 设置test topic为2个分区
bin/kafka-topics.sh --zookeeper kafkaip:端口 -alter --partition 2 --topic test
```



## 3.Kafka集群搭建

* kafka集群目前必须要zookeeper
* 集群每一个kafka结点都需要修改broker（每个节点标识，不能重复）
* log.dir数据存储目录需要配置



### 3.1 kafka的生产者/消费者

* 安装kafka集群，可以测试以下
  * 创建一个topic主题（消息都是放在topic中，类似mysql的建表过程）
  * 基于kafka的内置测试生产者脚本来读取标准输入（键盘输入）的数据，并放到topic中
  * 基于kafka的内置测试消费者脚本来消费topic中的数据

### 3.2 kafka的基准测试工具

* kafka中提供了内置的性能测试工具
  * 生产者：测试每秒传输的数据量（多少条数据，多少MB的数据）
  * 消费者：测试消费每次拉取的数据量
* 对比生产者和消费者：消费者的速度更快

## 4.Kafka基准测试

基准测试是一种测量和评估软件性能指标的活动。我们可以通过基准测试，了解到软件、硬件的性能水平。主要测试负载的执行时间、传输速度、吞吐量、资源占用率等。



### 4.1 基于1个分区1个副本的基准测试

测试步骤：

1.创建Kafka集群

2.创建1个分区1个副本的topic:benchmark

3.同时运行生产者、消费者基准测试程序

4.观察结果



### 4.2 生产消息基准测试

在生产环境推荐使用生产5000W消息，这样性能数据会更准确些

```shell
bin/kafka-producer-perf-test.sh --topic benchmark --num-records 5000000 --throughput -1 --record-size 1000 --producer-props bootstrap.servers=kafkaip:端口,kafkaip:端口 acks=1
```

命令含义

```
bin/kafka-producer-perf-test.sh

--topic topic的名字

--num-records	总共指定生产的数据量（默认5000W）

--throughput	指定吞吐量——限流（-1不指定）

--record-size	record数据大小（字节）

--producer-props bootstrap.server=ip:端口,ip:端口 acks=1	指定kafka集群地址 ACK模式
```



### 4.3 消费消息基准测试

```shell
bin/kafka-consumer-perf-test.sh --broker-list kafakip:端口,ip:端口 --topic benchmark--fetch-size 1048576 --messages 5000000
```

命令含义

```
bin/kafka-consumer-perf-test.sh

--broker-list	指定kafka集群地址
--topic	指定topic的名称
--fetch-size	每次拉取的数据大小
--messages	总共要消费的消息个数
```



在虚拟机上，因为都是共享的主机的CPU、内存、网络，所以分区越多，反而效率越低。但如果是真实的服务器，分区多效率是会有明显提升的



## 5.Kafka Java程序开发

### 5.1生产者程序开发

1.创建连接

* bootstrap.servers：Kafka的服务器地址
* acks：表示当前生产者生产数据到Kafka中，Kafka会以什么样的策略返回
* key.serializer：Kafka中的消息是以key、value键值对存储的，而且生产者生产的消息是需要在网络中传到的，这里指定的是StringSerializer方式，就是以字符串方式发送（还可以使用其他 的序列化框架：Google Protobuf、Avro）
* value.serializer：同上

2.创建生产者对象

3.调用send方法发送消息（ProducerRecord，封装的是key-value的键值对）

4.调用Future.get表示等待服务端的响应

5.关闭生产者

```Java
@Slf4j
public class KafkaProducerTest {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // 1.创建用于连接kafka的properties
        Properties props = new Properties();
        props.put("bootstrap.servers", "127.0.0.1:9092");
        props.put("acks", "all");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        // 2.创建kafka的生产者KafkaProducer
        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(props);
        // 3.发送1-100的消息到指定topic
        for (int i = 0; i < 100; i++) {
            // 构建一条消息，直接new ProducerRecord
            ProducerRecord<String, String> record = new ProducerRecord<>("test1", 0, i + "", i + "");
            Future<RecordMetadata> future = kafkaProducer.send(record);
            // 调用Future的get方法等待
            future.get();
            log.info("第{}条消息生产成功", i);
        }
        // 4.关闭producer
        kafkaProducer.close();
    }

}
```



### 5.2 消费者程序开发

* group.id：消费者组的概念，可以在一个消费者组中包含多个消费者。一个组中的消费者是共同消费kafka中topic的数据的
* Kafka是一种拉消息模式的消息队列，在消费者中会有一个offset，表示从哪条消息开始开始拉取数据
* kafkaConsumer.poll：kafka的消费者是一批一批数据拉取的

```Java
public class KafkaConsumerTest {

    public static void main(String[] args) {
        // 1.创建kafka的消费者配置
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", "127.0.0.1:9092");
        // 消费者组（可以使用消费者组将若干个消费者组织到一起）共同消费Kafka中topic中的数据
        // 每一个消费者需要指定一个消费者组，如果消费者的组名是一样的，表示这几个消费者是一个组中的
        props.setProperty("group.id", "group1");
        // 自动提交offset
        props.setProperty("enable.auto.commit", "true");
        // 自动提交offset的时间间隔
        props.setProperty("auto.commit.interval.ms", "1000");
        // key、value的反序列化
        props.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        // 2.创建kafka的消费者
        KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(props);

        // 3.订阅消费者的主题
        // 指定消费者从哪个topic中拉取数据，可以指定多个
        kafkaConsumer.subscribe(Arrays.asList("test1"));

        // 4.使用一个循环不断从kafka的topic中拉取数据
//        for (int i = 0; i < 100; i++) {
        while(true){
            // 一次拉去的是一批，poll的参数是超时时间
            ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofSeconds(5));
            // 5.将记录的offset、key、value都打印出来
            for (ConsumerRecord<String, String> record : records) {
                String topic = record.topic();
                long offset = record.offset();
                String key = record.key();
                String value = record.value();
                log.info("topic:{}, offset:{}, key:{}, value:{}", topic, offset, key, value);
            }
        }
//        kafkaConsumer.close();

    }

}
```



### 5.3生产者使用异步方式生产消息

* 使用匿名内部类实现Callback接口，该接口表示kafka服务器响应给客户端，会自动调用onCompletion
  * metadata：消息的元数据（属于哪个topic，属于哪个partition，对应的offset是什么）
  * exception：是kafka生产消息时产生的异常，如果发送成功则为null

```Java
// 二、使用异步回调的方式发送消息
for (int i = 0; i < 100; i++) {
    // 构建一条消息，直接new ProducerRecord
    ProducerRecord<String, String> record = new ProducerRecord<>("test1", 0, i + "", i + "");
    kafkaProducer.send(record, new Callback() {
        @Override
        public void onCompletion(RecordMetadata metadata, Exception exception) {
            // 1.判断消息是否发送成功，exception为null时说明成功
            if (exception == null) {
                // 发送成功
                String topic = metadata.topic();
                int partition = metadata.partition();
                long offset = metadata.offset();
                log.info("topic:{}, partition:{}, offset:{}", topic, partition, offset);
            } else {
                // 发送失败
                log.error("生产者生产失败",exception);
            }
        }
    });
}
```



## 6.架构

### 6.1 Kafka重要概念

#### 6.1.1 broker

<img src="image\image-20211009155717194.png" alt="image-20211009155717194" style="zoom:67%;" />

* 一个kakfa集群通常由多个broker组成，这样才能实现负载均衡以及容错
* broker是无状态（stateless）的，它们是通过zookeeper来维护集群状态的
* 一个kafka的broker每秒可以处理数十万次读写，每个broker都可以处理TB消息而不影响性能

#### 6.1.2 zookeeper

* zk用来管理和协调broker，并且存储了kafka的元数据（例如有多少个topic、partition、consumer）
* zk服务主要用于通知生产者和消费者kafka集群中有新的broker加入或kafka集群中出现了故障的broker
* 在kafka2.8后逐渐剥离zookeeper

#### 6.1.3 producer（生产者）

* 生产者负责将数据推送给broker的topic

#### 6.1.4 consumer（消费者）

* 消费者负责从broker的topic中拉取数据，并自己进行处理

#### 6.1.5 consumer group（消费者组）

<img src="image\image-20211009160924884.png" alt="image-20211009160924884" style="zoom:67%;" />

* consumer group是kafka提供的可扩展且具有容错性的消费者机制
* 一个消费者组可以包含多个消费者
* 一个消费者组有一个唯一的ID（group id）
* 组内的消费者共同消费主题的所有分区的数据，但每个消费者消费的分区不同
* 如果一个topic只有一个分区，那么这个分区只能被组内某一个消费者消费
* 组间的消费者独立消费

#### 6.1.6 partition（分区）

<img src="image\image-20211009161511280.png" alt="image-20211009161511280" style="zoom:67%;" />

* 在kafka集群中，一个topic中的消息分布在多个partition中

#### 6.1.7 replicas（副本）

<img src="image\image-20211009161922350.png" alt="image-20211009161922350" style="zoom:67%;" />

* 副本可以确保某个服务器出故障时，数据依然可以使用
* 在kafka中，一般都会设计副本的个数>1

#### 6.1.8 topic（主题）

<img src="image\image-20211009163754549.png" alt="image-20211009163754549" style="zoom:67%;" />

* 主题是一个逻辑概念，用于生产者发布数据，消费者拉去数据
* Kafka中的主题必须要有标识符，而且是唯一的，kafka中可以有任意多数量的主题，没有数量的上限
* 在主题中的消息是有结构的，一般一个主题包含某一类消息
* 一旦生产者发送消息到主题中，这些消息就不能被更新（修改）
* 一个topic可以包含多个分区

#### 6.1.9 offset（偏移量）

<img src="image\image-20211009165106985.png" alt="image-20211009165106985" style="zoom:67%;" />

* offset记录着下一条要发送给Consumer的消息的序号
* 默认kafka将offset存储在zookeeper中
* 在一个分区中，消息是按顺序存储的，在每个分区都有一个递增id，这个id就是偏移量offset
* 偏移量在分区中才有意义，在分区间offset没有任何意义 



## 7.Kafka生产者幂等性与事务

### 7.1 幂等性

#### 7.1.1 简介

以http为例，一次请求或多次请求，得到的响应是一致的（网络超时等问题除外），换句话说，就是执行一次和执行多次操作的影响结果是一样的。

如果某个系统不具备幂等性，用户重复提交了某个表单，就可能造成不良影响。例如用户在浏览器重复点击了提交订单的按钮，会在后台生成多个一模一样的订单。

#### 7.1.2 Kafka生产者幂等性

<img src="image\image-20211011152440061.png" alt="image-20211011152440061" style="zoom: 67%;" />

在生产者生产消息时，如果出现retry，有可能会将一条消息发送多次，如果Kafka不具备幂等性，就有可能在partition中保存多条一模一样的消息

#### 7.1.3 配置幂等性

```JAVA
props.put("enable.idempotence,"true");
```

#### 7.1.4幂等性原理

为了实现kafka的幂等性，Kafka引入了Producer ID（PID）和Sequence Number的概念

* PID：每个producer在初始化时，都会分配唯一一个PID，这个PID对用户来说，是透明的
* Sequence Number：针对每个生产者（对应PID）发送到指定主题分区的消息都对应一个从0开始递增的Sequence Number

<img src="image\image-20211011165146258.png" alt="image-20211011165146258" style="zoom:67%;" />



## 8.分区和副本机制

### 8.1生产者分区写入策略

生产者消息写到topic中，kafka将依据不同策略将数据分配到不同的分区中

1. 轮询分区策略
2. 随机分配策略
3. 按key分区分配策略
4. 自定义分区策略

#### 8.1.1 轮询策略

* 默认的策略，也是使用最多的策略，可以最大限度保证所有消息平均分配到一个分区
* 如果在生产消息时，**key为null**，则使用轮询算法均衡地分区

#### 8.1.2 随机策略（不推荐）

随机策略每次都随机地将消息分配到一个分区，在早期版本中默认的策略是随机策略，但后续轮询策略表现更佳，所以基本很少会使用随机策略

#### 8.1.3 按key分配策略

key.hash() % 分区数量

按key分配策略可能会出现【数据倾斜】，例如某个key包含了大量的数据，因为key值一样，将所有的数据都分配到一个分区中，造成该分区的消息数量远大于其他分区

#### 8.1.4 乱序问题

轮询策略、随机策略都会导致一个问题，生产到kafka中的数据是乱序存储的。而按key分区可以一定程度上实现数据有序存储——也就是局部有序，但这有可能会导致数据倾斜，所以在实际生产环境中要结合实际情况来取舍

kafka中的消息是全局乱序的，局部partition是有序的

如果要实现消息总是有序的，可以将连续的消息放到一个partition。

#### 8.1.5 自定义分区策略

自定义分区策略类实现`Partitioner`接口

```java
props.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, 自定义分区策略类.class.getName());
```



### 8.2 消费者组Rebalance机制

#### 8.2.1 Rebalance再均衡

Kafka中的Rebalance成为再均衡，是Kafka中确保Consumer group下所有consumer是如何达成一致的，分配订阅的topic的每个分区的机制

Rebalance触发的时机：

1. 消费者组中consumer的数量发生了变化

   例如有新的consumer加入或是某个consumer停止了

2. 订阅的topic个数发生了变化

   消费者可以订阅多个主题，假设当前的消费者组订阅了三个主题，但有一个主题突然被删除了，此时也需要发生再均衡

3. 订阅的topic分区数发生变化

#### 8.2.2 Rebalance的不良影响

发生Rebalance时，所有的consumer将不再工作，共同参与再均衡，直到每个消费者都被成功分配所需要消费的分区为止（rebalance结束）



### 8.3 消费者分区分配策略

#### 8.3.1 Range范围分配策略

Range范围分配策略是Kafka默认的分配策略，它可以确保每个消费者消费的数量是均衡的。

注意：Range范围分配策略是针对每个topic的

**配置**

配置消费者的partition.assignment.strategy为org.apache.kafka.clients.consumer.RangeAssignor

**算法公式**

n = 分区数量 / 消费者数量

m = 分区数量 % 消费者数量

前m个消费者消费n+1个分区，剩余消费者消费n个

#### 8.3.2 RoundRobin轮询策略

RoundRobinAssignor轮询策略是将消费组内所有消费者以及消费者所订阅的所有topic的partition

按照字典序排序（topic和分区的hashcode进行排序），然后通过轮询方式逐个将分区以此分配给每个消费者

**配置**

配置消费者的partition.assignment.strategy为org.apache.kafka.clients.consumer.RoundRobinAssignor

#### 8.3.3 Sticky粘性分配策略

从kafka 0.11.x开始，引入此分配策略。主要目的：

1. 分区分配尽可能均匀
2. 在发生rebalance时，分区的分配尽可能与上一次的分配保持相同
3. 没有发生rebalance时，Sticky粘性分配策略和RoundRobin分配策略类似



### 8.4 副本机制

副本的目的就是冗余备份，当某个broker上的分区数据丢失时，依然可以保障数据可用。因为在其他broker上的副本是可用的

#### 8.4.1 producer的acks参数

对副本关系较大的就是producer配置的acks参数了，acks参数表示当生产者生产消息时，写入到副本的要求严格程度。他决定了生产者如何在性能和可靠性之间做取舍。

**配置：**

```Java
Properties props = new Properties();
props.put("acks","all");
```

#### 8.4.2 acks配置为0

不等待broker确认，直接发送下一条数据，性能最高，但可能会存在数据丢失的情况

#### 8.4.3 acks配置为1

当生产者的ACK配置为1时，生产者会等待leader副本确认接收后，才会发送下一条数据，性能中等

#### 8.4.4 acks配置为-1或all

等待所有副本已经将数据同步后，才会发送下一条数据，性能最差

#### 8.4.5 acks的选择

根据业务情况来选择ack机制，要求性能高，一部分数据丢失影响不大，可以选0/1，要求数据一定不能丢失，就得配置为-1/all

**分区**是有leader和follower概念的，为了确保消费者消费的数据是一致的，只能从分区leader去读写消息，follower做的事情就是同步数据进行备份



## 9.Kafka原理

### 9.1 分区的leader和follower

#### 9.1.1 leader和follower

在kafka中，每个topic都可以配置多个分区及多个副本。每个分区都有一个leader以及0个或多个follower，在创建topic时，kafka会将每个分区的leader均匀分配在每个broker上。正常使用时是感受不到leader、follower的存在的。事实上，所有的操作都由leader处理，而所有的follower都复制leader的日志数据文件，当leader出现故障时，follower会被选举为leader。

总结：

* Kafka中的leader负责处理读写操作，而follower只负责副本数据的同步
* 如果leader出现故障，其他follower会被重新选举为leader
* follower像一个consumer一样，拉去leader对应分区的数据，并保存到日志数据文件中

和zookeeper的区别：

* ZK的leader负责读、写，follower可以读取
* Kafka的leader负责读写、follower不能读写数据（确保每个消费者消费的数据是一致的），Kafka一个topic有多个分区leader，也可以实现负载均衡

#### 9.1.2 AR、ISR、OSR

在实际环境中，leader可能会出现故障，所以Kafka一定会选举出一个新的leader。在Kafka中可以安装状态分为三类——AR、ISR、OSR

* 分区的所有副本成为【AR】（Assigned Replicas——已分配的副本）
* 所有与leader副本保持一定程度同步的副本（包括leader副本在内）组成【ISR】（In Sync Replicas——在同步中的副本）
* 同步滞后过多的follower副本（不包括leader副本）组成【OSR】（Out of Sync Replicas）
* AR = ISR + OSR
* 正常情况下，所有的follower副本都应该与leader保持同步，即AR = ISR，OSR集合为空



### 9.2 Leader选举

#### 9.2.1 Controller介绍

* Kafka启动时，会在所有的broker中选择一个Controller
* leader和follower是针对partition，而controller是针对broker的
* 创建topic、或者添加分区、修改副本数量之类的管理任务都是由controller来完成的
* Kafka分区leader的选举，也由controller决定

#### 9.2.2 Controller的选举

* 在Kafka集群启动时，每broker都会尝试去zookeeper上注册成为Controller（ZK临时节点），但只有一个能竞争成功，其他的broker会注册该节点的监视器，一旦该临时节点发生变化，就可以进行相应的处理
* Controller也是高可用的，一旦某个broker崩溃，其他的broker也会重新注册为Controller

#### 9.2.3 Controller选举partition

* 所有的partition的leader选举都由controller决定
* controller会将leader的改变直接通过**RPC**的方式通知需要为此做出响应的broker
* controller读取到当前分区的ISR，只要有一个replica还幸存，就选择一个作为leader，否则任意选择一个replica作为leader
* 如果该partition的所有replica都已经宕机，则新的leader为-1



不通过ZK的方式来选举partition的leader的理由：

Kafka业务很多的情况下会有很多partition，如果某个broker宕机就会出现很多partition都要重新选举leader的情况，如果使用zk来选举leader，会给zk带来巨大的压力，所以kafka中leader的选举不用zk实现



### 9.3 Leader的负载均衡

#### 9.3.1 Preferred Replica

* Kafka中引入了一个叫做【preferred-replica】的概念，意思是：优先的replica
* 在ISR列表中，第一个replica就是preferred-replica
* 在没有发生重新选举的情况下，第一个分区所在的broker，就是preferred-replica
* 当某个broker挂掉后，kafka会重新分配leader，此时leader分配是不均匀的，执行以下脚本可以将preferred-replica设置为leader，均匀分配每个分区的leader

```shell
./kafka-leader-election.sh --bootstrap-server ip:端口 --topic 主题 --partition=2 --election-type preferred
```

--partition：指定需要重新分配leader的partition编号



### 9.4 Kafka生产消费数据流程

#### 9.4.1 生产者写入数据流程

<img src="image\image-20211027101656184.png" alt="image-20211027101656184" style="zoom: 67%;" />

* 生产者从zookeeper的“/brokers/topics/主题名/partitions/分区名/state”节点找到该partition的leader
* broker进程上的leader将消息写入到本地log中（数学写）
* follower从leader上拉取消息，写入到本地log，并向leader发送ACK
* leader接收到所有的ISR的Replica的ACK后，并向生产者返回ACK

#### 9.4.2 Kafka数据消费流程

<img src="image\image-20211027112749046.png" alt="image-20211027112749046" style="zoom:67%;" />

* kafka采取拉模型，由消费者自己记录消费状态，每个消费者互相独立地顺序地拉去每个分区的消息
* 消费者可以按照任意的顺序消费消息。比如，消费者可以重置到旧的偏移量，重新处理之前已经消费过的消息，或者直接跳到最近的位置，从当前的时刻开始消费。



<img src="image\image-20211027113400293.png" alt="image-20211027113400293" style="zoom:67%;" />

* 每个consumer都可以根据默认分配策略（默认RangeAssignor），获得要消费的分区
* 获取到consumer对应的offset（默认从ZK获取上一次消费的offset）
* 找到该分区的leader，拉取数据
* 消费者提交offset



### 9.5 Kafka的数据存储形式

#### 9.5.1 Kafka的数据存储形式

* 一个topic由多个分区组成
* 一个分区（partition）由多个segment（段）组成
* 一个segment（段）由多个文件组成（log、index、timeindex）

<img src="image\image-20211027174213618.png" alt="image-20211027174213618" style="zoom:67%;" />

#### 9.5.2 存储日志

kafka数据在磁盘中存储的方式

* kafka中的数据保存在/data中
* 消息是保存在【主题名-分区ID】的文件夹中
* 文件夹中包含以下文件：

| 文件名                         | 说明                                                         |
| :----------------------------- | ------------------------------------------------------------ |
| 00000000000000000000.index     | 索引文件，根据offset查找数据就是通过该索引文件来操作的       |
| 00000000000000000000.log       | 日志数据文件                                                 |
| 00000000000000000000.timeindex | 时间索引                                                     |
| leader-epoch-checkpoint        | 持久化每个partition leader对应LEO（log end offset、日志文件中下一条消息的offset） |

* 每个日志文件的文件名是骑士偏移量，因为每个分区的起始偏移量是0，所以分区日志的文件都以00000000000000000000.log开始
* 默认的每个日志文件最大为【log.segment.bytes = 1024\*1024\*1024】1G

* 为简化根据offset查找消息，kafka日志文件名设计为开始的偏移量





#### 9.5.3删除消息

#### 9.5.4删除消息

* 在Kafka中，消息是会被**定期清理**（默认7天）的。一次删除一个segment段的日志文件

* Kafka的日志管理器，会根据Kafka的配置，来决定有哪些文件可以被删除



### 9.6 消息不丢失机制

#### 9.6.1 broker数据不丢失

生产者通过分区的leader写入数据后，所有在ISR中的follower都会从leader中复制数据，这样可以确保即使leader崩溃了，其他follower的数据仍然是可用的

#### 9.6.2 生产者数据不丢失

* 生产者写入leader时，可以通过ACK机制来确保数据已经成功写入。ACK机制有三个可选配置：

  1. 配置ACK响应要求为 -1 时：表示所有节点都收到数据（leader和follower都收到数据）

  2. 配置ACK响应要求为 1 时：表示leader收到数据
  3. 配置ACK影响要求为 0 时：生产者只负责发送数据，不关心数据是否丢失（可能会产生数据丢失，但性能是最好的）

* 生产者可以采用同步和异步两种方式发送数据

  * 同步：发送一批数据给kafka后，等待kafka返回结果
  * 异步：发送一批数据给kafka，只是提供一个回调函数

说明：如果broker迟迟不给ack，而buffer又满了，开发者可以设置是否直接清空buffer中的数据

#### 9.6.3 消费者数据不丢失

在消费数据的时候，只要每个消费者记录好offset值即可，就能保证数据不丢失
