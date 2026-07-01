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
            .andExpect(jsonPath("$", hasItem("resilient-order-api")));
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
    void unknownTopologyReturnsBadRequest() throws Exception {
        mvc.perform(post("/api/run")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"topology\":\"ghost\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").exists());
    }
}
