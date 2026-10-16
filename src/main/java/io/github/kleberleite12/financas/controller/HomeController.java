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

    private final SaldoContaRepository saldoContaRepository;


    public HomeController(
            LancamentoRepository lancamentoRepository,
            SaldoContaRepository saldoContaRepository) {

        this.lancamentoRepository =
                lancamentoRepository;

        this.saldoContaRepository =
                saldoContaRepository;
    }


    // =========================================
    // DASHBOARD
    // =========================================

    @GetMapping("/")
    public String home(
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) Integer mes,
            Authentication authentication,
            Model model) {


        String nomeUsuario =
                formatarNomeUsuario(
                        authentication.getName()
                );


        // =========================================
        // PERÍODO
        // =========================================

        YearMonth periodo;


        if (ano != null
                && mes != null
                && mes >= 1
                && mes <= 12) {

            periodo =
                    YearMonth.of(
                            ano,
                            mes
                    );

        } else {

            periodo =
                    YearMonth.now();
        }


        LocalDate inicioMes =
                periodo.atDay(1);


        LocalDate fimMes =
                periodo.atEndOfMonth();


        // =========================================
        // LANÇAMENTOS
        // =========================================

        List<Lancamento> lancamentosMes =
                lancamentoRepository
                        .findByDataBetween(
                                inicioMes,
                                fimMes
                        );


        List<Lancamento> todosLancamentos =
                lancamentoRepository
                        .findAll();


        // =========================================
        // KLEBER
        // =========================================

        DadosFinanceiros kleber =
                calcularDados(
                        lancamentosMes,
                        todosLancamentos,
                        "Kleber",
                        inicioMes
                );


        // =========================================
        // GIOVANNA
        // =========================================

        DadosFinanceiros giovanna =
                calcularDados(
                        lancamentosMes,
                        todosLancamentos,
                        "Giovanna",
                        inicioMes
                );


        // =========================================
        // USUÁRIO LOGADO
        // =========================================

        DadosFinanceiros usuarioAtual;


        if (nomeUsuario.equalsIgnoreCase(
                "Giovanna"
        )) {

            usuarioAtual =
                    giovanna;

        } else {

            usuarioAtual =
                    kleber;
        }


        // =========================================
        // NAVEGAÇÃO ENTRE MESES
        // =========================================

        YearMonth periodoAnterior =
                periodo.minusMonths(1);


        YearMonth proximoPeriodo =
                periodo.plusMonths(1);


        String nomeMes =
                periodo
                        .getMonth()
                        .getDisplayName(
                                TextStyle.FULL,
                                Locale.forLanguageTag(
                                        "pt-BR"
                                )
                        );


        nomeMes =
                nomeMes
                        .substring(
                                0,
                                1
                        )
                        .toUpperCase()

                        + nomeMes.substring(
                        1
                );


        // =========================================
        // DADOS GERAIS
        // =========================================

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


        // =========================================
        // DADOS INDIVIDUAIS
        // =========================================

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


        // =========================================
        // RESUMO DO USUÁRIO LOGADO
        // =========================================

        BigDecimal minhasReceitas =
                usuarioAtual
                        .rendaPrincipal()
                        .add(
                                usuarioAtual.rendaExtra()
                        );


        BigDecimal meusGastos =
                usuarioAtual
                        .cartaoCredito()
                        .add(
                                usuarioAtual.outrosGastos()
                        );


        model.addAttribute(
                "minhasReceitas",
                formatarMoeda(
                        minhasReceitas
                )
        );


        model.addAttribute(
                "meusGastos",
                formatarMoeda(
                        meusGastos
                )
        );


        model.addAttribute(
                "meuGuardadoMes",
                formatarMoeda(
                        usuarioAtual.guardadoMes()
                )
        );


        // =========================================
        // SALDO NA CONTA
        // =========================================

        model.addAttribute(
                "meuSaldoContaInicial",
                formatarMoeda(
                        usuarioAtual.saldoContaInicial()
                )
        );


        model.addAttribute(
                "meuValorTransferido",
                formatarMoeda(
                        usuarioAtual.valorTransferido()
                )
        );


        model.addAttribute(
                "meusGastosSaldoConta",
                formatarMoeda(
                        usuarioAtual.gastosSaldoConta()
                )
        );


        model.addAttribute(
                "meuSaldoContaAtual",
                formatarMoeda(
                        usuarioAtual.saldoContaAtual()
                )
        );


        // =========================================
        // DISPONÍVEL DAS RECEITAS
        // =========================================

        model.addAttribute(
                "meuDisponivelReceitas",
                formatarMoeda(
                        usuarioAtual.disponivelReceitas()
                )
        );


        // =========================================
        // TOTAL DISPONÍVEL
        // =========================================

        model.addAttribute(
                "meuTotalDisponivel",
                formatarMoeda(
                        usuarioAtual.totalDisponivel()
                )
        );


        model.addAttribute(
                "totalDisponivelNegativo",
                usuarioAtual
                        .totalDisponivel()
                        .compareTo(
                                BigDecimal.ZERO
                        ) < 0
        );


        /*
         * Mantidos por compatibilidade
         * com telas e testes existentes.
         */
        model.addAttribute(
                "meuDisponivelAgora",
                formatarMoeda(
                        usuarioAtual.totalDisponivel()
                )
        );


        model.addAttribute(
                "disponivelNegativo",
                usuarioAtual
                        .totalDisponivel()
                        .compareTo(
                                BigDecimal.ZERO
                        ) < 0
        );


        // =========================================
        // META
        // =========================================

        model.addAttribute(
                "metaIndividual",
                formatarMoeda(
                        META_INDIVIDUAL
                )
        );


        return "home";
    }


    // =========================================
    // CALCULAR DADOS FINANCEIROS
    // =========================================

    private DadosFinanceiros calcularDados(
            List<Lancamento> lancamentosMes,
            List<Lancamento> todosLancamentos,
            String responsavel,
            LocalDate inicioMes) {


        // =========================================
        // RECEITAS
        // =========================================

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


        // =========================================
        // GASTOS
        // =========================================

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


        // =========================================
        // GUARDADO
        // =========================================

        BigDecimal guardadoMes =
                somar(
                        lancamentosMes,
                        responsavel,
                        TipoLancamento.GUARDADO
                );


        // =========================================
        // DADOS DO SALDO NA CONTA
        // =========================================

        SaldoConta saldoConta =
                saldoContaRepository
                        .findByResponsavelIgnoreCaseAndPeriodo(
                                responsavel,
                                inicioMes
                        )
                        .orElse(null);


        BigDecimal saldoContaInicial =
                BigDecimal.ZERO;


        BigDecimal valorTransferido =
                BigDecimal.ZERO;


        if (saldoConta != null) {

            if (saldoConta.getValorInicial() != null) {

                saldoContaInicial =
                        saldoConta.getValorInicial();
            }


            valorTransferido =
                    saldoConta.getValorTransferido();
        }


        // =========================================
        // GASTOS PAGOS COM SALDO NA CONTA
        // =========================================

        BigDecimal gastosSaldoConta =
                somarGastosSaldoConta(
                        lancamentosMes,
                        responsavel
                );


        // =========================================
        // SALDO NA CONTA ATUAL
        // =========================================

        /*
         * saldo atual
         * =
         * saldo inicial
         * + dinheiro transferido das receitas
         * - gastos pagos com saldo
         */
        BigDecimal saldoContaAtual =
                saldoContaInicial
                        .add(
                                valorTransferido
                        )
                        .subtract(
                                gastosSaldoConta
                        );


        // =========================================
        // OUTROS GASTOS DAS RECEITAS
        // =========================================

        /*
         * "outrosGastos" também contém gastos
         * pagos usando Saldo na conta.
         *
         * Esses gastos precisam ser retirados
         * daqui para não serem descontados
         * duas vezes.
         */
        BigDecimal outrosGastosDasReceitas =
                outrosGastos
                        .subtract(
                                gastosSaldoConta
                        );


        // =========================================
        // DISPONÍVEL DAS RECEITAS
        // =========================================

        /*
         * Agora também descontamos aquilo
         * que foi transferido para
         * Saldo na conta.
         */
        BigDecimal disponivelReceitas =
                rendaPrincipal
                        .add(
                                rendaExtra
                        )
                        .subtract(
                                cartaoCredito
                        )
                        .subtract(
                                outrosGastosDasReceitas
                        )
                        .subtract(
                                guardadoMes
                        )
                        .subtract(
                                valorTransferido
                        );


        // =========================================
        // TOTAL DISPONÍVEL
        // =========================================

        /*
         * O valor transferido sai das receitas
         * e entra no saldo.
         *
         * Portanto, o TOTAL não muda.
         */
        BigDecimal totalDisponivel =
                disponivelReceitas
                        .add(
                                saldoContaAtual
                        );


        // =========================================
        // META ACUMULADA
        // =========================================

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
                                new BigDecimal(
                                        "100"
                                )
                        );


        BigDecimal percentualBarra =
                percentualMeta
                        .min(
                                new BigDecimal(
                                        "100"
                                )
                        );


        return new DadosFinanceiros(
                rendaPrincipal,
                rendaExtra,
                cartaoCredito,
                outrosGastos,
                guardadoMes,
                guardadoAcumulado,
                percentualMeta,
                percentualBarra,
                saldoContaInicial,
                valorTransferido,
                gastosSaldoConta,
                saldoContaAtual,
                disponivelReceitas,
                totalDisponivel
        );
    }


    // =========================================
    // SOMAR POR TIPO
    // =========================================

    private BigDecimal somar(
            List<Lancamento> lancamentos,
            String responsavel,
            TipoLancamento... tipos) {


        return lancamentos
                .stream()

                .filter(lancamento ->
                        lancamento.getResponsavel() != null
                                && lancamento
                                .getResponsavel()
                                .equalsIgnoreCase(
                                        responsavel
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
    // GASTOS PAGOS COM SALDO
    // =========================================

    private BigDecimal somarGastosSaldoConta(
            List<Lancamento> lancamentos,
            String responsavel) {


        return lancamentos
                .stream()

                .filter(lancamento ->
                        lancamento.getResponsavel() != null
                                && lancamento
                                .getResponsavel()
                                .equalsIgnoreCase(
                                        responsavel
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
    // ADICIONAR DADOS DA PESSOA
    // =========================================

    private void adicionarDadosPessoa(
            Model model,
            String prefixo,
            DadosFinanceiros dados) {


        model.addAttribute(
                prefixo + "RendaPrincipal",
                formatarMoeda(
                        dados.rendaPrincipal()
                )
        );


        model.addAttribute(
                prefixo + "RendaExtra",
                formatarMoeda(
                        dados.rendaExtra()
                )
        );


        model.addAttribute(
                prefixo + "CartaoCredito",
                formatarMoeda(
                        dados.cartaoCredito()
                )
        );


        model.addAttribute(
                prefixo + "OutrosGastos",
                formatarMoeda(
                        dados.outrosGastos()
                )
        );


        model.addAttribute(
                prefixo + "GuardadoMes",
                formatarMoeda(
                        dados.guardadoMes()
                )
        );


        model.addAttribute(
                prefixo + "GuardadoAcumulado",
                formatarMoeda(
                        dados.guardadoAcumulado()
                )
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


        // =========================================
        // SALDO
        // =========================================

        model.addAttribute(
                prefixo + "SaldoContaInicial",
                formatarMoeda(
                        dados.saldoContaInicial()
                )
        );


        model.addAttribute(
                prefixo + "ValorTransferido",
                formatarMoeda(
                        dados.valorTransferido()
                )
        );


        model.addAttribute(
                prefixo + "GastosSaldoConta",
                formatarMoeda(
                        dados.gastosSaldoConta()
                )
        );


        model.addAttribute(
                prefixo + "SaldoContaAtual",
                formatarMoeda(
                        dados.saldoContaAtual()
                )
        );


        model.addAttribute(
                prefixo + "DisponivelReceitas",
                formatarMoeda(
                        dados.disponivelReceitas()
                )
        );


        model.addAttribute(
                prefixo + "TotalDisponivel",
                formatarMoeda(
                        dados.totalDisponivel()
                )
        );


        /*
         * Compatibilidade com páginas antigas.
         */
        model.addAttribute(
                prefixo + "Saldo",
                formatarMoeda(
                        dados.totalDisponivel()
                )
        );
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


    // =========================================
    // DADOS FINANCEIROS
    // =========================================

    private record DadosFinanceiros(

            BigDecimal rendaPrincipal,

            BigDecimal rendaExtra,

            BigDecimal cartaoCredito,

            BigDecimal outrosGastos,

            BigDecimal guardadoMes,

            BigDecimal guardadoAcumulado,

            BigDecimal percentualMeta,

            BigDecimal percentualBarra,

            BigDecimal saldoContaInicial,

            BigDecimal valorTransferido,

            BigDecimal gastosSaldoConta,

            BigDecimal saldoContaAtual,

            BigDecimal disponivelReceitas,

            BigDecimal totalDisponivel

    ) {
    }
}