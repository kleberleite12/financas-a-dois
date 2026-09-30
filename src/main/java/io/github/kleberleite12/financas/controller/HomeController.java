package io.github.kleberleite12.financas.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {

        model.addAttribute("rendaTotal", "R$ 0,00");
        model.addAttribute("gastosTotais", "R$ 0,00");
        model.addAttribute("guardadoTotal", "R$ 0,00");
        model.addAttribute("saldo", "R$ 0,00");

        return "home";
    }
}