# Spring5

## 一、容器与Bean

### 1.1、BeanFactory的作用

* 表面上只有getBean，实际上**控制反转、基本的依赖注入、直至Bean的生命周期各种功能**，都由他的实现类提供

* beanFactory中有一个singletonObjects对象，其中存储着所有单例bean

```java
// 获取beanFactory中存储bean单例的map
Field singletonObjects = DefaultSingletonBeanRegistry.class.getDeclaredField("singletonObjects");
singletonObjects.setAccessible(true);
ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
Map<String, Object> map = (Map<String, Object>) singletonObjects.get(beanFactory);
```



### 1.2、BeanFactory与ApplicationContext的关系

idea里`ctrl + alt + U`可以查看类图

<img src="image\image-20240129165804126.png" alt="image-20240129165804126" style="zoom:50%;" />

BeanFactory是ApplicationContext的父接口，BeanFactory才是Spring的核心容器，主要的ApplicationContext实现都【组合】了他的功能。

ApplicationContext在实现BeanFactory接口外，还实现了其他接口：

* MessageSource是国际化相关的

* ApplicationEventPublisher是发布各种事件的事件发布器，方便代码解耦

  ```java
  context.publishEvent(new ApplicationEvent(context) {
      @Override
      public Object getSource() {
          return super.getSource();
      }
  });
  ```

* ResourcePatternResolver是通过通配符匹配资源的

  ```java
  Resource[] resources = context.getResources("classpath:application.yml");
  // 要读取内部jar的资源则需要在classpath后加*
  Resource[] resources2 = context.getResources("classpath*:META-INF/spring.factories");
  
  ```

* EnvironmentCapable是环境相关的，如配置文件、系统的环境变量

  ```java
  System.out.println(context.getEnvironment().getProperty("JAVA_HOME"));
  System.out.println(context.getEnvironment().getProperty("server.port"));
  ```

  









实现类中包含一个beanFactory的成员对象

<img src="image\image-20240129171417151.png" alt="image-20240129171417151" style="zoom: 50%;" />



### 1.3、容器实现

#### 1.3.1、BeanFactory实现的特点

bean的创建是由beanFactory根据bean的定义来创建的

bean的定义包括class，scope（单例、多例），初始化方法，销毁方法等



beanFactory的功能比较有限，许多功能都是后处理器进行增强补充的，例如BeanFactoryPostProcessor可以处理@Configuration、@Bean注解，BeanPostProcessor可以处理@Autowired注解等，



对于@Autowired注解，如果有多个相同类型的bean，会根据成员变量名与实现类进行匹配。

如果同时写了@Autowired和@Resource，则会优先按照@Autowired的匹配逻辑来，因为在默认添加后处理器时，autowired的后处理器是先添加的commonAnnotation的是后添加的



beanFactory不会做的事：

1. 不会主动调用BeanFactory后处理器
2. 不会主动添加Bean后处理器
3. 不会主动初始化单例
4. 不会解析${}（占位符） 和 #{}（spring的EL表达式）



单例bean默认是延迟加载的，即在第一次使用时才被创建，如果需要单例bean被提前加载，可以执行

```java
beanFactory.preInstantiateSingletons();
```



beanFactory的使用代码如下：

```java
public class TestBeanFactory {

    public static void main(String[] args) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        // bean的定义包括（class，scope（单例、多例），初始化方法，销毁方法等）
        AbstractBeanDefinition beanDefinition =
                BeanDefinitionBuilder.genericBeanDefinition(Config.class).setScope("singleton").getBeanDefinition();
        // 注册bean
        beanFactory.registerBeanDefinition("config", beanDefinition);

        System.out.println("注册bean后beanFactory包含的bean：");
        for (String name : beanFactory.getBeanDefinitionNames()) {
            System.out.println(name);
        }

        // 添加一些常用的后处理器到beanFactory中，但未工作
        AnnotationConfigUtils.registerAnnotationConfigProcessors(beanFactory);

        System.out.println("\n添加后处理器后beanFactory包含的bean：");
        for (String name : beanFactory.getBeanDefinitionNames()) {
            System.out.println(name);
        }

        System.out.println("\n与后处理器建立联系");
        // 使用BeanFactory与后处理器建立联系（在bean创建时要执行哪些后处理器），主要功能是补充一些bean的定义
        // 添加的顺序也决定了生效的顺序
        beanFactory.getBeansOfType(BeanFactoryPostProcessor.class).values()
                .forEach(beanFactoryPostProcessor -> {
                    // 执行beanFactory后处理器
                    beanFactoryPostProcessor.postProcessBeanFactory(beanFactory);
                });

        System.out.println("\n后处理器执行后beanFactory包含的bean：");
        for (String name : beanFactory.getBeanDefinitionNames()) {
            System.out.println(name);
        }

        // 直接使用自定义的bean1，可以发现并没有进行依赖注入，bean2为null
        // System.out.println(beanFactory.getBean(Bean1.class).getBean2());

        // 使用Bean后处理器，针对bean的生命周期的各个阶段进行扩展，例如@Autowired，@Resource 等
        beanFactory.getBeansOfType(BeanPostProcessor.class).values()
                .forEach(beanFactory::addBeanPostProcessor);


        // beanFactory中的只是一些bean的描述信息，bean只有在第一次被使用的时候才会被创建（默认延迟加载）
        // 可以调用下面的方法提前实例化bean
        beanFactory.preInstantiateSingletons();

        // 使用Bean后处理器后再使用自定义的bean1，可以发现成功进行依赖注入
        System.out.println(beanFactory.getBean(Bean1.class).getBean2());
    }

    @Configuration
    static class Config {
        @Bean
        public Bean1 bean1() {
            return new Bean1();
        }

        @Bean
        public Bean2 bean2() {
            return new Bean2();
        }
    }

    static class Bean1 {
        private static final Logger log = LoggerFactory.getLogger(Bean1.class);

        @Autowired
        private Bean2 bean2;

        public Bean1() {
            log.info("构造Bean1");
        }

        public Bean2 getBean2() {
            return bean2;
        }

    }

    static class Bean2 {
        private static final Logger log = LoggerFactory.getLogger(Bean2.class);

        public Bean2() {
            log.info("构造Bean2");
        }
    }
}

```





