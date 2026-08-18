#!/usr/bin/env python3

import argparse
import json
import os
import shutil
import stat
import sys
import tempfile
import urllib.error
import urllib.request
import zipfile
from datetime import datetime, timezone
from pathlib import Path, PurePosixPath

OWNER = "jmltoolkit"
REPOSITORY = "jmltk"

RELEASES_API_URL = f"https://api.github.com/repos/{OWNER}/{REPOSITORY}/releases?per_page=100"

# Change this to True if pre-releases should be included by default.
DEFAULT_INCLUDE_PRERELEASES = True

RELEASE_DATE_FILE_NAME = ".jmltk-release-date"
USER_AGENT = f"{REPOSITORY}-updater/1.0"


def github_request(url: str) -> urllib.request.Request:
    headers = {
        "Accept": "application/vnd.github+json",
        "User-Agent": USER_AGENT,
        "X-GitHub-Api-Version": "2022-11-28",
    }

    # Optional. This increases the GitHub API rate limit.
    token = os.environ.get("GITHUB_TOKEN")
    if token:
        headers["Authorization"] = f"Bearer {token}"

    return urllib.request.Request(url, headers=headers)


def parse_github_date(value: str) -> datetime:
    """
    Convert a GitHub ISO-8601 date, such as:
        2026-08-18T12:34:56Z
    into a timezone-aware datetime.
    """
    return datetime.fromisoformat(value.replace("Z", "+00:00")).astimezone(
        timezone.utc
    )


def format_github_date(value: datetime) -> str:
    """Return a normalized UTC ISO-8601 date."""
    return value.astimezone(timezone.utc).isoformat().replace("+00:00", "Z")


def get_latest_release(include_prereleases: bool) -> dict:
    request = github_request(RELEASES_API_URL)

    with urllib.request.urlopen(request, timeout=30) as response:
        releases = json.load(response)

    candidates = []

    for release in releases:
        # Draft releases should never be installed.
        if release.get("draft", False):
            continue

        if release.get("prerelease", False) and not include_prereleases:
            continue

        # Ignore releases that have not been published.
        if not release.get("published_at"):
            continue

        candidates.append(release)

    if not candidates:
        if include_prereleases:
            raise RuntimeError("No published releases were found.")
        raise RuntimeError("No published stable releases were found.")

    # Select by release publication date, not version or API ordering.
    return max(
        candidates,
        key=lambda release: parse_github_date(release["published_at"]),
    )


def find_zip_url(release: dict) -> tuple[str, str]:
    """
    Return the URL and filename of a ZIP release asset.

    If the release has no ZIP asset, use GitHub's automatically generated
    source ZIP.
    """
    for asset in release.get("assets", []):
        name = asset.get("name", "")

        if name.lower().endswith(".zip"):
            return asset["browser_download_url"], name

    tag = release.get("tag_name", "release")
    return release["zipball_url"], f"{REPOSITORY}-{tag}.zip"


def read_installed_release_date(date_file: Path) -> datetime | None:
    try:
        value = date_file.read_text(encoding="utf-8").strip()
    except FileNotFoundError:
        return None

    if not value:
        return None

    try:
        return parse_github_date(value)
    except ValueError:
        print(
            f"Warning: invalid release date in {date_file}; "
            "the release will be installed again.",
            file=sys.stderr,
        )
        return None


def download_file(url: str, destination: Path) -> None:
    print(f"Downloading: {url}")

    request = github_request(url)

    with urllib.request.urlopen(request, timeout=120) as response:
        total_header = response.headers.get("Content-Length")
        total_bytes = (
            int(total_header)
            if total_header and total_header.isdigit()
            else None
        )
        downloaded = 0

        with destination.open("wb") as output:
            while True:
                chunk = response.read(1024 * 1024)

                if not chunk:
                    break

                output.write(chunk)
                downloaded += len(chunk)

                if total_bytes:
                    percentage = downloaded * 100 / total_bytes
                    print(
                        f"\rDownloaded {downloaded / 1024 / 1024:.1f} MiB "
                        f"({percentage:.1f}%)",
                        end="",
                        flush=True,
                    )

    if total_bytes:
        print()


def safe_extract(zip_path: Path, destination: Path) -> None:
    """
    Extract a ZIP archive while preventing directory traversal.
    """
    destination = destination.resolve()

    with zipfile.ZipFile(zip_path) as archive:
        for member in archive.infolist():
            member_path = PurePosixPath(member.filename)

            if member_path.is_absolute() or ".." in member_path.parts:
                raise RuntimeError(
                    f"Unsafe path found in ZIP: {member.filename}"
                )

            output_path = destination.joinpath(*member_path.parts).resolve()

            try:
                output_path.relative_to(destination)
            except ValueError as error:
                raise RuntimeError(
                    f"ZIP entry escapes extraction directory: "
                    f"{member.filename}"
                ) from error

            if member.is_dir():
                output_path.mkdir(parents=True, exist_ok=True)
                continue

            output_path.parent.mkdir(parents=True, exist_ok=True)

            with archive.open(member) as source:
                with output_path.open("wb") as output:
                    shutil.copyfileobj(source, output)

            # Restore Unix permissions when included in the archive.
            permissions = member.external_attr >> 16

            if permissions:
                try:
                    output_path.chmod(stat.S_IMODE(permissions))
                except OSError:
                    pass


