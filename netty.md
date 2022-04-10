# NIO基础

non-blocking io 非阻塞io

## 1、三大组件

### 1.1 Channel & Buffer

channel类似于Stream，它就是读写的**双向通道**，可以从channel将数据读入buffer，也可以将buffer的数据写入channel，而Stream要么是输入要么是输出，channel比stream更为底层



常见的Channel有：

* FileChannel（文件）
* DatagramChannel（UDP）
* SocketChannel(TCP，客户端服务器都能用)
* SeverSocketChannel(TCP，专用于服务器)



buffer则用来缓冲读写数据，常见的buffer有：

* ByteBuffer
  * MappedByteBuffer
  * DirectByteBuffer
  * HeapByteBuffer
* ShortBuffer
* IntBuffer
* LongBuffer
* FloatBuffer
* DoubleBuffer
* CharBuffer



### 1.2 Selecter

服务器的设计演化过程如下：

#### 1.2.1 多线程版设计

<img src="image\image-20220320145835638.png" alt="image-20220320145835638" style="zoom: 67%;" />

**:warning:多线程版缺点**

* 内存占用高
* 线程上下文切换成本高
* 只适合连接数少的场景



#### 1.2.2 线程池版设计

<img src="image\image-20220320150418349.png" alt="image-20220320150418349" style="zoom:67%;" />

:warning:**线程池版设计缺点**

* 阻塞模式下，线程仅能处理一个socket连接
* 仅适合短连接场景



#### 1.2.3 Selector版设计

selector的作用就是配合一个线程来管理多个channel，获取这些channel上发生的事件，这些channel工作在非阻塞模式下，不会让线程吊死在一个channel上，适合连接数特别多，低流量的场景（low traffic）

<img src="image\image-20220320150815787.png" alt="image-20220320150815787" style="zoom:67%;" />

调用selector的select()会阻塞直到channel发生了读写就绪事件，这些事件发生后，select方法就会返回这些事件交给threa来处理



## 2、ByteBuffer

示例代码：

```java
@Slf4j
public class TestByteBuffer {

    public static void main(String[] args) {
        // FileChannel
        // 获取文件的两种方式1.输入输出流 2.RandomAccessFile
        try (FileChannel channel = new FileInputStream("testBuffer.txt").getChannel()) {
            // 准备缓冲区
            ByteBuffer buffer = ByteBuffer.allocate(10);
            // 从channel读取数据，向buffer写入
            while (channel.read(buffer) != -1) {
                // 切换至读模式
                buffer.flip();
                // 打印buffer数据
                while (buffer.hasRemaining()) {
                    byte b = buffer.get();
                    log.info("{}", (char) b);
                }
                // buffer切换为写模式，否则外面的while会死循环
                buffer.clear();
            }
        } catch (IOException e) {
            log.error("", e);
        }
    }
}
```



### 2.1 ByteBuffer的正确使用方式

1. 向buffer写入数据，例如调用channel.read(buffer)
2. 调用flip()切换至**读模式**
3. 从buffer读取数据，例如调用buffer.get()
4. 调用clear()或compact()切换至**写模式**
5. 重复步骤1~4



### 2.2 ByteBuffer结构

ByteBuffer有以下几个重要属性

* capacity
* position
* limit



一开始

<img src="image\image-20220320155117768.png" alt="image-20220320155117768" style="zoom: 67%;" />

写模式下，position是写入的位置，limit等于容量，下图表示了写入4个字节后的状态

<img src="image\image-20220320155131562.png" alt="image-20220320155131562" style="zoom:67%;" />

filp()后，position切换为读取位置，limit切换为读取限制

<img src="image\image-20220320160140608.png" alt="image-20220320160140608" style="zoom:67%;" />

读取4个字节后状态如下

<img src="image\image-20220320160155807.png" alt="image-20220320160155807" style="zoom:67%;" />

clear()后position=0，limit=position状态如下

<img src="image\image-20220320160425625.png" alt="image-20220320160425625" style="zoom:67%;" />

compact()是吧未读取完的部分向前压缩，然后切换至写模式

<img src="image\image-20220320160447992.png" alt="image-20220320160447992" style="zoom:67%;" />



### 2.3 ByteBuffer常见方法

#### 2.3.1 分配空间

可以使用allocate方法为ByteBuffer分配空间，其他buffer类也有该方法

