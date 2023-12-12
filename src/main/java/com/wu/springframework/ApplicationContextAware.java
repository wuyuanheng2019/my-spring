package com.wu.springframework;


// Spring 上下文回调
public interface ApplicationContextAware {

    void setApplicationContext(MyApplicationContext applicationContext);

}
