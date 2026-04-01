package br.com.alexandreluchetti.mibackend.config.shared;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("mi-backend – API de Processamento de Arquivos")
                        .description("""
                                API REST para upload e processamento de arquivos delimitados por pipe (|).

                                ## Autenticação
                                Utilize **Bearer Token** no header `Authorization`. Existem dois tokens estáticos:

                                | Token | Role | Permissões |
                                |---|---|---|
                                | `token-envio-secreto` | ENVIO | Upload + Consulta de Progresso |
                                | `token-consulta-secreto` | CONSULTA | Consulta de Progresso + Resultado |

                                ## Formato do Arquivo
                                O arquivo deve ser delimitado por `|`. O primeiro campo de cada linha é o **Código do Registro**.

                                **Validações obrigatórias do cabeçalho:**
                                - Linha 1: deve iniciar com `|0000|017|` ou `|0000|006|`
                                - Linha 2: deve conter exatamente `|0001|0|`
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Alexandre Lucchetta")
                                .email("luchetti.92@gmail.com")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME,
                                new SecurityScheme()
                                        .name(BEARER_SCHEME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("StaticToken")
                                        .description("Informe o token estático: `token-envio-secreto` (role ENVIO) ou `token-consulta-secreto` (role CONSULTA)")));
    }
}
