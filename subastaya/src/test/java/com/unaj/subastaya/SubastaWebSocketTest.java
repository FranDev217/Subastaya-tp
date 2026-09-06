package com.unaj.subastaya;

import com.unaj.subastaya.dto.SubastaEvento;
import com.unaj.subastaya.dto.TipoEvento;
import com.unaj.subastaya.model.EstadoSubasta;
import com.unaj.subastaya.service.SubastaNotificador;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "subastaya.worker.initial-delay-ms=600000")
class SubastaWebSocketTest {

    private static final long SUBASTA_ID = 1L;

    @LocalServerPort
    private int port;

    @Autowired
    private SubastaNotificador subastaNotificador;

    @Test
    void suscriptorRecibeEstadoActual() throws Exception {
        BlockingQueue<SubastaEvento> eventos = new LinkedBlockingQueue<>();
        StompSession session = conectar();
        session.subscribe("/app/subastas/" + SUBASTA_ID, frameHandler(eventos));

        SubastaEvento evento = eventos.poll(5, TimeUnit.SECONDS);

        session.disconnect();
        assertThat(evento).isNotNull();
        assertThat(evento.tipo()).isEqualTo(TipoEvento.ESTADO_ACTUAL);
        assertThat(evento.subastaId()).isEqualTo(SUBASTA_ID);
        assertThat(evento.fechaFin()).isNotNull();
    }

    @Test
    void nuevaPujaSeDifundeALosSuscriptores() throws Exception {
        BlockingQueue<SubastaEvento> eventos = new LinkedBlockingQueue<>();
        StompSession session = conectar();
        session.subscribe("/topic/subastas/" + SUBASTA_ID, frameHandler(eventos));

        SubastaEvento evento = new SubastaEvento(
                TipoEvento.NUEVA_PUJA,
                SUBASTA_ID,
                EstadoSubasta.ACTIVA,
                new BigDecimal("47000"),
                LocalDateTime.now(),
                null
        );

        // subscribe() encola el frame SUBSCRIBE de forma asincronica: el broker simple de
        // Spring no confirma la suscripcion con un receipt, asi que no hay forma de esperar
        // una confirmacion explicita. Reintentamos la publicacion en vez de asumir un delay
        // fijo, para no perder el mensaje si el primer intento le gana la carrera al broker.
        SubastaEvento recibido = null;
        for (int intento = 0; intento < 10 && recibido == null; intento++) {
            subastaNotificador.notificar(SUBASTA_ID, evento);
            recibido = eventos.poll(300, TimeUnit.MILLISECONDS);
        }

        session.disconnect();
        assertThat(recibido).isNotNull();
        assertThat(recibido.tipo()).isEqualTo(TipoEvento.NUEVA_PUJA);
        assertThat(recibido.montoActual()).isEqualByComparingTo("47000");
    }

    private StompSession conectar() throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new JacksonJsonMessageConverter());
        return client.connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);
    }

    private StompFrameHandler frameHandler(BlockingQueue<SubastaEvento> eventos) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return SubastaEvento.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                eventos.add((SubastaEvento) payload);
            }
        };
    }
}
