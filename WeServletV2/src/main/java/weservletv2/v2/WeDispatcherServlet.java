package weservletv2.v2;

import weservletv2.annotation.*;
import weservletv2.context.WeApplicationContext;

import javax.servlet.ServletConfig;
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

public class WeDispatcherServlet extends HttpServlet {

    private Map<String, Object> ioc = new HashMap<>();

    private Properties contextConfig = new Properties();

    private List<String> classNames = new ArrayList<>();

    private Map<String, Method> handlerMapping = new HashMap<>();

    private WeApplicationContext context;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        //6、调用
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Exception Detail :" + Arrays.toString(e.getStackTrace()));
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath, "").replaceAll("/+", "/");
        if (!this.handlerMapping.containsKey(url)) {
            resp.getWriter().write("404 Not Found");
            return;
        }

        Method method = this.handlerMapping.get(url);

        //拿到method的形参列表
        Class<?>[] parameterTypes = method.getParameterTypes();

        //设置实参列表
        Object[] paramValues = new Object[parameterTypes.length];

        Map<String, String[]> params = req.getParameterMap();

        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            if (parameterType == HttpServletRequest.class) {
                paramValues[i] = req;
            } else if (parameterType == HttpServletResponse.class) {
                paramValues[i] = resp;
            } else if (parameterType == String.class) {
                Annotation[][] annotations = method.getParameterAnnotations();
                for (Annotation annotation : annotations[i]) {
                    if (annotation instanceof WeRequestParam) {
                        String paramName = ((WeRequestParam) annotation).value();
                        String value = Arrays.toString(params.get(paramName))
                                .replaceAll("[\\[\\]]","")
                                .replaceAll("\\s","");
                        paramValues[i] = value;
                    }
                }
            }else {
                paramValues[i] = null;
            }
        }



        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        method.invoke(ioc.get(beanName), paramValues);
    }

    @Override
    public void init(ServletConfig config) {

        System.out.println("开始初始化");

        context = new WeApplicationContext(config.getInitParameter("contextConfigLocation"));

       /* //1、加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        //2、扫描所有相关的类
        doScanner(contextConfig.getProperty("scanPackage"));

        //==============IOC==============
        //3、实例化相关的类，并且将示例对象缓存到Ioc容器中
        doInstance();

        //==============DI==============
        //4、完成依赖注入
        doAutowired();*/

        //==============mvc=============
        //5、初始化HandleMapping
        doInitHandleMapping();

        System.out.println("Spring初始化完成");
    }

    private void doInitHandleMapping() {
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(WeController.class)) {
                continue;
            }

            String baseUrl = "";
            if (clazz.isAnnotationPresent(WeRequestMapping.class)) {
                WeRequestMapping requestMapping = clazz.getAnnotation(WeRequestMapping.class);

                baseUrl = requestMapping.value();
            }

            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(WeRequestMapping.class)) {
                    continue;
                }

                WeRequestMapping requestMapping = method.getAnnotation(WeRequestMapping.class);

                String url = ("/" + baseUrl + "/" + requestMapping.value()).replaceAll("/+", "/");

                handlerMapping.put(url, method);

                System.out.println("Mapped : " + url + "," + method);
            }
        }
    }

    private void doAutowired() {
        if (ioc.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {

            //包含public、private、protected、default修饰的字段
            Field[] fields = entry.getValue().getClass().getDeclaredFields();

            for (Field field : fields) {
                if (!field.isAnnotationPresent(WeAutowired.class)) {
                    continue;
                }
                WeAutowired autowired = field.getAnnotation(WeAutowired.class);
                String beanName = autowired.value().trim();
                if (!"".equals(beanName)) {
                    beanName = field.getType().getName();
                }

                //强制访问
                field.setAccessible(true);

                try {
                    //从IOC容器中找到beanName对应实例，动态赋值给加了@WeAutowired注解的字段
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    private void doInstance() {
        if (classNames.isEmpty()) {
            return;
        }
        try {
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);

                if (clazz.isAnnotationPresent(WeController.class)) {
                    //1、默认类名首字母小写
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    Object instance = clazz.newInstance();
                    ioc.put(beanName, instance);
                } else if (clazz.isAnnotationPresent(WeService.class)) {

                    //1、默认类名首字母小写
                    String beanName = toLowerFirstCase(clazz.getSimpleName());

                    //2、不同包下出现相同类名，只能自定义beanName
                    WeService service = clazz.getAnnotation(WeService.class);
                    if (!"".equals(service.value())) {
                        beanName = service.value();
                    }

                    Object instance = clazz.newInstance();
                    ioc.put(beanName, instance);

                    //3、如果是接口，new它的实现类，将接口的全类名作为key
                    for (Class<?> i : clazz.getInterfaces()) {
                        if (ioc.containsKey(i.getName())) {
                            throw new Exception("The beanName is exists!!");
                        }
                        ioc.put(i.getName(), instance);

                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}
