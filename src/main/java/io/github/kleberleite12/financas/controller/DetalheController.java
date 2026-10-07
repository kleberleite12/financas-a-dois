package io.github.kleberleite12.financas.controller;

import io.github.kleberleite12.financas.model.Lancamento;
import io.github.kleberleite12.financas.model.TipoLancamento;
import io.github.kleberleite12.financas.repository.LancamentoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Controller
public class DetalheController {

    private final LancamentoRepository lancamentoRepository;

    public DetalheController(
            LancamentoRepository lancamentoRepository) {

        this.lancamentoRepository = lancamentoRepository;
    }

    @GetMapping("/detalhes")
    public String detalhes(
            @RequestParam String periodo,
            @RequestParam String responsavel,
            @RequestParam TipoLancamento tipo,
            Model model) {

        YearMonth periodoSelecionado;
        try {
            periodoSelecionado = YearMonth.parse(periodo);
        } catch (DateTimeParseException erro) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Mês e ano inválidos.", erro);
        }

        List<Lancamento> lancamentos =
                lancamentoRepository.findAll(
                                Sort.by(
                                        Sort.Order.desc("id")
                                )
                        )
                        .stream()

                        .filter(lancamento ->
                                lancamento.getResponsavel()
                                        .equalsIgnoreCase(responsavel)
                        )

                        .filter(lancamento ->
                                YearMonth.from(
                                        lancamento.getData()
                                ).equals(periodoSelecionado)
                        )

                        .filter(lancamento ->
                                tipoCorresponde(
                                        lancamento.getTipo(),
                                        tipo
                                )
                        )

                        .toList();

        BigDecimal total =
                lancamentos.stream()
                        .map(Lancamento::getValor)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        String nomeMes =
                periodoSelecionado
                        .getMonth()
                        .getDisplayName(
                                TextStyle.FULL,
                                Locale.forLanguageTag("pt-BR")
                        );

        nomeMes =
                nomeMes.substring(0, 1).toUpperCase()
                        + nomeMes.substring(1);

        model.addAttribute(
                "lancamentos",
                lancamentos
        );

        model.addAttribute(
                "responsavel",
                responsavel
        );

        model.addAttribute(
                "titulo",
                tipo.getDescricao()
        );

        model.addAttribute(
                "total",
                formatarMoeda(total)
        );

        model.addAttribute(
                "nomeMes",
                nomeMes
        );

        model.addAttribute(
                "ano",
                periodoSelecionado.getYear()
        );

        model.addAttribute(
                "anoVoltar",
                periodoSelecionado.getYear()
        );

        model.addAttribute(
                "mesVoltar",
                periodoSelecionado.getMonthValue()
        );

        return "detalhes";
    }

    private boolean tipoCorresponde(
            TipoLancamento tipoLancamento,
            TipoLancamento tipoFiltro) {

        if (tipoFiltro == TipoLancamento.RENDA_PRINCIPAL) {

            return tipoLancamento == TipoLancamento.RENDA_PRINCIPAL
                    || tipoLancamento == TipoLancamento.RECEITA;
        }

        if (tipoFiltro == TipoLancamento.CARTAO_CREDITO) {

            return tipoLancamento == TipoLancamento.CARTAO_CREDITO
                    || tipoLancamento == TipoLancamento.GASTO;
        }

        return tipoLancamento == tipoFiltro;
    }

    private String formatarMoeda(
            BigDecimal valor) {

        NumberFormat formato =
                NumberFormat.getCurrencyInstance(
                        Locale.forLanguageTag("pt-BR")
                );

        return formato.format(valor);
    }
}
