package cn.ccf.service;

import cn.ccf.annotation.Service;

/**
 * @author charles
 * @date 2019/7/2 15:51
 */
@Service
public class DemoServiceImpl implements DemoService{
    @Override
    public String query(String name, Integer age) {
        return "name=" + name + ",age=" + age;
    }
}
