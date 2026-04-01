package br.com.alexandreluchetti.mibackend.entrypoint.controller;

import br.com.alexandreluchetti.mibackend.core.usecase.ArquivoUseCase;
import br.com.alexandreluchetti.mibackend.entrypoint.dto.ErrorResponseDTO;
import br.com.alexandreluchetti.mibackend.entrypoint.dto.ProgressoResponseDTO;
import br.com.alexandreluchetti.mibackend.entrypoint.dto.ResultadoResponseDTO;
import br.com.alexandreluchetti.mibackend.entrypoint.dto.UploadResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/arquivos")
@Tag(name = "Arquivos", description = "Endpoints para upload e consulta de processamento de arquivos")
public class ArquivoController {

    private final ArquivoUseCase arquivoUseCase;

    public ArquivoController(ArquivoUseCase arquivoUseCase) {
        this.arquivoUseCase = arquivoUseCase;
    }

    @Operation(
            summary = "Upload de arquivo",
            description = """
                    Recebe um arquivo delimitado por pipe (|), valida o cabeçalho e inicia o processamento em background.

                    **Validações obrigatórias:**
                    - Linha 1: deve iniciar com `|0000|017|` ou `|0000|006|`
                    - Linha 2: deve conter exatamente `|0001|0|`

                    Retorna um **ID único** para acompanhamento do processamento.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            tags = "Arquivos"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Upload aceito – ID retornado",
                    content = @Content(schema = @Schema(implementation = UploadResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Cabeçalho do arquivo inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Role insuficiente (necessário: ENVIO)",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponseDTO> upload(
            @Parameter(description = "Arquivo delimitado por pipe (|) com cabeçalho válido", required = true)
            @RequestParam("file") MultipartFile file) throws IOException {

        UploadResponseDTO response = arquivoUseCase.upload(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Consulta de progresso",
            description = "Retorna o status atual do processamento para o ID informado. Acessível por ENVIO e CONSULTA.",
            security = @SecurityRequirement(name = "bearerAuth"),
            tags = "Arquivos"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status retornado com sucesso",
                    content = @Content(schema = @Schema(implementation = ProgressoResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "ID não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Role insuficiente (necessário: ENVIO ou CONSULTA)",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/{id}/progresso")
    public ResponseEntity<ProgressoResponseDTO> consultarProgresso(
            @Parameter(description = "ID único retornado no upload", required = true)
            @PathVariable String id) {

        return ResponseEntity.ok(ProgressoResponseDTO.fromModel(arquivoUseCase.consultarProgresso(id)));
    }

    @Operation(
            summary = "Consulta do resultado do processamento",
            description = """
                    Retorna o resumo do processamento com a contagem de cada código de registro.

                    - Se o arquivo ainda estiver sendo processado, retorna **400** com status `EM_PROCESSAMENTO`.
                    - Se finalizado, retorna o status e o resumo completo.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            tags = "Arquivos"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultado retornado com sucesso",
                    content = @Content(schema = @Schema(implementation = ResultadoResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Processamento ainda em andamento",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "ID não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Role insuficiente (necessário: CONSULTA)",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/{id}/resultado")
    public ResponseEntity<ResultadoResponseDTO> consultarResultado(
            @Parameter(description = "ID único retornado no upload", required = true)
            @PathVariable String id) {

        return ResponseEntity.ok(arquivoUseCase.consultarResultado(id));
    }
}
