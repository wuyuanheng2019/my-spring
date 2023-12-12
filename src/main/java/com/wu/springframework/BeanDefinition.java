package com.wu.springframework;

public class BeanDefinition {


    // 类型
    private Class clazz;

    // 单例 or 原型
    private String type;

    // 是否为懒加载
    private boolean isLazy;


    public Class getClazz() {
        return clazz;
    }

    public void setClazz(Class clazz) {
        this.clazz = clazz;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isLazy() {
        return isLazy;
    }

    public void setLazy(boolean lazy) {
        isLazy = lazy;
    }
}