```java
// java堆内存，读写效率较低，受到GC影响 java.nio.HeapByteBuffer
ByteBuffer buf = ByteBuffer.allocate(16);
// 直接内存，读写效率较高（少一次拷贝），不会收GC影响，分配的效率低 java.nio.DirectByteBuffer
ByteBuffer buf = ByteBuffer.allocateDirect(16);
```



#### 2.3.2 向buffer写入数据

有两种方法

* 调用channel的read方法
* 调用buffer自己的put方法

```java
int readBytes = channle.read(buf);
```

和

```java
buf.put((byte) 127);
```



#### 2.3.3 从buffer读取数据

同样有两种方法

* 调用channel的write方法
* 调用buffer自己的get方法

```java
int writeBytes = channle.write(buf);
```

和

```java
byte b = buf.get();
```

get方法会使position读指针向后移动，如果想重复读取数据

* 可以调用rewind方法将position重置为0
* 或者调用get(int i)方法获取索引i的内容，它不会移动读指针



#### 2.3.4 mark和reset

mark是在读取的时候做一个标记，即使position改变，只要调用reset就能回到mark的位置



#### 2.3.5 字符串与ByteBuffer互转

```java
// 1.字符串转为ByteBuffer（需要手动调flip()方法才能读取）
ByteBuffer buffer1 = ByteBuffer.allocate(16);
buffer1.put("hello".getBytes());

//2.Charset（会自动将buffer切换为读模式）
ByteBuffer buffer2 = StandardCharsets.UTF_8.encode("hello");

//3.wrap（会自动将buffer切换为读模式）
ByteBuffer buffer3 = ByteBuffer.wrap("hello".getBytes());

buffer1.flip();
String str1 = StandardCharsets.UTF_8.decode(buffer1).toString();
String str2 = StandardCharsets.UTF_8.decode(buffer2).toString();
```



### 2.4 Scattering Reads

分散读取，有一个文本文件data.txt

```
1234567890abc
```

注意`channel.read(new ByteBuffer[]{b1, b2, b3});`，这样相比于一次性读完再进行分割，减少了数据在bytebuffer间的拷贝，这就是分散读取

```java
try (FileChannel channel = new RandomAccessFile("testBuffer.txt", "r").getChannel()) {
    ByteBuffer b1 = ByteBuffer.allocate(5);
    ByteBuffer b2 = ByteBuffer.allocate(5);
    ByteBuffer b3 = ByteBuffer.allocate(3);
    channel.read(new ByteBuffer[]{b1, b2, b3});
    b1.flip();
    b2.flip();
    b3.flip();
    System.out.println(StandardCharsets.UTF_8.decode(b1));
    System.out.println(StandardCharsets.UTF_8.decode(b2));
    System.out.println(StandardCharsets.UTF_8.decode(b3));
} catch (IOException e) {
    e.printStackTrace();
}
```



### 2.5 Gathering Writes

集中写和分散读类似，通过`channel.write(new ByteBuffer[]{b1, b2, b3});`一次性将多个buffer的内容写到一个文件中，避免手动拼接时创建额外的bytebuffer

```java
ByteBuffer b1 = StandardCharsets.UTF_8.encode("hello");
ByteBuffer b2 = StandardCharsets.UTF_8.encode("world");
ByteBuffer b3 = StandardCharsets.UTF_8.encode("你好");

try (FileChannel channel = new RandomAccessFile("words.txt", "rw").getChannel()) {
    channel.write(new ByteBuffer[]{b1, b2, b3});
} catch (IOException e) {
    e.printStackTrace();
}
```



### 2.6 黏包与半包

#### 黏包

发送方在发送数据时，并不是一条一条地发送数据，而是**将数据整合在一起**，当数据达到一定数量后再一起发送，这就会导致多条信息被放在一个缓冲区中被发送出去 



#### 半包

接收方的缓冲区大小是有限的，当接收方缓冲区满了以后，就需要将信息截断，等缓冲区空了以后再继续放入数据，这就会发生一段完整的数据最后被截断的现象

## 3、文件编程

### 3.1 FileChannel

**:warning:注意：**FileChannel只能工作在阻塞模式下



#### 获取

不能直接打开FileChannel，必须通过FileInputStream、FileOutputStream或者RandomAccessFile来获取FileChannel，它们都有getChannel方法

* 通过FileInputChannel获取的channel只能读
* 通过FileOutputChannel获取的channel只能写
* 通过RandomAccessFile是否能读写根据构造RandomAccessFile时的读写模式决定



#### 读取

