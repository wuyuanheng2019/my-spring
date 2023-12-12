package com.wu.service;

import com.wu.springframework.*;

@Component
//@Transactional
@Scope("prototype")
public class UserService implements InitializingBean, BeanNameAware {

    @Autowired
    private OrderService orderService;

    public void test(){
        System.out.println("=========== test ============");
    }

    @PostConstruct
    public void a() {
        System.out.println("=========== 初始化前 ============");
    }

    @Override
    public void afterPropertiesSet() {
        System.out.println("=========== 初始化 ============");
    }

    @Override
    public void setBeanName(String name) {
        System.out.println("=========== beanName: " + name + " ============");
    }
}
