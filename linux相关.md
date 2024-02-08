# linux命令

## grep

grep -A n xxx yyy.log   可以显示指定内容及之后的n行

grep -B n xxx yyy.log   可以显示指定内容及之前的n行

grep -C n xxx yyy.log   可以显示指定内容及前后的各n行



## SCP 远程复制命令

scp [-R] 本地服务器文件 root@ip:目标服务器地址

其中-R参数是递归复制文件夹



## 查看硬盘空间

df -TH



## java导出堆栈

jmap -dump:live,format=b,file=output.hprof pid

其中live参数是导出存活对象的意思，所以会导致一次fullgc



## java查看gc情况

jstat -gcutil pid 时间间隔（毫秒）



## java查看线程堆栈，建议多抓几次并导出到文件

jstack pid > xxx.log



## 网络抓包

tcpdump -ni eth0 port xxxx -w yyy.cap

-w参数是导出为wireshark可看的cap格式文件



## 硬盘扩容原有目录

参考文章：[linux新增磁盘或增加磁盘容量后，如何进行扩容 - 知乎 (zhihu.com)](https://zhuanlan.zhihu.com/p/602888861)

[linux系统下添加新硬盘、分区及挂载全过程详解 - 知乎 (zhihu.com)](https://zhuanlan.zhihu.com/p/117651379)



## LVM

LVM(Logical Volume Manager)逻辑卷管理，是一种将一个或多个硬盘的分区在逻辑上集合，相当于一个大硬盘来使用，当硬盘的空间不够使用的时候，可以继续将其它的硬盘的分区加入其中，是一种磁盘空间的动态管理，相对于普通的磁盘分区有很大的灵活性。

先介绍下关于LVM中几个关键术语：

* **PE：**物理区域，PV中可以用于分配的最小存储单元，可以在创建PV的时候指定，如1M, 2M, 4M, 8M, 32M, 64M。组成同一VG中所有PV的PE大小都是相同的。

* **PV：**物理卷，处于LVM最底层，可以是物理硬盘或者分区。

* **VG：**卷组，建立在PV之上，可以含有一个到多个PV。

* **LV：**逻辑卷，建立在VG之上，相当于原来分区的概念。不过大小可以动态改变。



## 查看java线程对应操作系统的堆栈

1. 先用`jstack pid`命令获取到所有线程堆栈，信息大致如下：

   ```
   "线程名" #32 daemon prio=5 os_prio=0 tid=0x00007fc7c9094000 nid=0x24cf80 runnable [0x00007fc5fc5a8000]
   ```

2. 将nid转为10进制，在操作系统中执行`cat /proc/十进制线程id/stack`即可获得该线程在操作系统的堆栈信息



## 获取linux的符号表，用于定位堆栈行数

获取vmlinux方式参考链接如下：https://www.linkedin.com/pulse/extracting-linux-kernel-executable-elf-file-from-compressed-venu

1. 如果没有vmlinux文件，可以在/boot目录下找到linuz文件（一般全名带着版本号，大概8.5MB），将其拷贝到其他目录，vmlinuz文件是vmlinux加一些其他信息后的压缩文件

2. gzip文件一般是"0x1F 0x8B 0x08"前缀开头的，而vmlinuz文件开头包含着其他信息，所以可以找到这段数据的起始

   ```sh
   sudo od -Ad -tx1 vmlinuz-5.0.0-050000-generic | grep '1f 8b 08'
   ```

   要注意，得到的结果是该行的开头，要自己算一下1f的位置

3. 通过dd命令提取出其中的符号表压缩文件

   ```shell
   sudo dd if=vmlinuz-5.0.0-050000-generic bs=1 skip=0018353 of=vmlinuz
   ```

4. 解压出vmlinux

   ```sh
   zcat vmlinuz > vmlinux
   ```

   

定位行数参考链接如下：https://blog.csdn.net/jinron10/article/details/114537465

1. 如果堆栈前面包含了地址，可以直接用命令获取

   ```sh
   addr2line -e vmlinux [地址]
   ```

2.  如果堆栈地址为[<0>]，则需要通过函数名+偏移地址计算

   先通过下列”获得函数的地址

   ```sh
   nm vmlinux | grep [函数名]
   ```

   再加上偏移地址，则得到堆栈的绝对地址，然后再用第一种情况获得代码行
