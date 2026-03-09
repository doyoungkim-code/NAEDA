from prometheus_client import CONTENT_TYPE_LATEST, Counter, Histogram, generate_latest

REQUESTS_TOTAL = Counter(
    "ai_requests_total",
    "Total HTTP requests handled by AI service",
    ["method", "path", "status"],
)

REQUEST_DURATION_SECONDS = Histogram(
    "ai_request_duration_seconds",
    "HTTP request latency in seconds",
    ["method", "path"],
    buckets=(0.05, 0.1, 0.25, 0.5, 1, 2, 5, 10),
)

INFERENCE_ERRORS_TOTAL = Counter(
    "ai_inference_errors_total",
    "Total AI inference errors by endpoint and code",
    ["endpoint", "code"],
)

INFERENCE_FALLBACK_TOTAL = Counter(
    "ai_inference_fallback_total",
    "Total AI inference fallback usage by endpoint",
    ["endpoint"],
)

INFERENCE_STATUS_TOTAL = Counter(
    "ai_inference_status_total",
    "Total AI inference status count by endpoint and status",
    ["endpoint", "status"],
)


def render_metrics() -> tuple[bytes, str]:
    return generate_latest(), CONTENT_TYPE_LATEST
