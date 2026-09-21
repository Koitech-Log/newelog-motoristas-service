package br.com.newelog.motoristas.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.CrossOrigin;
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
import br.com.newelog.motoristas.dto.PaginaDTO;
import br.com.newelog.motoristas.model.StatusMotorista;
import br.com.newelog.motoristas.service.MotoristaService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/motoristas")
@CrossOrigin(originPatterns = "http://localhost:*")
public class MotoristaController {

    private static final int TAMANHO_PAGINA_PADRAO = 12;

    private final MotoristaService motoristaService;

    public MotoristaController(MotoristaService motoristaService) {
        this.motoristaService = motoristaService;
    }

    @GetMapping
    public PaginaDTO<MotoristaResumoDTO> listar(
            @RequestParam(required = false) StatusMotorista status,
            @RequestParam(required = false) String destino,
            @RequestParam(required = false, name = "busca") String busca,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "" + TAMANHO_PAGINA_PADRAO) int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("nome").ascending());
        return motoristaService.listar(status, destino, busca, pageable);
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
