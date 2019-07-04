package cn.ccf.controller;

import cn.ccf.annotation.Autowired;
import cn.ccf.annotation.Controller;
import cn.ccf.annotation.RequestMapping;
import cn.ccf.annotation.RequestParam;
import cn.ccf.service.DemoService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author charles
 * @date 2019/7/2 15:53
 */
@Controller
@RequestMapping("charles")
public class DemoController {
    @Autowired
    public DemoService demoService;

    @RequestMapping("query")
    public void query(HttpServletRequest request, HttpServletResponse response, @RequestParam("name") String name,
                      @RequestParam("age" ) Integer age) {
        try {
            PrintWriter pw = response.getWriter();
            String result = demoService.query(name, age);
            pw.write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
