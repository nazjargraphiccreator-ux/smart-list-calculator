# 🛒 Smart-list-Calculator (Android 2026)
### *Aplicație Mobilă Inteligentă de Cumpărături, Calculator Pre-Plată la Raft & Monitorizare Reduceri Magazine Militari București*

[![Platform](https://img.shields.io/badge/Platform-Android_8.0+_(API_26--35)-green.svg?style=for-the-badge&logo=android)](https://www.android.com/)
[![Language](https://img.shields.io/badge/Language-Kotlin_2.0-purple.svg?style=for-the-badge&logo=kotlin)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack_Compose_Material3-blue.svg?style=for-the-badge&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Clean_Architecture_+_MVI-orange.svg?style=for-the-badge)]()
[![Database](https://img.shields.io/badge/Database-Room_SQLite_(100%25_Offline)-red.svg?style=for-the-badge&logo=sqlite)](https://developer.android.com/training/data-storage/room)
[![Release](https://img.shields.io/badge/Release-v1.0.0--PRO_APK_Ready-success.svg?style=for-the-badge)](Smart-list-Calculator-Release-2026.apk)

---

## 📱 Prezentare Generală & Identitate Vizuală

**Smart-list-Calculator** este o aplicație modernă, performantă și complet independentă, concepută pentru a transforma cumpărăturile din supermarket într-o experiență calculată, rapidă și economică. 

Aplicația combină un **calculator de buget la raft în timp real** cu un **Agent Inteligent dedicat marilor magazine din zona Militari, București** (*Kaufland Valea Cascadelor* și *Auchan Militari*), salvare automată a bazei de date **100% offline** și export dedicat în **fișiere `.txt` structurate direct în memoria telefonului tău**.

<div align="center">
  <img src="docs/mockups/smart_list_calculator_icon.jpg" alt="Smart-list-Calculator App Icon" width="180" style="border-radius: 28px; box-shadow: 0 10px 30px rgba(0,0,0,0.3); margin-bottom: 20px;" />
  <br/>
  <em>Iconiță Ultra-Modernă 3D Glassmorphic • Smart-list-Calculator</em>
</div>

---

## 📸 Mockup-uri & Prezentare Ecrane

Aplicația a fost proiectată conform celor mai stricte standarde Material Design 3, oferind temă întunecată cu accente neon vibrante, contrast ridicat pentru lizibilitate optimă sub luminile puternice din supermarket și micro-animații fluide la 120Hz.

### 1. Prezentare Generală Liste & Monitorizare Buget
Gestionare dinamică a listelor, stadiu de bifare cu procente, urmărire vizuală a bugetului și acces rapid la funcții cheie.

<div align="center">
  <img src="docs/mockups/mockup_app_overview.jpg" alt="Prezentare Generală Liste Smart-list-Calculator" width="85%" style="border-radius: 16px; margin: 15px 0;" />
</div>

---

### 2. Agent Inteligent & Monitorizare Reduceri (Kaufland & Auchan Militari)
Comparație instantă între coșul de la **Kaufland Militari** și **Auchan Militari**, economie netă calculată în RON, navigator la raft (număr raion) și alerte automate de promoții în bara de notificări.

<div align="center">
  <img src="docs/mockups/mockup_agent_deals.jpg" alt="Agent Inteligent Reduceri Militari" width="85%" style="border-radius: 16px; margin: 15px 0;" />
</div>

---

### 3. Calculator Pre-Plată la Raft & Comparație Preț Unitar
Calculează suma exactă înainte de casa de marcat, adaugă automat garanția **SGR (0.50 RON)** pe ambalaj, compară prețul pe kilogram / litru și semnalizează prin cod de culoare atingerea pragului de cheltuieli.

<div align="center">
  <img src="docs/mockups/mockup_prepayment_calc.jpg" alt="Calculator Pre-Plata Raft si SGR" width="85%" style="border-radius: 16px; margin: 15px 0;" />
</div>

---

## 🌟 Funcționalități Majore

### 🤖 1. Agent Inteligent & Monitorizare Reduceri Magazine Militari
Aplicația integrează un modul nativ cu date sincronizate pentru principalele hipermarketuri din cartierul Militari, București:
* **Kaufland Militari** — *Strada Valea Cascadelor nr. 3, Sector 6*
* **Auchan Militari** — *Centrul Comercial Militari, Bulevardul Iuliu Maniu nr. 560A*

#### Capabilități Agent:
* **Comparație Coș**: Evaluează automat produsele din lista ta și îți arată care magazin are cel mai mic total și câți RON economisești.
* **Găsește pe Raft (In-Store Aisle Navigator)**: Îți indică raionul exact unde se află fiecare produs (ex: *Raionul 4 - Lactate*, *Raionul 7 - Băuturi*, *Raionul 1 - Panificație*).
* **Catalog Reduceri Active**: Reduceri de până la 35% actualizate pe categorii esențiale (ulei, cafea, lactate, detergenți etc.).
* **Alerte Automate în Bara de Notificări**: Sistem de notificări Android cu sunet și vibrație pentru reduceri fulger la magazinele din Militari.

---

### 💾 2. Salvare Locală pe Telefon & Export în Fisiere `.txt`
Intimitatea și siguranța datelor tale sunt prioritare:
* **Bază de date 100% Locală (Room SQLite)**: Niciun cont obligatoriu pe cloud, toate listele, istoricul de prețuri și produsele rămân stocate intern pe telefon.
* **Folder Dedicat pe Dispozitiv**: Aplicația creează și întreține automat folderul dedicat:
  ```text
  📁 Stocare Internă / Documents / SmartListCalculator /
  ```
* **Fișiere `.txt` Detaliate Generate**:
  Fiecare listă poate fi salvată cu 1 tap sub forma unui fișier text structurat și ușor de citit pe orice dispozitiv sau de trimis prin Bluetooth/USB:
  * `Lista_Cumparaturi_Familie_2026-10-15_22-00.txt`
  * `Index_Toate_Listele_SmartListCalculator.txt`

#### Exemplu format fișier `.txt` salvat:
```text
=================================================================
         SMART-LIST-CALCULATOR • RAPORT DETALIAT LISTĂ           
=================================================================

📋 NUME LISTĂ: Cumpărături Săptămână
📅 DATA SALVĂRII: 15 octombrie 2026, 22:15
🏪 MAGAZIN ASOCIAT: Kaufland Militari
📍 ADRESĂ MAGAZIN: Str. Valea Cascadelor nr. 3, Sector 6
💰 BUGET LIMITĂ: 250.00 RON
📊 TOTAL ESTIMAT: 198.40 RON
💳 TOTAL CHELTUIT EFECTIV: 184.20 RON
🎯 PROGRES: 8/8 produse (100%)

-----------------------------------------------------------------
                       PRODUSE ȘI DETALII                        
-----------------------------------------------------------------
01. [CUMPĂRAT] Lapte Zuzu 1.5% 1L
    Cantitate: 2.0 buc (2 ambalaje/buc) • Preț unitar: 6.89 RON • Total: 13.78 RON
    Categorie: Lactate
    Magazin/Raion: Raionul 4 - Frigidere Lactate

02. [CUMPĂRAT] Apă Minerală Borsec 2L
    Cantitate: 6.0 buc (6 ambalaje/buc) • Preț unitar: 3.49 RON • Total: 20.94 RON
    Categorie: Băuturi
    Magazin/Raion: Raionul 7 - Ape și Sucuri
    Note: Include garanție SGR 0.50 RON/sticlă
=================================================================
Folder dedicat telefon: /storage/emulated/0/Documents/SmartListCalculator/
Generat automat de aplicația Smart-list-Calculator (Android 2026)
=================================================================
```

---

### 🧮 3. Calculator Pre-Plată & Detector Garanție SGR
* **Estimare Real-Time**: Pe măsură ce adaugi produse în coș la cumpărături, totalul se recalculează instant.
* **Garanție SGR (0.50 RON)**: Recunoaște automat ambalajele de sticlă, plastic și doze de aluminiu supuse Sistemului de Garanție-Returnare din România și adaugă 0.50 RON/bucată în defalcarea financiară.
* **Comparație Preț pe Unitate**: Vezi instant dacă un pachet mare de 800g este mai avantajos decât 2 pachete de 350g (calcul automat RON/kg).
* **Indicator Vizual Buget**:
  * 🟢 **Verde**: Consum sub 80% din bugetul alocat.
  * 🟡 **Galben/Portocaliu**: Între 80% și 100% din buget (Avertizare prag).
  * 🔴 **Roșu**: Depășire de buget cu alertă haptică.

---

### 🚀 4. Alte Funcționalități Integrate
* **Flux de Cumpărături în 3 Stadii**: `De Căutat` ➔ `În Coș` ➔ `Achiziționat / Plătit`.
* **Smart Text Parser**: Copiază o listă simplă din WhatsApp sau notițe și aplicația o împarte automat în produse cu cantități și unități.
* **Partajare Instant WhatsApp**: Trimite lista aranjată cu bifă către partenerul de viață sau colegi direct pe WhatsApp.
* **Sincronizare Google Opțională**: Pentru utilizatorii care doresc și backup securizat în contul propriu Google Firebase.

---

## 📦 Instalare Rapidă pe Dispozitiv Android

Pachetul de instalare final optimizat și semnat este gata în rădăcina proiectului:
📂 **`Smart-list-Calculator-Release-2026.apk`** *(2.77 MB)*

### Opțiunea 1: Instalare Directă din Telefon (Recomandat)
1. Descarcă fișierul `Smart-list-Calculator-Release-2026.apk` pe telefonul tău Android.
2. Deschide fișierul descărcat din aplicația de gestionare a fișierelor (Files / Descărcări).
3. Dacă este solicitat, activează permisiunea **„Permite instalarea din această sursă”**.
4. Apasă **Instalați** și deschide aplicația!

### Opțiunea 2: Instalare prin Cablu USB (ADB)
Dacă ai telefonul conectat la computer cu modul *USB Debugging* activat:
```bash
adb install Smart-list-Calculator-Release-2026.apk
```

---

## 🛠️ Arhitectură & Tehnologii

Proiectul este dezvoltat conform principiilor **Clean Architecture**, decuplat pe straturi cu dependențe unidirecționale:

```text
ro.smartshopping.list/
├── data/
│   ├── local/
│   │   ├── dao/                     # Room Data Access Objects
│   │   ├── database/                # Room SQLite Database Instance & Migrations
│   │   ├── entity/                  # Entități SQLite
│   │   └── LocalBackupStorageManager.kt # Gestionare folder dedicat & export .TXT
│   └── repository/
│       ├── ShoppingListRepositoryImpl.kt
│       └── StoreDealsRepository.kt  # Sursă date Kaufland & Auchan Militari
├── domain/
│   ├── model/                       # Modele imutabile (ShoppingList, StoreDeal, etc.)
│   └── usecase/
│       ├── SmartDiscountAgentUseCase.kt  # Algoritm comparație coș & economii
│       ├── PrePaymentCalculatorUseCase.kt # Calcule preț unitar & SGR
│       └── CalculateFinancialSummaryUseCase.kt
├── ui/
│   ├── components/                  # Componente UI refolosibile Material 3
│   ├── navigation/                  # AppNavHost, BottomNavBar, Rute
│   ├── screens/
│   │   ├── lists/                   # Ecran Liste Principale
│   │   ├── detail/                  # Ecran Detaliu Listă & Buton Export TXT
│   │   ├── shopping/                # Mod Cumpărături la Raft & Pre-Payment
│   │   └── agent/                   # Ecran Agent Inteligent Magazine Militari
│   └── theme/                       # Paletă cromatică Neon Glass, Tipografie, Shape
└── util/
    └── StoreNotificationHelper.kt   # Notificări locale Android NotificationCompat
```

---

## 🧪 Rulare Teste & Compilare din Cod Sursă

Aplicația beneficiază de o suită completă de 26 de teste unitare automate:

```bash
# Rulare toate testele unitare
./gradlew test

# Rulare build complet în mod Release
./gradlew assembleRelease
```

---

## 📄 Licență & Credite
Dezvoltat cu pasiune pentru cumpărături inteligente în România de echipa **Nazjar Graphic Creator UX**.  
Toate drepturile rezervate © 2026.
