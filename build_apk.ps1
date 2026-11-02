<#
.SYNOPSIS
    Script d'installation automatique des outils Android (sans Android Studio)
    et compilation de l'APK AthanSalat.

.DESCRIPTION
    Ce script :
    1. Télécharge et installe JDK 17 (Temurin / Adoptium)
    2. Télécharge Android SDK Command-Line Tools (léger)
    3. Installe seulement les composants nécessaires via sdkmanager
    4. Configure local.properties
    5. Compile l'APK debug

    Total téléchargé : ~600 MB (contre 4 GB pour Android Studio)
#>

# ─── CONFIGURATION ─────────────────────────────────────────────────────────────
$PROJECT_DIR   = "C:\Users\M\Desktop\salat_quran\AthanSalat"
$TOOLS_DIR     = "C:\AndroidSdkLite"          # Dossier d'installation SDK léger
$JDK_DIR       = "$TOOLS_DIR\jdk17"
$SDK_DIR       = "$TOOLS_DIR\sdk"

# URLs de téléchargement
$JDK_URL    = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.11%2B9/OpenJDK17U-jdk_x64_windows_hotspot_17.0.11_9.zip"
$CMDTOOLS_URL = "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"

# ─── COULEURS ──────────────────────────────────────────────────────────────────
function Write-Step($msg)  { Write-Host "`n[>] $msg" -ForegroundColor Cyan }
function Write-OK($msg)    { Write-Host "[OK] $msg" -ForegroundColor Green }
function Write-Warn($msg)  { Write-Host "[!] $msg"  -ForegroundColor Yellow }
function Write-Fail($msg)  { Write-Host "[X] $msg"  -ForegroundColor Red; exit 1 }

# ─── ÉTAPE 0 : Vérifications préliminaires ─────────────────────────────────────
Write-Step "Vérification de l'espace disque..."
$drive = Split-Path $TOOLS_DIR -Qualifier
$free  = (Get-PSDrive ($drive -replace ':','')).Free / 1GB
if ($free -lt 2) { Write-Fail "Pas assez d'espace disque (besoin de 2 GB minimum, disponible: $([math]::Round($free,1)) GB)" }
Write-OK "Espace disponible : $([math]::Round($free,1)) GB"

# ─── ÉTAPE 1 : Créer les dossiers ──────────────────────────────────────────────
Write-Step "Création des dossiers..."
New-Item -ItemType Directory -Force -Path $TOOLS_DIR | Out-Null
New-Item -ItemType Directory -Force -Path $JDK_DIR   | Out-Null
New-Item -ItemType Directory -Force -Path $SDK_DIR   | Out-Null
Write-OK "Dossiers créés dans : $TOOLS_DIR"

# ─── ÉTAPE 2 : Installer JDK 17 ────────────────────────────────────────────────
$JDK_ZIP = "$TOOLS_DIR\jdk17.zip"
if (-not (Test-Path "$JDK_DIR\bin\java.exe")) {
    Write-Step "Téléchargement JDK 17 (Adoptium Temurin)..."
    Write-Warn "Taille : ~180 MB - Patience..."
    try {
        Invoke-WebRequest -Uri $JDK_URL -OutFile $JDK_ZIP -UseBasicParsing
        Write-OK "JDK 17 téléchargé"
    } catch {
        Write-Fail "Échec du téléchargement JDK : $_"
    }

    Write-Step "Extraction JDK 17..."
    Expand-Archive -Path $JDK_ZIP -DestinationPath "$TOOLS_DIR\jdk17_tmp" -Force
    # Déplacer le contenu du sous-dossier (ex: jdk-17.0.11+9) vers $JDK_DIR
    $extracted = Get-ChildItem "$TOOLS_DIR\jdk17_tmp" | Where-Object { $_.PSIsContainer } | Select-Object -First 1
    if ($extracted) {
        Get-ChildItem $extracted.FullName | Move-Item -Destination $JDK_DIR -Force
        Remove-Item "$TOOLS_DIR\jdk17_tmp" -Recurse -Force
    }
    Remove-Item $JDK_ZIP -Force -ErrorAction SilentlyContinue
    Write-OK "JDK 17 installé dans : $JDK_DIR"
} else {
    Write-OK "JDK 17 déjà installé"
}

# Configurer JAVA_HOME pour cette session
$env:JAVA_HOME = $JDK_DIR
$env:PATH = "$JDK_DIR\bin;$env:PATH"
$javaVer = & "$JDK_DIR\bin\java.exe" -version 2>&1 | Select-String "version"
Write-OK "Java actif : $javaVer"

