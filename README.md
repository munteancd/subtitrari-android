# Subtitrări

Android app that translates captions in the YouTube app and draws the translation over the video.

You keep using the normal YouTube app. Turn on CC, and the translation appears in a band on top of the original caption.

*[Română mai jos](#română)*

## How it works

1. An accessibility service reads the caption text YouTube has on screen.
2. Gemini translates it, with the two previous captions as context. That context is what makes auto-generated captions, cut mid-sentence, usable.
3. The translation is drawn in a band over the original. The band ignores touches, so the player works normally.

If Gemini doesn't answer, ML Kit translates on the phone. It is weaker and handles English videos only; for other languages the original caption stays visible.

## What you can choose

- Language to translate into (22 languages)
- Text colour
- Text size

## Limits

- It only translates captions that exist. A video with no captions, not even auto-generated ones, stays untranslated.
- It depends on a view ID inside the YouTube app (`subtitle_window_identifier`), so a YouTube update can break it.
- Caption text is sent to Google when Gemini translates it.
- The on-device fallback can get the meaning wrong, including the exact opposite. Don't rely on the translation for safety instructions.
- Gemini's free tier has request limits. The app falls back to ML Kit when it reaches them.

## Build

Needs Android Studio and a [Gemini API key](https://aistudio.google.com/apikey). Add the key to `local.properties`, which git ignores:

```properties
GEMINI_API_KEY=your-key
```

Then:

```bash
./gradlew assembleDebug
```

`gradle.properties` points `org.gradle.java.home` at Android Studio's default Windows folder. Change or remove that line on other machines.

**Don't publish a built APK.** It contains your API key.

After installing, turn on **Subtitrări** under Settings → Accessibility. Android may ask you to allow restricted settings first, from the app's info screen.

---

## Română

Aplicație Android care traduce subtitrările din aplicația YouTube și afișează traducerea peste video.

Folosești YouTube-ul obișnuit. Pornești CC, iar traducerea apare într-o bandă peste subtitrarea originală.

### Cum funcționează

1. Un serviciu de accesibilitate citește textul subtitrării de pe ecran.
2. Gemini îl traduce, ținând cont de cele două subtitrări de dinainte. Așa ajung să se înțeleagă și subtitrările automate, care vin tăiate.
3. Traducerea apare într-o bandă peste original. Banda nu blochează atingerile, deci playerul merge normal.

Dacă Gemini nu răspunde, traduce ML Kit pe telefon. Traducerea e mai slabă și merge doar la clipuri în engleză. La celelalte limbi rămâne vizibilă subtitrarea originală.

### Ce poți alege

- Limba în care se traduce (22 de limbi)
- Culoarea textului
- Mărimea textului

### Limite

- Traduce doar subtitrări care există. Un clip fără nicio subtitrare, nici măcar automată, rămâne netradus.
- Depinde de un identificator din aplicația YouTube (`subtitle_window_identifier`), deci o actualizare YouTube o poate strica.
- Textul subtitrărilor ajunge la Google atunci când îl traduce Gemini.
- Traducerea de rezervă poate greși sensul, chiar invers. Nu te baza pe ea la instrucțiuni de siguranță.
- Planul gratuit Gemini are limite de cereri. Când sunt atinse, aplicația trece pe ML Kit.

### Compilare

Ai nevoie de Android Studio și de o [cheie Gemini](https://aistudio.google.com/apikey). Pune cheia în `local.properties`, fișier ignorat de git:

```properties
GEMINI_API_KEY=cheia-ta
```

Apoi:

```bash
./gradlew assembleDebug
```

`gradle.properties` indică folderul implicit al Android Studio pe Windows prin `org.gradle.java.home`. Pe alte calculatoare, schimbă sau șterge linia.

**Nu publica un APK compilat.** Conține cheia ta.

După instalare, pornește **Subtitrări** din Setări → Accesibilitate. Android poate cere întâi să permiți setările restricționate, din ecranul de informații al aplicației.
