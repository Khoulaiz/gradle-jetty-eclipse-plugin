package example.gradle;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.annotation.Resource;

@WebServlet(urlPatterns = {"/HelloGradle"}, name="HelloGradle")
@ServletSecurity(@HttpConstraint(rolesAllowed={"hacker"}))
public class HelloGradle extends HttpServlet {

    private @Resource(name="devmode") Boolean devmode;

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter out = response.getWriter();
        out.println("Hello Gradle!");
        if(devmode != null && devmode)
            out.println("devmode is turned on");
        else
            out.println("devmode is turned off");
    }

}
