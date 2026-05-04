import http from "k6/http";
import { check, sleep } from "k6";

/**
 * ~1M requests at ~250 RPS (constant arrival rate).
 * Run with stack up: docker compose up --build
 *   k6 run scripts/load/k6-payments.js
 *
 * Capture PCAP (example, macOS loopback):
 *   sudo tcpdump -i lo0 -w swiftpay-8080.pcap tcp port 8080
 */
export const options = {
  scenarios: {
    steady: {
      executor: "constant-arrival-rate",
      rate: 250,
      timeUnit: "1s",
      duration: "4000s",
      preAllocatedVUs: 300,
      maxVUs: 600,
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.05"],
  },
};

const URL = __ENV.GATEWAY_URL || "http://localhost:8080/v1/payments";

export default function () {
  const id = `k6-${__VU}-${__ITER}-${Date.now()}-${Math.random().toString(16).slice(2)}`;
  const body = JSON.stringify({
    transactionId: id,
    senderId: "user-alice",
    receiverId: "user-bob",
    amount: "0.01",
    currency: "USD",
  });
  const res = http.post(URL, body, {
    headers: { "Content-Type": "application/json" },
    timeout: "30s",
  });
  check(res, {
    "202 accepted": (r) => r.status === 202,
  });
  sleep(0.01);
}
