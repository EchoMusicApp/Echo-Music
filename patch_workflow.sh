#!/bin/bash
# 1. Add tags trigger
sed -i '' -e '/branches:/i\
    tags:\
      - "v*"\
' /Users/aditya/Development/EchoMusic/.github/workflows/android-release.yml

# 2. Append the release job
cat << 'INNER_EOF' >> /Users/aditya/Development/EchoMusic/.github/workflows/android-release.yml

  create-github-release:
    name: Create GitHub Release
    runs-on: ubuntu-latest
    needs: [build-full-release, build-foss-release]
    # Only run this job if a tag was pushed (e.g. v1.0.0)
    if: startsWith(github.ref, 'refs/tags/v')
    permissions:
      contents: write
    steps:
      - name: Download Full Release APK
        uses: actions/download-artifact@v4
        with:
          name: app-full-release
          path: release-artifacts/full

      - name: Download FOSS Release APK
        uses: actions/download-artifact@v4
        with:
          name: app-foss-release
          path: release-artifacts/foss

      - name: Create Release and Upload Assets
        uses: softprops/action-gh-release@v2
        with:
          # Automatically uses the pushed tag
          name: Echo Music ${{ github.ref_name }}
          draft: false
          prerelease: false
          generate_release_notes: true
          files: |
            release-artifacts/full/*.apk
            release-artifacts/foss/*.apk
INNER_EOF
chmod +x patch_workflow.sh && ./patch_workflow.sh
