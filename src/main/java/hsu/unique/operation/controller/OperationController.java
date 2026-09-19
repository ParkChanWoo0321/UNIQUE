package hsu.unique.operation.controller;

import hsu.unique.operation.dto.BonusExecutionResponse;
import hsu.unique.operation.dto.FinishGameResponse;
import hsu.unique.operation.service.OperationService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operations")
public class OperationController {

    private final OperationService operationService;

    public OperationController(OperationService operationService) {
        this.operationService = operationService;
    }

    @PostMapping("/bonus")
    public BonusExecutionResponse executeBonus(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate) {
        return operationService.executeBonus(eventDate);
    }

    @PostMapping("/finish")
    public FinishGameResponse finishGame(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate) {
        return operationService.finishGame(eventDate);
    }
}