会从channel读取数据填充ByteBuffer，返回值表示读到了多少字节，-1表示到达了文件的末尾

```java
int readBytes = channel.read(buffer);
```



#### 写入

写入的正确方式如下：

```java
ByteBuffer buffer = ...;
// 存入数据
buffer.put(...);
// 切换读写模式
buffer.flip();

while(buffer.hasRemaining()){
    channel.write(buffer);
}
```

在while中调用channel.write是因为write方法并不能保证一次将buffer中的内容全部写入channel



#### 关闭

channel必须关闭，不过调用了FileInputStream、FileOutputStream或者RandomAccessFile的close方法会间接地调用channel的close方法



#### 位置

获取当前位置

```java
long pos = channel.position();
```

设置当前位置

```java
long newPos = ...;
channel.position(newPos);
```

设置当前位置时，如果设置为文件的末尾：

* 此时读取会返回-1
* 此时写入会追加内容，但要注意如果postition超过了文件末尾，再写入时在新内容和原末尾之间会有空洞（00）



#### 大小

使用size方法获取文件的大小



#### 强制写入

操作系统出于性能的考虑，会将数据缓存，不是立刻写入磁盘。可以调用force(true)方法将文件内容和元数据（文件的权限等信息）立刻写入磁盘



### 3.2 两个Channel传输数据

效率高，底层会利用操作系统的零拷贝进行优化，最大传输2g数据

```java
String from = "helloworld/from.txt";
String to = "helloworld/to.txt";
long start = System.nanoTime();
try (FileChannel fromChannel = new FileInputStream(from).getChannel();
     FileChannel toChannel = new FileOutputStream(to).getChannel()) {
    // 效率高，底层会利用操作系统的零拷贝进行优化，最大传输2g数据
    fromChannel.transferTo(0, fromChannel.size(), toChannel);
    // 二选一
    //toChannel.transferFrom(fromChannel, 0, fromChannel.size());
} catch (IOException e) {
    e.printStackTrace();
}
log.info("transferTo耗时：{}", (System.nanoTime() - start) / 1000000);
```



对于大文件，使用循环的方式

```java
String from = "helloworld/from.txt";
String to = "helloworld/to.txt";
long start = System.nanoTime();
try (FileChannel fromChannel = new FileInputStream(from).getChannel();
     FileChannel toChannel = new FileOutputStream(to).getChannel()) {
    // 效率高，底层会利用操作系统的零拷贝进行优化
    long size = fromChannel.size();
    for (long left = size; left > 0; ) {
        left -= fromChannel.transferTo(size - left, left, toChannel);
        // 二选一
    	//left -= toChannel.transferFrom(fromChannel, size - left, left);
    }
} catch (IOException e) {
    e.printStackTrace();
}
log.info("transferTo耗时：{}", (System.nanoTime() - start) / 1000000);
```



### 3.3 Path

jdk7引入了Path和Paths类

* Path用来表示文件路径
* Paths是工具类，用来获取Path实例

```java
// 相对路径 使用user.dir环境变量来定位1.txt
Path source = Paths.get("1.txt");

// 绝对路径 代表了d:\1.txt
Path source = Paths.get("d:\\1.txt");

// 绝对路径 代表了d:\1.txt
Path source = Paths.get("d:/1.txt");

// 代表了 d:\data\projects
Path source = Paths.get("d:\\data","projects");
```

* `.`代表了当前路径
* `..`代表了上一级路径

`path.normalize()`会将`.` `..`等进行解析



### 3.4 Files

检查文件是否存在

```java
Path path = Paths.get("helloworld/data.txt");
log.info("{}", Files.exist(path));
```



创建一级目录

```java
Path path = Paths.get("helloworld/d1");
Files.createDirectory(path);
```

* 如果目录已经存在，会抛异常FileAlreadyExistsException
* 不能一次创建多级目录，否则会抛NoSuchFileException



创建多级目录用

```java
Path path = Paths.get("helloworld/d1/d2");
Files.createDirectories(path);
```



拷贝文件（channel的transferTo底层不一样）

```java
Path source = Paths.get("helloworld/data.txt");
Path target = Paths.get("helloworld/target.txt");
Files.copy(source, target);
```

* 如果文件已存在会抛异常FileAlreadyExistsException

如果希望用source覆盖掉target，需要使用StandardCopyOption来控制

```java
Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
```



移动文件

```java
Path source = Paths.get("helloworld/data.txt");
Path target = Paths.get("helloworld/target.txt");
Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
```

