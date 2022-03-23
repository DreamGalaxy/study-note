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

