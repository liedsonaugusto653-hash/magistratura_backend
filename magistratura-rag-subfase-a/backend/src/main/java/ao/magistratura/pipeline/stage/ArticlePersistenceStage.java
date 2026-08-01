package ao.magistratura.pipeline.stage;

import ao.magistratura.entity.Artigo;
import ao.magistratura.entity.Documento;
import ao.magistratura.pipeline.model.PipelineContexto;
import ao.magistratura.pipeline.model.PipelineEtapa;
import ao.magistratura.repository.ArtigoRepository;
import ao.magistratura.service.pdf.ArtigoExtraido;
import ao.magistratura.service.pdf.EstruturaJuridicaParser;
import ao.magistratura.util.ContentHashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArticlePersistenceStage implements PipelineStage {

    private final ArtigoRepository artigoRepository;

    @Override
    public PipelineEtapa etapa() {
        return PipelineEtapa.PERSISTINDO_ARTIGOS;
    }

    @Override
    public void executar(PipelineContexto ctx) {
        Documento documento = ctx.getDocumento();
        // Limpa apenas derivados deste documento (reprocessamento seguro)
        artigoRepository.deleteByDocumentoId(documento.getId());

        var lista = EstruturaJuridicaParser.ordenarPorNumeroCrescente(ctx.getArtigosExtraidos());
        int ordem = 0;
        for (ArtigoExtraido extraido : lista) {
            ordem++;
            String hash = ContentHashUtil.hashArtigo(
                    extraido.numero(), extraido.titulo(), extraido.texto());

            Artigo artigo = Artigo.builder()
                    .diploma(ctx.getDiploma())
                    .documento(documento)
                    .numero(extraido.numero())
                    .titulo(extraido.titulo())
                    .texto(extraido.texto())
                    .ordem(ordem)
                    .capitulo(extraido.capitulo())
                    .seccao(extraido.seccao())
                    .paginaInicio(extraido.paginaInicio())
                    .paginaFim(extraido.paginaFim())
                    .hashConteudo(hash)
                    .build();
            artigoRepository.save(artigo);
        }
    }
}
