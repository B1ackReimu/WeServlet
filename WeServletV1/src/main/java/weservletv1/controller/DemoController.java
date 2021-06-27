package weservletv1.controller;

import weservletv1.annotation.WeController;
import weservletv1.annotation.WeRequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WeController
@WeRequestMapping("/demo")
public class DemoController {

    @WeRequestMapping("/test")
    public void test(HttpServletResponse resp, String name) throws IOException {
        resp.getWriter().write("my name is " + name);
        System.out.println("yes!");
    }

}
