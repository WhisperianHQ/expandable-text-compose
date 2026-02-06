#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  ./scripts/release.sh <version> [--no-checks] [--no-gh] [--notes-file <path>]

What it does:
  - Updates VERSION_NAME in gradle.properties
  - Runs ./gradlew build (unless --no-checks)
  - Commits + tags v<version> + pushes
  - Creates a GitHub Release with gh (unless --no-gh), using CHANGELOG.md notes by default

Examples:
  ./scripts/release.sh 0.1.1
  ./scripts/release.sh 0.1.1 --no-gh
  ./scripts/release.sh 0.1.1 --notes-file /tmp/notes.md
EOF
}

if [[ ${1:-} == "-h" || ${1:-} == "--help" || -z ${1:-} ]]; then
  usage
  exit 0
fi

version="$1"
shift

run_checks=true
run_gh=true
notes_file=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --no-checks) run_checks=false ;;
    --no-gh) run_gh=false ;;
    --notes-file)
      shift
      notes_file="${1:-}"
      if [[ -z "${notes_file}" ]]; then
        echo "--notes-file requires a path argument." >&2
        exit 2
      fi
      ;;
    *)
      echo "Unknown arg: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
  shift
done

tag="v${version}"

extract_changelog_notes() {
  local ver="$1"
  local changelog="CHANGELOG.md"

  if [[ ! -f "${changelog}" ]]; then
    echo "CHANGELOG.md not found. Create it or pass --notes-file." >&2
    return 1
  fi

  # Extract section that starts at "## [<version>]" and ends before the next "## [" heading.
  perl -0777 -ne '
    my $ver = $ENV{VER};
    if ($_ !~ /(^|\n)## \[\Q$ver\E\].*?\n/s) { exit 2; }
    if ($_ =~ /(^|\n)(## \[\Q$ver\E\][^\n]*\n)(.*?)(\n## \[|\z)/s) {
      print $2;
      my $body = $3;
      $body =~ s/\s+\z/\n/s;
      print $body;
      exit 0;
    }
    exit 3;
  ' "${changelog}"
}

if ! git diff --quiet; then
  echo "Working tree is dirty; commit or stash first." >&2
  exit 1
fi

if git rev-parse "$tag" >/dev/null 2>&1; then
  echo "Tag $tag already exists." >&2
  exit 1
fi

if [[ ! -f gradle.properties ]]; then
  echo "gradle.properties not found in repo root." >&2
  exit 1
fi

perl -0777 -i -pe "s/^VERSION_NAME=.*\$/VERSION_NAME=${version}/m" gradle.properties

if $run_checks; then
  ./gradlew --no-daemon --stacktrace build
fi

git add gradle.properties
git commit -m "chore: release ${tag}"
git tag "${tag}"

git push origin HEAD
git push origin "${tag}"

if $run_gh; then
  if command -v gh >/dev/null 2>&1; then
    if [[ -z "${notes_file}" ]]; then
      notes_file="$(mktemp -t "release-notes-${version}.XXXXXX.md")"
      if ! VER="${version}" extract_changelog_notes "${version}" > "${notes_file}"; then
        echo "Could not extract notes for ${version} from CHANGELOG.md." >&2
        echo "Add a section starting with: ## [${version}] - YYYY-MM-DD" >&2
        echo "Or pass --notes-file <path>." >&2
        exit 1
      fi
    fi

    gh release create "${tag}" --title "${tag}" --notes-file "${notes_file}"
  else
    echo "gh not found; skipping GitHub Release creation." >&2
  fi
fi

echo "Release prepared: ${tag}"
echo "If you publish to Maven Central, run the Gradle publish task for this repo (credentials/signing required)."
