package io.github.kleberleite12.financas.controller;

import io.github.kleberleite12.financas.model.Lancamento;
import io.github.kleberleite12.financas.model.TipoLancamento;
import io.github.kleberleite12.financas.repository.LancamentoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class LancamentoController {

    private final LancamentoRepository lancamentoRepository;

    public LancamentoController(LancamentoRepository lancamentoRepository) {
        this.lancamentoRepository = lancamentoRepository;
    }

    @GetMapping("/lancamentos")
    public String listar(
            Model model,
            Authentication authentication) {

        String nomeUsuario =
                formatarNomeUsuario(authentication.getName());

        model.addAttribute(
                "lancamentos",
                lancamentoRepository.findAll()
        );

        model.addAttribute(
                "lancamento",
                new Lancamento()
        );

        model.addAttribute(
                "tipos",
                TipoLancamento.values()
        );

        model.addAttribute(
                "modoEdicao",
                false
        );

        model.addAttribute(
                "nomeUsuario",
                nomeUsuario
        );

        return "lancamentos";
    }

    @GetMapping("/lancamentos/editar/{id}")
    public String editar(
            @PathVariable Long id,
            Model model,
            Authentication authentication) {

        String nomeUsuario =
                formatarNomeUsuario(authentication.getName());

        Lancamento lancamento =
                lancamentoRepository.findById(id)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Lançamento não encontrado"
                                )
                        );

        verificarDono(
                lancamento,
                nomeUsuario
        );

        model.addAttribute(
                "lancamentos",
                lancamentoRepository.findAll()
        );

        model.addAttribute(
                "lancamento",
                lancamento
        );

        model.addAttribute(
                "tipos",
                TipoLancamento.values()
        );

        model.addAttribute(
                "modoEdicao",
                true
        );

        model.addAttribute(
                "nomeUsuario",
                nomeUsuario
        );

        return "lancamentos";
    }

    @PostMapping("/lancamentos")
    public String salvar(
            Lancamento lancamento,
            Authentication authentication) {

        String nomeUsuario =
                formatarNomeUsuario(authentication.getName());

        if (lancamento.getId() == null) {

            lancamento.setResponsavel(
                    nomeUsuario
            );

        } else {

            Lancamento existente =
                    lancamentoRepository
                            .findById(lancamento.getId())
                            .orElseThrow(() ->
                                    new ResponseStatusException(
                                            HttpStatus.NOT_FOUND,
                                            "Lançamento não encontrado"
                                    )
                            );

            verificarDono(
                    existente,
                    nomeUsuario
            );

            lancamento.setResponsavel(
                    existente.getResponsavel()
            );
        }

        lancamentoRepository.save(
                lancamento
        );

        return "redirect:/lancamentos";
    }

    @PostMapping("/lancamentos/excluir/{id}")
    public String excluir(
            @PathVariable Long id,
            Authentication authentication) {

        String nomeUsuario =
                formatarNomeUsuario(authentication.getName());

        Lancamento lancamento =
                lancamentoRepository.findById(id)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Lançamento não encontrado"
                                )
                        );

        verificarDono(
                lancamento,
                nomeUsuario
        );

        lancamentoRepository.delete(
                lancamento
        );

        return "redirect:/lancamentos";
    }

    private void verificarDono(
            Lancamento lancamento,
            String nomeUsuario) {

        if (!lancamento
                .getResponsavel()
                .equalsIgnoreCase(nomeUsuario)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Você não pode alterar o lançamento de outro usuário."
            );
        }
    }

    private String formatarNomeUsuario(
            String usuario) {

        if (usuario == null || usuario.isBlank()) {
            return "Usuário";
        }

        return usuario.substring(0, 1).toUpperCase()
                + usuario.substring(1).toLowerCase();
    }
}