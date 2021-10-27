

# [Spring Cloud Gateway与后端服务问题处理总结](https://www.jianshu.com/p/ee180c78f999)

## 问题1（Connection prematurely closed BEFORE response）

后端服务偶尔报错Connection prematurely closed BEFORE response。

这个问题的产生原因和解决办法网上很容易找到。我这里只贴出问题原理图和解决办法。
 详细说明请参考[https://javazhiyin.blog.csdn.net/article/details/112914264](https://links.jianshu.com/go?to=https%3A%2F%2Fjavazhiyin.blog.csdn.net%2Farticle%2Fdetails%2F112914264)

**原理图**

![img](https:////upload-images.jianshu.io/upload_images/7908692-747f6e7c3976e39c.png?imageMogr2/auto-orient/strip|imageView2/2/w/1200/format/webp)

file



**解决办法**

1. spring cloud gateway增加jvm启动参数
    后进先出策略，确保获取的连接最大概率是最近刚被用过的



```shell
-Dreactor.netty.pool.leasingStrategy=lifo
```

1. 后端服务配置
    后端服务连接超时时长改为10秒（默认20s），超时没有数据交互则关闭连接。



```yaml
server:
  tomcat:
    connection-timeout: 10000
```

1. spring cloud gateway增加配置
    设置连接的最大空闲时长为5秒（默认NULL：响应完成即可关闭），超时则关闭连接释放资源。
    这个时长的设置要小于后端服务的连接超时时长，确保网关回收请求在后端服务回收请求之前完成。



```yaml
spring:
  cloud:
    gateway:
      httpclient:
        pool:
          max-idle-time: 5000
```

## 问题2（浪涌导致网关报错分析）

> 每天不定时出现响应失败，Nginx响应状态码出现大量的500和504，网关同样出现大量的500和504，后端服务正常。

Nginx、Gateway、Service每小时统计数如下，其中Nginx，0点的数比较少是因为日志文件截取导致缺失



![img](https:////upload-images.jianshu.io/upload_images/7908692-a6d348070989a49d.png?imageMogr2/auto-orient/strip|imageView2/2/w/620/format/webp)

file

- 经过分析得到，2点是正常情况，Nginx->Gateway->Service数都对的上。

- 1点的数据显示Service收到的请求数减少，响应时间也正常，Gateway报错分为504：Gateway响应时间超过导致（配置的60s），500：Gateway连接超过导致（配置的3s），说明Gateway请求并未到达Service端。

- 查看Nginx和Gateway的连接数出现了激增，因为外部流量瞬间涌入导致服务器连接数资源被占用。

  ![img](https:////upload-images.jianshu.io/upload_images/7908692-1a65994ba12f9814.png?imageMogr2/auto-orient/strip|imageView2/2/w/1200/format/webp)

  file

  ![img](https:////upload-images.jianshu.io/upload_images/7908692-5dd278db287922f9.png?imageMogr2/auto-orient/strip|imageView2/2/w/1200/format/webp)

  file

**优化方案**

1. 开启Gateway限流策略



```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - name: RequestRateLimiter
          args:
            redis-rate-limiter.replenishRate: 200
            redis-rate-limiter.burstCapacity: 50
            redis-rate-limiter.requestedTokens: 1
            key-resolver: "#{@userKeyResolver}"
            deny-empty-key: false
```

1. Gateway请求Service超时配置的60s，根据业务需要超过10s响应都视作无效，所以配置响应超时时间为10秒



```yaml
spring:
  cloud:
    gateway:
      httpclient:
        pool:
          response-timeout: 10s
```

1. Gateway的连接池使用弹性方式，导致服务器连接数资源被占满，改为固定方式。



```yaml
spring:
  cloud:
    gateway:
      httpclient:
        pool:
          # 线程池类型，ELASTIC：弹性，FIXED：固定
          type: FIXED
          # 超过此时间连接不使用就关闭
          max-idle-time: 5000
          # 线程池最大连接数，type=FIXED时有效
          max-connections: 200
          # 从线程池获取线程的最大等待时间，type=FIXED时有效
          acquire-timeout: 45000
```

1. 由于Gateway到不同的Service，响应时间不一样，可以在Service端的元数据信息中修改连接超时时间和响应超时时间



```yaml
spring:
  cloud:
      nacos:
          discovery:
              metadata:
                  response-timeout: 10000
                    connect-timeout: 3000
```