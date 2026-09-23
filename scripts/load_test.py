"""Small standard-library load generator for the demo endpoint."""

import argparse
import concurrent.futures
import urllib.error
import urllib.request
from collections import Counter


def request(url: str, api_key: str) -> int:
    req = urllib.request.Request(url, headers={"X-API-Key": api_key})
    try:
        with urllib.request.urlopen(req, timeout=5) as response:
            return response.status
    except urllib.error.HTTPError as error:
        return error.code


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", default="http://localhost:8080/api/demo")
    parser.add_argument("--api-key", default="load-test")
    parser.add_argument("--requests", type=int, default=30)
    parser.add_argument("--concurrency", type=int, default=5)
    args = parser.parse_args()
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.concurrency) as pool:
        statuses = list(pool.map(lambda _: request(args.url, args.api_key), range(args.requests)))
    print(dict(sorted(Counter(statuses).items())))


if __name__ == "__main__":
    main()
