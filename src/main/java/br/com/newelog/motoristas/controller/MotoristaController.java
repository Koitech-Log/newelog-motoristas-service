package br.com.newelog.motoristas.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.newelog.motoristas.dto.AtualizarStatusRequestDTO;
import br.com.newelog.motoristas.dto.MotoristaDetalheDTO;
import br.com.newelog.motoristas.dto.MotoristaResumoDTO;
import br.com.newelog.motoristas.model.StatusMotorista;
import br.com.newelog.motoristas.service.MotoristaService;
import jakarta.validation.Valid;

/**
 * Endpoints REST de motoristas. Os parâmetros de filtro (status, destino, busca)
 * espelham exatamente os filtros já existentes na tela de Motoristas do protótipo
 * front-end (ver /view-drivers no repositório newelog-frota-web).
 */
@RestController
@RequestMapping("/api/motoristas")
public class MotoristaController {

    private final MotoristaService motoristaService;

    public MotoristaController(MotoristaService motoristaService) {
        this.motoristaService = motoristaService;
    }

    @GetMapping
    public List<MotoristaResumoDTO> listar(
            @RequestParam(required = false) StatusMotorista status,
            @RequestParam(required = false) String destino,
            @RequestParam(required = false, name = "busca") String busca
    ) {
        return motoristaService.listar(status, destino, busca);
    }

    @GetMapping("/{id}")
    public MotoristaDetalheDTO buscarDetalhe(@PathVariable Long id) {
        return motoristaService.buscarDetalhe(id);
    }

    /**
     * Atualiza a disponibilidade do motorista. Permitido tanto para o perfil
     * Operador quanto Gestor (ver documento "Respostas do Parceiro", seção 1.4) —
     * a checagem de perfil acontece no gateway, não aqui.
     */
    @PatchMapping("/{id}/status")
    public MotoristaDetalheDTO atualizarStatus(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarStatusRequestDTO request
    ) {
        return motoristaService.atualizarStatus(id, request.status());
    }
}
