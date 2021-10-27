# Java并发

##  线程安全定义：

  当多个线程访问某个类时，不管运行时环境采用何种调度方式或者这些进程如何交替执行，并且在主调代码中不需要任何额外的同步或协同，这个类都能表现出正确的行为，那么就称这个类是线程安全的  

- 原子性：提供了互斥访问，同一时刻只能有一个线程来对它进行操作
- 可见性：一个线程对主内存的修改可以及时的被其他线程观察到
- 有序性：一个线程观察其他线程中的指令执行顺序，由于指令重排序的存在，该观察结果一般杂乱无序  



## Thread的六种状态：

1. NEW(创建)  线程被创建但还没有start()
2. RUNNABLE （涵盖了操作系统层面的可运行、运行、阻塞(读文件)三种状态）可能分到了时间片，也可能没分到时间片，还可能IO阻塞
3. BLOCKED            获取不到锁
4. WAITING            没有时限的等待，例如join()
5. TIME_WAITING  有时限的等待，例如sleep()
6. TERMINATED     结束



##### sleep()
- sleep()会使当前线程从Running进入Timed Waiting状态					
- 其他线程可以调用interrupt方法打断正在睡眠的线程，这时sleep()会抛出InterruptedException
- 睡眠结束后的线程未必会立刻得到执行，也需要等待cpu调度
- 建议使用TimeUtil的sleep()方法代替Thread的sleep()来获得更好的可读性



##### yield()

- 调用yield()会让当前线程从Running进入Runnable就绪状态，然后调度执行其他线程
- 具体的实现依赖于操作系统的任务调度器



##### join()

- 调用 `某个线程.join()`后，本线程会阻塞等待该线程结束
- `join(long n)`，n代表join的最长时间，如果到了该时间其他线程未返回，本线程也会继续



##### interrupt()

- 直接打断正常运行的线程，打断标记变为true(t.isInterrupted())
- 打断sleep、wait、join的线程，会清空打断状态，抛出InterruptedExceptopn的异常，打断标志为false 



##### isInterrupted() 和 interrupted()区别

 isInterrupted只返回打断标记，interrupted返回打断标记后会将打断标记置为false



##### 优雅停止线程：

两阶段终止：while(true)判断打断标志是否为true，如果为true进行相关结束处理并结束循环，如果为false判断线程是否抛出InterruptedExceptopn的异常，如果有则将打断标志设为true

注意：

1. 不要使用stop()停止线程，stop()方法会杀死线程，这时如果线程锁住了共享资源，那它被杀死后就再也没有机会释放锁，其他线程永远无法获取锁
2. 不要使用System.exit(int)方法停止线程，因为此方法会使整个程序（进程）停止



##### 主线程与守护线程

默认情况下，java进程需要等待所有线程都运行结束，才会结束。有一种特殊的线叫守护线程，只要其他非守护线程运行结束了，即使守护线程的代码没有执行完，也会强制结束。



## synchronized

- synchronized实际是用对象锁保证了临界区内代码的原子性，临界区内的代码对外是不可分割的，不会被线程切换所打断
- synchronized只能锁对象，加在方法上的时候是锁的this对象，加在静态方法上锁的是类对象



### Monitor

- Monitor翻译为 监视器 或 管程 
- 每个Java对象都可以关联一个Monitor对象，如果使用synchronized给对象上锁（重量级）之后，该对象头的Mark Word中就被设置为指向Monitor对象的指针
- 刚开始Monitor中Owner为null
- 当Thread-1执行synchronized(obj)就会将Monitor的所有者置为Thread-1，Monitor只能有一个Owner
- Thread-1上锁时，其他线程执行到synchronized(obj)，就会进入Monitor的EntryList，由RUNNABLE变为BLOCKED
- Thread-1执行完同步代码块内容后，会唤醒EntryListi中等待的线程来竞争锁，竞争是非公平的



### 轻量级锁：

#### 使用场景：

- 如果一个对象虽然有多线程访问，但多线程访问的时间是错开的（没有竞争），那么可以使用轻量级锁来优化

- 轻量级锁对使用者是透明的，即语法仍是synchronized

#### 过程：

