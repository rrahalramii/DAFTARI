# DAFTARI — Your business. Your numbers. Your insight.

Daftari is an offline-first Android small-business manager built with **Kotlin + Jetpack Compose + Room**. Normal business operations stay on the phone. Internet is needed only when the owner explicitly asks the **GPT-powered Daftari AI Business Analyst** to analyze a selected date range or answer a follow-up question.

## Included

- Modern blue/white Daftari branding
- Dashboard
- Products & inventory
- Sales
- Purchases
- Expenses
- Customers
- Waste / damaged stock
- Reports
- Date-range AI Business Analyst
- AI follow-up questions
- Demo seed data for a car-accessories shop
- Room database stored locally as `daftari.db`
- No login

## AI behavior

The app calculates structured business data locally, then sends the selected period's relevant dataset and metrics to OpenAI. The AI is instructed to connect:

**Sales + Purchases + Inventory + Expenses + Waste**

It is explicitly told to distinguish facts from hypotheses, show the numbers behind findings, and avoid inventing records.

Example insight the demo data is designed to support:
- Continued purchases of LED headlights despite relatively low sell-through.
- Electricity expense changing while activity changes, prompting the owner to investigate cost-per-job rather than asserting a fault.

## OpenAI setup

1. Build/run the Android app.
2. Open **Settings**.
3. Enter your OpenAI API key.
4. Keep the model field as `gpt-5.6` unless you want to use another model available to your account.
5. Go to **AI Analyst**, choose the date range, and tap **Analyze selected period**.

The app uses the OpenAI **Responses API** over HTTPS.

### Important security note

This competition prototype stores the API key locally on the device because there is no backend/login requirement. Do **not** ship a real secret key inside a public APK or production client. For a public release, move the OpenAI call behind your own secure backend and keep the API key server-side.

## VS Code / Windows

Open the `Daftari` folder in VS Code. If Android SDK/Gradle are installed, use:

```powershell
gradle assembleDebug
```

or use the Gradle/Android tooling available in your existing Kotlin setup.

The project targets Android API 35 and requires Java 17.

## Project structure

```text
Daftari/
├── app/
│   └── src/main/
│       ├── java/com/daftari/app/
│       │   ├── MainActivity.kt
│       │   ├── SettingsStore.kt
│       │   ├── ai/OpenAiClient.kt
│       │   ├── data/Entities.kt
│       │   ├── data/Daos.kt
│       │   ├── data/AppDatabase.kt
│       │   ├── data/DaftariRepository.kt
│       │   ├── domain/BusinessAnalyzer.kt
│       │   └── ui/...
│       └── res/...
├── docs/
├── build.gradle.kts
└── settings.gradle.kts
```

## Design reference

The ZIP also contains the generated Daftari branding/mockup image used as the visual direction for the app.
