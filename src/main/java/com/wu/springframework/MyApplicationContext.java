package com.wu.springframework;

import com.wu.service.AppConfig;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.beans.Introspector;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyApplicationContext {


    private Class configClass;
    private Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();
    private Map<String, Object> singletonObjects = new HashMap<>();


    public MyApplicationContext(Class appConfigClass) {

        this.configClass = appConfigClass;

        /*
         * 扫描
         * 1、首先拿到 配置类上 ComponentScan 注解所对应的值 (扫描路径)
         * 2、找到对应扫描路径下 存在 Component 的类
         * 3、创建所以的 BeanDefinition , 并存入Map
         */
        scan(appConfigClass);

        /*
         * 1、遍历  beanDefinitionMap, 创建对象
         * 2、创建对象  -->  依赖注入  -->  Aware  -->  初始化前  -->  初始化  -->  初始化后 (aop)
         */
        for (String beanName : beanDefinitionMap.keySet()) {
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            // 为单例 且 不为懒加载
            if (beanDefinition.getType().equals("singleton") && !beanDefinition.isLazy()) {
                Object o = createBean(beanName, beanDefinition);
                singletonObjects.put(beanName, o);
            }
        }

    }

    public Object getBean(String beanName) {

        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        if (beanDefinition == null) {
            throw new RuntimeException("不是Bean");
        }

        // 如果不为单例 则每次创建
        String type = beanDefinition.getType();
        if (!"singleton".equals(type)) {
            return createBean(beanName, beanDefinition);
        } else {
            return singletonObjects.get(beanName);
        }

    }

    private Object createBean(String beanName, BeanDefinition beanDefinition) {

        // 创建对象  -->  依赖注入  -->  Aware  -->  初始化前  -->  初始化  -->  初始化后 (aop)
        Class clazz = beanDefinition.getClazz();
        try {
            Object object = clazz.newInstance();

            // 获取属性 判断属性是否存在 Autowired , 并注入
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    Object bean = getBean(field.getName());
                    field.setAccessible(true);
                    field.set(object, bean);
                }
            }

            // Aware 回调
            if (object instanceof BeanNameAware) {
                ((BeanNameAware) object).setBeanName(beanName);
            }
            if (object instanceof ApplicationContextAware) {
                ((ApplicationContextAware) object).setApplicationContext(this);
            }

            // 初始化前
            for (Method method : clazz.getMethods()) {
                if (method.isAnnotationPresent(PostConstruct.class)) {
                    try {
                        method.invoke(object);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            // 初始化
            if (object instanceof InitializingBean) {
                ((InitializingBean) object).afterPropertiesSet();
            }

            // 初始化后 aop
            if (clazz.isAnnotationPresent(Transactional.class)) {
                Enhancer enhancer = new Enhancer();
                enhancer.setSuperclass(clazz);
                Object target = object;

                enhancer.setCallback(new MethodInterceptor() {
                    @Override
                    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                        System.out.println(" ======== 开启事务 ============");
                        // 这里真正执行方法的是 被代理的对象
                        Object invoke = method.invoke(target, objects);
                        System.out.println(" ======== 提交事务 ============");
                        return invoke;
                    }
                });
                // 这里生成 代理对象
                object = enhancer.create();
            }
            return object;

        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    private void scan(Class appConfigClass) {
        if (appConfigClass.isAnnotationPresent(ComponentScan.class)) {

            // 拿到对应的路径, 加载文件夹下对应的类
            ComponentScan componentScan = (ComponentScan) appConfigClass.getAnnotation(ComponentScan.class);
            String path = componentScan.value().replace(".", "/");
            ClassLoader classLoader = this.getClass().getClassLoader();
            URL resource = classLoader.getResource(path);
            File f = new File(resource.getFile());

            // 去除目录, 添加文件
            List<File> fileList = getFiles(f);
            for (File file : fileList) {
                String absolutePath = file.getAbsolutePath();
                String className = absolutePath.substring(absolutePath.indexOf("com"), absolutePath.indexOf(".class"))
                        .replace("\\", ".");

                // 加载，并判断是否为 Bean
                try {
                    Class<?> aClass = classLoader.loadClass(className);
                    if (aClass.isAnnotationPresent(Component.class)) {

                        // 创建 Bean 定义, 为后续 Bean 的创建做准备
                        BeanDefinition beanDefinition = new BeanDefinition();
                        beanDefinition.setClazz(aClass);
                        beanDefinition.setLazy(aClass.isAnnotationPresent(Lazy.class));

                        // 单例 or 原型
                        if (aClass.isAnnotationPresent(Scope.class)) {
                            Scope scopeAnnotation = aClass.getAnnotation(Scope.class);
                            String value = scopeAnnotation.value();
                            if (value.isEmpty()) {
                                beanDefinition.setType("singleton");
                            } else {
                                beanDefinition.setType(value);
                            }
                        } else {
                            beanDefinition.setType("singleton");
                        }

                        // 放入 beanDefinition Map (缓存池中)
                        String beanName = aClass.getAnnotation(Component.class).value();
                        if (beanName.isEmpty()) {
                            beanName = Introspector.decapitalize(aClass.getSimpleName());
                        }
                        beanDefinitionMap.put(beanName, beanDefinition);
                    }
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private List<File> getFiles(File file) {

        List<File> list = new ArrayList<>();
        if (file.isDirectory()) {
            for (File listFile : file.listFiles()) {
                if (file.isDirectory()) {
                    list.addAll(getFiles(listFile));
                } else {
                    list.add(file);
                }
            }
        } else {
            list.add(file);
        }
        return list;
    }

}