1. 创建锁记录（Lock Record）对象，每个线程的栈帧都会包含一个锁记录（Lock Record）的结构，内部可以存储锁定对象的Mark Word，Lock Record内部包含lock Record地址信息，Object reference两部分

2. 将锁记录中Object reference指向被锁的对象，并用CAS尝试交换锁记录内部的地址信息和被锁对象Object的Mark Word（01是无锁状态可以加锁）

3. 如果cas替换成功，对象头中存储了锁记录地址和状态00，表示由该线程给对象加锁

4. 如果cas失败有两种情况：

   （1）如果是其他线程已经持有了该Object的轻量级锁，表明有竞争，进入锁膨胀过程
   （2）如果是自己执行了synchronized锁重入，那么再添加一条Lock Record作为重入计数，此条锁记录内部地址信息为null

代码举例：

```java
Object obj = new Object();
public static void method1(){
	synchronized(obj){
		method2();
	}
} 
public static void method2(){
	synchronized(obj){
		// TODO
	}
}
```

5. 当退出synchronized代码块解锁时，如果有有取值为null的锁记录，表示有重入，这时重置锁记录，表示重入计数减一
   如果锁记录锁记录的值不为null，这时使用cas将Mark Word的值恢复给对象头，成功说明解锁成功，失败说明锁进行了锁膨胀或已经升级为重量级锁，进入重量级锁解锁流程



### 锁膨胀

- 如果在尝试加轻量级锁的时候，CAS操作无法成功，这时一种情况就是有其他的线程为此对象加上了轻量级锁（有竞争），这时需要进行锁膨胀，将轻量级锁变为重量级锁

- 例如Thread-0为对象加上了轻量级锁，Thread-1尝试加轻量级锁失败，进入锁膨胀的过程，Thread-1为对象申请Monitor锁，Monitor的Owner指向Thread-0，对象中指向Thread-0的地址改为指向Monitor，然后Thread-1进入Monitor的EntryList阻塞BLOCKED
- 当Thread-0退出同步块解锁时，使用CAS将Mark Word的值恢复给对象头，失败。这时会进入重量级锁解锁流程，即按照Monitor地址找到Monitor对象，设置Owner为null，唤醒EntryList中的BLOCKED线程



### 自旋优化

- 重量级锁竞争的时候，还可以使用自旋来进行优化。如果当前线程自旋成功（即这时候持锁线程已经退出了同步块，释放了锁），这时当前线程就可以避免阻塞。自旋会会占用cpu时间，单核cpu自旋就是浪费，多核cpu才能发挥优势
- Java6之后锁是自适应的，如果刚刚自旋成功过，那么认为这次自旋成功的可能性会高，就多自旋几次；反之，就少自旋甚至不自旋
- Java7之后不能控制是否开启自旋功能



### 偏向锁

- 轻量级锁在没有竞争时（就自己这个线程），每次重入仍需要执行CAS操作。
- Java6中引入了偏向锁来进一步优化：只有第一次使用CAS将【线程ID】（操作系统分配的，占64位的前54位）设置到对象的Mark Word头，之后发现这个线程ID是自己的就表示没有竞争，不用重新CAS。以后只要不发生竞争，这个对象就归该线程所有
- 解锁后线程ID部分不变

#### 一个对象创建时：

- 如果开启了偏向锁（默认开启），那么对象创建后，MarkWord的最后3位值为0x05即101（倒数第3位表示是否启用偏向锁），这时他的thread、epoch、age都为0

- 偏向锁默认是延迟的，不会在程序启动时立即生效，如果想避免延迟，可以加VM参数-XX:BaisedLockingStartUpDelay=0来禁用延迟
- 如果没有创建偏向锁，那么对象创建后markword的最后三位为0x001，这时他的hashCode、age都为0，第一次用到hashCode时才会赋值
- 使用VM命令-XX:-UseBaisedLocking禁用偏向锁，Use前是-表示关闭，+表示启用，默认启用

#### 偏向锁撤销：

1. 当一个可偏向的对象调了hashCode()方法，就会撤销这个对象的偏向状态（因为偏向锁没有空间存储这个对象的hashCode）
2. 当有其他线程使用偏向锁对象且无竞争时，会将偏向锁升级为轻量级锁
3. 使用wait()/notify()，因为wait()notify()的机制是重量级锁

