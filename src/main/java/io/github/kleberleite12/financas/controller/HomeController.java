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
import java.util.List;
import java.util.Locale;

@Controller
public class HomeController {

    private static final BigDecimal META_INDIVIDUAL =
            new BigDecimal("20000.00");

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

        if (ano != null
                && mes != null
                && mes >= 1
                && mes <= 12) {

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

        DadosFinanceiros kleber =
                calcularDados(
                        lancamentosMes,
                        todosLancamentos,
                        "Kleber"
                );

        DadosFinanceiros giovanna =
                calcularDados(
                        lancamentosMes,
                        todosLancamentos,
                        "Giovanna"
                );

        DadosFinanceiros usuarioAtual;

        if (nomeUsuario.equalsIgnoreCase("Giovanna")) {
            usuarioAtual = giovanna;
        } else {
            usuarioAtual = kleber;
        }

        YearMonth periodoAnterior =
                periodo.minusMonths(1);

        YearMonth proximoPeriodo =
                periodo.plusMonths(1);

        String nomeMes =
                periodo.getMonth()
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
                "periodoAtual",
                periodo.toString()
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

        adicionarDadosPessoa(
                model,
                "kleber",
                kleber
        );

        adicionarDadosPessoa(
                model,
                "giovanna",
                giovanna
        );

        BigDecimal minhasReceitas =
                usuarioAtual.rendaPrincipal()
                        .add(usuarioAtual.rendaExtra());

        BigDecimal meusGastos =
                usuarioAtual.cartaoCredito()
                        .add(usuarioAtual.outrosGastos());

        model.addAttribute(
                "minhasReceitas",
                formatarMoeda(minhasReceitas)
        );

        model.addAttribute(
                "meusGastos",
                formatarMoeda(meusGastos)
        );

        model.addAttribute(
                "meuGuardadoMes",
                formatarMoeda(
                        usuarioAtual.guardadoMes()
                )
        );

        model.addAttribute(
                "meuDisponivelAgora",
                formatarMoeda(
                        usuarioAtual.saldo()
                )
        );

        model.addAttribute(
                "disponivelNegativo",
                usuarioAtual.saldo()
                        .compareTo(BigDecimal.ZERO) < 0
        );

        model.addAttribute(
                "metaIndividual",
                formatarMoeda(META_INDIVIDUAL)
        );

        return "home";
    }

    private DadosFinanceiros calcularDados(
            List<Lancamento> lancamentosMes,
            List<Lancamento> todosLancamentos,
            String responsavel) {

        BigDecimal rendaPrincipal =
                somar(
                        lancamentosMes,
                        responsavel,
                        TipoLancamento.RENDA_PRINCIPAL,
                        TipoLancamento.RECEITA
                );

        BigDecimal rendaExtra =
                somar(
                        lancamentosMes,
                        responsavel,
                        TipoLancamento.RENDA_EXTRA
                );

        BigDecimal cartaoCredito =
                somar(
                        lancamentosMes,
                        responsavel,
                        TipoLancamento.CARTAO_CREDITO,
                        TipoLancamento.GASTO
                );

        BigDecimal outrosGastos =
                somar(
                        lancamentosMes,
                        responsavel,
                        TipoLancamento.OUTRO_GASTO
                );

        BigDecimal guardadoMes =
                somar(
                        lancamentosMes,
                        responsavel,
                        TipoLancamento.GUARDADO
                );

        BigDecimal saldo =
                rendaPrincipal
                        .add(rendaExtra)
                        .subtract(cartaoCredito)
                        .subtract(outrosGastos)
                        .subtract(guardadoMes);

        BigDecimal guardadoAcumulado =
                somar(
                        todosLancamentos,
                        responsavel,
                        TipoLancamento.GUARDADO
                );

        BigDecimal percentualMeta =
                guardadoAcumulado
                        .divide(
                                META_INDIVIDUAL,
                                4,
                                RoundingMode.HALF_UP
                        )
                        .multiply(
                                new BigDecimal("100")
                        );

        BigDecimal percentualBarra =
                percentualMeta.min(
                        new BigDecimal("100")
                );

        return new DadosFinanceiros(
                rendaPrincipal,
                rendaExtra,
                cartaoCredito,
                outrosGastos,
                guardadoMes,
                saldo,
                guardadoAcumulado,
                percentualMeta,
                percentualBarra
        );
    }

    private BigDecimal somar(
            List<Lancamento> lancamentos,
            String responsavel,
            TipoLancamento... tipos) {

        return lancamentos.stream()

                .filter(lancamento ->
                        lancamento.getResponsavel()
                                .equalsIgnoreCase(responsavel)
                )

                .filter(lancamento -> {

                    for (TipoLancamento tipo : tipos) {

                        if (lancamento.getTipo() == tipo) {
                            return true;
                        }
                    }

                    return false;
                })

                .map(Lancamento::getValor)

                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }

    private void adicionarDadosPessoa(
            Model model,
            String prefixo,
            DadosFinanceiros dados) {

        model.addAttribute(
                prefixo + "RendaPrincipal",
                formatarMoeda(dados.rendaPrincipal())
        );

        model.addAttribute(
                prefixo + "RendaExtra",
                formatarMoeda(dados.rendaExtra())
        );

        model.addAttribute(
                prefixo + "CartaoCredito",
                formatarMoeda(dados.cartaoCredito())
        );

        model.addAttribute(
                prefixo + "OutrosGastos",
                formatarMoeda(dados.outrosGastos())
        );

        model.addAttribute(
                prefixo + "GuardadoMes",
                formatarMoeda(dados.guardadoMes())
        );

        model.addAttribute(
                prefixo + "Saldo",
                formatarMoeda(dados.saldo())
        );

        model.addAttribute(
                prefixo + "GuardadoAcumulado",
                formatarMoeda(dados.guardadoAcumulado())
        );

        model.addAttribute(
                prefixo + "PercentualMeta",
                dados.percentualMeta()
                        .setScale(
                                1,
                                RoundingMode.HALF_UP
                        )
        );

        model.addAttribute(
                prefixo + "PercentualBarra",
                dados.percentualBarra()
                        .setScale(
                                1,
                                RoundingMode.HALF_UP
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

        if (usuario == null
                || usuario.isBlank()) {

            return "Usuário";
        }

        return usuario.substring(0, 1).toUpperCase()
                + usuario.substring(1).toLowerCase();
    }

    private record DadosFinanceiros(

            BigDecimal rendaPrincipal,
            BigDecimal rendaExtra,
            BigDecimal cartaoCredito,
            BigDecimal outrosGastos,
            BigDecimal guardadoMes,
            BigDecimal saldo,
            BigDecimal guardadoAcumulado,
            BigDecimal percentualMeta,
            BigDecimal percentualBarra

    ) {
    }
}