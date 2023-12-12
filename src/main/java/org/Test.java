package org;

import com.wu.service.AppConfig;
import com.wu.service.UserService;
import com.wu.springframework.MyApplicationContext;

public class Test {

    public static void main(String[] args) {

        MyApplicationContext  context = new MyApplicationContext(AppConfig.class);
        UserService userService = (UserService) context.getBean("userService");
        System.out.println(userService);

        UserService userService1 = (UserService) context.getBean("userService");
        System.out.println(userService);

        UserService userService2 = (UserService) context.getBean("userService");
        System.out.println(userService);

        userService.test();



    }

}
