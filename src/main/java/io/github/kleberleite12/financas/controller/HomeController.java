package io.github.kleberleite12.financas.controller;

import io.github.kleberleite12.financas.model.Lancamento;
import io.github.kleberleite12.financas.model.TipoLancamento;
import io.github.kleberleite12.financas.repository.LancamentoRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

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
            Authentication authentication,
            Model model) {

        String nomeUsuario =
                formatarNomeUsuario(authentication.getName());

        YearMonth periodo;

        if (ano != null && mes != null && mes >= 1 && mes <= 12) {
            periodo = YearMonth.of(ano, mes);
        } else {
            periodo = YearMonth.now();
        }

        LocalDate inicioMes = periodo.atDay(1);
        LocalDate fimMes = periodo.atEndOfMonth();

        List<Lancamento> lancamentosMes =
                lancamentoRepository.findByDataBetween(
                        inicioMes,
                        fimMes
                );

        List<Lancamento> todosLancamentos =
                lancamentoRepository.findAll();


        BigDecimal rendaKleber =
                somarPorTipoEResponsavel(
                        lancamentosMes,
                        TipoLancamento.RECEITA,
                        "Kleber"
                );

        BigDecimal rendaGiovanna =
                somarPorTipoEResponsavel(
                        lancamentosMes,
                        TipoLancamento.RECEITA,
                        "Giovanna"
                );

        BigDecimal rendaTotal =
                rendaKleber.add(rendaGiovanna);


        BigDecimal gastosKleber =
                somarPorTipoEResponsavel(
                        lancamentosMes,
                        TipoLancamento.GASTO,
                        "Kleber"
                );

        BigDecimal gastosGiovanna =
                somarPorTipoEResponsavel(
                        lancamentosMes,
                        TipoLancamento.GASTO,
                        "Giovanna"
                );

        BigDecimal gastosTotais =
                gastosKleber.add(gastosGiovanna);


        BigDecimal guardadoKleber =
                somarPorTipoEResponsavel(
                        lancamentosMes,
                        TipoLancamento.GUARDADO,
                        "Kleber"
                );

        BigDecimal guardadoGiovanna =
                somarPorTipoEResponsavel(
                        lancamentosMes,
                        TipoLancamento.GUARDADO,
                        "Giovanna"
                );

        BigDecimal guardadoTotal =
                guardadoKleber.add(guardadoGiovanna);


        BigDecimal saldo = rendaTotal
                .subtract(gastosTotais)
                .subtract(guardadoTotal);


        Map<String, String> gastosPorCategoria =
                calcularGastosPorCategoria(
                        lancamentosMes
                );


        BigDecimal metaFinanceira =
                new BigDecimal("20000.00");

        BigDecimal guardadoAcumulado =
                somarPorTipo(
                        todosLancamentos,
                        TipoLancamento.GUARDADO
                );

        BigDecimal percentualMeta;

        if (metaFinanceira.compareTo(BigDecimal.ZERO) > 0) {

            percentualMeta = guardadoAcumulado
                    .divide(
                            metaFinanceira,
                            4,
                            RoundingMode.HALF_UP
                    )
                    .multiply(
                            new BigDecimal("100")
                    );

        } else {

            percentualMeta = BigDecimal.ZERO;
        }

        BigDecimal percentualBarra =
                percentualMeta.min(
                        new BigDecimal("100")
                );


        YearMonth periodoAnterior =
                periodo.minusMonths(1);

        YearMonth proximoPeriodo =
                periodo.plusMonths(1);


        String nomeMes = periodo
                .getMonth()
                .getDisplayName(
                        TextStyle.FULL,
                        Locale.forLanguageTag("pt-BR")
                );

        nomeMes =
                nomeMes.substring(0, 1).toUpperCase()
                        + nomeMes.substring(1);


        model.addAttribute(
                "nomeUsuario",
                nomeUsuario
        );

        model.addAttribute(
                "nomeMes",
                nomeMes
        );

        model.addAttribute(
                "ano",
                periodo.getYear()
        );

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


        model.addAttribute(
                "gastosPorCategoria",
                gastosPorCategoria
        );


        model.addAttribute(
                "metaFinanceira",
                formatarMoeda(metaFinanceira)
        );

        model.addAttribute(
                "guardadoAcumulado",
                formatarMoeda(guardadoAcumulado)
        );

        model.addAttribute(
                "percentualMeta",
                percentualMeta.setScale(
                        1,
                        RoundingMode.HALF_UP
                )
        );

        model.addAttribute(
                "percentualBarra",
                percentualBarra.setScale(
                        1,
                        RoundingMode.HALF_UP
                )
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

    private BigDecimal somarPorTipo(
            List<Lancamento> lancamentos,
            TipoLancamento tipo) {

        return lancamentos.stream()
                .filter(lancamento ->
                        lancamento.getTipo() == tipo)
                .map(Lancamento::getValor)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }

    private Map<String, String> calcularGastosPorCategoria(
            List<Lancamento> lancamentos) {

        Map<String, BigDecimal> totais =
                lancamentos.stream()
                        .filter(lancamento ->
                                lancamento.getTipo()
                                        == TipoLancamento.GASTO)
                        .collect(
                                Collectors.groupingBy(
                                        lancamento ->
                                                lancamento
                                                        .getCategoria()
                                                        .trim(),

                                        Collectors.reducing(
                                                BigDecimal.ZERO,
                                                Lancamento::getValor,
                                                BigDecimal::add
                                        )
                                )
                        );

        return totais.entrySet()
                .stream()
                .sorted(
                        Map.Entry
                                .<String, BigDecimal>
                                        comparingByValue()
                                .reversed()
                )
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,

                                entrada ->
                                        formatarMoeda(
                                                entrada.getValue()
                                        ),

                                (valor1, valor2) ->
                                        valor1,

                                LinkedHashMap::new
                        )
                );
    }

    private String formatarMoeda(
            BigDecimal valor) {

        NumberFormat formato =
                NumberFormat.getCurrencyInstance(
                        Locale.forLanguageTag("pt-BR")
                );

        return formato.format(valor);
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