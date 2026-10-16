package io.github.kleberleite12.financas.repository;

import io.github.kleberleite12.financas.model.SaldoConta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface SaldoContaRepository
        extends JpaRepository<SaldoConta, Long> {

    Optional<SaldoConta>
    findByResponsavelIgnoreCaseAndPeriodo(
            String responsavel,
            LocalDate periodo
    );
}