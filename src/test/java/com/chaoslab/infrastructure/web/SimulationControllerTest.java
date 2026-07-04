package com.chaoslab.infrastructure.web;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Integración del API del dashboard sobre el contexto web completo. */
@SpringBootTest
@AutoConfigureMockMvc
class SimulationControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void listsAvailableTopologies() throws Exception {
        mvc.perform(get("/api/topologies"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasItem("order-api")))
            .andExpect(jsonPath("$", hasItem("resilient-order-api")))
            .andExpect(jsonPath("$", hasItem("slow-database")))
            .andExpect(jsonPath("$", hasItem("network-partition")));
    }

    @Test
    void exampleTopologiesRunAndExhibitTheirFailureMode() throws Exception {
        // slow-database: la DB no da abasto -> fallos por CAPACITY.
        mvc.perform(post("/api/run")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"topology\":\"slow-database\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.report.failuresByReason.CAPACITY").isNumber());

        // network-partition: la partición corta api-2 <-> shared-db -> NETWORK_PARTITION.
        mvc.perform(post("/api/run")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"topology\":\"network-partition\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.report.failuresByReason.NETWORK_PARTITION").isNumber());
    }

    @Test
    void topologyEndpointReturnsGraphWithoutRunning() throws Exception {
        mvc.perform(post("/api/topology")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"topology\":\"order-api\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.entryPointId").value("gateway"))
            .andExpect(jsonPath("$.nodes").isArray())
            .andExpect(jsonPath("$.edges").isArray());
    }

    @Test
    void runReturnsTopologyStructureAndTimeline() throws Exception {
        mvc.perform(post("/api/run")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"topology\":\"order-api\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.topology.nodes").isArray())
            .andExpect(jsonPath("$.topology.edges").isArray())
            .andExpect(jsonPath("$.report.generatedRequests").isNumber())
            .andExpect(jsonPath("$.report.timeline").isArray());
    }

    @Test
    void injectingACrashFaultProducesFailures() throws Exception {
        mvc.perform(post("/api/run")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"topology\":\"resilient-order-api\",\"seed\":42,\"faults\":[\"crash:api-2:0:0\"]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.report.failedRequests").isNumber());
    }

    @Test
    void runsRawYamlPastedByTheUser() throws Exception {
        String yaml = """
            name: mi-sistema
            components:
              - { id: api, type: Service, capacity: 50, base_latency_ms: 20 }
              - { id: db, type: Database, max_connections: 30, read_latency_ms: 15 }
            connections:
              - { from: api, to: db }
            workload: { requests_per_second: 100, duration_seconds: 5 }
            """;
        String body = "{\"yaml\":" + jsonString(yaml) + "}";

        mvc.perform(post("/api/run").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.topology.entryPointId").value("api"))
            .andExpect(jsonPath("$.report.timeline").isArray());
    }

    @Test
    void rejectsInvalidRawYaml() throws Exception {
        // Falta 'workload' -> el loader lo valida y responde 400.
        String yaml = "name: roto\ncomponents:\n  - { id: api, type: Service, capacity: 10, base_latency_ms: 5 }\n";
        String body = "{\"yaml\":" + jsonString(yaml) + "}";

        mvc.perform(post("/api/run").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").exists());
    }

    private static String jsonString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    @Test
    void unknownTopologyReturnsBadRequest() throws Exception {
        mvc.perform(post("/api/run")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"topology\":\"ghost\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").exists());
    }
}
