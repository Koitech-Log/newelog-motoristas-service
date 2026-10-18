package br.com.newelog.motoristas.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import br.com.newelog.motoristas.dto.RentabilidadeMediaResponseDTO;
import br.com.newelog.motoristas.service.RentabilidadeAnaliseService;

@RestController
@RequestMapping("/api/dashboard/rentabilidade-media")
public class RentabilidadeMediaController {

    private final RentabilidadeAnaliseService service;

    public RentabilidadeMediaController(RentabilidadeAnaliseService service) {
        this.service = service;
    }

    @GetMapping
    public RentabilidadeMediaResponseDTO buscar(
            @RequestParam("ano") int ano,
            @RequestParam("mes") int mes
    ) {
        if (ano < 1 || ano > 9999) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O ano deve estar entre 1 e 9999.");
        }
        if (mes < 1 || mes > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O mês deve estar entre 1 e 12.");
        }
        return service.buscar(ano, mes);
    }
}