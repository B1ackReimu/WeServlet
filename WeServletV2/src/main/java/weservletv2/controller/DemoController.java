package weservletv2.controller;

import weservletv1.annotation.WeController;
import weservletv1.annotation.WeRequestMapping;
import weservletv1.annotation.WeRequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WeController
@WeRequestMapping("/demo")
public class DemoController {

    @WeRequestMapping("/test")
    public void test(HttpServletResponse resp,@WeRequestParam("name") String name) throws IOException {
        resp.getWriter().write("my name is " + name);
        resp.setStatus(404);
        System.out.println("yes!");
    }

}
