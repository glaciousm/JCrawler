# JCrawler - Portable Build Guide

## The JavaFX Portability Challenge

**Problem:** JavaFX applications cannot be distributed as simple executable JARs because JavaFX uses the Java Module System, which requires explicit module configuration at runtime.

**Solution:** Use `jpackage` to create a native executable with bundled JRE and JavaFX.

---

## ✅ Best Solution: Native Executable with jpackage

This creates a **truly portable** Windows `.exe` (or `.app` for Mac, binary for Linux) with everything bundled - no Java installation needed!

### Quick Start

**Option 1: Simple Build (Easiest)**
```bash
build-native-simple.bat
```

This creates: `target\installer\JCrawler\JCrawler.exe`

**Option 2: Optimized Build (Smaller size)**
```bash
build-native.bat
```

This creates a custom JRE with only required modules - results in smaller distribution.

---

## Manual Build Instructions

### Prerequisites
- JDK 17 or higher installed
- Maven installed
- JavaFX SDK (if not using JDK with JavaFX bundled)

### Step 1: Build the JAR
```bash
mvn clean package
```

### Step 2: Create Native Executable

**Windows:**
```bash
jpackage --type app-image ^
         --input target ^
         --name JCrawler ^
         --main-jar jcrawler-1.0.0.jar ^
         --main-class com.jcrawler.JCrawlerApplication ^
         --dest target\installer ^
         --app-version 1.0.0
```

**Linux:**
```bash
jpackage --type app-image \
         --input target \
         --name JCrawler \
         --main-jar jcrawler-1.0.0.jar \
         --main-class com.jcrawler.JCrawlerApplication \
         --dest target/installer \
         --app-version 1.0.0
```

**macOS:**
```bash
jpackage --type app-image \
         --input target \
         --name JCrawler \
         --main-jar jcrawler-1.0.0.jar \
         --main-class com.jcrawler.JCrawlerApplication \
         --dest target/installer \
         --app-version 1.0.0
```

---

## Result

You'll get a folder structure like:
```
target/installer/JCrawler/
├── JCrawler.exe          (Windows executable)
├── bin/
├── lib/
└── runtime/              (Bundled JRE + JavaFX)
```

**This entire folder is portable!** You can:
- Zip it and send to users
- Copy to any Windows machine
- Run without installing Java
- Double-click `JCrawler.exe` to launch

---

## Distribution Size

- **With custom runtime** (~60-80 MB): Includes minimal JRE
- **With full JRE** (~150-200 MB): Includes complete JRE

Compare this to:
- **Spring Boot JAR**: 50-60 MB (requires Java installed)
- **Our lightweight JAR**: 15-20 MB (requires Java + JavaFX installed)
- **Native executable**: 60-200 MB (nothing required!)

---

## Why Not Just `java -jar`?

You asked for portability, and here's the honest answer:

**The `java -jar jcrawler.jar` approach requires:**
1. User has Java 17+ installed
2. User has JavaFX installed or configured
3. User knows how to run: `java --module-path ... --add-modules ... -jar ...`

**The native executable approach:**
1. User double-clicks `JCrawler.exe`
2. That's it!

**True portability = Native executable**

---

## Creating an Installer

To create a Windows installer (.msi or .exe):

```bash
jpackage --type msi ^
         --input target ^
         --name JCrawler ^
         --main-jar jcrawler-1.0.0.jar ^
         --main-class com.jcrawler.JCrawlerApplication ^
         --dest target\installer ^
         --app-version 1.0.0 ^
         --win-per-user-install ^
         --win-menu ^
         --win-shortcut
```

This creates `JCrawler-1.0.0.msi` that users can install like any Windows app.

---

## Optimizing Size with jlink

For the smallest distribution, use `jlink` to create a custom JRE with only required modules:

```bash
# Create custom runtime
jlink --add-modules java.base,java.desktop,java.sql,java.naming,java.management,javafx.controls,javafx.fxml,javafx.graphics,javafx.web ^
      --output target\custom-runtime ^
      --strip-debug ^
      --compress=2 ^
      --no-header-files ^
      --no-man-pages

# Use custom runtime in jpackage
jpackage --runtime-image target\custom-runtime ^
         --input target ^
         --name JCrawler ^
         --main-jar jcrawler-1.0.0.jar ^
         --main-class com.jcrawler.JCrawlerApplication ^
         --dest target\installer
```

This can reduce the distribution size to ~40-60 MB.

---

## Alternative: Use Maven's JavaFX Plugin (Development Only)

For **development** (not distribution), you can run:
```bash
mvn javafx:run
```

But this requires:
- Maven installed
- Source code available
- Not suitable for end users

---

## Summary

| Approach | Portability | Size | User Requirements |
|----------|-------------|------|-------------------|
| `java -jar` | ❌ Poor | 15-20 MB | Java 17 + JavaFX + CLI knowledge |
| `mvn javafx:run` | ❌ Poor | N/A | Maven + Source code |
| **jpackage exe** | ✅ **Excellent** | 60-200 MB | **None!** |
| jpackage installer | ✅ **Excellent** | 60-200 MB | **None!** |

---

## Recommendation

For **true portability** as you requested:

1. Run `build-native-simple.bat`
2. Zip the `target\installer\JCrawler` folder
3. Send to users
4. Users unzip and double-click `JCrawler.exe`

**No Java required. No setup. Just works.**

---

## Troubleshooting

### "jpackage: command not found"
- Install JDK 17+ (not just JRE)
- Make sure JAVA_HOME is set
- Add `%JAVA_HOME%\bin` to PATH

### "Error: Missing JavaFX modules"
- If using OpenJDK, download JavaFX jmods separately
- Or use a JDK with JavaFX bundled (e.g., Liberica Full JDK)
- Place jmods in: `%JAVA_HOME%\jmods`

### Build succeeds but exe won't run
- Make sure main class is correct: `com.jcrawler.JCrawlerApplication`
- Check that all dependencies are in target folder
- Try running: `target\installer\JCrawler\JCrawler.exe` from command line to see errors

---

## Next Steps

Once you have the native executable, you can:
1. Distribute the folder as a ZIP
2. Create an installer with jpackage
3. Sign the executable (for Windows SmartScreen)
4. Upload to your website or GitHub releases

Your users will thank you for the easy installation!