StandardCopyOption.ATOMIC_MOVE保证文件移动的原子性



删除文件

```java
Path target = Paths.get("helloworld/target.txt");
Files.delete(target);
```

* 如果文件不存在会抛异常NoSuchFileException



删除目录

```java
Path target = Paths.get("helloworld/d1");
Files.delete(target);
```

* 如果还有内容，会抛异常DirectoryNotEmptyException



`Files.walkFileTree(Paths.get("helloworld/d1"), SimpleFileVisitor<Path>)`方法可以遍历目录下的所有文件和目录，通过重新写preVisitDirectory、visitFile、visitFileFailed、postVisitDirectory方法可以遍历达到递归删除、输出文件名、计数等操作



Files.walk(Paths.get("helloworld/d1"))会返回流，可以通过forEach等方法遍历



## 4、 网络编程

### 4.1 阻塞 vs 非阻塞

#### 阻塞

* 在没有数据可读时，包括数据复制过程中，线程必须阻塞等待，不会占用cpu，但线程相当于闲置
* 32位jvm一个线程320k，64位jvm一个线程1024k，为了减少线程数，需要采用线程池技术
* 即使用了线程池技术，如果有很多连接建立，但长时间inactive，会阻塞线程池中的所有线程



#### 非阻塞

* 在某个channel没有可读事件时，线程不必阻塞，他可以去处理其他有可读事件的channel
* 数据复制过程中，线程实际还是阻塞的（AIO改进的地方）
* 写数据时，线程只是等待数据写入channel即可，无需等待channel通过网络把数据发送出去



### 4.2 Selector多路复用

线程必须配合Selector才能完成对多个Channel可读写事件的监控，这称之为多路复用

* 多路复用仅针对网络IO，普通文件的IO没法利用多路复用
* 如果不用Selector的非阻塞模式，那么Channel读取到的字节很多时候都是0，而Selector 保证了有可读事件才去读取
* Channel输入的数据一旦准备好，会触发Selector的可读事件



#### 流程

1. 通过`Selector selector = Selector.open();`获得Selector

2. 将通道设为非阻塞模式，并注册到选择器中，然后设置感兴趣的事件

   * channel必须工作在非阻塞模式下
   * FileChannel没有非阻塞模式，所以不能配合Selector一起使用
   * 绑定的事件类型有：
     * connect - 客户端连接成功时触发
     * accept - 服务端成功接受连接时触发
     * read - 数据可读入时触发，有因为接收能力弱，数据暂不能读入的情况
     * write - 数据可写出时触发，有因为发送能力弱，数据暂时不能写出的情况

   ```java
   // channel设置为非阻塞
   ssc.configureBlocking(false);
   // 2.建立selector和channel的联系（channel注册到selector上），第二个参数表示关注事件的值，0表示不关注任何事件
   SelectionKey sscKey = ssc.register(selector, 0, null);
   // 设置key只关注 accept 事件
   sscKey.interestOps(SelectionKey.OP_ACCEPT);
   ```

3. 通过Selector监听事件，并获得就绪通道个数，若没有就绪通道，线程会被阻塞

   * 阻塞直到绑定事件发生

     ```java
     int count = selector.select();
     ```

   * 阻塞直到绑定事件发生，**或是超时**（时间单位为ms）

     ```java
     int count = selector.select(long timeout);
     ```

   * 不会阻塞，也就是不管有没有事件，立刻返回，自己根据返回值检查是否有事件

     ```java
     int count = selector.selectNow();
     ```

4. 获取就绪事件并**得到对应的通道**，然后进行处理

5. 当处理完一个事件后，一定要调用迭代器的remove方法移除对应事件，否则会出现错误。

6. 断开连接。当客户端与服务器之间**连接断开时，会给服务端发送一个读事件**，对正常断开和异常断开需要用不同的方式进行处理

   * **正常断开**时，服务端的channel.read(buffer)方法的返回值为-1，所以当结束到返回值为-1时，需要调用key的cancel方法取消此事件，并在取消后移除该事件
   * **异常断开**时，会抛出IOException异常，使用try-catch捕获异常并在异常处理中调用`key.cancel()`即可



<font color="red">**注意**</font>：事件发生后**要么处理，要么取消（cancel）**，不能什么都不做，<font color='red'>**否则下次该事件仍会发生**</font>，这是因为nio底层使用的是水平触发

#### 示例代码

