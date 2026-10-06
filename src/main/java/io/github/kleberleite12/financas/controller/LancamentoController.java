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
import java.util.List;
import java.util.Locale;

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

    public LancamentoController(LancamentoRepository lancamentoRepository) {
        this.lancamentoRepository = lancamentoRepository;
    }

    @GetMapping("/lancamentos")
    public String listar(
            @RequestParam(required = false) String periodo,
            @RequestParam(required = false) String responsavel,
            @RequestParam(required = false) TipoLancamento tipo,
            @RequestParam(required = false) String busca,
            Model model,
            Authentication authentication) {

        String nomeUsuario =
                formatarNomeUsuario(authentication.getName());

        List<Lancamento> lancamentos =
                buscarLancamentosFiltrados(
                        periodo,
                        responsavel,
                        tipo,
                        busca
                );

        prepararPagina(
                model,
                new Lancamento(),
                lancamentos,
                nomeUsuario,
                false,
                periodo,
                responsavel,
                tipo,
                busca
        );

        return "lancamentos";
    }

    @GetMapping("/lancamentos/editar/{id}")
    public String editar(
            @PathVariable Long id,
            Model model,
            Authentication authentication) {

        String nomeUsuario =
                formatarNomeUsuario(authentication.getName());

        Lancamento lancamento =
                lancamentoRepository.findById(id)
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

        if (lancamento.getTipo() == TipoLancamento.RECEITA) {
            lancamento.setTipo(
                    TipoLancamento.RENDA_PRINCIPAL
            );
        }

        if (lancamento.getTipo() == TipoLancamento.GASTO) {
            lancamento.setTipo(
                    TipoLancamento.CARTAO_CREDITO
            );
        }

        prepararPagina(
                model,
                lancamento,
                buscarLancamentosFiltrados(
                        null,
                        null,
                        null,
                        null
                ),
                nomeUsuario,
                true,
                null,
                null,
                null,
                null
        );

        return "lancamentos";
    }

    @PostMapping("/lancamentos")
    public String salvar(
            Lancamento lancamento,
            @RequestParam String periodoLancamento,
            Authentication authentication) {

        String nomeUsuario =
                formatarNomeUsuario(authentication.getName());

        try {

            YearMonth periodo =
                    YearMonth.parse(periodoLancamento);

            lancamento.setData(
                    periodo.atDay(1)
            );

        } catch (Exception erro) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mês e ano inválidos."
            );
        }

        if (lancamento.getId() == null) {

            lancamento.setResponsavel(
                    nomeUsuario
            );

        } else {

            Lancamento existente =
                    lancamentoRepository
                            .findById(lancamento.getId())
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

    @PostMapping("/lancamentos/excluir/{id}")
    public String excluir(
            @PathVariable Long id,
            Authentication authentication) {

        String nomeUsuario =
                formatarNomeUsuario(authentication.getName());

        Lancamento lancamento =
                lancamentoRepository.findById(id)
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

    private List<Lancamento> buscarLancamentosFiltrados(
            String periodo,
            String responsavel,
            TipoLancamento tipo,
            String busca) {

        List<Lancamento> lancamentos =
                lancamentoRepository.findAll(
                        Sort.by(
                                Sort.Order.desc("data"),
                                Sort.Order.desc("id")
                        )
                );

        return lancamentos.stream()

                .filter(lancamento ->
                        filtrarPeriodo(
                                lancamento,
                                periodo
                        )
                )

                .filter(lancamento ->
                        responsavel == null
                                || responsavel.isBlank()
                                || lancamento.getResponsavel()
                                .equalsIgnoreCase(responsavel)
                )

                .filter(lancamento ->
                        tipo == null
                                || lancamento.getTipo() == tipo
                )

                .filter(lancamento ->
                        filtrarBusca(
                                lancamento,
                                busca
                        )
                )

                .toList();
    }

    private boolean filtrarPeriodo(
            Lancamento lancamento,
            String periodo) {

        if (periodo == null || periodo.isBlank()) {
            return true;
        }

        try {

            YearMonth periodoSelecionado =
                    YearMonth.parse(periodo);

            YearMonth periodoLancamento =
                    YearMonth.from(
                            lancamento.getData()
                    );

            return periodoSelecionado
                    .equals(periodoLancamento);

        } catch (Exception erro) {

            return true;
        }
    }

    private boolean filtrarBusca(
            Lancamento lancamento,
            String busca) {

        if (busca == null || busca.isBlank()) {
            return true;
        }

        String textoBusca =
                busca.trim()
                        .toLowerCase(Locale.ROOT);

        String descricao =
                lancamento.getDescricao() == null
                        ? ""
                        : lancamento.getDescricao()
                        .toLowerCase(Locale.ROOT);

        String categoria =
                lancamento.getCategoria() == null
                        ? ""
                        : lancamento.getCategoria()
                        .toLowerCase(Locale.ROOT);

        return descricao.contains(textoBusca)
                || categoria.contains(textoBusca);
    }

    private void prepararPagina(
            Model model,
            Lancamento lancamento,
            List<Lancamento> lancamentos,
            String nomeUsuario,
            boolean modoEdicao,
            String periodo,
            String responsavel,
            TipoLancamento tipo,
            String busca) {

        String periodoLancamento;

        if (lancamento.getData() != null) {

            periodoLancamento =
                    YearMonth.from(
                            lancamento.getData()
                    ).toString();

        } else {

            periodoLancamento =
                    YearMonth.now().toString();
        }

        model.addAttribute(
                "lancamentos",
                lancamentos
        );

        model.addAttribute(
                "lancamento",
                lancamento
        );

        model.addAttribute(
                "tipos",
                TIPOS_ATIVOS
        );

        model.addAttribute(
                "modoEdicao",
                modoEdicao
        );

        model.addAttribute(
                "nomeUsuario",
                nomeUsuario
        );

        model.addAttribute(
                "periodoLancamento",
                periodoLancamento
        );

        model.addAttribute(
                "periodoSelecionado",
                periodo
        );

        model.addAttribute(
                "responsavelSelecionado",
                responsavel
        );

        model.addAttribute(
                "tipoSelecionado",
                tipo
        );

        model.addAttribute(
                "busca",
                busca
        );

        model.addAttribute(
                "quantidadeResultados",
                lancamentos.size()
        );
    }

    private void verificarDono(
            Lancamento lancamento,
            String nomeUsuario) {

        if (!lancamento
                .getResponsavel()
                .equalsIgnoreCase(nomeUsuario)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Você não pode alterar o lançamento de outro usuário."
            );
        }
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