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



#### 多路复用

线程必须配合Selector才能完成对多个Channel可读写事件的监控，这称之为多路复用

* 多路复用仅针对网络IO，普通文件的IO没法利用多路复用
* 如果不用Selector的非阻塞模式，那么Channel读取到的字节很多时候都是0，而Selector 保证了有可读事件才去读取
* Channel输入的数据一旦准备好，会触发Selector的可读事件
