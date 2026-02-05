#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  ./scripts/release.sh <version> [--no-checks] [--no-gh]

What it does:
  - Updates VERSION_NAME in gradle.properties
  - Runs ./gradlew build (unless --no-checks)
  - Commits + tags v<version> + pushes
  - Creates a GitHub Release with gh (unless --no-gh)

Examples:
  ./scripts/release.sh 0.1.1
  ./scripts/release.sh 0.1.1 --no-gh
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
while [[ $# -gt 0 ]]; do
  case "$1" in
    --no-checks) run_checks=false ;;
    --no-gh) run_gh=false ;;
    *)
      echo "Unknown arg: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
  shift
done

tag="v${version}"

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
    gh release create "${tag}" --title "${tag}" --generate-notes
  else
    echo "gh not found; skipping GitHub Release creation." >&2
  fi
fi

echo "Release prepared: ${tag}"
echo "If you publish to Maven Central, run the Gradle publish task for this repo (credentials/signing required)."
