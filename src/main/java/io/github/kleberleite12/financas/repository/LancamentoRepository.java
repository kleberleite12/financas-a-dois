package io.github.kleberleite12.financas.repository;

import io.github.kleberleite12.financas.model.Lancamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LancamentoRepository extends JpaRepository<Lancamento, Long> {

    List<Lancamento> findByDataBetween(LocalDate inicio, LocalDate fim);

}