def archive_content_root(extraction_directory: Path) -> Path:
    """
    Strip the single top-level directory normally present in GitHub ZIPs.
    """
    entries = list(extraction_directory.iterdir())

    if len(entries) == 1 and entries[0].is_dir():
        return entries[0]

    return extraction_directory


def install_files(source: Path, target: Path) -> None:
    """
    Copy release files to the target directory.

    Existing files with matching names are replaced. Files not present in the
    release are left untouched.
    """
    target.mkdir(parents=True, exist_ok=True)

    for item in source.iterdir():
        destination = target / item.name

        if item.is_dir() and not item.is_symlink():
            # Replace a conflicting file with a directory.
            if destination.exists() and not destination.is_dir():
                destination.unlink()

            shutil.copytree(
                item,
                destination,
                dirs_exist_ok=True,
                copy_function=shutil.copy2,
            )
        else:
            # Replace a conflicting directory with a file.
            if destination.is_dir() and not destination.is_symlink():
                shutil.rmtree(destination)

            shutil.copy2(
                item,
                destination,
                follow_symlinks=False,
            )


def update(
    target: Path,
    include_prereleases: bool,
    force: bool = False,
) -> bool:
    target = target.resolve()
    date_file = target / RELEASE_DATE_FILE_NAME

    print(f"Checking {OWNER}/{REPOSITORY} for updates...")
    print(f"Installation directory: {target}")
    print(
        "Pre-releases: "
        + ("enabled" if include_prereleases else "disabled")
    )

    release = get_latest_release(include_prereleases)
    latest_date = parse_github_date(release["published_at"])
    installed_date = read_installed_release_date(date_file)

    release_name = (
        release.get("name")
        or release.get("tag_name")
        or "unnamed release"
    )

    release_type = (
        "pre-release" if release.get("prerelease", False) else "stable"
    )

    print(f"Selected release: {release_name} ({release_type})")
    print(
        "Installed release date: "
        + (
            format_github_date(installed_date)
            if installed_date
            else "unknown"
        )
    )
    print(f"Latest release date:    {format_github_date(latest_date)}")

    # Only install releases newer than the recorded release date.
    if installed_date is not None and latest_date <= installed_date and not force:
        print("No newer release is available.")
        return False

    zip_url, zip_name = find_zip_url(release)

    with tempfile.TemporaryDirectory(
        prefix=f"{REPOSITORY}-update-"
    ) as temporary_name:
        temporary_directory = Path(temporary_name)
        zip_path = temporary_directory / zip_name
        extraction_directory = temporary_directory / "extracted"

        extraction_directory.mkdir()

        download_file(zip_url, zip_path)

        print("Extracting release...")
        safe_extract(zip_path, extraction_directory)

        source_directory = archive_content_root(extraction_directory)

        print(f"Installing files into: {target}")
        install_files(source_directory, target)

    # Record the publication date only after installation succeeds.
    date_file.write_text(
        format_github_date(latest_date) + "\n",
        encoding="utf-8",
    )

    print(
        f"Successfully installed {release_name}, published "
        f"{format_github_date(latest_date)}."
    )
    return True


def main() -> int:
    script_directory = Path(__file__).resolve().parent

    parser = argparse.ArgumentParser(
        description=(
            f"Update {OWNER}/{REPOSITORY} from GitHub Releases, "
            "using the publication date to detect updates."
        )
    )

    parser.add_argument(
        "--target",
        type=Path,
        default=script_directory.parent,
        help=(
            "Installation directory. The default is the parent directory "
            "of this script."
        ),
    )

    prerelease_group = parser.add_mutually_exclusive_group()

    prerelease_group.add_argument(
        "--pre-releases",
        dest="include_prereleases",
        action="store_true",
        help="Include pre-releases when looking for updates.",
    )

    prerelease_group.add_argument(
        "--no-pre-releases",
        dest="include_prereleases",
        action="store_false",
        help="Only consider stable releases.",
    )

    parser.set_defaults(
        include_prereleases=DEFAULT_INCLUDE_PRERELEASES
    )

    parser.add_argument(
        "--force",
        action="store_true",
        help="Install the selected release regardless of its publication date.",
    )

    args = parser.parse_args()

    try:
        update(
            target=args.target,
            include_prereleases=args.include_prereleases,
            force=args.force,
        )
        return 0
    except urllib.error.HTTPError as error:
        print(
            f"GitHub returned HTTP {error.code}: {error.reason}",
            file=sys.stderr,
        )
    except urllib.error.URLError as error:
        print(f"Network error: {error.reason}", file=sys.stderr)
    except zipfile.BadZipFile:
        print(
            "The downloaded file is not a valid ZIP archive.",
            file=sys.stderr,
        )
    except PermissionError as error:
        print(f"Permission denied: {error}", file=sys.stderr)
    except (OSError, RuntimeError, KeyError, ValueError) as error:
        print(f"Update failed: {error}", file=sys.stderr)

    return 1


if __name__ == "__main__":
    raise SystemExit(main())