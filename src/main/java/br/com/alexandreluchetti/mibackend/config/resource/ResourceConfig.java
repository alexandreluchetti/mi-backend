package br.com.alexandreluchetti.mibackend.config.resource;

import br.com.alexandreluchetti.mibackend.core.repository.ResumoRepository;
import br.com.alexandreluchetti.mibackend.core.repository.UploadRepository;
import br.com.alexandreluchetti.mibackend.core.usecase.ArquivoUseCase;
import br.com.alexandreluchetti.mibackend.core.usecase.ProcessamentoUseCase;
import br.com.alexandreluchetti.mibackend.core.usecase.impl.ArquivoUseCaseImpl;
import br.com.alexandreluchetti.mibackend.core.usecase.impl.ProcessamentoUseCaseImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResourceConfig {

    @Bean
    public ArquivoUseCase loadArquivoUseCase(
            UploadRepository uploadRepository,
            ResumoRepository resumoRepository,
            ProcessamentoUseCase processamentoUseCase
    ) {
        return new ArquivoUseCaseImpl(uploadRepository, resumoRepository, processamentoUseCase);
    }

    @Bean
    public ProcessamentoUseCase loadProcessamentoUseCase(
            UploadRepository uploadRepository,
            ResumoRepository resumoRepository
    ) {
        return new ProcessamentoUseCaseImpl(uploadRepository, resumoRepository);
    }
}
