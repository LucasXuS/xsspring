package com.xusong.springframework.servlet;

import com.xusong.springframework.annotation.XSAutowired;
import com.xusong.springframework.annotation.XSController;
import com.xusong.springframework.annotation.XSRequestMapping;
import com.xusong.springframework.annotation.XSService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class XSDispatcherServlet extends HttpServlet {

    private Properties contextConfig = new Properties();
    private List<String> classNames = new ArrayList<>();
    private Map<String, Object> ioc = new HashMap<>();
    private Map<String, Method> handlerMapping = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println(req.getRequestURI());
        doDispatch(req, resp);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        // 1、加载配置文件 application.properties
        doLoadConfig(config.getInitParameter("contextConfigLocation"));
        // 2、扫描相关的类
        doScanner(this.getClass().getClassLoader().getResource("").getPath()
                + "/" + contextConfig.getProperty("packageName").replace(".", "/"));


        // 3、实例化所有的相关的类，并且保存到ioc容器
        doInstance();
        // 4、完成依赖注入 DI
        doAutowired();
        // 5、初始化handlerMapping  url 和 method建立对应关系
        doInitHandlerMapping();
        System.out.println("Initialize is done.");


    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) {
        try {
            String url = req.getRequestURI().replaceAll("/+", "/");
            if (!handlerMapping.containsKey(url)) {
                resp.getWriter().print("404 error!");
                return;
            }
            Method method = handlerMapping.get(url);
            // 我们获取了类对象 因为是controller所以我们只需要获得类名即可
            Class<?> clazz = method.getDeclaringClass();
            String beanName = lowerFirstCase(clazz.getSimpleName());

            method.invoke(ioc.get(beanName), req, resp);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }


    }

    private void doInitHandlerMapping() {
        if (ioc == null || ioc.size() == 0) {
            return;
        }

        try {
            for (Map.Entry<String, Object> entry : ioc.entrySet()) {
                String classUrl = "/";
                Object object = entry.getValue();
                if (object.getClass().isAnnotationPresent(XSController.class) && object.getClass().isAnnotationPresent(XSRequestMapping.class)) {
                    classUrl = classUrl + object.getClass().getAnnotation(XSRequestMapping.class).value();
                    for (Method method : object.getClass().getMethods()) {
                        if (method.isAnnotationPresent(XSRequestMapping.class)) {
                            String methodUrl = method.getAnnotation(XSRequestMapping.class).value();
                            String url = classUrl + "/" + methodUrl;
                            url = url.replaceAll("/+", "/");
                            if (handlerMapping.containsKey(url)) {
                                throw new Exception("One url leads to multiple methods!");
                            }
                            handlerMapping.put(url, method);
                        }
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doAutowired() {
        if (ioc == null || ioc.size() == 0) {
            return;
        }
        try {
            for (Map.Entry<String, Object> entry : ioc.entrySet()) {
                Object object = entry.getValue();
                Field[] fields = object.getClass().getDeclaredFields();
                for (Field field : fields) {
                    if (field.isAnnotationPresent(XSAutowired.class)) {
                        field.setAccessible(true);

                        Object o = ioc.get(field.getName());
                        if (o == null) {
                            throw new Exception("Autowired Type needs Controller/Service Annotation!");
                        }
                        field.set(object, o);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private void doInstance() {
        if (classNames == null || classNames.size() == 0) {
            return;
        }
        // 轮询classNames为class创建类的实例，并且为bean名和实例创建映射（注入ioc）
        for (String className : classNames) {
            try {
                // 获取类型对象和新建实例。
                Class<?> clazz = Class.forName(className);
                boolean interfHasAnnotaion = false;
                if (clazz.getInterfaces() != null && clazz.getInterfaces().length > 0) {
                    if (clazz.getInterfaces().length > 1) {
                        throw new Exception("One class has multiple interfaces.");
                    }

                    Class<?> interf = clazz.getInterfaces()[0];
                    interfHasAnnotaion = interf != null && interf.isAnnotationPresent(XSService.class);
                }


                // 如果是接口我们可以按照优先级去做
                // 优先级：1 注解值 2 类型名（首字母小写）
                if (clazz.isAnnotationPresent(XSController.class)) {
                    // 这个肯定是类
                    String beanName = lowerFirstCase(clazz.getSimpleName());
                    Object instance = clazz.newInstance();
                    ioc.put(beanName, instance);
                } else if (clazz.isAnnotationPresent(XSService.class) || interfHasAnnotaion) {
                    // 我认为有以下规则
                    // ioc只保存一个对象实例 优先级 1 接口注解 2 接口名（此时接口有注解却没定义） 3 类注解 4 类名
                    String beanName = null;
                    // 1 自定义
                    XSService xsService = clazz.getAnnotation(XSService.class);
                    if (!clazz.isInterface()) {
                        Object instance = clazz.newInstance();
                        // 3 如果是接口，则初始化他的实现类，如果没有实现类，则动态代理创建一个
                        if (interfHasAnnotaion) {
                            Class<?> interf = clazz.getInterfaces()[0];
                            XSService xsService2 = interf.getAnnotation(XSService.class);
                            beanName = xsService2.value();
                            if ("".equals(beanName)) {
                                beanName = lowerFirstCase(interf.getSimpleName());
                            }
                        } else {
                            beanName = xsService.value();
                            // 2 bean名为首字母第一个小写的首字母（controller只支持这种模式，不支持自定义bean名）
                            if ("".equals(beanName)) {
                                beanName = lowerFirstCase(clazz.getSimpleName());
                            }
                        }

                        if (!beanName.equals("")) {
                            ioc.put(beanName, instance);
                        }

                    }

                }


            } catch (Exception e) {
                e.printStackTrace();
            }


        }


    }

    private String lowerFirstCase(String simpleName) {
        char[] cs = simpleName.toCharArray();
        cs[0] += 32;
        return String.valueOf(cs);
    }

    private void doScanner(String url) {

        // 获取文件路径
        // 获取路径中所有文件
        // 通过递归文件夹，获取所有类文件的类名，并且保存所有类名
        File file = new File(url);
        if (file.listFiles() != null && file.listFiles().length > 0) {
            for (File f : file.listFiles()) {
                if (f.isDirectory()) {
                    doScanner(url + "/" + f.getName());
                } else {

                    if (f.getName().endsWith(".class")) {
                        String fileName = url + "/" + f.getName();
                        String className = fileName.replaceAll("/+", ".").replace(".class", "");
                        classNames.add(className.substring(className.indexOf(contextConfig.getProperty("packageName"))));
                    }
                }
            }

        }
    }

    private void doLoadConfig(String contextConfigLocation) {
        // 获取配置文件文件流
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        // 装进properties对象
        try {
            contextConfig.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
