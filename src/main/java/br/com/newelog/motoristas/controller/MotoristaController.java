package br.com.newelog.motoristas.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.newelog.motoristas.dto.AtualizarStatusRequestDTO;
import br.com.newelog.motoristas.dto.ManifestoCadastroDTO;
import br.com.newelog.motoristas.dto.MotoristaDetalheDTO;
import br.com.newelog.motoristas.dto.MotoristaResumoDTO;
import br.com.newelog.motoristas.dto.PaginaDTO;
import br.com.newelog.motoristas.model.Motorista;
import br.com.newelog.motoristas.model.StatusMotorista;
import br.com.newelog.motoristas.service.ManifestoCadastroService;
import br.com.newelog.motoristas.service.ManifestoValidacaoService;
import br.com.newelog.motoristas.service.MotoristaService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/motoristas")
@CrossOrigin(originPatterns = "http://localhost:*")
public class MotoristaController {

    private static final int TAMANHO_PAGINA_PADRAO = 12;

    private final MotoristaService motoristaService;
    private final ManifestoValidacaoService manifestoValidacaoService;
    private final ManifestoCadastroService manifestoCadastroService;

    public MotoristaController(
            MotoristaService motoristaService,
            ManifestoValidacaoService manifestoValidacaoService,
            ManifestoCadastroService manifestoCadastroService
    ) {
        this.motoristaService = motoristaService;
        this.manifestoValidacaoService = manifestoValidacaoService;
        this.manifestoCadastroService = manifestoCadastroService;
    }

    @PostMapping
    public ResponseEntity<Void> salvar(@RequestBody Motorista motorista) {
        motoristaService.salvarMotorista(motorista);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/validar-manifesto")
    public ResponseEntity<Void> validarManifesto(@Valid @RequestBody ManifestoValidacaoDTO dto) {
        manifestoValidacaoService.validar(dto);
        return ResponseEntity.ok().build();
    }

    /**
     * Efetiva o cadastro/atualização de um motorista a partir de uma linha de
     * manifesto — chamado pelo manifesto-service depois que a linha passa por
     * /validar-manifesto. Diferente de POST /api/motoristas (cadastro manual
     * genérico): aqui o casamento é por CPF (upsert) e o veículo é resolvido
     * pela placa, já que o manifesto não referencia um veiculoId existente.
     */
    @PostMapping("/cadastrar-de-manifesto")
    public ResponseEntity<MotoristaDetalheDTO> cadastrarDeManifesto(
            @Valid @RequestBody ManifestoCadastroDTO dto
    ) {
        return ResponseEntity.ok(manifestoCadastroService.cadastrar(dto));
    }

    @GetMapping
    public PaginaDTO<MotoristaResumoDTO> listar(
            @RequestParam(required = false) StatusMotorista status,
            @RequestParam(required = false) String destino,
            @RequestParam(required = false, name = "busca") String busca,
            @RequestParam(required = false) String tipoVeiculo,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "" + TAMANHO_PAGINA_PADRAO) int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("nome").ascending());
        return motoristaService.listar(status, destino, busca, tipoVeiculo, pageable);
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
