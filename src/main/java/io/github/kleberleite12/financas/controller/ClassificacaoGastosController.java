package io.github.kleberleite12.financas.controller;

import io.github.kleberleite12.financas.model.FormaPagamento;
import io.github.kleberleite12.financas.model.Lancamento;
import io.github.kleberleite12.financas.model.TipoLancamento;
import io.github.kleberleite12.financas.repository.LancamentoRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class ClassificacaoGastosController {

    private final LancamentoRepository lancamentoRepository;


    public ClassificacaoGastosController(
            LancamentoRepository lancamentoRepository) {

        this.lancamentoRepository =
                lancamentoRepository;
    }


    // =========================================
    // TELA DE CLASSIFICAÇÃO
    // =========================================

    @GetMapping("/classificar-gastos")
    public String classificarGastos(
            Authentication authentication,
            Model model) {


        String nomeUsuario =
                formatarNomeUsuario(
                        authentication.getName()
                );


        List<Lancamento> gastosSemClassificacao =
                lancamentoRepository
                        .findAll()
                        .stream()

                        .filter(lancamento ->
                                nomeUsuario
                                        .equalsIgnoreCase(
                                                lancamento.getResponsavel()
                                        )
                        )

                        .filter(lancamento ->
                                lancamento.getTipo()
                                        == TipoLancamento.OUTRO_GASTO
                        )

                        .filter(lancamento ->
                                lancamento.getFormaPagamento()
                                        == null
                        )

                        .toList();


        model.addAttribute(
                "nomeUsuario",
                nomeUsuario
        );


        model.addAttribute(
                "gastosSemClassificacao",
                gastosSemClassificacao
        );


        model.addAttribute(
                "quantidade",
                gastosSemClassificacao.size()
        );


        return "classificar-gastos";
    }


    // =========================================
    // SALVAR CLASSIFICAÇÕES
    // =========================================

    @PostMapping("/classificar-gastos")
    public String salvarClassificacoes(
            @RequestParam Map<String, String> parametros,
            Authentication authentication) {


        String nomeUsuario =
                formatarNomeUsuario(
                        authentication.getName()
                );


        List<Lancamento> alterados =
                new ArrayList<>();


        for (Map.Entry<String, String> entrada
                : parametros.entrySet()) {


            String chave =
                    entrada.getKey();


            /*
             * Esperamos campos com nomes como:
             *
             * formaPagamento_15
             * formaPagamento_27
             */

            if (!chave.startsWith(
                    "formaPagamento_"
            )) {

                continue;
            }


            String idTexto =
                    chave.replace(
                            "formaPagamento_",
                            ""
                    );


            Long id;


            try {

                id =
                        Long.parseLong(
                                idTexto
                        );

            } catch (NumberFormatException erro) {

                continue;
            }


            String valor =
                    entrada.getValue();


            FormaPagamento formaPagamento;


            try {

                formaPagamento =
                        FormaPagamento.valueOf(
                                valor
                        );

            } catch (Exception erro) {

                continue;
            }


            Lancamento lancamento =
                    lancamentoRepository
                            .findById(id)
                            .orElse(null);


            if (lancamento == null) {

                continue;
            }


            /*
             * Segurança:
             * só pode alterar lançamentos
             * do próprio usuário.
             */

            if (!nomeUsuario
                    .equalsIgnoreCase(
                            lancamento.getResponsavel()
                    )) {

                continue;
            }


            /*
             * Só permitimos classificar
             * Outro gasto.
             */

            if (lancamento.getTipo()
                    != TipoLancamento.OUTRO_GASTO) {

                continue;
            }


            /*
             * Só aceitamos as duas formas
             * disponíveis atualmente.
             */

            if (formaPagamento
                    != FormaPagamento.SALDO_NA_CONTA

                    && formaPagamento
                    != FormaPagamento.CARTAO_CREDITO) {

                continue;
            }


            lancamento.setFormaPagamento(
                    formaPagamento
            );


            alterados.add(
                    lancamento
            );
        }


        lancamentoRepository.saveAll(
                alterados
        );


        return "redirect:/classificar-gastos";
    }


    // =========================================
    // FORMATAR NOME
    // =========================================

    private String formatarNomeUsuario(
            String usuario) {


        if (usuario == null
                || usuario.isBlank()) {

            return "Usuário";
        }


        return usuario
                .substring(
                        0,
                        1
                )
                .toUpperCase()

                + usuario
                .substring(
                        1
                )
                .toLowerCase();
    }
}