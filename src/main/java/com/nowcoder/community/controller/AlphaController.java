package com.nowcoder.community.controller;

import com.nowcoder.community.service.AlphaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/alpha")
public class AlphaController {

    @Autowired
    private AlphaService alphaService;

    @RequestMapping("/hello")
    @ResponseBody
    public String sayHello(){
        return "Hello World!";
    }

    @RequestMapping("/getData")
    @ResponseBody
    public String getData(){
        return alphaService.find();
    }

    @RequestMapping(path = "/students", method = RequestMethod.GET)
    @ResponseBody
    public String getStudents(@RequestParam(name = "page", required = false, defaultValue = "1") int page,
                              @RequestParam(name = "limit", required = false, defaultValue = "20") int limit){
        System.out.println(page);
        System.out.println(limit);
        return "students";
    }

    @RequestMapping(path = "/student/{id}", method = RequestMethod.GET)
    @ResponseBody
    public String getStudent(@PathVariable("id") int id){
        System.out.println(id);
        return "a student";
    }

    @RequestMapping(path = "/student", method = RequestMethod.POST)
    @ResponseBody
    public String saveStudent(String name, String id){
        System.out.println(name);
        System.out.println(id);
        return "success";
    }

    @RequestMapping(path = "/teacher", method = RequestMethod.GET)
    public String getTeacher(Model model){
        model.addAttribute("name", "张三");
        model.addAttribute("age", "18");
        return "/demo/view";
    }

    @RequestMapping(path = "/teachers", method = RequestMethod.GET)
    @ResponseBody
    public List<Map<String, Object>> getTeachers(){
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> teacher = new HashMap<>();
        teacher.put("name","张三");
        teacher.put("age",18);
        teacher.put("salary",6000.0);
        list.add(teacher);

        teacher.put("name","李四");
        teacher.put("age",19);
        teacher.put("salary",7000.0);
        list.add(teacher);

        teacher.put("name","王五");
        teacher.put("age",20);
        teacher.put("salary",8000.0);
        list.add(teacher);
        return list;
    }
}
