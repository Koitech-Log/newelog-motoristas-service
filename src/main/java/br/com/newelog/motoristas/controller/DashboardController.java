package br.com.newelog.motoristas.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import br.com.newelog.motoristas.dto.DashboardResponseDTO;
import br.com.newelog.motoristas.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public DashboardResponseDTO buscar(
            @RequestParam("ano") int ano,
            @RequestParam("mes") int mes
    ) {
        if (ano < 1 || ano > 9999) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O ano deve estar entre 1 e 9999."
            );
        }

        if (mes < 1 || mes > 12) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O mês deve estar entre 1 e 12."
            );
        }

        return dashboardService.buscar(ano, mes);
    }
}