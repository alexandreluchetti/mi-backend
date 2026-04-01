package br.com.alexandreluchetti.mibackend.entrypoint.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/arquivos")
@Tag(name = "Arquivos", description = "Endpoints para upload e consulta de processamento de arquivos")
public class ArquivoController {


    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void upload(
            @Parameter(description = "Arquivo delimitado por pipe (|) com cabeçalho válido", required = true)
            @RequestParam("file") MultipartFile file) throws IOException {

    }

    @GetMapping("/{id}/progresso")
    public void consultarProgresso(
            @Parameter(description = "ID único retornado no upload", required = true)
            @PathVariable String id) {

    }

    @GetMapping("/{id}/resultado")
    public void consultarResultado(
            @Parameter(description = "ID único retornado no upload", required = true)
            @PathVariable String id) {

    }
}
