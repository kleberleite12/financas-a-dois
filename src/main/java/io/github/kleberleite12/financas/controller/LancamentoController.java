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

    // =========================
    // SEUS LANÇAMENTOS
    // =========================

    @GetMapping("/lancamentos")
    public String listar(
            @RequestParam(required = false) String visualizacao,
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

        List<GrupoLancamentos> gruposLancamentos =
                agruparPorMes(
                        lancamentos
                );

        model.addAttribute(
                "lancamentos",
                lancamentos
        );

        model.addAttribute(
                "gruposLancamentos",
                gruposLancamentos
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

        return "lancamentos";
    }

    // =========================
    // NOVO LANÇAMENTO
    // =========================

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

    // =========================
    // EDITAR
    // =========================

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

    // =========================
    // SALVAR
    // =========================

    @PostMapping("/lancamentos")
    public String salvar(
            Lancamento lancamento,
            @RequestParam String periodoLancamento,
            Authentication authentication) {

        String nomeUsuario =
                formatarNomeUsuario(
                        authentication.getName()
                );

        try {

            YearMonth periodo =
                    YearMonth.parse(
                            periodoLancamento
                    );

            lancamento.setData(
                    periodo.atDay(1)
            );

        } catch (Exception erro) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mês e ano inválidos."
            );
        }

        preencherCamposOpcionais(
                lancamento
        );

        if (lancamento.getId() == null) {

            lancamento.setResponsavel(
                    nomeUsuario
            );

        } else {

            Lancamento existente =
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

            lancamento.setResponsavel(
                    existente.getResponsavel()
            );
        }

        lancamentoRepository.save(
                lancamento
        );

        return "redirect:/lancamentos";
    }

    // =========================
    // EXCLUIR
    // =========================

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

    // =========================
    // AGRUPAR POR MÊS
    // =========================

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

            resultado.add(
                    new GrupoLancamentos(
                            formatarPeriodo(
                                    entrada.getKey()
                            ),
                            entrada.getValue()
                    )
            );
        }

        return resultado;
    }

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

        return mes + " de " + periodo.getYear();
    }

    // =========================
    // FORMULÁRIO
    // =========================

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
                    lancamento.getTipo()
                            .getDescricao()
            );
        }

        if (lancamento.getCategoria() == null
                || lancamento.getCategoria().isBlank()) {

            lancamento.setCategoria(
                    lancamento.getTipo()
                            .getDescricao()
            );
        }
    }

    // =========================
    // SEGURANÇA
    // =========================

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

    // =========================
    // GRUPO DE LANÇAMENTOS
    // =========================

    public static class GrupoLancamentos {

        private final String titulo;
        private final List<Lancamento> lancamentos;

        public GrupoLancamentos(
                String titulo,
                List<Lancamento> lancamentos) {

            this.titulo = titulo;
            this.lancamentos = lancamentos;
        }

        public String getTitulo() {
            return titulo;
        }

        public List<Lancamento> getLancamentos() {
            return lancamentos;
        }
    }
}