```java
// 1.创建selector，管理多个channel
Selector selector = Selector.open();

ServerSocketChannel ssc = ServerSocketChannel.open();
// channel设置为非阻塞
ssc.configureBlocking(false);
// 2.建立selector和channel的联系（channel注册到selector上），0表示不关注任何事件
SelectionKey sscKey = ssc.register(selector, 0, null);
// 设置key只关注 accept 事件
sscKey.interestOps(SelectionKey.OP_ACCEPT);
log.info("register key:{}", sscKey);

ssc.bind(new InetSocketAddress(8080));
while (true) {
    // 3. select方法，在没有事件发生时会阻塞线程，有事件发生才会恢复运行
    // 但是事件未处理时，他不会阻塞，事件发生后要么处理要么取消，不能置之不理
    selector.select();
    // 4.处理事件，selectedKeys内部包含了所有发生的事件，事件发生后会向集合中添加key，但不会主动删除key
    Iterator<SelectionKey> it = selector.selectedKeys().iterator();
    while (it.hasNext()) {
        SelectionKey key = it.next();
        // 拿到key后将selectedKey集合中的key删除，否则会一直存在每次循环都会读取到
        it.remove();
        log.info("key:{}", key);
        // 5. 区分事件类型
        if (key.isAcceptable()) {
            // 是什么channel取决于这个key是什么channel注册的
            ServerSocketChannel channel = (ServerSocketChannel) key.channel();
            SocketChannel sc = channel.accept();
            sc.configureBlocking(false);
            SelectionKey scKey = sc.register(selector, 0, null);
            scKey.interestOps(SelectionKey.OP_READ);
        } else if (key.isReadable()) {
            try {
                SocketChannel channel = (SocketChannel) key.channel();
                ByteBuffer buffer = ByteBuffer.allocate(16);
                // 从channel读取数据，向buffer写入
                if (channel.read(buffer) == -1) {
                    // 客户端正常断开时，read返回的值是-1
                    key.cancel();
                    channel.close();
                } else {
                    // 切换至读模式
                    buffer.flip();
                    ByteBufferUtil.debugRead(buffer);
                    buffer.clear();
                }
                channel.read(buffer);
                buffer.flip();
            } catch (IOException e) {
                // 客服端异常断开后，需要将key取消（从selector管理的channel集合中删除）
                key.cancel();
                e.printStackTrace();
            }
        }
    }
}
```



#### select何时不阻塞

* 事件发生时
  * 客户端发起连接请求，会触发accept事件
  * 客户端发送数据过来，客户端正常、异常关闭时，都会触发read事件，另外如果发送的数据大于buffer缓冲区，会触发多次读事件
  * channel可写，会触发write事件
  * 在linux下nio bug发生时
* 调用selector.wakeup()
* 调用selectot.close()
* selector所在线程interrupt



#### 处理消息的边界

<img src="image\image-20220409164153878.png" alt="image-20220409164153878" style="zoom:67%;" />

* 一种思路是固定消息长度，数据包大小一样，服务器按照预定长度读取，缺点是浪费带宽
* 另一种思路是按分隔符拆分，缺点是效率低
* TLV格式，及Type类型、Length长度、Value数据，类型和长度已知的情况下，就可以方便获取消息大小，分配合适的buffer，缺点是buffer需要提前分配，如果内容过大，则影响server吞吐量
  * Http1.1是TLV格式
  * Http2.0是LTV格式



下文边界处理演示采用第二种按分隔符拆分

Channel的regiser方法还有第三个参数“附件”，可以向其中放入一个Object类型的对象，该对象会与登记的channel及其对应的SelectionKey进行绑定，可以从selectionKey中获取channel对应的附件

```java
// 第三个参数是附件，随着channel将其注册到selector上，和channel一一对应
ByteBuffer buffer = ByteBuffer.allocate(16);
SelectionKey scKey = sc.register(selector, 0, buffer);
// 后续获取
ByteBuffer buffer = (ByteBuffer) key.attachment();
// 对附件进行替换
ByteBuffer newBuffer = ByteBuffer.allocate(buffer.capacity() << 1);
key.attach(newBuffer);
```

我们可以给每个channel添加一个ByteBuffer附件，避免不同channel使用同一个buffer出现冲突，当channel中的数据大于缓冲区时，则对缓冲区进行扩容操作，这里示例代码的做法是：channel调用compact()方法压缩后，buffer的position和limit仍相等，说明buffer满了并且不足以读取到一条完整的消息，此时创建一个新的ByteBuffer并且容量为原buffer的2倍，将原buffer的数据放入新buffer中，再通过key.attach用新buffer代替旧buffer作为附件与channel绑定