# ─── ÉTAPE 3 : Télécharger Android Command-Line Tools ─────────────────────────
$CMDTOOLS_ZIP = "$TOOLS_DIR\cmdtools.zip"
$CMDTOOLS_DIR = "$SDK_DIR\cmdline-tools\latest"
if (-not (Test-Path "$CMDTOOLS_DIR\bin\sdkmanager.bat")) {
    Write-Step "Téléchargement Android Command-Line Tools..."
    Write-Warn "Taille : ~150 MB - Patience..."
    try {
        Invoke-WebRequest -Uri $CMDTOOLS_URL -OutFile $CMDTOOLS_ZIP -UseBasicParsing
        Write-OK "Command-Line Tools téléchargés"
    } catch {
        Write-Fail "Échec du téléchargement : $_"
    }

    Write-Step "Extraction..."
    Expand-Archive -Path $CMDTOOLS_ZIP -DestinationPath "$TOOLS_DIR\cmdtools_tmp" -Force
    New-Item -ItemType Directory -Force -Path $CMDTOOLS_DIR | Out-Null
    $extracted = Get-ChildItem "$TOOLS_DIR\cmdtools_tmp\cmdline-tools" -ErrorAction SilentlyContinue
    if (-not $extracted) {
        $extracted = Get-ChildItem "$TOOLS_DIR\cmdtools_tmp" | Where-Object { $_.PSIsContainer } | Select-Object -First 1
        if ($extracted) {
            Get-ChildItem $extracted.FullName | Move-Item -Destination $CMDTOOLS_DIR -Force
        }
    } else {
        Get-ChildItem "$TOOLS_DIR\cmdtools_tmp\cmdline-tools" | Move-Item -Destination $CMDTOOLS_DIR -Force
    }
    Remove-Item "$TOOLS_DIR\cmdtools_tmp" -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item $CMDTOOLS_ZIP -Force -ErrorAction SilentlyContinue
    Write-OK "Command-Line Tools installés"
} else {
    Write-OK "Android Command-Line Tools déjà installés"
}

# ─── ÉTAPE 4 : Configurer ANDROID_HOME ─────────────────────────────────────────
$env:ANDROID_HOME     = $SDK_DIR
$env:ANDROID_SDK_ROOT = $SDK_DIR
$env:PATH = "$CMDTOOLS_DIR\bin;$SDK_DIR\platform-tools;$env:PATH"
Write-OK "ANDROID_HOME = $SDK_DIR"

# ─── ÉTAPE 5 : Accepter les licences et installer les composants SDK ───────────
Write-Step "Acceptation des licences Android..."
$licDir = "$SDK_DIR\licenses"
New-Item -ItemType Directory -Force -Path $licDir | Out-Null
# Hashes des licences officielles
"24333f8a63b6825ea9c5514f83c2829b004d1fee" | Out-File "$licDir\android-sdk-license" -Encoding ascii
"8933bad161af4178b1185d1a37fbf41ea5269c55" | Add-Content "$licDir\android-sdk-license"
"d56f5187479451eabf01fb78af6dfcb131a6481e" | Add-Content "$licDir\android-sdk-license"
"84831b9409646a918e30573bab4c9c91346d8abd" | Out-File "$licDir\android-sdk-preview-license" -Encoding ascii
Write-OK "Licences acceptées"

Write-Step "Installation des composants SDK nécessaires..."
Write-Warn "Téléchargement : platform-35 + build-tools-35.0.0 (~250 MB)"

$sdkManager = "$CMDTOOLS_DIR\bin\sdkmanager.bat"
$sdkComponents = @(
    "platforms;android-35",
    "build-tools;35.0.0",
    "platform-tools"
)
foreach ($comp in $sdkComponents) {
    Write-Host "  → Installation : $comp" -ForegroundColor DarkCyan
    $result = echo "y" | & $sdkManager $comp 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Warn "Problème avec $comp (code: $LASTEXITCODE) - Continuation..."
    } else {
        Write-OK "  $comp installé"
    }
}

# ─── ÉTAPE 6 : Configurer local.properties ─────────────────────────────────────
Write-Step "Configuration de local.properties..."
$localProps = "$PROJECT_DIR\local.properties"
$sdkPathForGradle = $SDK_DIR -replace '\\', '/'
@"
## Ce fichier est généré automatiquement - NE PAS committer dans Git
sdk.dir=$sdkPathForGradle
"@ | Out-File $localProps -Encoding ascii -Force
Write-OK "local.properties créé : sdk.dir=$sdkPathForGradle"

# ─── ÉTAPE 7 : Compiler l'APK ─────────────────────────────────────────────────
Write-Step "Compilation de l'APK Debug..."
Write-Warn "Première compilation = téléchargements Gradle supplémentaires (~100 MB)"
Write-Warn "Patience, cela peut prendre 5-10 minutes..."

Set-Location $PROJECT_DIR

$env:JAVA_HOME = $JDK_DIR  # S'assurer que JAVA_HOME est correct pour Gradle

try {
    & ".\gradlew.bat" assembleDebug --stacktrace 2>&1 | Tee-Object -FilePath "$TOOLS_DIR\build.log"
    
    $apkPath = "$PROJECT_DIR\app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path $apkPath) {
        $apkSize = [math]::Round((Get-Item $apkPath).Length / 1MB, 1)
        Write-OK "`n=== APK CRÉÉ AVEC SUCCÈS ==="
        Write-OK "Chemin : $apkPath"
        Write-OK "Taille : $apkSize MB"
        Write-Host "`nPour installer sur votre téléphone :" -ForegroundColor White
        Write-Host "  1. Copiez l'APK sur votre téléphone" -ForegroundColor White
        Write-Host "  2. Activez 'Sources inconnues' dans Paramètres" -ForegroundColor White
        Write-Host "  3. Ouvrez l'APK depuis votre gestionnaire de fichiers" -ForegroundColor White
    } else {
        Write-Fail "APK non trouvé. Consultez le log : $TOOLS_DIR\build.log"
    }
} catch {
    Write-Fail "Erreur de compilation : $_`nConsultez : $TOOLS_DIR\build.log"
}
