package com.oriento.api.services;

import com.oriento.api.model.Usuario;
import com.oriento.api.repositories.UsuarioRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticatedUserServiceTest {
    final UsuarioRepository repository = mock(UsuarioRepository.class);
    final AuthenticatedUserService service = new AuthenticatedUserService(repository);

    @Test
    void missingPrincipalIsUnauthorized() {
        unauthorized(() -> service.resolve(null));
        verifyNoInteractions(repository);
    }

    @Test
    void malformedSubjectIsUnauthorized() {
        unauthorized(() -> service.resolve(token("1-1-1-1-1")));
        verifyNoInteractions(repository);
    }

    @Test
    void unregisteredUserIsUnauthorized() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        unauthorized(() -> service.resolve(token(id.toString())));
    }

    @Test
    void registeredUserIsResolvedFromTokenSubject() {
        UUID id = UUID.randomUUID();
        Usuario user = new Usuario();
        user.setIdUsuario(id);
        when(repository.findById(id)).thenReturn(Optional.of(user));
        assertThat(service.resolve(token(id.toString()))).isSameAs(user);
    }

    private Jwt token(String subject) {
        return Jwt.withTokenValue("test-jwt").header("alg", "RS256").subject(subject).build();
    }

    private void unauthorized(ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOfSatisfying(ResponseStatusException.class,
                error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }
}
