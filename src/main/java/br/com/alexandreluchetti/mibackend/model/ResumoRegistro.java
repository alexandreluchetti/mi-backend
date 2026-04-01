package br.com.alexandreluchetti.mibackend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumoRegistro {

    private Long id;
    private UUID uploadId;
    private String registro;
    private Long total;
}
