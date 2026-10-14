package br.com.newelog.motoristas.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.newelog.motoristas.dto.ImportacaoVeiculosRequestDTO;
import br.com.newelog.motoristas.dto.ResultadoImportacaoVeiculosDTO;
import br.com.newelog.motoristas.service.VeiculoImportacaoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/veiculos")
public class VeiculoController {

    private final VeiculoImportacaoService veiculoImportacaoService;

    public VeiculoController(VeiculoImportacaoService veiculoImportacaoService) {
        this.veiculoImportacaoService = veiculoImportacaoService;
    }

    /**
     * Efetiva o cadastro/atualização de veículos a partir das linhas de um CSV —
     * chamado pelo manifesto-service depois de ler o arquivo enviado em
     * POST /veiculos/upload. O acesso é restrito ao perfil GESTOR em SecurityConfig.
     */
    @PostMapping("/cadastrar-de-csv")
    public ResponseEntity<ResultadoImportacaoVeiculosDTO> cadastrarDeCsv(
            @Valid @RequestBody ImportacaoVeiculosRequestDTO request
    ) {
        return ResponseEntity.ok(veiculoImportacaoService.importar(request.veiculos()));
    }
}