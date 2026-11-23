# Legal Notice, Compliance & Terms of Interoperability

## 1. Project Purpose & Scope

**Echo Web** is an open-source, non-commercial research and interoperability project designed to provide an interface for music playback across modern web platforms. It is developed independently as part of the Echo Music ecosystem.

This software is developed and distributed under the principles of interoperability and fair use:

- **United States Code:** 17 U.S.C. § 107 (Limitations on exclusive rights: Fair Use) and 17 U.S.C. § 1201(f) (Reverse Engineering for Interoperability of independently created computer programs).
- **European Union Law:** Directive 2009/24/EC of the European Parliament and of the Council, Article 6 (Decompilation solely to achieve interoperability with independently created programs).

This project is strictly non-commercial:

- There are no advertisements, subscriptions, monetized tiers, or commercial transactions.
- The project does not sell access to any third-party content or service.

---

## 2. No Hosting, Storage, or Distribution of Media

- **Zero Server-Side Storage:** Echo Web does **not** host, store, archive, index, or distribute any copyrighted sound recordings, musical compositions, album artwork, or lyrics on its servers or repositories. The application ships no media files.
- **Transient Streaming & CORS Relay:** When the local development proxy is used, streamed media chunks are handled transiently in memory to satisfy browser Cross-Origin Resource Sharing (CORS) and range header constraints for the Web Audio API. No streamed media data is written to server-side disk.
- **Local File Import:** The application offers a local music library feature. When a user chooses to import local audio files from their device, the metadata and audio blobs are stored locally in that user's browser (IndexedDB) on that user's device, solely for personal playback within the application. This data is not uploaded, synced, shared, or redistributed by the project.
- **Client-Side Processing:** All playback, digital signal processing (equalizers, stereo widening, audio graphing), and state management take place entirely in the user's browser client.

---

## 3. Third-Party Trademarks & Non-Affiliation

All product names, logos, brands, and registered trademarks referenced in this repository and documentation are the property of their respective owners:

- **YouTube** and **YouTube Music** are trademarks of **Google LLC**.
- **Apple** and **Apple Music** are trademarks of **Apple Inc.**
- **TIDAL** is a registered trademark of **Aspiro AB**.
- **KuGou** is a registered trademark of **Tencent Music Entertainment Group**.
- **SponsorBlock** is a crowd-sourced database developed by Ajay Ramachandran.

**Non-Affiliation Disclaimer:** Echo Music and Echo Web are independent open-source projects. They are **not** affiliated with, endorsed by, sponsored by, or associated with Google LLC, Apple Inc., Aspiro AB, Tencent Music Entertainment Group, or any of their subsidiaries.

---

## 4. Metadata, Lyrics, and Visual Art Interoperability

- **Lyrics:** Echo Web queries public and community-driven APIs (including LRCLIB and BetterLyrics) or standard Base64-encoded text feeds. The repository does not include or distribute proprietary decryption keys or circumvent digital rights management (DRM) systems.
- **Canvas Visuals:** Video backgrounds and preview assets are referenced directly from public content delivery endpoints or web embeds and are subject to the intellectual property rights of their respective copyright holders.
- **User Responsibility:** Users who compile, deploy, or run instances of this software are solely responsible for ensuring that their use complies with applicable local laws, regulations, and third-party terms of service.

---

## 5. DMCA Notice and Takedown Procedure

Echo Music respects the intellectual property rights of copyright holders. If you believe that your copyrighted work is being infringed or that any link in this project violates your rights under the Digital Millennium Copyright Act (17 U.S.C. § 512) or equivalent international legislation, please submit a notice to the repository maintainers via GitHub Issues or the designated contact with:

1. Identification of the copyrighted work claimed to have been infringed.
2. Identification of the specific material or URL to be removed or disabled.
3. Your contact information (name, address, telephone number, and email address).
4. A statement made in good faith that the disputed use is not authorized by the copyright owner, its agent, or the law.
5. A statement made under penalty of perjury that the information in the notification is accurate and that you are authorized to act on behalf of the owner.
