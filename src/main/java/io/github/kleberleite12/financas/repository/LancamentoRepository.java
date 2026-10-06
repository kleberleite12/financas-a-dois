package io.github.kleberleite12.financas.repository;

import io.github.kleberleite12.financas.model.Lancamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LancamentoRepository extends JpaRepository<Lancamento, Long> {
}