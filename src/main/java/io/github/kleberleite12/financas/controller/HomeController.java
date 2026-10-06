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

        BigDecimal rendaTotal = somarPorTipo(lancamentos, TipoLancamento.RECEITA);
        BigDecimal gastosTotais = somarPorTipo(lancamentos, TipoLancamento.GASTO);
        BigDecimal guardadoTotal = somarPorTipo(lancamentos, TipoLancamento.GUARDADO);

        BigDecimal saldo = rendaTotal
                .subtract(gastosTotais)
                .subtract(guardadoTotal);

        model.addAttribute("rendaTotal", formatarMoeda(rendaTotal));
        model.addAttribute("gastosTotais", formatarMoeda(gastosTotais));
        model.addAttribute("guardadoTotal", formatarMoeda(guardadoTotal));
        model.addAttribute("saldo", formatarMoeda(saldo));

        return "home";
    }

    private BigDecimal somarPorTipo(List<Lancamento> lancamentos, TipoLancamento tipo) {
        return lancamentos.stream()
                .filter(lancamento -> lancamento.getTipo() == tipo)
                .map(Lancamento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String formatarMoeda(BigDecimal valor) {
        NumberFormat formato = NumberFormat.getCurrencyInstance(
                Locale.of("pt", "BR")
        );

        return formato.format(valor);
    }
}