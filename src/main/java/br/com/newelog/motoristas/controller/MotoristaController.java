package br.com.newelog.motoristas.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import br.com.newelog.motoristas.infrastructure.entity.Motorista;
import br.com.newelog.motoristas.service.ManifestoValidacaoService;
import br.com.newelog.motoristas.service.MotoristaService;

@RestController
@RequestMapping("/motoristas")
public class MotoristaController {

    private final MotoristaService service;
    private final ManifestoValidacaoService manifestoValidacaoService;

    public MotoristaController(MotoristaService service, ManifestoValidacaoService manifestoValidacaoService) {
        this.service = service;
        this.manifestoValidacaoService = manifestoValidacaoService;
}

    @PostMapping
    public ResponseEntity<Void> salvar(@RequestBody Motorista motorista) {
        service.salvarMotorista(motorista);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/validar-manifesto")
    public ResponseEntity<Void> validarManifesto(@Valid @RequestBody ManifestoValidacaoDTO dto) {
        manifestoValidacaoService.validar(dto);
        return ResponseEntity.ok().build();
}

    @GetMapping("/{id}")
    public ResponseEntity<Motorista> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(service.buscarMotoristaPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<Motorista>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/pesquisar")
    public ResponseEntity<List<Motorista>> pesquisarPorNome(@RequestParam String nome) {
        return ResponseEntity.ok(service.pesquisarPorNome(nome));
    }

    @GetMapping("/cpf-cnpj/{cpfCnpj}")
    public ResponseEntity<Motorista> buscarPorCpfCnpj(@PathVariable String cpfCnpj) {
        return ResponseEntity.ok(service.buscarMotoristaPorCpfCnpj(cpfCnpj));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Motorista> atualizar(@PathVariable Integer id, @RequestBody Motorista motorista) {
        return ResponseEntity.ok(service.atualizarMotorista(id, motorista));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Integer id) {
        service.removerMotorista(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}