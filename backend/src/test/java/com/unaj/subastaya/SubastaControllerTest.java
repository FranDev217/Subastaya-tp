package com.unaj.subastaya;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unaj.subastaya.repository.SubastaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "subastaya.worker.initial-delay-ms=600000")
@Sql(scripts = "/sql/reset-seed.sql")
class SubastaControllerTest {

    private static final long VENDEDOR = 1L;
    private static final long CATEGORIA_TECNOLOGIA = 1L;
    private static final long CATEGORIA_INEXISTENTE = 999999L;
    private static final long VENDEDOR_INEXISTENTE = 999999L;
    private static final int ID_MAX_SEED = 5;

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private SubastaRepository subastaRepository;

    @AfterEach
    void limpiarSubastasCreadas() {
        subastaRepository.findAll().stream()
                .filter(subasta -> subasta.getId() > ID_MAX_SEED)
                .forEach(subastaRepository::delete);
    }

    @Test
    void creaSubastaProgramadaCuandoFechaInicioEsFutura() throws Exception {
        Map<String, Object> body = subastaValida(
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));

        HttpResponse<String> response = post(body);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.headers().firstValue("Location")).isPresent();
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.get("estado").asText()).isEqualTo("PROGRAMADA");
        assertThat(json.get("id").asLong()).isGreaterThan(ID_MAX_SEED);
    }

    @Test
    void creaSubastaActivaCuandoElInicioYaPaso() throws Exception {
        Map<String, Object> body = subastaValida(
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusDays(1));

        HttpResponse<String> response = post(body);

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.get("estado").asText()).isEqualTo("ACTIVA");
    }

    @Test
    void fechaFinAnteriorAInicioDevuelve400() throws Exception {
        Map<String, Object> body = subastaValida(
                LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(1));

        HttpResponse<String> response = post(body);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(objectMapper.readTree(response.body()).get("errores").has("fechasCoherentes")).isTrue();
    }

    @Test
    void fechaFinEnElPasadoDevuelve400() throws Exception {
        Map<String, Object> body = subastaValida(
                LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1));

        HttpResponse<String> response = post(body);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(objectMapper.readTree(response.body()).get("errores").has("fechaFin")).isTrue();
    }

    @Test
    void precioBaseNoPositivoDevuelve400() throws Exception {
        Map<String, Object> body = subastaValida(
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));
        body.put("precioBase", 0);

        HttpResponse<String> response = post(body);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(objectMapper.readTree(response.body()).get("errores").has("precioBase")).isTrue();
    }

    @Test
    void incrementoMinimoNoPositivoDevuelve400() throws Exception {
        Map<String, Object> body = subastaValida(
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));
        body.put("incrementoMinimo", -10);

        HttpResponse<String> response = post(body);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(objectMapper.readTree(response.body()).get("errores").has("incrementoMinimo")).isTrue();
    }

    @Test
    void camposObligatoriosVaciosDevuelven400() throws Exception {
        Map<String, Object> body = subastaValida(
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));
        body.put("titulo", "");
        body.put("descripcion", "");

        HttpResponse<String> response = post(body);

        assertThat(response.statusCode()).isEqualTo(400);
        JsonNode errores = objectMapper.readTree(response.body()).get("errores");
        assertThat(errores.has("titulo")).isTrue();
        assertThat(errores.has("descripcion")).isTrue();
    }

    @Test
    void categoriaInexistenteDevuelve404() throws Exception {
        Map<String, Object> body = subastaValida(
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));
        body.put("categoriaId", CATEGORIA_INEXISTENTE);

        assertThat(post(body).statusCode()).isEqualTo(404);
    }

    @Test
    void vendedorInexistenteDevuelve404() throws Exception {
        Map<String, Object> body = subastaValida(
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));
        body.put("vendedorId", VENDEDOR_INEXISTENTE);

        assertThat(post(body).statusCode()).isEqualTo(404);
    }

    private Map<String, Object> subastaValida(LocalDateTime inicio, LocalDateTime fin) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("titulo", "Producto de prueba");
        body.put("descripcion", "Descripción de prueba para el test");
        body.put("urlImagen", "https://picsum.photos/seed/test/600/400");
        body.put("categoriaId", CATEGORIA_TECNOLOGIA);
        body.put("precioBase", 1000);
        body.put("incrementoMinimo", 100);
        body.put("fechaInicio", formatear(inicio));
        body.put("fechaFin", formatear(fin));
        body.put("vendedorId", VENDEDOR);
        return body;
    }

    private String formatear(LocalDateTime fecha) {
        return fecha.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private HttpResponse<String> post(Map<String, Object> body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/subastas"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