#### 1.3.2、ApplicationContext的常见实现和用法

```java
public class A02Application {

    public static void main(String[] args) {
        testClassPathXmlApplicationContext();
        testFileSystemXmlApplicationContext();
        testAnnotationConfigApplicationContext();
        testAnnotationConfigServletWebServerApplicationContext();

        // 加载xml bean的原理
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        // 读取xml bean定义
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
        // 此处文件获取的方式为ClassPathXml的
        reader.loadBeanDefinitions(new ClassPathResource("b01.xml"));
        // 对应FileSystemXml
        //reader.loadBeanDefinitions(new FileSystemResource("D:\\study\\spring-learn\\src\\main\\resources\\b01.xml"));
        // 后续添加后置处理器与TestBeanFactory中类似
    }

    // 较为经典的容器，基于 classpath 下xml格式的配置文件来创建
    private static void testClassPathXmlApplicationContext() {
        ClassPathXmlApplicationContext context =
                new ClassPathXmlApplicationContext("b01.xml");
        for (String name : context.getBeanDefinitionNames()) {
            System.out.println(name);
        }
        System.out.println(context.getBean(Bean2.class).getBean1());
    }


    // 基于磁盘路径下xml的配置文件来创建
    private static void testFileSystemXmlApplicationContext() {
        FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext("D:\\study\\spring-learn\\src\\main\\resources\\b01.xml");
        for (String name : context.getBeanDefinitionNames()) {
            System.out.println(name);
        }
        System.out.println(context.getBean(Bean2.class).getBean1());
    }

    // 较为经典的容器，基于java配置来创建
    private static void testAnnotationConfigApplicationContext() {
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(Config.class);
        for (String name : context.getBeanDefinitionNames()) {
            System.out.println(name);
        }
        System.out.println(context.getBean(Bean2.class).getBean1());
    }

    // 较为经典的容器，基于java配置来创建，用于web环境
    private static void testAnnotationConfigServletWebServerApplicationContext() {
        AnnotationConfigServletWebServerApplicationContext context =
                new AnnotationConfigServletWebServerApplicationContext(WebConfig.class);
        for (String name : context.getBeanDefinitionNames()) {
            System.out.println(name);
        }
        System.out.println(context.getBean(Bean2.class).getBean1());
    }

    static class Bean1 {
    }

    static class Bean2 {
        private Bean1 bean1;

        public Bean1 getBean1() {
            return bean1;
        }

        public void setBean1(Bean1 bean1) {
            this.bean1 = bean1;
        }
    }

    @Configuration
    static class Config {
        @Bean
        public Bean1 bean1() {
            return new Bean1();
        }

        @Bean
        public Bean2 bean2(Bean1 bean1) {
            Bean2 bean2 = new Bean2();
            bean2.setBean1(bean1);
            return bean2;
        }
    }

    @Configuration
    static class WebConfig {
        @Bean
        public ServletWebServerFactory servletWebServerFactory() {
            // 产生内嵌的tomcat容器
            return new TomcatServletWebServerFactory();
        }

        @Bean
        public DispatcherServlet dispatcherServlet() {
            // 创建servlet的对象
            return new DispatcherServlet();
        }

        @Bean
        public DispatcherServletRegistrationBean registrationBean(DispatcherServlet dispatcherServlet) {
            // 将前控制器与tomcat容器关联
            return new DispatcherServletRegistrationBean(dispatcherServlet, "/");
        }

        @Bean("/hello")
        public Controller controller1() {
            // 写一个简单controller用于展示效果
            return (request, response) -> {
                response.getWriter().println("hello");
                return null;
            };
        }
    }
}

```



#### 1.3.3、Bean的生命周期



#### 1.3.4、内嵌容器、注册DispatcherServlet



## 二、AOP

## 三、Web MVC

## 四、Spring Boot

## 五、其他