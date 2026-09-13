package br.com.newelog.motoristas.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.newelog.motoristas.infrastructure.entity.Motorista;

import java.util.*;

public interface MotoristaRepository extends JpaRepository<Motorista, Integer>{


List<Motorista> findByNomeContainingIgnoreCase(String nome);

Optional<Motorista> findByCpfCnpj(String cpfCnpj);
    
}