#### 批量重偏向：

- 如果对象虽然被多个线程访问，但没有竞争，这时偏向了线程T1的对象仍有机会重新偏向T2，重偏向会重置对象的Thread ID
- 当撤销偏向锁（某个类的对象）达到阈值19次后（阈值是20），从第20个对象开始jvm会批量给对象重偏向至加锁线程，但是前19个对象则升级为轻量级锁



#### 批量撤销偏向：

- 当撤销偏向锁达到39次后（阈值是40次），jvm会将整个类对象都变为不可偏向的，新建的对象也不可偏向

+ 39个对象开始偏向于T1，T2遍历加锁0-18被撤销升级为轻量级锁，19-38被批量重偏向至T2，然后T3遍历加锁，19-38都被升级为轻量级锁，其他对象及后续new()的对象都是不可偏向的



#### 锁消除

JIT编译代码时，对于没有竞争的锁会优化为无锁，默认是开启锁消除的

可以通过命令 java -XX:-EliminateLocks xxx.jar关闭锁消除（-代表关闭）



#### wait/notify原理

![wait/notify原理](C:\Users\admin\Desktop\学习笔记\image\image-20210615170101555.png)

+ Owner线程发现条件不满足，调用wait方法，即可进入WaitSet变为WAITING状态
+ BLOCKED和WAITING线程都处于阻塞状态，不占用CPU时间片
+ BLOCKED线程会在Owner线程释放锁时唤醒
+ WAITING线程会在Owner线程调用notify或notifyAll时唤醒，但唤醒后不代表会立刻获得锁，仍需要进入EntryList重新竞争



##### sleep(long n)和wait(long n)的区别

1. sleep是Thread方法，而wait是Object方法
2. sleep不需要强制和synchronized配合使用，但wait需要和synchronized一起使用
3. sleep在睡眠的同时，不会释放锁对象，但wait在等待的时候会释放锁对象
4. 共同点：他们的状态都会转为TIMED_WAITING



## 同步模式之保护暂停

* 有一个结果需要从一个线程传递到另一个线程，让他们关联同一个GuardedObject
* 如果有不断从一个线程到另一个线程那么可以使用消息队列
* JDK中，join的实现采用的就是此模式
* 因为要等待另一方的结果，因此归结到同步模式

![image-20210705152511956](C:\Users\admin\Desktop\学习笔记\image\image-20210705152511956.png)

join()的实现就是使用的保护性等待



## 异步模式之生产者/消费者

* 与保护性暂停中的GuardObject不同	，不需要产生和消费结果的线程一一对应
* 消费队列可以用来平衡生产和消费的线程资源
* 生产者仅产生结果数据，不关心数据该如何处理，而消费者专心处理结果数据
* 消息队列是有容量限制的，满时不会再加入数据，空时不会再消耗数据
* JDK中的各种阻塞队列采用的就是这种模式

![image-20210714173537557](C:\Users\admin\Desktop\学习笔记\image\image-20210714173537557.png)



## park()&unpark()

特点：

与Object的wait和notify相比

* wait，notify，notifyAll必须配合ObjectMonitor一起使用，而unpark不必
* park&unpark 是以线程为单位来【阻塞】和【唤醒】，而notify只能随机唤醒一个等待线程，notifyAll是唤醒所有等待线程，就不那么【精确】
* park&unpark可以先unpark，而wait&notify不能先notify

原理：

每个线程都有自己的一个park对象（由C语言实现），由三部分组成\_counter,\_cound和\_mutex



## 线程状态转换

![image-20210715152032616](C:\Users\admin\Desktop\学习笔记\image\image-20210715152032616.png)

假设有线程t

### 情况1：NEW --> RUNNABLE

* 当调用t.start()方法时，由NEW --> RUNNABLE

### 情况2：RUNNABLE <--> WAITING

t线程用synchronized(obj)获取了对象之后

* 调用obj.wait()方法时，t线程从RUNNABLE -- > WAITING
* 调用obj.notify()，obj. notifyAll()，t.interrupt()时
  * 竞争锁成功，t线程从WAITING --> RUNNING
  * 竞争锁失败，t线程从WAITING --> BLOCKED

