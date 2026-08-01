package ao.magistratura.service;

import ao.magistratura.dto.biblioteca.*;
import ao.magistratura.entity.*;
import ao.magistratura.exception.RecursoNaoEncontradoException;
import ao.magistratura.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BibliotecaService {

    private final DiplomaRepository diplomaRepository;
    private final ArtigoRepository artigoRepository;
    private final CategoriaRepository categoriaRepository;
    private final TemaRepository temaRepository;

    // ---------- Categorias ----------

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarCategorias() {
        return categoriaRepository.findAll().stream()
                .map(c -> new CategoriaResponse(c.getId(), c.getNome(), c.getDescricao()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse obterCategoria(UUID id) {
        Categoria c = categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada"));
        return new CategoriaResponse(c.getId(), c.getNome(), c.getDescricao());
    }

    // ---------- Temas ----------

    @Transactional(readOnly = true)
    public List<TemaResponse> listarTemas() {
        return temaRepository.findAll().stream().map(this::mapTema).toList();
    }

    @Transactional(readOnly = true)
    public TemaResponse obterTema(UUID id) {
        Tema t = temaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tema não encontrado"));
        return mapTema(t);
    }

    private TemaResponse mapTema(Tema t) {
        return new TemaResponse(
                t.getId(),
                t.getNome(),
                t.getDescricao(),
                t.getCategoria() != null ? t.getCategoria().getId() : null,
                t.getCategoria() != null ? t.getCategoria().getNome() : null
        );
    }

    // ---------- Diplomas ----------

    @Transactional
    public DiplomaDetailResponse criarDiploma(DiplomaRequest dto) {
        Categoria categoria = null;
        if (dto.categoriaId() != null) {
            categoria = categoriaRepository.findById(dto.categoriaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada"));
        }

        Diploma diploma = Diploma.builder()
                .numero(dto.numero())
                .titulo(dto.titulo())
                .descricao(dto.descricao())
                .dataPublicacao(dto.dataPublicacao())
                .categoria(categoria)
                .build();

        diploma = diplomaRepository.save(diploma);

        return obterDiploma(diploma.getId());
    }


    @Transactional
    public DiplomaDetailResponse actualizarDiploma(UUID id, DiplomaRequest dto) {
        Diploma diploma = diplomaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Diploma não encontrado"));
        if (dto.numero() != null && !dto.numero().isBlank()) {
            diploma.setNumero(dto.numero().trim());
        }
        if (dto.titulo() != null && !dto.titulo().isBlank()) {
            diploma.setTitulo(dto.titulo().trim());
        }
        if (dto.descricao() != null) {
            diploma.setDescricao(dto.descricao());
        }
        if (dto.dataPublicacao() != null) {
            diploma.setDataPublicacao(dto.dataPublicacao());
        }
        if (dto.categoriaId() != null) {
            Categoria categoria = categoriaRepository.findById(dto.categoriaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada"));
            diploma.setCategoria(categoria);
        }
        diplomaRepository.save(diploma);
        return obterDiploma(diploma.getId());
    }

    @Transactional
    public void eliminarDiploma(UUID id) {
        Diploma diploma = diplomaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Diploma não encontrado"));
        long artigos = artigoRepository.countByDiplomaId(id);
        if (artigos > 0) {
            // Remove artigos associados (e embeddings derivados ficam órfãos limpos no pipeline futuro)
            artigoRepository.deleteByDiplomaId(id);
        }
        diplomaRepository.delete(diploma);
    }

    @Transactional(readOnly = true)
    public Page<DiplomaResumoResponse> listarDiplomas(String termo, UUID categoriaId, Pageable pageable) {
        Page<Diploma> page;
        if (termo != null && !termo.isBlank()) {
            page = diplomaRepository.pesquisar(termo.trim(), pageable);
        } else if (categoriaId != null) {
            page = diplomaRepository.findByCategoriaId(categoriaId, pageable);
        } else {
            page = diplomaRepository.findAll(pageable);
        }
        return page.map(this::mapDiplomaResumo);
    }

    @Transactional(readOnly = true)
    public DiplomaDetailResponse obterDiploma(UUID id) {
        Diploma d = diplomaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Diploma não encontrado"));

        List<ArtigoResumoResponse> artigos = artigoRepository
                .findByDiplomaIdOrderByOrdemAsc(id)
                .stream()
                .sorted(comparadorOrdemLegalArtigo())
                .map(this::mapArtigoResumo)
                .toList();

        return new DiplomaDetailResponse(
                d.getId(),
                d.getNumero(),
                d.getTitulo(),
                d.getDescricao(),
                d.getDataPublicacao(),
                d.getEstado() != null ? d.getEstado().name() : null,
                d.getResumo(),
                d.getPdfUrl(),
                d.getVersao(),
                d.getDataCriacao(),
                d.getCategoria() != null ? d.getCategoria().getId() : null,
                d.getCategoria() != null ? d.getCategoria().getNome() : null,
                artigos
        );
    }

    private DiplomaResumoResponse mapDiplomaResumo(Diploma d) {
        return new DiplomaResumoResponse(
                d.getId(),
                d.getNumero(),
                d.getTitulo(),
                d.getDescricao(),
                d.getDataPublicacao(),
                d.getEstado() != null ? d.getEstado().name() : null,
                d.getCategoria() != null ? d.getCategoria().getId() : null,
                d.getCategoria() != null ? d.getCategoria().getNome() : null
        );
    }

    // ---------- Artigos ----------

    @Transactional(readOnly = true)
    public Page<ArtigoResumoResponse> listarArtigos(String termo, UUID diplomaId, Pageable pageable) {
        if (termo != null && !termo.isBlank()) {
            return artigoRepository.pesquisar(termo.trim(), pageable).map(this::mapArtigoResumo);
        }
        if (diplomaId != null) {
            // Lista completa ordenada (sem paginação para manter ordem legal)
            List<ArtigoResumoResponse> lista = artigoRepository
                    .findByDiplomaIdOrderByOrdemAsc(diplomaId)
                    .stream()
                    .sorted(comparadorOrdemLegalArtigo())
                    .map(this::mapArtigoResumo)
                    .toList();
            return new org.springframework.data.domain.PageImpl<>(lista, pageable, lista.size());
        }
        return artigoRepository.findAll(pageable).map(this::mapArtigoResumo);
    }

    @Transactional(readOnly = true)
    public ArtigoDetailResponse obterArtigo(UUID id) {
        Artigo a = artigoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Artigo não encontrado"));
        return mapArtigoDetail(a);
    }


    /** Ordem legal: Art. 1, 2, 3… (número inteiro), não ordem lexicográfica nem OCR. */
    private static Comparator<Artigo> comparadorOrdemLegalArtigo() {
        return Comparator
                .comparingInt((Artigo a) -> numeroOrdenavelArtigo(a.getNumero()))
                .thenComparingInt(a -> a.getOrdem() != null ? a.getOrdem() : Integer.MAX_VALUE)
                .thenComparing(a -> a.getNumero() != null ? a.getNumero() : "", String::compareToIgnoreCase);
    }

    private static int numeroOrdenavelArtigo(String numero) {
        if (numero == null || numero.isBlank()) {
            return Integer.MAX_VALUE;
        }
        Matcher m = Pattern.compile("(\\d{1,6})").matcher(numero.trim());
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                return Integer.MAX_VALUE;
            }
        }
        return Integer.MAX_VALUE;
    }

    private ArtigoResumoResponse mapArtigoResumo(Artigo a) {
        return new ArtigoResumoResponse(
                a.getId(),
                a.getNumero(),
                a.getTitulo(),
                a.getOrdem(),
                a.getDiploma() != null ? a.getDiploma().getId() : null,
                a.getDiploma() != null ? a.getDiploma().getTitulo() : null,
                a.getTema() != null ? a.getTema().getId() : null,
                a.getTema() != null ? a.getTema().getNome() : null,
                a.getCapitulo(),
                a.getSeccao()
        );
    }

    private ArtigoDetailResponse mapArtigoDetail(Artigo a) {
        Diploma d = a.getDiploma();
        Documento doc = a.getDocumento();

        UUID anteriorId = null;
        UUID seguinteId = null;
        if (d != null && a.getOrdem() != null) {
            anteriorId = artigoRepository
                    .findFirstByDiplomaIdAndOrdemLessThanOrderByOrdemDesc(d.getId(), a.getOrdem())
                    .map(Artigo::getId)
                    .orElse(null);
            seguinteId = artigoRepository
                    .findFirstByDiplomaIdAndOrdemGreaterThanOrderByOrdemAsc(d.getId(), a.getOrdem())
                    .map(Artigo::getId)
                    .orElse(null);
        }

        return new ArtigoDetailResponse(
                a.getId(),
                a.getNumero(),
                a.getTitulo(),
                a.getTexto(),
                a.getOrdem(),
                a.getResumo(),
                a.getPaginaInicio(),
                a.getPaginaFim(),
                d != null ? d.getId() : null,
                d != null ? d.getTitulo() : null,
                d != null ? d.getNumero() : null,
                a.getTema() != null ? a.getTema().getId() : null,
                a.getTema() != null ? a.getTema().getNome() : null,
                a.getCapitulo(),
                a.getSeccao(),
                d != null && d.getEstado() != null ? d.getEstado().name() : null,
                d != null ? d.getDataPublicacao() : null,
                d != null ? d.getVersao() : null,
                d != null && d.getCategoria() != null ? d.getCategoria().getId() : null,
                d != null && d.getCategoria() != null ? d.getCategoria().getNome() : null,
                doc != null ? doc.getId() : null,
                doc != null ? doc.getTitulo() : null,
                doc != null ? doc.getFonte() : null,
                doc != null ? doc.getOficial() : null,
                doc != null ? doc.getDataImportacao() : null,
                anteriorId,
                seguinteId
        );
    }
}
