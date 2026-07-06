#!/usr/bin/env python3
import sys,re
import json
import requests
from urllib.error import URLError, HTTPError


def fetch_latest_version(group_id: str, artifact_id: str) -> str:
    # Maven Central Search API endpoint
    base_url = "https://search.maven.org/solrsearch/select"
    
    # Build the query parameters
    params = { "q": f"g:{group_id} AND a:{artifact_id}",
               "rows": 1, "wt": "json", "core": "gav" }
    try:
        response = requests.get(base_url, params=params)
        response.raise_for_status()
        data = response.json()

        if 'response' not in data:
            raise KeyError("Invalid response format from Maven Central")

        docs = data['response']['docs']
        if not docs:
            #raise ValueError(f"No artifacts found for {group_id}:{artifact_id}")
            with open('gradle.properties') as gradle_fh:
                gradle_properties = gradle_fh.read()
            return re.search(r'^\s*version\s*=\s*(.+)$', gradle_properties, re.M).group(1)

        return docs[0]['v']

    except requests.RequestException as e:
        raise requests.RequestException(f"Request failed: {e}")



def main():
    sys.argv[1]

    with open(sys.argv[1]) as fh:
        text = fh.read()

    central = fetch_latest_version("io.github.jmltoolkit", "jmlparser-parent")

    print(
        text.replace("$$mvnversion$$", central)
    )


if __name__ == "__main__":
    main()