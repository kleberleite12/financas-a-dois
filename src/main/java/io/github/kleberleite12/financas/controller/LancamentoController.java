package io.github.kleberleite12.financas.controller;

import io.github.kleberleite12.financas.model.Lancamento;
import io.github.kleberleite12.financas.model.TipoLancamento;
import io.github.kleberleite12.financas.repository.LancamentoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class LancamentoController {

    private final LancamentoRepository lancamentoRepository;

    private static final BigDecimal VALOR_MAXIMO =
            new BigDecimal("99999999.99");

    private static final List<TipoLancamento> TIPOS_ATIVOS = List.of(
            TipoLancamento.RENDA_PRINCIPAL,
            TipoLancamento.RENDA_EXTRA,
            TipoLancamento.CARTAO_CREDITO,
            TipoLancamento.OUTRO_GASTO,
            TipoLancamento.GUARDADO
    );

    public LancamentoController(
            LancamentoRepository lancamentoRepository) {

        this.lancamentoRepository = lancamentoRepository;
    }


    // =========================================
    // SEUS LANÇAMENTOS
    // =========================================

    @GetMapping("/lancamentos")
    public String listar(
            @RequestParam(required = false) String visualizacao,
            @RequestParam(required = false) String periodo,
            @RequestParam(required = false) String resumo,
            Model model,
            Authentication authentication) {

        String nomeUsuario =
                formatarNomeUsuario(
                        authentication.getName()
                );

        String nomeOutroUsuario =
                nomeUsuario.equalsIgnoreCase("Kleber")
                        ? "Giovanna"
                        : "Kleber";

        boolean visualizandoOutro =
                "outro".equalsIgnoreCase(
                        visualizacao
                );

        String responsavelExibido =
                visualizandoOutro
                        ? nomeOutroUsuario
                        : nomeUsuario;


        List<Lancamento> lancamentos =
                lancamentoRepository.findAll(
                                Sort.by(
                                        Sort.Order.desc("data"),
                                        Sort.Order.desc("id")
                                )
                        )
                        .stream()
                        .filter(lancamento ->
                                responsavelExibido.equalsIgnoreCase(
                                        lancamento.getResponsavel()
                                )
                        )
                        .toList();


        // =========================================
        // FILTRO RECEITAS / GASTOS / GUARDADO
        // =========================================

        boolean filtroAtivo =
                periodo != null
                        && !periodo.isBlank()
                        && resumo != null
                        && !resumo.isBlank();

        List<Lancamento> lancamentosFiltrados =
                new ArrayList<>();

        String tituloFiltro = "";

        String periodoFiltroFormatado = "";

        String totalFiltro =
                formatarMoeda(
                        BigDecimal.ZERO
                );


        if (filtroAtivo) {

            try {

                YearMonth periodoSelecionado =
                        YearMonth.parse(
                                periodo
                        );

                periodoFiltroFormatado =
                        formatarPeriodo(
                                periodoSelecionado
                        );

                lancamentosFiltrados =
                        lancamentos.stream()

                                .filter(lancamento ->
                                        YearMonth
                                                .from(
                                                        lancamento.getData()
                                                )
                                                .equals(
                                                        periodoSelecionado
                                                )
                                )

                                .filter(lancamento ->
                                        pertenceAoResumo(
                                                lancamento,
                                                resumo
                                        )
                                )

                                .toList();


                if ("receitas".equalsIgnoreCase(resumo)) {

                    tituloFiltro = "Receitas";

                } else if ("gastos".equalsIgnoreCase(resumo)) {

                    tituloFiltro = "Gastos";

                } else if ("guardado".equalsIgnoreCase(resumo)) {

                    tituloFiltro = "Guardado";

                } else {

                    filtroAtivo = false;
                }


                BigDecimal total =
                        lancamentosFiltrados.stream()
                                .map(
                                        Lancamento::getValor
                                )
                                .reduce(
                                        BigDecimal.ZERO,
                                        BigDecimal::add
                                );

                totalFiltro =
                        formatarMoeda(
                                total
                        );

            } catch (Exception erro) {

                filtroAtivo = false;
            }
        }


        List<GrupoLancamentos> gruposLancamentos =
                agruparPorMes(
                        lancamentos
                );


        model.addAttribute(
                "gruposLancamentos",
                gruposLancamentos
        );

        model.addAttribute(
                "lancamentosFiltrados",
                lancamentosFiltrados
        );

        model.addAttribute(
                "nomeUsuario",
                nomeUsuario
        );

        model.addAttribute(
                "nomeOutroUsuario",
                nomeOutroUsuario
        );

        model.addAttribute(
                "responsavelExibido",
                responsavelExibido
        );

        model.addAttribute(
                "visualizandoOutro",
                visualizandoOutro
        );

        model.addAttribute(
                "quantidadeResultados",
                lancamentos.size()
        );

        model.addAttribute(
                "filtroAtivo",
                filtroAtivo
        );

        model.addAttribute(
                "tituloFiltro",
                tituloFiltro
        );

        model.addAttribute(
                "periodoFiltroFormatado",
                periodoFiltroFormatado
        );

        model.addAttribute(
                "totalFiltro",
                totalFiltro
        );

        return "lancamentos";
    }


    // =========================================
    // NOVO LANÇAMENTO
    // =========================================

    @GetMapping("/lancamentos/novo")
    public String novo(
            Model model,
            Authentication authentication) {

        String nomeUsuario =
                formatarNomeUsuario(
                        authentication.getName()
                );

        prepararFormulario(
                model,
                new Lancamento(),
                nomeUsuario,
                false
        );

        return "novo-lancamento";
    }


    // =========================================
    // EDITAR
    // =========================================

    @GetMapping("/lancamentos/editar/{id}")
    public String editar(
            @PathVariable Long id,
            Model model,
            Authentication authentication) {

        String nomeUsuario =
                formatarNomeUsuario(
                        authentication.getName()
                );

        Lancamento lancamento =
                lancamentoRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Lançamento não encontrado"
                                )
                        );

        verificarDono(
                lancamento,
                nomeUsuario
        );


        // Compatibilidade com registros antigos

        if (lancamento.getTipo()
                == TipoLancamento.RECEITA) {

            lancamento.setTipo(
                    TipoLancamento.RENDA_PRINCIPAL
            );
        }

        if (lancamento.getTipo()
                == TipoLancamento.GASTO) {

            lancamento.setTipo(
                    TipoLancamento.CARTAO_CREDITO
            );
        }


        prepararFormulario(
                model,
                lancamento,
                nomeUsuario,
                true
        );

        return "novo-lancamento";
    }


    // =========================================
    // SALVAR
    // =========================================

    @PostMapping("/lancamentos")
    public String salvar(
            Lancamento lancamento,
            @RequestParam(required = false) String periodoLancamento,
            Authentication authentication,
            Model model) {

        String nomeUsuario =
                formatarNomeUsuario(
                        authentication.getName()
                );


        /*
         * Se for edição, primeiro confirmamos se o
         * lançamento realmente pertence ao usuário.
         *
         * Isso acontece ANTES de qualquer alteração.
         */

        Lancamento existente = null;

        if (lancamento.getId() != null) {

            existente =
                    lancamentoRepository
                            .findById(
                                    lancamento.getId()
                            )
                            .orElseThrow(() ->
                                    new ResponseStatusException(
                                            HttpStatus.NOT_FOUND,
                                            "Lançamento não encontrado"
                                    )
                            );

            verificarDono(
                    existente,
                    nomeUsuario
            );
        }


        // =========================================
        // VALIDAÇÃO NO SERVIDOR
        // =========================================

        String erroValidacao =
                validarLancamento(
                        lancamento,
                        periodoLancamento
                );


        if (erroValidacao != null) {

            prepararFormularioComErro(
                    model,
                    lancamento,
                    nomeUsuario,
                    periodoLancamento,
                    erroValidacao
            );

            return "novo-lancamento";
        }


        // =========================================
        // PERÍODO
        // =========================================

        YearMonth periodo =
                YearMonth.parse(
                        periodoLancamento
                );

        lancamento.setData(
                periodo.atDay(1)
        );


        // =========================================
        // DESCRIÇÃO / CATEGORIA
        // =========================================

        preencherCamposOpcionais(
                lancamento
        );


        // =========================================
        // RESPONSÁVEL
        // =========================================

        if (existente == null) {

            /*
             * Novo lançamento:
             * sempre pertence ao usuário autenticado.
             */

            lancamento.setResponsavel(
                    nomeUsuario
            );

        } else {

            /*
             * Edição:
             * preservamos o responsável original.
             */

            lancamento.setResponsavel(
                    existente.getResponsavel()
            );
        }


        lancamentoRepository.save(
                lancamento
        );

        return "redirect:/lancamentos";
    }


    // =========================================
    // EXCLUIR
    // =========================================

    @PostMapping("/lancamentos/excluir/{id}")
    public String excluir(
            @PathVariable Long id,
            Authentication authentication) {

        String nomeUsuario =
                formatarNomeUsuario(
                        authentication.getName()
                );

        Lancamento lancamento =
                lancamentoRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Lançamento não encontrado"
                                )
                        );

        verificarDono(
                lancamento,
                nomeUsuario
        );

        lancamentoRepository.delete(
                lancamento
        );

        return "redirect:/lancamentos";
    }


    // =========================================
    // VALIDAÇÃO
    // =========================================

    private String validarLancamento(
            Lancamento lancamento,
            String periodoLancamento) {

        // Valor obrigatório

        if (lancamento.getValor() == null) {

            return "Informe o valor do lançamento.";
        }


        // Não permite zero nem valor negativo

        if (lancamento
                .getValor()
                .compareTo(BigDecimal.ZERO) <= 0) {

            return "O valor deve ser maior que zero.";
        }


        // Evita valor maior que o suportado pelo banco

        if (lancamento
                .getValor()
                .compareTo(VALOR_MAXIMO) > 0) {

            return "O valor informado é muito alto.";
        }


        // Apenas duas casas decimais

        BigDecimal valorNormalizado =
                lancamento
                        .getValor()
                        .stripTrailingZeros();

        if (valorNormalizado.scale() > 2) {

            return "O valor pode ter no máximo duas casas decimais.";
        }


        // Tipo obrigatório

        if (lancamento.getTipo() == null) {

            return "Selecione o tipo do lançamento.";
        }


        // Impede envio manual de tipos que não existem mais na tela

        if (!TIPOS_ATIVOS.contains(
                lancamento.getTipo()
        )) {

            return "Tipo de lançamento inválido.";
        }


        // Período obrigatório

        if (periodoLancamento == null
                || periodoLancamento.isBlank()) {

            return "Informe o mês e o ano.";
        }


        // Período precisa estar no formato correto

        try {

            YearMonth.parse(
                    periodoLancamento
            );

        } catch (Exception erro) {

            return "Mês e ano inválidos.";
        }


        // Descrição: máximo 255 caracteres

        if (lancamento.getDescricao() != null
                && lancamento.getDescricao().length() > 255) {

            return "A descrição pode ter no máximo 255 caracteres.";
        }


        // Categoria: máximo 255 caracteres

        if (lancamento.getCategoria() != null
                && lancamento.getCategoria().length() > 255) {

            return "A categoria pode ter no máximo 255 caracteres.";
        }


        return null;
    }


    private void prepararFormularioComErro(
            Model model,
            Lancamento lancamento,
            String nomeUsuario,
            String periodoLancamento,
            String mensagemErro) {

        boolean modoEdicao =
                lancamento.getId() != null;

        prepararFormulario(
                model,
                lancamento,
                nomeUsuario,
                modoEdicao
        );


        if (periodoLancamento != null) {

            model.addAttribute(
                    "periodoLancamento",
                    periodoLancamento
            );
        }


        model.addAttribute(
                "mensagemErro",
                mensagemErro
        );
    }


    // =========================================
    // FILTROS RÁPIDOS
    // =========================================

    private boolean pertenceAoResumo(
            Lancamento lancamento,
            String resumo) {

        TipoLancamento tipo =
                lancamento.getTipo();


        if ("receitas".equalsIgnoreCase(resumo)) {

            return tipo == TipoLancamento.RENDA_PRINCIPAL
                    || tipo == TipoLancamento.RENDA_EXTRA
                    || tipo == TipoLancamento.RECEITA;
        }


        if ("gastos".equalsIgnoreCase(resumo)) {

            return tipo == TipoLancamento.CARTAO_CREDITO
                    || tipo == TipoLancamento.OUTRO_GASTO
                    || tipo == TipoLancamento.GASTO;
        }


        if ("guardado".equalsIgnoreCase(resumo)) {

            return tipo == TipoLancamento.GUARDADO;
        }


        return false;
    }


    // =========================================
    // AGRUPAMENTO POR MÊS
    // =========================================

    private List<GrupoLancamentos> agruparPorMes(
            List<Lancamento> lancamentos) {

        Map<YearMonth, List<Lancamento>> grupos =
                new LinkedHashMap<>();


        for (Lancamento lancamento : lancamentos) {

            YearMonth periodo =
                    YearMonth.from(
                            lancamento.getData()
                    );

            grupos.computeIfAbsent(
                    periodo,
                    chave -> new ArrayList<>()
            ).add(lancamento);
        }


        List<GrupoLancamentos> resultado =
                new ArrayList<>();


        for (Map.Entry<YearMonth, List<Lancamento>> entrada
                : grupos.entrySet()) {

            List<Lancamento> lancamentosMes =
                    entrada.getValue();


            BigDecimal receitas =
                    calcularReceitas(
                            lancamentosMes
                    );

            BigDecimal gastos =
                    calcularGastos(
                            lancamentosMes
                    );

            BigDecimal guardado =
                    calcularGuardado(
                            lancamentosMes
                    );


            resultado.add(
                    new GrupoLancamentos(
                            entrada.getKey().toString(),
                            formatarPeriodo(
                                    entrada.getKey()
                            ),
                            lancamentosMes,
                            formatarMoeda(receitas),
                            formatarMoeda(gastos),
                            formatarMoeda(guardado)
                    )
            );
        }


        return resultado;
    }


    private BigDecimal calcularReceitas(
            List<Lancamento> lancamentos) {

        return lancamentos.stream()

                .filter(lancamento -> {

                    TipoLancamento tipo =
                            lancamento.getTipo();

                    return tipo
                            == TipoLancamento.RENDA_PRINCIPAL

                            || tipo
                            == TipoLancamento.RENDA_EXTRA

                            || tipo
                            == TipoLancamento.RECEITA;
                })

                .map(
                        Lancamento::getValor
                )

                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }


    private BigDecimal calcularGastos(
            List<Lancamento> lancamentos) {

        return lancamentos.stream()

                .filter(lancamento -> {

                    TipoLancamento tipo =
                            lancamento.getTipo();

                    return tipo
                            == TipoLancamento.CARTAO_CREDITO

                            || tipo
                            == TipoLancamento.OUTRO_GASTO

                            || tipo
                            == TipoLancamento.GASTO;
                })

                .map(
                        Lancamento::getValor
                )

                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }


    private BigDecimal calcularGuardado(
            List<Lancamento> lancamentos) {

        return lancamentos.stream()

                .filter(lancamento ->
                        lancamento.getTipo()
                                == TipoLancamento.GUARDADO
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
    // PERÍODO / MOEDA
    // =========================================

    private String formatarPeriodo(
            YearMonth periodo) {

        String mes =
                periodo.getMonth()
                        .getDisplayName(
                                TextStyle.FULL,
                                Locale.forLanguageTag("pt-BR")
                        );

        mes =
                mes.substring(0, 1).toUpperCase()
                        + mes.substring(1);

        return mes
                + " de "
                + periodo.getYear();
    }


    private String formatarMoeda(
            BigDecimal valor) {

        NumberFormat formato =
                NumberFormat.getCurrencyInstance(
                        Locale.forLanguageTag("pt-BR")
                );

        return formato.format(
                valor
        );
    }


    // =========================================
    // FORMULÁRIO
    // =========================================

    private void prepararFormulario(
            Model model,
            Lancamento lancamento,
            String nomeUsuario,
            boolean modoEdicao) {

        String periodoLancamento;


        if (lancamento.getData() != null) {

            periodoLancamento =
                    YearMonth
                            .from(
                                    lancamento.getData()
                            )
                            .toString();

        } else {

            periodoLancamento =
                    YearMonth.now()
                            .toString();
        }


        model.addAttribute(
                "lancamento",
                lancamento
        );

        model.addAttribute(
                "tipos",
                TIPOS_ATIVOS
        );

        model.addAttribute(
                "nomeUsuario",
                nomeUsuario
        );

        model.addAttribute(
                "modoEdicao",
                modoEdicao
        );

        model.addAttribute(
                "periodoLancamento",
                periodoLancamento
        );
    }


    private void preencherCamposOpcionais(
            Lancamento lancamento) {

        if (lancamento.getDescricao() == null
                || lancamento.getDescricao().isBlank()) {

            lancamento.setDescricao(
                    lancamento
                            .getTipo()
                            .getDescricao()
            );

        } else {

            lancamento.setDescricao(
                    lancamento
                            .getDescricao()
                            .trim()
            );
        }


        if (lancamento.getCategoria() == null
                || lancamento.getCategoria().isBlank()) {

            lancamento.setCategoria(
                    lancamento
                            .getTipo()
                            .getDescricao()
            );

        } else {

            lancamento.setCategoria(
                    lancamento
                            .getCategoria()
                            .trim()
            );
        }
    }


    // =========================================
    // SEGURANÇA
    // =========================================

    private void verificarDono(
            Lancamento lancamento,
            String nomeUsuario) {

        if (!nomeUsuario.equalsIgnoreCase(
                lancamento.getResponsavel()
        )) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Você não pode alterar o lançamento de outro usuário."
            );
        }
    }


    private String formatarNomeUsuario(
            String usuario) {

        if (usuario == null
                || usuario.isBlank()) {

            return "Usuário";
        }

        return usuario
                .substring(0, 1)
                .toUpperCase()
                + usuario
                .substring(1)
                .toLowerCase();
    }


    // =========================================
    // GRUPO DO MÊS
    // =========================================

    public static class GrupoLancamentos {

        private final String periodo;

        private final String titulo;

        private final List<Lancamento> lancamentos;

        private final String receitas;

        private final String gastos;

        private final String guardado;


        public GrupoLancamentos(
                String periodo,
                String titulo,
                List<Lancamento> lancamentos,
                String receitas,
                String gastos,
                String guardado) {

            this.periodo = periodo;
            this.titulo = titulo;
            this.lancamentos = lancamentos;
            this.receitas = receitas;
            this.gastos = gastos;
            this.guardado = guardado;
        }


        public String getPeriodo() {
            return periodo;
        }

        public String getTitulo() {
            return titulo;
        }

        public List<Lancamento> getLancamentos() {
            return lancamentos;
        }

        public String getReceitas() {
            return receitas;
        }

        public String getGastos() {
            return gastos;
        }

        public String getGuardado() {
            return guardado;
        }
    }
}