改造后的完整代码如下:

```java
public static void main(String[] args) throws IOException {
    // 1.创建selector，管理多个channel
    Selector selector = Selector.open();

    ServerSocketChannel ssc = ServerSocketChannel.open();
    // channel设置为非阻塞
    ssc.configureBlocking(false);
    // 2.建立selector和channel的联系（channel注册到selector上），第二个参数表示关注事件的值，0表示不关注任何事件
    SelectionKey sscKey = ssc.register(selector, 0, null);
    // 设置key只关注 accept 事件
    sscKey.interestOps(SelectionKey.OP_ACCEPT);
    log.info("register key:{}", sscKey);

    ssc.bind(new InetSocketAddress(8080));
    while (true) {
        // 3. select方法，在没有事件发生时会阻塞线程，有事件发生才会恢复运行
        // 但是事件未处理时，他不会阻塞，事件发生后要么处理要么取消，不能置之不理
        selector.select();
        // 4.处理事件，selectedKeys内部包含了所有发生的事件，事件发生后会向集合中添加key，但不会主动删除key
        Iterator<SelectionKey> it = selector.selectedKeys().iterator();
        while (it.hasNext()) {
            SelectionKey key = it.next();
            // 拿到key后将selectedKey集合中的key删除，否则会一直存在每次循环都会读取到
            it.remove();
            log.info("key:{}", key);
            // 5. 区分事件类型
            if (key.isAcceptable()) {
                // 是什么channel取决于这个key是什么channel注册的
                ServerSocketChannel channel = (ServerSocketChannel) key.channel();
                SocketChannel sc = channel.accept();
                sc.configureBlocking(false);
                ByteBuffer buffer = ByteBuffer.allocate(16);
                // 第三个参数是附件，随着channel将其注册到selector上，和channel一一对应
                SelectionKey scKey = sc.register(selector, 0, buffer);
                scKey.interestOps(SelectionKey.OP_READ);
            } else if (key.isReadable()) {
                try {
                    SocketChannel channel = (SocketChannel) key.channel();
                    // 从selectionKey上关联的附件
                    ByteBuffer buffer = (ByteBuffer) key.attachment();
                    // 从channel读取数据，向buffer写入
                    if (channel.read(buffer) == -1) {
                        // 客户端正常断开时，read返回的值是-1
                        key.cancel();
                        channel.close();
                    } else {
                        split(buffer);
                        // 如果buffer还是满的，说明容量不够读取一条数据，进行容量翻倍
                        if (buffer.position() == buffer.limit()) {
                            ByteBuffer newBuffer = ByteBuffer.allocate(buffer.capacity() << 1);
                            buffer.flip();
                            newBuffer.put(buffer);
                            // 替换掉原有channel绑定的附件buffer
                            key.attach(newBuffer);
                        }
                    }
                    channel.read(buffer);
                    buffer.flip();
                } catch (IOException e) {
                    // 客服端异常断开后，需要将key取消（从selector管理的channel集合中删除）
                    key.cancel();
                    e.printStackTrace();
                }
            }
        }
    }
}

private static void split(ByteBuffer source) {
    source.flip();
    for (int i = 0; i < source.limit(); i++) {
        // 通过分隔符\n找到完整信息，并且get方法不会改变position
        if (source.get(i) == '\n') {
            int length = i + 1 - source.position();
            ByteBuffer target = ByteBuffer.allocate(length);
            for (int j = 0; j < length; j++) {
                target.put(source.get());
            }
            ByteBufferUtil.debugAll(target);
        }
        source.compact();
    }
}
```



#### ByteBuffer大小分配

* 每个channel都需要记录可能被切分的消息，因为ByteBuffer不是线程安全的，因此需要为每个channel维护一个独立的byteBuffer
* ByteBuffe不能太大，比如一个ByteBuffer大小为1Mb，要支持一百万的连接就需要1Tb的内存，这是不实际的，所以需要设计大小可变的ByteBuffer
  * 一种思路是先分配一个较小的buffer，例如4k，当发现容量不够时再分配8k，并将数据进行拷贝。优点是消息连续易处理，缺点是数据拷贝耗费性能（还需要考虑缩容）
  * 另一种思路是用多个数组组成buffer，一个数组不够用就将多出来的内容写入新的数组，缺点是消息存储不连续解析复杂，优点是避免了数据拷贝引起的性能损耗



