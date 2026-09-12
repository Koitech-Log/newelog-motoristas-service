package br.com.newelog.motoristas.service;

import java.util.*;
import org.springframework.stereotype.Service;

import br.com.newelog.motoristas.infrastructure.entity.Motorista;
import br.com.newelog.motoristas.infrastructure.repository.MotoristaRepository;

@Service 
public class MotoristaService {
    private final MotoristaRepository repository;

    public MotoristaService(MotoristaRepository repository){
        this.repository=repository;
    }

    public void salvarMotorista(Motorista motorista){
        repository.saveAndFlush(motorista);
    }

    public Motorista buscarMotoristaPorId(Integer id){
        return repository.findById(id).orElseThrow(()-> new RuntimeException("Motorista não encontrado: "+id));

    }
    public List<Motorista> pesquisarPorNome(String nome) {
    return repository.findByNomeContainingIgnoreCase(nome);
    }
    
    public Motorista buscarMotoristaPorCpfCnpj(String cpfCnpj){
        return repository.findByCpfCnpj(cpfCnpj).orElseThrow(()-> new RuntimeException("Motorista não encontrado:" + cpfCnpj));
    }
    
}
