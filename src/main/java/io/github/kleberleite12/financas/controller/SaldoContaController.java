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
    // SALVAR SALDO INICIAL
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
                validarValorInicial(
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


        /*
         * Se for um registro novo,
         * iniciamos as transferências em zero.
         *
         * Se já existir, preservamos aquilo
         * que já foi transferido.
         */
        if (saldoConta.getValorTransferido() == null) {

            saldoConta.setValorTransferido(
                    BigDecimal.ZERO
            );
        }


        saldoContaRepository.save(
                saldoConta
        );


        return "redirect:/saldo-conta?periodo="
                + periodoSelecionado;
    }


    // =========================================
    // TRANSFERIR DAS RECEITAS PARA O SALDO
    // =========================================

    @PostMapping("/saldo-conta/transferir")
    public String transferirParaSaldo(
            @RequestParam String periodo,
            @RequestParam BigDecimal valorTransferencia,

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
                    "mensagemErroTransferencia",
                    "Mês e ano inválidos."
            );


            return "saldo-conta";
        }


        String erroValidacao =
                validarTransferencia(
                        valorTransferencia
                );


        if (erroValidacao != null) {

            carregarDadosTela(
                    model,
                    nomeUsuario,
                    periodoSelecionado
            );


            model.addAttribute(
                    "valorTransferencia",
                    valorTransferencia
            );


            model.addAttribute(
                    "mensagemErroTransferencia",
                    erroValidacao
            );


            return "saldo-conta";
        }


        LocalDate inicioMes =
                periodoSelecionado.atDay(1);


        LocalDate fimMes =
                periodoSelecionado.atEndOfMonth();


        List<Lancamento> lancamentosMes =
                lancamentoRepository
                        .findByDataBetween(
                                inicioMes,
                                fimMes
                        );


        SaldoConta saldoConta =
                saldoContaRepository
                        .findByResponsavelIgnoreCaseAndPeriodo(
                                nomeUsuario,
                                inicioMes
                        )
                        .orElseGet(
                                SaldoConta::new
                        );


        BigDecimal transferidoAtual =
                saldoConta.getValorTransferido();


        BigDecimal disponivelReceitas =
                calcularDisponivelReceitas(
                        lancamentosMes,
                        nomeUsuario,
                        transferidoAtual
                );


        /*
         * Impede transferir dinheiro
         * que não existe nas receitas.
         */
        if (valorTransferencia.compareTo(
                disponivelReceitas
        ) > 0) {


            carregarDadosTela(
                    model,
                    nomeUsuario,
                    periodoSelecionado
            );


            model.addAttribute(
                    "valorTransferencia",
                    valorTransferencia
            );


            model.addAttribute(
                    "mensagemErroTransferencia",
                    "Você não pode transferir mais do que possui disponível nas receitas."
            );


            return "saldo-conta";
        }


        /*
         * Se ainda não existia registro de saldo
         * para o mês, criamos um começando em zero.
         */
        saldoConta.setResponsavel(
                nomeUsuario
        );


        saldoConta.setPeriodo(
                inicioMes
        );


        if (saldoConta.getValorInicial() == null) {

            saldoConta.setValorInicial(
                    BigDecimal.ZERO
            );
        }


        /*
         * Soma a nova transferência
         * ao que já havia sido transferido.
         */
        saldoConta.setValorTransferido(

                transferidoAtual.add(
                        valorTransferencia
                )
        );


        saldoContaRepository.save(
                saldoConta
        );


        return "redirect:/saldo-conta?periodo="
                + periodoSelecionado;
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


        SaldoConta saldoConta =
                saldoContaRepository
                        .findByResponsavelIgnoreCaseAndPeriodo(
                                nomeUsuario,
                                inicioMes
                        )
                        .orElse(null);


        BigDecimal valorInicial =
                saldoConta != null

                        ? saldoConta.getValorInicial()

                        : BigDecimal.ZERO;


        if (valorInicial == null) {

            valorInicial =
                    BigDecimal.ZERO;
        }


        BigDecimal valorTransferido =
                saldoConta != null

                        ? saldoConta.getValorTransferido()

                        : BigDecimal.ZERO;


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


        /*
         * SALDO ATUAL:
         *
         * saldo inicial
         * + transferências
         * - gastos pagos com saldo
         */
        BigDecimal valorAtual =
                valorInicial
                        .add(
                                valorTransferido
                        )
                        .subtract(
                                valorUsado
                        );


        /*
         * Quanto ainda pode ser movido
         * das receitas para o saldo.
         */
        BigDecimal disponivelReceitas =
                calcularDisponivelReceitas(
                        lancamentosMes,
                        nomeUsuario,
                        valorTransferido
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
                "valorTransferidoFormatado",
                formatarMoeda(
                        valorTransferido
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
                "disponivelReceitasFormatado",
                formatarMoeda(
                        disponivelReceitas
                )
        );


        model.addAttribute(
                "podeTransferir",
                disponivelReceitas
                        .compareTo(
                                BigDecimal.ZERO
                        ) > 0
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
    // CALCULAR DISPONÍVEL DAS RECEITAS
    // =========================================

    private BigDecimal calcularDisponivelReceitas(
            List<Lancamento> lancamentos,
            String nomeUsuario,
            BigDecimal valorTransferido) {


        BigDecimal rendaPrincipal =
                somar(
                        lancamentos,
                        nomeUsuario,
                        TipoLancamento.RENDA_PRINCIPAL,
                        TipoLancamento.RECEITA
                );


        BigDecimal rendaExtra =
                somar(
                        lancamentos,
                        nomeUsuario,
                        TipoLancamento.RENDA_EXTRA
                );


        BigDecimal cartaoCredito =
                somar(
                        lancamentos,
                        nomeUsuario,
                        TipoLancamento.CARTAO_CREDITO,
                        TipoLancamento.GASTO
                );


        BigDecimal outrosGastos =
                somar(
                        lancamentos,
                        nomeUsuario,
                        TipoLancamento.OUTRO_GASTO
                );


        BigDecimal guardado =
                somar(
                        lancamentos,
                        nomeUsuario,
                        TipoLancamento.GUARDADO
                );


        BigDecimal gastosSaldoConta =
                calcularValorUsado(
                        lancamentos,
                        nomeUsuario
                );


        /*
         * Gastos que saíram do saldo
         * não podem ser descontados
         * também das receitas.
         */
        BigDecimal outrosGastosReceitas =
                outrosGastos
                        .subtract(
                                gastosSaldoConta
                        );


        return rendaPrincipal
                .add(
                        rendaExtra
                )
                .subtract(
                        cartaoCredito
                )
                .subtract(
                        outrosGastosReceitas
                )
                .subtract(
                        guardado
                )
                .subtract(
                        valorTransferido
                );
    }


    // =========================================
    // SOMAR LANÇAMENTOS
    // =========================================

    private BigDecimal somar(
            List<Lancamento> lancamentos,
            String nomeUsuario,
            TipoLancamento... tipos) {


        return lancamentos
                .stream()

                .filter(lancamento ->
                        nomeUsuario
                                .equalsIgnoreCase(
                                        lancamento.getResponsavel()
                                )
                )

                .filter(lancamento -> {

                    for (TipoLancamento tipo : tipos) {

                        if (lancamento.getTipo()
                                == tipo) {

                            return true;
                        }
                    }


                    return false;
                })

                .map(
                        Lancamento::getValor
                )

                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }


    // =========================================
    // CALCULAR VALOR USADO DO SALDO
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
    // VALIDAR SALDO INICIAL
    // =========================================

    private String validarValorInicial(
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
    // VALIDAR TRANSFERÊNCIA
    // =========================================

    private String validarTransferencia(
            BigDecimal valor) {


        if (valor == null) {

            return "Informe o valor que deseja transferir.";
        }


        if (valor.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            return "O valor da transferência deve ser maior que zero.";
        }


        if (valor.compareTo(
                VALOR_MAXIMO
        ) > 0) {

            return "O valor informado é muito alto.";
        }


        BigDecimal valorNormalizado =
                valor.stripTrailingZeros();


        if (valorNormalizado.scale() > 2) {

            return "A transferência pode ter no máximo duas casas decimais.";
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
    // FORMATAR MOEDA
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
    // FORMATAR USUÁRIO
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