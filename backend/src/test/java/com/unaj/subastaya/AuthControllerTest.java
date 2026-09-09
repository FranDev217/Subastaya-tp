package com.unaj.subastaya;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unaj.subastaya.dto.LoginRequest;
import com.unaj.subastaya.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerTest {

    private static final String PASSWORD_VALIDA = "Password123!";

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void loginConCredencialesValidasDevuelveDatosDelUsuario() throws Exception {
        HttpResponse<String> respuesta = login(new LoginRequest("comprador1@test.com", PASSWORD_VALIDA));

        assertThat(respuesta.statusCode()).isEqualTo(200);
        LoginResponse cuerpo = objectMapper.readValue(respuesta.body(), LoginResponse.class);
        assertThat(cuerpo.email()).isEqualTo("comprador1@test.com");
        assertThat(cuerpo.usuarioId()).isNotNull();
    }

    @Test
    void loginConContraseñaIncorrectaDevuelve401() throws Exception {
        HttpResponse<String> respuesta = login(new LoginRequest("comprador1@test.com", "otraCosa"));

        assertThat(respuesta.statusCode()).isEqualTo(401);
    }

    @Test
    void loginConEmailInexistenteDevuelve401() throws Exception {
        HttpResponse<String> respuesta = login(new LoginRequest("no-existe@test.com", PASSWORD_VALIDA));

        assertThat(respuesta.statusCode()).isEqualTo(401);
    }

    @Test
    void loginConCamposVaciosDevuelve400() throws Exception {
        HttpResponse<String> respuesta = login(new LoginRequest("", ""));

        assertThat(respuesta.statusCode()).isEqualTo(400);
    }

    private HttpResponse<String> login(LoginRequest request) throws Exception {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
                .build();
        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    }
}