### 情况3：RUNNABLE <--> WAITING

* 当前线程调用t.join()方法时，当前线程从RUNNABLE --> WAITING
  * 注意当前线程在t线程对象的monitor上等待

* t线程运行结束，或者调用了当前线程的interrupt()时，当前线程从WAITING --> RUNNABLE

### 情况4：RUNNABLE <--> WAITING

* 当前线程调用LockSupport.park()方法会让当前线程从RUNNABLE --> WAITING
* 调用LockSupport.unpark（目标线程）或调用了线程的interrupt()，会让目标线程从从WAITING --> RUNNABLE

### 情况5：RUNNABLE <--> TIMED_WAITING

t线程调用synchronized(obj)获取了对象锁后

* 调用obj.wait(**long n**)方法时，t线程从RUNNABLE --> TIME_WAITING
* t线程等待时间超过了n毫秒，或调用了obj.notify(),obj.notifyAll(),t.interrupt()时
  * 竞争锁成功，t线程从TIMED_WAITING --> RUNNABLE
  * 竞争锁失败，t线程从TIMED_WAITING --> BLOCKED

### 情况6：RUNNABLE <--> TIMED_WAITING

* 当前线程调用t.join(**long n**)方法时，当前线程从RUNNABLE --> TIMED_WAITING
  * 注意当前线程在t线程对象的监视器上等待
* 当前线程等待时间超过了n毫秒，或t线程运行结束，或调用了当前线程的interrupt()时，当前线程从TIMED_WAITING --> RUNNABLE

### 情况7：RUNNABLE <-->TIMED_WAITING

* 当前线程调用Thread.sleep(**long n**)，当前线程从RUNNABLE --> TIMED_WAITING
* 当前线程等待时间超过n毫秒，当前线程从TIMED_WAITING --> RUNNABLE

### 情况8：RUNNABLE <--> TIMED_WAITING

* 当前线程调用LockSupport.parkNanos(long nanos)或LockSupport.parkUtil(long mills)时，当前线程从RUNNABLE --> TIMED_WAITING
* 调用LockSupport.unpark(目标线程)或调用了线程的interrupt()，或是等待超时，会让目标线程从TIMED_WAITING --> RUNNABLE

### 情况9：RUNNABLE <--> BLOCKED

* t线程用synchronized(obj)获取对象锁时如果竞争失败，从RUNNABLE --> BLOCKED
* 持有obj锁线程的同步代码块执行完毕，会唤醒该对象上的所有BLOCKED的线程重新竞争，如果其中t线程竞争成功，从BLOCKED --> RUNNABLE，其他失败的线程仍然是BLOCKED

### 情况10：RUNNABLE <--> TERMINATED

当前线程所有代码运行完毕，进入TERMINATED



## ReentrantLock

相对于synchronized它具备以下特点

* 可中断
* 可以设置超时时间（超过超时时间就放弃竞争锁）
* 可以设置为公平锁（排队等待，而不是随机竞争）
* 支持多个条件变量

与synchronized一样，都支持可重入

基本语法

```java
// 获取锁
reentrantLock.lock();
try{
   // 临界区
} finally {
    // 释放锁
    reentrantLock.unlock();
}
```



### 可重入

可重入是指同一个线程如果首次获得了这把锁，那么因为它是这把锁的拥有者，因此有权力再次获取这把锁

如果是不可重入锁，那么第二次获得锁时，自己也会被锁挡住

### 可打断

调用`reentrantLock.lockInterruptibly`在阻塞排队等待锁的时候，可以使用`t.interrupt()`打断

### 锁超时

reentrantLock可以调用`tryLock()`或`tryLock(long timeout, TimeUtil util)`方法尝试获取锁

### 公平锁

ReentrantLock默认是不公平的，通过`ReentrantLock(boolean fair)`可以构造公平锁

### 条件变量

reentrantlock支持多个条件变量，可以根据不同的条件等待唤醒

通过`reentrantLock.newCondition()`可以创建条件，类似wait()notify()，需要先通过reentrantlock.lock()获得锁才能调用condition1.await()和condition1.signal()/conditionAll()



## 同步模式之顺序控制

### 固定顺序运行

### 交替输出
