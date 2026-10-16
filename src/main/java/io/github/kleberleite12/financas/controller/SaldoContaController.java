package io.github.kleberleite12.financas.controller;

import io.github.kleberleite12.financas.model.FormaPagamento;
import io.github.kleberleite12.financas.model.Lancamento;
import io.github.kleberleite12.financas.model.SaldoConta;
import io.github.kleberleite12.financas.model.TipoLancamento;
import io.github.kleberleite12.financas.repository.LancamentoRepository;
import io.github.kleberleite12.financas.repository.SaldoContaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

@Controller
public class SaldoContaController {

    private static final BigDecimal VALOR_MAXIMO =
            new BigDecimal("99999999.99");


    private final SaldoContaRepository saldoContaRepository;

    private final LancamentoRepository lancamentoRepository;


    public SaldoContaController(
            SaldoContaRepository saldoContaRepository,
            LancamentoRepository lancamentoRepository) {

        this.saldoContaRepository =
                saldoContaRepository;

        this.lancamentoRepository =
                lancamentoRepository;
    }


    // =========================================
    // TELA SALDO NA CONTA
    // =========================================

    @GetMapping("/saldo-conta")
    public String saldoConta(
            @RequestParam(required = false)
            String periodo,

            Authentication authentication,
            Model model) {


        String nomeUsuario =
                formatarNomeUsuario(
                        authentication.getName()
                );


        YearMonth periodoSelecionado =
                obterPeriodo(
                        periodo
                );


        carregarDadosTela(
                model,
                nomeUsuario,
                periodoSelecionado
        );


        return "saldo-conta";
    }


    // =========================================
    // SALVAR SALDO
    // =========================================

    @PostMapping("/saldo-conta")
    public String salvarSaldoConta(
            @RequestParam String periodo,
            @RequestParam BigDecimal valorInicial,

            Authentication authentication,
            Model model) {


        String nomeUsuario =
                formatarNomeUsuario(
                        authentication.getName()
                );


        YearMonth periodoSelecionado;


        try {

            periodoSelecionado =
                    YearMonth.parse(
                            periodo
                    );

        } catch (Exception erro) {

            carregarDadosTela(
                    model,
                    nomeUsuario,
                    YearMonth.now()
            );


            model.addAttribute(
                    "valorInicial",
                    valorInicial
            );


            model.addAttribute(
                    "mensagemErro",
                    "Mês e ano inválidos."
            );


            return "saldo-conta";
        }


        String erroValidacao =
                validarValor(
                        valorInicial
                );


        if (erroValidacao != null) {

            carregarDadosTela(
                    model,
                    nomeUsuario,
                    periodoSelecionado
            );


            model.addAttribute(
                    "valorInicial",
                    valorInicial
            );


            model.addAttribute(
                    "mensagemErro",
                    erroValidacao
            );


            return "saldo-conta";
        }


        SaldoConta saldoConta =
                saldoContaRepository
                        .findByResponsavelIgnoreCaseAndPeriodo(
                                nomeUsuario,
                                periodoSelecionado.atDay(1)
                        )
                        .orElseGet(
                                SaldoConta::new
                        );


        saldoConta.setResponsavel(
                nomeUsuario
        );


        saldoConta.setPeriodo(
                periodoSelecionado.atDay(1)
        );


        saldoConta.setValorInicial(
                valorInicial
        );


        saldoContaRepository.save(
                saldoConta
        );


        return "redirect:/?ano="
                + periodoSelecionado.getYear()
                + "&mes="
                + periodoSelecionado.getMonthValue();
    }


    // =========================================
    // CARREGAR DADOS DA TELA
    // =========================================

    private void carregarDadosTela(
            Model model,
            String nomeUsuario,
            YearMonth periodo) {


        LocalDate inicioMes =
                periodo.atDay(1);


        LocalDate fimMes =
                periodo.atEndOfMonth();


        BigDecimal valorInicial =
                saldoContaRepository
                        .findByResponsavelIgnoreCaseAndPeriodo(
                                nomeUsuario,
                                inicioMes
                        )
                        .map(
                                SaldoConta::getValorInicial
                        )
                        .orElse(
                                BigDecimal.ZERO
                        );


        List<Lancamento> lancamentosMes =
                lancamentoRepository
                        .findByDataBetween(
                                inicioMes,
                                fimMes
                        );


        BigDecimal valorUsado =
                calcularValorUsado(
                        lancamentosMes,
                        nomeUsuario
                );


        BigDecimal valorAtual =
                valorInicial
                        .subtract(
                                valorUsado
                        );


        model.addAttribute(
                "nomeUsuario",
                nomeUsuario
        );


        model.addAttribute(
                "periodoSaldo",
                periodo.toString()
        );


        model.addAttribute(
                "valorInicial",
                valorInicial
        );


        model.addAttribute(
                "valorInicialFormatado",
                formatarMoeda(
                        valorInicial
                )
        );


        model.addAttribute(
                "valorUsadoFormatado",
                formatarMoeda(
                        valorUsado
                )
        );


        model.addAttribute(
                "valorAtualFormatado",
                formatarMoeda(
                        valorAtual
                )
        );


        model.addAttribute(
                "saldoNegativo",
                valorAtual
                        .compareTo(
                                BigDecimal.ZERO
                        ) < 0
        );
    }


    // =========================================
    // CALCULAR VALOR USADO
    // =========================================

    private BigDecimal calcularValorUsado(
            List<Lancamento> lancamentos,
            String nomeUsuario) {


        return lancamentos
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
                                == FormaPagamento.SALDO_NA_CONTA
                )

                .map(
                        Lancamento::getValor
                )

                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }


    // =========================================
    // VALIDAÇÃO
    // =========================================

    private String validarValor(
            BigDecimal valor) {


        if (valor == null) {

            return "Informe o saldo na conta.";
        }


        if (valor.compareTo(
                BigDecimal.ZERO
        ) < 0) {

            return "O saldo na conta não pode ser negativo.";
        }


        if (valor.compareTo(
                VALOR_MAXIMO
        ) > 0) {

            return "O valor informado é muito alto.";
        }


        BigDecimal valorNormalizado =
                valor.stripTrailingZeros();


        if (valorNormalizado.scale() > 2) {

            return "O saldo pode ter no máximo duas casas decimais.";
        }


        return null;
    }


    // =========================================
    // PERÍODO
    // =========================================

    private YearMonth obterPeriodo(
            String periodo) {


        if (periodo == null
                || periodo.isBlank()) {

            return YearMonth.now();
        }


        try {

            return YearMonth.parse(
                    periodo
            );

        } catch (Exception erro) {

            return YearMonth.now();
        }
    }


    // =========================================
    // FORMATAÇÃO DE MOEDA
    // =========================================

    private String formatarMoeda(
            BigDecimal valor) {


        NumberFormat formato =
                NumberFormat
                        .getCurrencyInstance(
                                Locale.forLanguageTag(
                                        "pt-BR"
                                )
                        );


        return formato.format(
                valor
        );
    }


    // =========================================
    // USUÁRIO
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