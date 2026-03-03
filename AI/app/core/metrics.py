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


def render_metrics() -> tuple[bytes, str]:
    return generate_latest(), CONTENT_TYPE_LATEST