#### 一次写不完的事件

服务器通过buffer向通道写入数据时，可能因为通道容量小于buffer中的数据大小，导致无法一次性将buffer中的数据全部写入到channel中，这便需要分多次写入，具体步骤如下：

1. 执行一次写操作，将buffer中的内容写入到SocketChannel，然后判断buffer中是否还有数据

2. 如果还有数据，则需要将SocketChannel注册到Selector中，并关注写事件，同时将未写完的buffer作为附件一起放入到SelectionKey中

```java
int write = socket.write(buffer);
// 通道中可能无法放入缓冲区中的所有数据
if (buffer.hasRemaining()) {
    // 注册到Selector中，关注可写事件，并将buffer添加到key的附件中
    socket.configureBlocking(false);
    socket.register(selector, SelectionKey.OP_WRITE, buffer);
}
```

3. 添加写事件的相关操作key.isWriteable()，对buffer再次进行写操作
4. 每次写后需要判断Buffer中是否还有数据（是否写完），若写完，需要移除SelectionKey中的buffer附件，避免其占用过多的内存，同时还需要移除对写事件的关注

整体代码如下：

```java
public class WriteServer {
    public static void main(String[] args) {
        try(ServerSocketChannel server = ServerSocketChannel.open()) {
            server.bind(new InetSocketAddress(8080));
            server.configureBlocking(false);
            Selector selector = Selector.open();
            server.register(selector, SelectionKey.OP_ACCEPT);
            while (true) {
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    // 处理后就移除事件
                    iterator.remove();
                    if (key.isAcceptable()) {
                        // 获得客户端的通道
                        SocketChannel socket = server.accept();
                        // 写入数据
                        StringBuilder builder = new StringBuilder();
                        for(int i = 0; i < 500000000; i++) {
                            builder.append("a");
                        }
                        ByteBuffer buffer = StandardCharsets.UTF_8.encode(builder.toString());
                        // 先执行一次Buffer->Channel的写入，如果未写完，就添加一个可写事件
                        int write = socket.write(buffer);
                        System.out.println(write);
                        // 通道中可能无法放入缓冲区中的所有数据
                        if (buffer.hasRemaining()) {
                            // 注册到Selector中，关注可写事件，并将buffer添加到key的附件中
                            socket.configureBlocking(false);
                            socket.register(selector, SelectionKey.OP_WRITE, buffer);
                        }
                    } else if (key.isWritable()) {
                        SocketChannel socket = (SocketChannel) key.channel();
                        // 获得buffer
                        ByteBuffer buffer = (ByteBuffer) key.attachment();
                        // 执行写操作
                        int write = socket.write(buffer);
                        System.out.println(write);
                        // 如果已经完成了写操作，需要移除key中的附件，同时不再对写事件感兴趣
                        if (!buffer.hasRemaining()) {
                            key.attach(null);
                            key.interestOps(0);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```



### 4.3 优化

####  :bulb: 利用多线程优化

一个事件处理事件长时会导致其他事件阻塞等待，可以充分利用多核cpu，分两组选择器

* 单线程配一个选择器，专门处理accept事件（Boss）
* 创建cpu核心数的线程，每个线程配一个选择器，轮流处理read事件（Worker）



#### :bulb: 获取cpu个数

通过`Runtime.getRuntime().availableProcessors()`方法可以获取可用核心数，但在docker下，因为容器不是物理隔离的，会拿到物理cpu个数，而不是容器申请时的个数，这个问题直到jdk10才修复，使用UseContainerSupport配置，默认开启



## 5、NIO vs BIO

### 5.1 stream vs channel

* stream不会自动缓冲数据，channel会利用系统提供的发送缓冲区、接收缓冲区（更为底层）

* stream仅支持阻塞API，channel同时支持阻塞、非阻塞API，网络channel可配合selector实现多路复用
* 二者均为全双工，即读写可以同时进行



### 5.2 IO模型

当调用一次channel.read或stream.read后，会切换至操作系统内核态来完成真正数据读取，而读取又分为两个阶段，分别为：

* 等待数据阶段

* 复制数据阶段

  <img src="image\image-20220409200200217.png" alt="image-20220409200200217" style="zoom: 67%;" />



### 5.3 零拷贝

#### 传统IO问题

传统的IO将一个文件写出

