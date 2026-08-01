package ao.magistratura.security;

import ao.magistratura.entity.Utilizador;
import ao.magistratura.repository.UtilizadorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UtilizadorRepository utilizadorRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Utilizador utilizador = utilizadorRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilizador não encontrado: " + email));

        return User.builder()
                .username(utilizador.getEmail())
                .password(utilizador.getPasswordHash())
                .disabled(!utilizador.isAtivo())
                .authorities(Collections.emptyList())
                .build();
    }
}
