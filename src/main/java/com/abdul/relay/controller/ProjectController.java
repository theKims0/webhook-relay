package com.abdul.relay.controller;

import com.abdul.relay.dto.ProjectRequestDTO;
import com.abdul.relay.entity.Project;
import com.abdul.relay.entity.User;
import com.abdul.relay.service.ProjectService;
import com.abdul.relay.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/projects")
public class ProjectController {

    @Autowired
    private final UserService userService;
    @Autowired
    private ProjectService projectService;

    public ProjectController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String form(Model model){
        System.out.println("MASUK");
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findUserByUsername(username);
        model.addAttribute("user", user);
        model.addAttribute("projectRequestDTO", new ProjectRequestDTO());
        return "project/form";
    }
    @PostMapping
    public String create(@Valid @ModelAttribute("projectRequestDTO") ProjectRequestDTO dto, BindingResult result, RedirectAttributes ra, Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findUserByUsername(username);
        try {
            System.out.println("INI MASUK ====> "+ dto.getPublicUrl());
             projectService.createProject(dto, user);
            return  "redirect:/projects/"+dto.getPublicUrl();
        }catch (Exception e){
            model.addAttribute("error", e.getMessage());
            return "redirect:/projects";
        }
    }
    @GetMapping("/{slug}")
    public String detail(@PathVariable("slug") String slug,
                         Model model, RedirectAttributes ra) {
        System.out.println("masuk detail 2");
        try {


            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userService.findUserByUsername(username);

            Project project = projectService.getProjectBySlugAndUser(slug, user);

            String relayToken = projectService.getRelayTokenProject(user.getId().toString(), slug);


            model.addAttribute("user", user);
            model.addAttribute("project", project);
            model.addAttribute("relayToken", relayToken);

            return "project/detail";
        }catch (Exception e){
            e.getMessage();
            ra.addFlashAttribute("error", "Gagal memuat detail proyek: " + e.getMessage());
            return "redirect:/dashboard";
        }
    }

    @DeleteMapping("/{slug}")
    public String delete(@PathVariable("slug") String slug, Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findUserByUsername(username);
        projectService.deleteProject(slug, user);
        return "redirect:/dashboard";
    }
    @PutMapping("/{slug}/toggle")
    public String changeStatus(@PathVariable("slug") String slug, Model model) {
        System.out.println("masuk changeStatus");
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findUserByUsername(username);
        projectService.changeStatusProject(slug, user);
        return "redirect:/projects/"+slug;
    }

    @PutMapping("/{slug}/generate-token")
    public String generateToken(@PathVariable("slug") String slug, Model model) {
        System.out.println("masuk generate token");
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findUserByUsername(username);
        projectService.generateTokenProject(slug, user);
        return "redirect:/projects/"+slug;
    }
}