```java
File f = new File("data.txt");
RandomAccessFile file = new RandomAccessFile(file, "r");

byte[] buf = new byte[(int) file.length()];
file.read(buf);

Socket socket = ...;
socket.getOutputStream().write(buf);
```



内部工作流程如下：

<img src="image\image-20220409202531197.png" alt="image-20220409202531197" style="zoom:67%;" />

1. java本身不具备IO读写能力，因此read方法调用后，要从java程序的**用户态切换至内核态**，去调用操作系统（Kernel）的读能力，将数据读入**内核缓冲区**。这期间用户线程阻塞，操作系统使用DMA（Direct Memory Access）来实现文件读，期间也不会使用cpu
2. **从内核态切换回用户态**，将数据从**内核缓冲区读入用户缓冲区**（即byte[] buf），这期间cpu会参与拷贝，无法利用DMA
3. 调用write方法，这时将数据从**用户缓冲区**（byte[] buf）**写入socket缓冲区**，cpu会参与拷贝
4. 然后向网卡写数据，java同样不具备这个能力，因此又需要从**用户态切换至内核态**，调用操作系统的写能力，使用DMA将**socket缓冲区的数据写入网卡**，不使用cpu



可以发现中间环节较多，java的IO实际不是物理设备级别的读写，而是缓存的复制，底层真正的读写是操作系统完成的

* 用户态与内核态的切换发生了3次，这个操作比较消耗资源
* 数据共拷贝了4次



#### NIO优化

通过DirectByteBuf

* ByteBuffer.allocate(10) 	这是堆内存HeapByteBuffer，是java的内存
* ByteBuffer.allocateDirect(10)	这是直接内存DireByteBuffer，是操作系统的内存，java程序和操作系统都能访问

<img src="image\image-20220409204344573.png" alt="image-20220409204344573" style="zoom:67%;" />

大部分步骤与优化前相同，但有一点：java可以使用DirectByteBuf将堆外内存映射到jvm内存中来直接访问使用

* 这块内存不受jvm垃圾回收影响，因此内存地址固定，有助于IO读写
* java中的DirectByteBuf对象仅维护了此内存的虚引用，内存回收分成两步
  * DirectByteBuf对象被垃圾回收，将虚引用加入引用队列
  * 通过专门的线程访问引用队列，根据虚引用释放堆外内存
* 减少了一次数据拷贝，**用户态与内核态的切换次数没有减少**



**进一步优化**（底层采用了linux2.1后提供的sendFile方法），java中对应着两个channel调用transferTo/transferFrom方法拷贝数据

<img src="image\image-20220409205021793.png" alt="image-20220409205021793" style="zoom:67%;" />

1. java调用transferTo方法后，要从java程序的**用户态切换至内核态**，使用DMA将数据**读入内核缓冲区**，不会使用cpu
2. 数据从**内核缓冲区传输到socket缓冲区**，cpu会参与拷贝
3. 最后使用DMA将**socket缓冲区的数据写入到网卡**，不会使用cpu

可以发现：

* 只发生了一次用户态与内核态的切换
* 数据拷贝了3次



进一步优化（linux2.4）

<img src="image\image-20220409205538245.png" alt="image-20220409205538245" style="zoom:67%;" />

1. java调用transferTo方法后，要从java程序的**用户态切换至内核态**，使用DMA将数据读入内核缓冲区，不会使用cpu
2. 只会将一些offset和length**信息拷入socket缓冲区**，几乎无消耗
3. 使用DMA将**内核缓冲区的数据写入网卡**，不会使用cpu

整个过程只发生了一次用户态与内核态的切换，数据拷贝了2次。所谓的<font color='red'>【零拷贝】并不是真正的无拷贝，而是不会再将数据重复拷贝到jvm中</font>，零拷贝的优点有：

* 更少的用户态与内核态的切换
* 不利用cpu计算，减少cpu缓存伪共享
* 零拷贝适合小文件传输



### 5.4 AIO

AIO用来解决数据复制阶段的阻塞问题

* 同步意味着，在进行读写操作时，线程需要等待结果，还是相当于闲置
* 异步意味着，在进行读写操作时，线程不必等待结果，而是由操作系统通过回调的方式由另外的线程获得结果

> 异步模型需要底层操作系统（Kernel）提供支持
>
> * Windows系统通过IOCP实现了真正的异步IO
> * Linux系统异步IO在2.6版本引入，但其底层实现还是用多路复用模拟了异步IO，性能没有优势

