# AI Hub

Moderní, agentově konfigurovatelná AI aplikace s lokální historií (Room db), pokročilým modelovým routingem na bázi poskytovatelů API a bezpečným lokálním úložištěm klíčů.

## Shrnutí Architektury a Funkcionalit

Tato aplikace dodržuje **Clean Architecture** (MVI) s `ViewModels` v každé stěžejní obrazovce, asynchronním rozhraním (Kotlin Coroutines & Flow), síťovou vrstvou používající **Ktor** (s implementací `OpenAiCompatibleProvider` a `GeminiProvider`) a bezpečným ukládáním citlivých položek (SecureKeyStore, využívá Android Keystore System s `EncryptedSharedPreferences`).

- **Chat Engine:** Plná podpora system promptů (ukládaná na úroveň konverzací), multimodální nahrávání souborů a obrázků do kontextu, streamování odpovědí přes `Server Sent Events`.
- **Lokální perzistence:** Rozsáhlá implementace přes Jetpack Room rozložená do entit (`ProviderEntity`, `AiModelEntity`, `ConversationEntity`, `MessageEntity`).
- **Provideři a Klíče:** Pokročilá správa providerů s možností přidávat libovolné open-source zástupce využívající OpenAI / Groq standard, s rotací vícero API klíčů pro jednoho providera. Tlačítka pro manuální testování stavu klíče i načtení dostupných remote modelů přes `/v1/models`.

## Instrukce k Sestavení a Spuštění

Pro spuštění a kompilaci je vyžadován standardní nástroj s Kotlin DSL Gradle uvnitř AI Studio platformy (nebo lokálně za podmínek verze JDK 17+ a platného AGP).
Veškeré závislosti (`Coil` na multimodál, `Ktor` klient na backend, `Room` a `Coroutines`) jsou již začleněny a uzamknuty ve Version Catalog `gradle/libs.versions.toml`.

Můžete přímo volat např. `gradle bundleDebug` ke stavbě `.aab` nebo aplikaci rovnou otevřít pomocí `compile_applet` ke kompilaci.

## Správa API Klíčů (Secure Key Management)

Aplikace nenakládá s žádným API klíčem v root složce, ani nezajímá `.env` systém jakožto jediný způsob logování. Poskytuje robustní grafické UI, což dělá z nástroje **plně samostatný ekosystém**:

1. Přejděte v horní levé navigaci ("hamburger") do sekce _AI Hub_ a klikněte na ikonu ozubeného kola. Zde jděte do **Provideři & Klíče**. 
2. Nativní provideři (Google Gemini) se zde nachází buď již od instalace (jakmile je přidáte), nebo můžete ručně kliknout na "+ (Přidat providera)".
3. Vyberte pro něj požadovaný target (např. klíč `Groq`, formát `OpenAI Compatible`, Base URL `https://api.groq.com/openai/v1`).
4. Ve sbalovacím menu každého Providera je sekce "API Klíče" klikněte na `Přidat klíč` a vložte zde svůj validní klíč.
5. V sekci modely pak přes `Načíst z API` natáhnete automaticky všechny povolené endpointy pro vybraný systém, klikem si je označíte do Favorites. Poté se zobrazí v hlavním chatu.

S klíči není nijak dál volně manipulováno a jsou trvale zašifrovány ve vrstvě zařízení.

## Podpora Providerů (Out-of-the-box)

- Google Gemini 1.5 a dřívější (skrze `generativelanguage` format mapping APIs)
- Groq, TogetherAI, NVIDIA NGC a další modely podporující standard portovaný z OpenAI.
- Local inference servery jako Ollama nebo LM-Studio (stačí zadat formát "OpenAI Compatible" a `http://localhost:11434/v1` base adresa - z Android cloudu dostupné složitěji, avšak aplikovatelné pro local zařízení!).
