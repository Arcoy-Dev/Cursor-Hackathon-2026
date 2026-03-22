# Market Compare (Kotlin + Compose)

Modern Android app concept for searching and comparing product prices across supermarket inventories, with background history storage and search analytics over time.

## Usage

Since the mock data was set up in German only German keywords work with search. Please use German keywords.
Examples: 
- Rinderhack (minced meat)
- Milch (milk)
- Tomaten (tomatoes)

## Features
- Apple-like clean UI with rounded cards, gradients, and minimal design language.
- Product search and cross-market comparison.
- Cheapest offer highlighting and savings potential per product.
- Background persistence of every search and comparison using Room.
- Statistics screen with 7d/30d/all-time views plus savings-over-time chart.
- Account tab with sign-in state persistence.
- Cloud-sync-ready pipeline for unsynced analytics history.
- Periodic background analytics and sync worker with WorkManager.

## Tech Stack
- Kotlin
- Jetpack Compose (Material 3)
- Room (local database)
- WorkManager (background tasks)
- DataStore Preferences (auth session persistence)
- Retrofit + Gson (API and cloud sync integration points)
- MVVM-style ViewModel and Repository flow

## Project Structure
- `app/src/main/java/com/example/marketcompare/MainActivity.kt`
- `app/src/main/java/com/example/marketcompare/ui/screens/`
- `app/src/main/java/com/example/marketcompare/data/`
- `app/src/main/java/com/example/marketcompare/worker/`

## Run
1. Open the folder in Android Studio.
2. Let Android Studio sync the Gradle project.
3. If needed, generate the Gradle wrapper from Android Studio.
4. Run on an emulator or Android device (min SDK 26).

## Next Improvements
- Point `NetworkModule` base URL to your backend and implement endpoints:
  - `GET /inventory/search?q=...`
  - `POST /analytics/sync`
- Add real authentication provider (Firebase/Auth0/own backend).
- Add barcode scanning and favorites.
- Export monthly spending/saving reports.
