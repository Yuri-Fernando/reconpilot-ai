package com.reconpilot.api.importation;

import com.reconpilot.api.transaction.TransactionSource;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/imports")
public class ImportController {
    private final ImportService service;

    public ImportController(ImportService service) { this.service = service; }

    @PostMapping("/bank")
    @ResponseStatus(HttpStatus.CREATED)
    public ImportResponse importBank(@RequestParam("file") MultipartFile file) {
        return service.importCsv(TransactionSource.BANK, file);
    }

    @PostMapping("/ledger")
    @ResponseStatus(HttpStatus.CREATED)
    public ImportResponse importLedger(@RequestParam("file") MultipartFile file) {
        return service.importCsv(TransactionSource.LEDGER, file);
    }

    @GetMapping
    public List<ImportBatchResponse> list() { return service.list(); }
}
