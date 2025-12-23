# Releasing jwarc

1. Prepare release notes in [CHANGELOG.md](CHANGELOG.md)
2. Prepare maven release: `mvn release:prepare -Prelease`
3. Perform maven release: `mvn release:perform -Prelease`
5. Copy release notes from [CHANGELOG.md](CHANGELOG.md) into [Github release](https://github.com/iipc/jwarc/releases)
6. Attach the jar to the release.
7. Build docker images:
```bash
version=$(git describe --tags --abbrev=0 | sed 's/^v//')
git checkout v$version
podman build --platform linux/amd64,linux/arm64 --manifest iipc/jwarc:$version .
podman manifest push --all iipc/jwarc:$version
podman manifest push --all iipc/jwarc:$version iipc/jwarc:latest
```
