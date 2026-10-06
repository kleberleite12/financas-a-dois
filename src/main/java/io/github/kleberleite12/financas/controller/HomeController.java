package io.github.kleberleite12.financas.controller;

import io.github.kleberleite12.financas.model.Lancamento;
import io.github.kleberleite12.financas.model.TipoLancamento;
import io.github.kleberleite12.financas.repository.LancamentoRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Controller
public class HomeController {

    private final LancamentoRepository lancamentoRepository;

    public HomeController(LancamentoRepository lancamentoRepository) {
        this.lancamentoRepository = lancamentoRepository;
    }

    @GetMapping("/")
    public String home(
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) Integer mes,
            Model model) {

        YearMonth periodo;

        if (ano != null && mes != null && mes >= 1 && mes <= 12) {
            periodo = YearMonth.of(ano, mes);
        } else {
            periodo = YearMonth.now();
        }

        LocalDate inicioMes = periodo.atDay(1);
        LocalDate fimMes = periodo.atEndOfMonth();

        List<Lancamento> lancamentos =
                lancamentoRepository.findByDataBetween(inicioMes, fimMes);

        BigDecimal rendaKleber =
                somarPorTipoEResponsavel(
                        lancamentos,
                        TipoLancamento.RECEITA,
                        "Kleber"
                );

        BigDecimal rendaGiovanna =
                somarPorTipoEResponsavel(
                        lancamentos,
                        TipoLancamento.RECEITA,
                        "Giovanna"
                );

        BigDecimal rendaTotal =
                rendaKleber.add(rendaGiovanna);


        BigDecimal gastosKleber =
                somarPorTipoEResponsavel(
                        lancamentos,
                        TipoLancamento.GASTO,
                        "Kleber"
                );

        BigDecimal gastosGiovanna =
                somarPorTipoEResponsavel(
                        lancamentos,
                        TipoLancamento.GASTO,
                        "Giovanna"
                );

        BigDecimal gastosTotais =
                gastosKleber.add(gastosGiovanna);


        BigDecimal guardadoKleber =
                somarPorTipoEResponsavel(
                        lancamentos,
                        TipoLancamento.GUARDADO,
                        "Kleber"
                );

        BigDecimal guardadoGiovanna =
                somarPorTipoEResponsavel(
                        lancamentos,
                        TipoLancamento.GUARDADO,
                        "Giovanna"
                );

        BigDecimal guardadoTotal =
                guardadoKleber.add(guardadoGiovanna);


        BigDecimal saldo = rendaTotal
                .subtract(gastosTotais)
                .subtract(guardadoTotal);


        YearMonth periodoAnterior = periodo.minusMonths(1);
        YearMonth proximoPeriodo = periodo.plusMonths(1);


        String nomeMes = periodo
                .getMonth()
                .getDisplayName(
                        TextStyle.FULL,
                        Locale.forLanguageTag("pt-BR")
                );

        nomeMes =
                nomeMes.substring(0, 1).toUpperCase()
                        + nomeMes.substring(1);


        model.addAttribute("nomeMes", nomeMes);
        model.addAttribute("ano", periodo.getYear());


        model.addAttribute(
                "anoAnterior",
                periodoAnterior.getYear()
        );

        model.addAttribute(
                "mesAnterior",
                periodoAnterior.getMonthValue()
        );


        model.addAttribute(
                "proximoAno",
                proximoPeriodo.getYear()
        );

        model.addAttribute(
                "proximoMes",
                proximoPeriodo.getMonthValue()
        );


        model.addAttribute(
                "rendaKleber",
                formatarMoeda(rendaKleber)
        );

        model.addAttribute(
                "rendaGiovanna",
                formatarMoeda(rendaGiovanna)
        );

        model.addAttribute(
                "rendaTotal",
                formatarMoeda(rendaTotal)
        );


        model.addAttribute(
                "gastosKleber",
                formatarMoeda(gastosKleber)
        );

        model.addAttribute(
                "gastosGiovanna",
                formatarMoeda(gastosGiovanna)
        );

        model.addAttribute(
                "gastosTotais",
                formatarMoeda(gastosTotais)
        );


        model.addAttribute(
                "guardadoKleber",
                formatarMoeda(guardadoKleber)
        );

        model.addAttribute(
                "guardadoGiovanna",
                formatarMoeda(guardadoGiovanna)
        );

        model.addAttribute(
                "guardadoTotal",
                formatarMoeda(guardadoTotal)
        );


        model.addAttribute(
                "saldo",
                formatarMoeda(saldo)
        );

        return "home";
    }

    private BigDecimal somarPorTipoEResponsavel(
            List<Lancamento> lancamentos,
            TipoLancamento tipo,
            String responsavel) {

        return lancamentos.stream()
                .filter(lancamento ->
                        lancamento.getTipo() == tipo)
                .filter(lancamento ->
                        lancamento.getResponsavel()
                                .equalsIgnoreCase(responsavel))
                .map(Lancamento::getValor)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }

    private String formatarMoeda(BigDecimal valor) {

        NumberFormat formato =
                NumberFormat.getCurrencyInstance(
                        Locale.forLanguageTag("pt-BR")
                );

        return formato.format(valor);
    }
}