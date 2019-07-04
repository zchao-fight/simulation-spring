package cn.ccf.servlet;

import cn.ccf.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * 此类作为启动的入口类
 *
 * @author charles
 * @date 2019/7/2 15:29
 */
public class DispatcherServlet extends HttpServlet {

    // 跟web.xml中的param-name的值一致
    private static final String LOCATION = "contextConfigLocation";


    /**
     * 保存所有的配置信息
     */
    private Properties prop = new Properties();

    /**
     * 保存所有被扫描到的相关的类名
     */
    private List<String> classNames = new ArrayList<>();

    /**
     * 核心ioc容器，保存所有初始化的bean
     */
    private Map<String, Object> ioc = new HashMap<>();

    /**
     * 保存所有的url和方法的映射关系
     */
    private Map<String, Method> handlerMapping = new HashMap<>();


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (handlerMapping.isEmpty()) {
            return;
        }
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        uri = uri.replace(contextPath, "");
        if (!this.handlerMapping.containsKey(uri)) {
            resp.getWriter().write("404 NOT Found");
            return;
        }
        Method method = handlerMapping.get(uri);
        Object[] args = handle(req, resp, method);
        String beanName = this.lowerFirstCase(method.getDeclaringClass().getSimpleName());
        try {
            method.invoke(this.ioc.get(beanName), args);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private static Object[] handle(HttpServletRequest request, HttpServletResponse response, Method method) {
        // 拿到当前待执行的方法有哪些参数
        Class<?>[] paramClazzs = method.getParameterTypes();
        // 根据参数的个数new一个参数的数组，将方法里的所有参数赋值到args来
        Object[] args = new Object[paramClazzs.length];

        int args_i = 0;
        int index = 0;

        for (Class<?> paramClazz : paramClazzs) {
            if (ServletRequest.class.isAssignableFrom(paramClazz)) {
                args[args_i++] = request;
            }
            if (ServletResponse.class.isAssignableFrom(paramClazz)) {
                args[args_i++] = response;
            }
            Annotation[] paramAns = method.getParameterAnnotations()[index++];
            if (paramAns.length > 0) {
                for (Annotation paramAn : paramAns) {
                    if (RequestParam.class.isAssignableFrom(paramAn.getClass())) {

                        // 类型需要严格匹配 否则容易报类型不匹配

                        if (String.class.isAssignableFrom(paramClazz)) {
                            args[args_i++] = request.getParameter(((RequestParam) paramAn).value());
                        }
                        if (Integer.class.isAssignableFrom(paramClazz)) {
                            args[args_i++] = Integer.valueOf(request.getParameter(((RequestParam) paramAn).value()));
                        }
                    }
                }
            }
        }

        return args;
    }

    /**
     * tomcat启动过程要做的事情 包扫描 class实例 容器创建 autowired 路径映射
     *
     * @param config
     * @throws ServletException
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        System.out.println(config.getInitParameter(LOCATION));
        // 1.加载配置文件
        doLoadConfig(config.getInitParameter(LOCATION));

        // 2.扫描所有相关的类
        doScan(prop.getProperty("scanPackage"));

        // 3.初始化所有相关类的实例，并保存到IOC容器中
        doInstance();

        // 4.依赖注入
        doAutowired();

        // 5.构造HandlerMapping
        initHandlerMapping();

        // 6.等待请求，匹配URL，定位方法，反射调用执行
        // 调用doGet or doPost方法

        System.out.println("mvcframework is init");
    }

    private void initHandlerMapping() {
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(Controller.class)) {
                continue;
            }
            String baseUrl = "";
            if (clazz.isAnnotationPresent(RequestMapping.class)) {
                RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
                baseUrl = requestMapping.value();
            }
            // 获取Method的url配置
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                // 没有加RequestMapping注解的直接忽略
                if (!method.isAnnotationPresent(RequestMapping.class)) {
                    continue;
                }
                RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                String url = "/" + baseUrl + "/" + requestMapping.value();
                handlerMapping.put(url, method);
                System.out.println("mapped" + url + "," + method);
            }
        }
    }

    private void doAutowired() {
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            // 拿到实例对象中所有属性
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(Autowired.class)) {
                    continue;
                }
                Autowired autowired = field.getAnnotation(Autowired.class);
                String beanName = autowired.value().trim();
                if ("".equals(beanName)) {
                    beanName = field.getType().getName();
                }
                // 设置私有属性的访问权限
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }
    }

    private String lowerFirstCase(String str) {
        char[] chars = str.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    private void doInstance() {
        if (classNames.size() == 0) {
            return;
        }
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(Controller.class)) {
                    // 默认将首字母小写的类名作为beanName
                    String beanName = lowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName, clazz.newInstance());
                } else if (clazz.isAnnotationPresent(Service.class)) {
                    Service service = clazz.getAnnotation(Service.class);
                    String beanName = service.value();
                    if (!"".equals(beanName.trim())) {
                        ioc.put(beanName, clazz.newInstance());
                        continue;
                    }
                    // 如果自己没设置，就按接口类型创建一个实例
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for (Class<?> i : interfaces) {
                        ioc.put(i.getName(), clazz.newInstance());
                    }
                } else {
                    continue;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private void doScan(String packageName) {
        // 将所有的包转换为文件路径
        URL url = this.getClass().getClassLoader().getResource(packageName.replaceAll("\\.", "/"));
        System.out.println(url.getFile());
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()) {
            // 如果是文件夹，继续递归
            if (file.isDirectory()) {
                System.out.println(file.getName());
                doScan(packageName + "." + file.getName());
            } else {
                classNames.add(packageName + "." + file.getName().replace(".class", "").trim());
            }
        }
    }

    private void doLoadConfig(String location) {
        InputStream in = null;
        in = this.getClass().getClassLoader().getResourceAsStream(location);
        // 1.读取配置文件
        try {
            prop.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
