package ao.magistratura.service;

import ao.magistratura.dto.favorito.FavoritoRequest;
import ao.magistratura.dto.favorito.FavoritoResponse;
import ao.magistratura.entity.Artigo;
import ao.magistratura.entity.Diploma;
import ao.magistratura.entity.Favorito;
import ao.magistratura.entity.Utilizador;
import ao.magistratura.exception.RecursoNaoEncontradoException;
import ao.magistratura.exception.RegraNegocioException;
import ao.magistratura.repository.ArtigoRepository;
import ao.magistratura.repository.DiplomaRepository;
import ao.magistratura.repository.FavoritoRepository;
import ao.magistratura.repository.UtilizadorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FavoritoService {

    private final FavoritoRepository favoritoRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final ArtigoRepository artigoRepository;
    private final DiplomaRepository diplomaRepository;

    @Transactional(readOnly = true)
    public List<FavoritoResponse> listar(String email) {
        Utilizador u = obterUtilizador(email);
        return favoritoRepository.findByUtilizadorIdOrderByDataCriacaoDesc(u.getId())
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isArtigoFavorito(String email, UUID artigoId) {
        Utilizador u = obterUtilizador(email);
        return favoritoRepository.existsByUtilizadorIdAndArtigoId(u.getId(), artigoId);
    }

    @Transactional
    public FavoritoResponse adicionar(String email, FavoritoRequest request) {
        if (request.artigoId() == null && request.diplomaId() == null) {
            throw new RegraNegocioException("Indica um artigo ou um diploma para favoritar.");
        }
        Utilizador u = obterUtilizador(email);

        Artigo artigo = null;
        Diploma diploma = null;

        if (request.artigoId() != null) {
            artigo = artigoRepository.findById(request.artigoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Artigo não encontrado"));
            diploma = artigo.getDiploma();
            var existente = favoritoRepository.findByUtilizadorIdAndArtigoId(u.getId(), artigo.getId());
            if (existente.isPresent()) {
                return map(existente.get());
            }
        } else {
            diploma = diplomaRepository.findById(request.diplomaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Diploma não encontrado"));
            var existente = favoritoRepository.findByUtilizadorIdAndDiplomaId(u.getId(), diploma.getId());
            if (existente.isPresent()) {
                return map(existente.get());
            }
        }

        Favorito favorito = Favorito.builder()
                .utilizador(u)
                .artigo(artigo)
                .diploma(diploma)
                .build();
        return map(favoritoRepository.save(favorito));
    }

    @Transactional
    public void removerArtigo(String email, UUID artigoId) {
        Utilizador u = obterUtilizador(email);
        favoritoRepository.deleteByUtilizadorIdAndArtigoId(u.getId(), artigoId);
    }

    @Transactional
    public void remover(String email, UUID favoritoId) {
        Utilizador u = obterUtilizador(email);
        Favorito f = favoritoRepository.findById(favoritoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Favorito não encontrado"));
        if (!f.getUtilizador().getId().equals(u.getId())) {
            throw new RecursoNaoEncontradoException("Favorito não encontrado");
        }
        favoritoRepository.delete(f);
    }

    private Utilizador obterUtilizador(String email) {
        return utilizadorRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador não encontrado"));
    }

    private FavoritoResponse map(Favorito f) {
        Artigo a = f.getArtigo();
        Diploma d = f.getDiploma();
        if (d == null && a != null) {
            d = a.getDiploma();
        }
        return new FavoritoResponse(
                f.getId(),
                a != null ? a.getId() : null,
                a != null ? a.getNumero() : null,
                a != null ? a.getTitulo() : null,
                d != null ? d.getId() : null,
                d != null ? d.getTitulo() : null,
                f.getDataCriacao()
        );
    }
}
