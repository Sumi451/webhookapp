package com.bfh.webhookapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class WebhookService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${user.name}")
    private String name;

    @Value("${user.regNo}")
    private String regNo;

    @Value("${user.email}")
    private String email;

    public void process() {
        try {
            // 1. Generate Webhook
            String url = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook";
            Map<String, String> req = Map.of("name", name, "regNo", regNo, "email", email);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(req, headers);

            ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, request, JsonNode.class);
            JsonNode body = response.getBody();

            String webhook = body.get("webhook").asText();
            String token = body.get("accessToken").asText();
            JsonNode data = body.get("data");

            Object outcome;
            if (Integer.parseInt(regNo.replaceAll("\\D", "")) % 2 != 0) {
                outcome = solveMutual(data.get("users"));
            } else {
                outcome = solveNth(data);
            }

            Map<String, Object> payload = Map.of("regNo", regNo, "outcome", outcome);
            postWithRetry(webhook, token, payload);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<List<Integer>> solveMutual(JsonNode users) {
        Map<Integer, Set<Integer>> map = new HashMap<>();
        for (JsonNode u : users) {
            int id = u.get("id").asInt();
            Set<Integer> follows = new HashSet<>();
            u.get("follows").forEach(f -> follows.add(f.asInt()));
            map.put(id, follows);
        }

        List<List<Integer>> res = new ArrayList<>();
        for (int a : map.keySet()) {
            for (int b : map.get(a)) {
                if (map.containsKey(b) && map.get(b).contains(a) && a < b) {
                    res.add(List.of(a, b));
                }
            }
        }
        return res;
    }

    private List<Integer> solveNth(JsonNode root) {
        int n = root.get("n").asInt();
        int findId = root.get("findId").asInt();
        JsonNode users = root.get("users");

        Map<Integer, List<Integer>> graph = new HashMap<>();
        for (JsonNode u : users) {
            int id = u.get("id").asInt();
            List<Integer> follows = new ArrayList<>();
            u.get("follows").forEach(f -> follows.add(f.asInt()));
            graph.put(id, follows);
        }

        Set<Integer> visited = new HashSet<>();
        Queue<Integer> q = new LinkedList<>();
        q.add(findId);
        visited.add(findId);

        for (int level = 0; level < n; level++) {
            int size = q.size();
            for (int i = 0; i < size; i++) {
                int curr = q.poll();
                for (int next : graph.getOrDefault(curr, List.of())) {
                    if (!visited.contains(next)) {
                        q.add(next);
                        visited.add(next);
                    }
                }
            }
        }

        return new ArrayList<>(q);
    }

    private void postWithRetry(String url, String token, Map<String, Object> payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> req = new HttpEntity<>(payload, headers);

        for (int i = 1; i <= 4; i++) {
            try {
                restTemplate.postForEntity(url, req, String.class);
                System.out.println("Webhook success");
                break;
            } catch (Exception e) {
                System.out.println("Retry " + i + " failed. Retrying...");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {}
            }
        }
    }
}
