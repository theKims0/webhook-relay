package com.abdul.relay.controller;

import com.abdul.relay.dto.LoginRequestDTO;
import com.abdul.relay.dto.RegisterRequestDTO;
import com.abdul.relay.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@AllArgsConstructor
public class AuthController {

    UserService userService;
    private final HttpServletRequest request; // inject via constructor

    private boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }
    @GetMapping("/")
    public String landingPage(Model model) {
        model.addAttribute("isLoggedIn", isAuthenticated());
        return "landing";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (isAuthenticated()) return "redirect:/dashboard";
        model.addAttribute("user", new RegisterRequestDTO());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("user") RegisterRequestDTO dto,
                           BindingResult result, RedirectAttributes ra, Model model) {
        if (result.hasErrors()) {
            System.out.println("DEBUG: Validasi gagal! Jumlah error: " + result.getErrorCount());
            return "auth/register";
        }

        try {
            userService.register(dto);
        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage());
            return "auth/register";
        }

        ra.addFlashAttribute("success", "Akun berhasil dibuat, silakan login.");
        return "redirect:/login";
    }
    @GetMapping("/login")
    public String loginPage(Model model) {
        if (isAuthenticated()) return "redirect:/dashboard";
        model.addAttribute("login", new LoginRequestDTO());
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("login") LoginRequestDTO dto,
                           BindingResult result, RedirectAttributes ra, Model model) {
        if (result.hasErrors()) {
            System.out.println("DEBUG: Validasi gagal! Jumlah error: " + result.getErrorCount());
            return "auth/login";
        }

        if (userService.login(dto)) {
            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext());

            ra.addFlashAttribute("success", "Login berhasil.");
            return "redirect:/dashboard";
        }
        model.addAttribute("error", "Username atau password salah.");
        model.addAttribute("user", dto);
        return "auth/login";
    }
    @PostMapping("/logout")
    public String logout(HttpServletRequest request, RedirectAttributes ra) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        ra.addFlashAttribute("success", "Logout berhasil.");
        return "redirect:/";
    }
}
