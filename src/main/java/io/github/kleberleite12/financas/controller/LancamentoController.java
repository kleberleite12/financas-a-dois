package io.github.kleberleite12.financas.controller;

import io.github.kleberleite12.financas.model.Lancamento;
import io.github.kleberleite12.financas.model.TipoLancamento;
import io.github.kleberleite12.financas.repository.LancamentoRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class LancamentoController {

    private final LancamentoRepository lancamentoRepository;

    public LancamentoController(LancamentoRepository lancamentoRepository) {
        this.lancamentoRepository = lancamentoRepository;
    }

    @GetMapping("/lancamentos")
    public String listar(Model model) {

        model.addAttribute("lancamentos", lancamentoRepository.findAll());
        model.addAttribute("lancamento", new Lancamento());
        model.addAttribute("tipos", TipoLancamento.values());
        model.addAttribute("modoEdicao", false);

        return "lancamentos";
    }

    @GetMapping("/lancamentos/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {

        Lancamento lancamento = lancamentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Lançamento não encontrado: " + id
                ));

        model.addAttribute("lancamentos", lancamentoRepository.findAll());
        model.addAttribute("lancamento", lancamento);
        model.addAttribute("tipos", TipoLancamento.values());
        model.addAttribute("modoEdicao", true);

        return "lancamentos";
    }

    @PostMapping("/lancamentos")
    public String salvar(Lancamento lancamento) {

        lancamentoRepository.save(lancamento);

        return "redirect:/lancamentos";
    }

    @PostMapping("/lancamentos/excluir/{id}")
    public String excluir(@PathVariable Long id) {

        lancamentoRepository.deleteById(id);

        return "redirect:/lancamentos";
    }
}