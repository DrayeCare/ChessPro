# ♟️ CHESS PRO - Java Desktop Edition

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Swing](https://img.shields.io/badge/GUI-Java_Swing-007396?style=for-the-badge&logo=java&logoColor=white)
![Status](https://img.shields.io/badge/Status-Active-success?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

**Chess Pro** adalah aplikasi permainan catur desktop berbasis **Java Swing** yang dirancang dengan tampilan modern, mekanik permainan layaknya platform catur profesional (seperti *Chess.com*), serta dilengkapi dengan AI Bot dengan berbagai tingkat kesulitan.

---

## ✨ Fitur Utama

* 🎨 **Tema Visual Modern ("Neo"):** Menggunakan aset bidak catur bergaya *Neo* beresolusi tinggi dengan papan 2D kustom (`Graphics2D`) lengkap dengan *anti-aliasing*.
* 🤖 **Mode Permainan Variatif:**
  * **Player vs Bot (PvE):** Tantang AI dengan 3 tingkat kesulitan:
    * `Easy` — Langkah acak yang valid.
    * `Medium` — Memprioritaskan pemakanan bidak lawan (*capture*).
    * `Hard` — Evaluasi posisi papan berdasarkan nilai materi bidak dan keamanan petak dari serangan lawan.
  * **Local PvP:** Bermain bersama teman secara bergantian dalam satu perangkat.
  * **Online Matchmaking:** *(Dalam tahap pengembangan)*.
* ⚡ **Mekanik Catur Pro:**
  * **Premove System:** Antrekan langkahmu saat Bot sedang berpikir untuk permainan tempo cepat.
  * **Tactical Arrows:** Gambar panah taktis berwarna oranye di atas papan untuk merencanakan strategi.
  * **Smart Highlighting:** Petunjuk visual titik langkah legal serta penanda merah untuk bidak lawan yang bisa dimakan.
  * **Auto-Board Flip:** Papan otomatis berputar jika kamu memilih bermain sebagai bidak **Hitam** melawan Bot.
* ⏱️ **Kontrol Waktu (Chess Clock):** Pilihan durasi pertandingan **1 Menit** (*Bullet*), **3 Menit** (*Blitz*), dan **10 Menit** (*Rapid*).
* 🔊 **Synthesized Audio SFX:** Efek suara bawaan (*move*, *capture*, *check/game over*) yang dihasilkan secara real-time menggunakan `javax.sound.sampled` tanpa memerlukan file audio eksternal.
* 📜 **Live Move History & ELO System:** Catatan notasi pergerakan bidak secara langsung serta sistem peringkat (*Rank* & *ELO*) untuk mode PvE.

---

## 🎮 Kontrol Permainan

| Aksi | Kontrol Mouse |
| :--- | :--- |
| **Memilih / Memindahkan Bidak** | **Klik Kiri** pada bidak lalu klik petak tujuan, atau **Drag & Drop** bidak. |
| **Membuat Panah Strategi** | **Klik Kanan + Tahan (Drag)** dari petak asal ke petak tujuan. |
| **Menghapus Panah** | **Klik Kiri** di mana saja pada papan. |
| **Melakukan Premove (Mode PvE)** | **Drag & Drop** bidakmu saat giliran Bot sedang berlangsung. |

---

## 🚀 Cara Menjalankan Aplikasi

### Prasyarat
* **Java Development Kit (JDK)** versi 8 atau yang lebih baru.
* **Koneksi Internet** (diperlukan saat pertama kali dijalankan untuk memuat tekstur bidak catur *Neo*).

### Langkah Instalasi

1. **Clone repositori ini:**
   ```bash
   git clone https://github.com/username-kamu/chess-pro-java.git
   cd chess-pro-java
   ```

2. **Compile kode sumber Java:**
   ```bash
   javac ChessGame.java
   ```

3. **Jalankan game:**
   ```bash
   java ChessGame
   ```

---

## 📸 Cuplikan Layar (Screenshots)

> *Tip: Tambahkan screenshot tampilan Main Menu, papan permainan dengan panah taktis, dan layar Post-Game di bagian ini agar repositorimu terlihat semakin menarik!*

| Main Menu | Gameplay & Tactical Arrows |
| :---: | :---: |
| ![Main Menu](https://via.placeholder.com/400x300?text=Screenshot+Main+Menu) | ![Gameplay](https://via.placeholder.com/400x300?text=Screenshot+Gameplay) |

---

## 🛠️ Rencana Pengembangan (Roadmap)

- [x] Validasi aturan dasar catur, Skak, Skakmat, Pat (Stalemate), & Rokade (*Castling*).
- [x] Fitur *Premove* dan *Right-Click Tactical Arrows*.
- [x] 3 Tingkat kesulitan Bot AI & Sistem ELO lokal.
- [ ] Menambahkan aturan *En Passant* dan pilihan promosi pion (*Underpromotion* ke Kuda/Benteng/Gajah).
- [ ] Peningkatan algoritma Bot menggunakan *Minimax* dengan *Alpha-Beta Pruning*.
- [ ] Implementasi fitur *Play Online (Matchmaking)* berbasis Socket/Server.

---

## 🤝 Kontribusi

Kontribusi, laporan *bug*, dan permintaan fitur baru sangat dipersilakan! Silakan buat *Issue* atau ajukan *Pull Request* pada repositori ini.

---

## 📄 Lisensi

Proyek ini didistribusikan di bawah lisensi **MIT**. Silakan gunakan dan kembangkan lebih lanjut untuk keperluan belajar maupun proyek pribadi.