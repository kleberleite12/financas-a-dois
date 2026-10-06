package io.github.kleberleite12.financas.controller;

import io.github.kleberleite12.financas.model.Lancamento;
import io.github.kleberleite12.financas.model.TipoLancamento;
import io.github.kleberleite12.financas.repository.LancamentoRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@Controller
public class HomeController {

    private final LancamentoRepository lancamentoRepository;

    public HomeController(LancamentoRepository lancamentoRepository) {
        this.lancamentoRepository = lancamentoRepository;
    }

    @GetMapping("/")
    public String home(Model model) {

        List<Lancamento> lancamentos = lancamentoRepository.findAll();

        BigDecimal rendaKleber =
                somarPorTipoEResponsavel(lancamentos, TipoLancamento.RECEITA, "Kleber");

        BigDecimal rendaGiovanna =
                somarPorTipoEResponsavel(lancamentos, TipoLancamento.RECEITA, "Giovanna");

        BigDecimal rendaTotal = rendaKleber.add(rendaGiovanna);


        BigDecimal gastosKleber =
                somarPorTipoEResponsavel(lancamentos, TipoLancamento.GASTO, "Kleber");

        BigDecimal gastosGiovanna =
                somarPorTipoEResponsavel(lancamentos, TipoLancamento.GASTO, "Giovanna");

        BigDecimal gastosTotais = gastosKleber.add(gastosGiovanna);


        BigDecimal guardadoKleber =
                somarPorTipoEResponsavel(lancamentos, TipoLancamento.GUARDADO, "Kleber");

        BigDecimal guardadoGiovanna =
                somarPorTipoEResponsavel(lancamentos, TipoLancamento.GUARDADO, "Giovanna");

        BigDecimal guardadoTotal = guardadoKleber.add(guardadoGiovanna);


        BigDecimal saldo = rendaTotal
                .subtract(gastosTotais)
                .subtract(guardadoTotal);


        model.addAttribute("rendaKleber", formatarMoeda(rendaKleber));
        model.addAttribute("rendaGiovanna", formatarMoeda(rendaGiovanna));
        model.addAttribute("rendaTotal", formatarMoeda(rendaTotal));

        model.addAttribute("gastosKleber", formatarMoeda(gastosKleber));
        model.addAttribute("gastosGiovanna", formatarMoeda(gastosGiovanna));
        model.addAttribute("gastosTotais", formatarMoeda(gastosTotais));

        model.addAttribute("guardadoKleber", formatarMoeda(guardadoKleber));
        model.addAttribute("guardadoGiovanna", formatarMoeda(guardadoGiovanna));
        model.addAttribute("guardadoTotal", formatarMoeda(guardadoTotal));

        model.addAttribute("saldo", formatarMoeda(saldo));

        return "home";
    }

    private BigDecimal somarPorTipoEResponsavel(
            List<Lancamento> lancamentos,
            TipoLancamento tipo,
            String responsavel) {

        return lancamentos.stream()
                .filter(lancamento -> lancamento.getTipo() == tipo)
                .filter(lancamento ->
                        lancamento.getResponsavel().equalsIgnoreCase(responsavel))
                .map(Lancamento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String formatarMoeda(BigDecimal valor) {

        NumberFormat formato =
                NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

        return formato.format(valor);
    }
}