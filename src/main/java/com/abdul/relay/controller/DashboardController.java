package com.abdul.relay.controller;

import com.abdul.relay.entity.Project;
import com.abdul.relay.entity.User;
import com.abdul.relay.service.ProjectService;
import com.abdul.relay.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class DashboardController {

    @Autowired
    private final UserService userService;

    @Autowired
    private final ProjectService projectService;

    public DashboardController(UserService userService, ProjectService projectService) {
        this.userService = userService;
        this.projectService = projectService;
    }


    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User user = userService.findUserByUsername(username);
        List<Project> projects = projectService.getProjectsByUser(user);
        model.addAttribute("user", user);
        model.addAttribute("projects", projects);
        model.addAttribute("totalProjects", projects.size());
        model.addAttribute("activeCount", projects.stream().filter(Project::getIsActive).count());
        model.addAttribute("inactiveCount", projects.stream().filter(p -> !p.getIsActive()).count());
        return "dashboard/dashboard";
    }
}
