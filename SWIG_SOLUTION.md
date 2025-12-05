# SWIG Installation & Build Solution

## Problem
CMake cannot find SWIG 3.0+ when building the OpenVPN module. Error:
```
Could NOT find SWIG (missing: SWIG_EXECUTABLE SWIG_DIR) (Required is at least version "3.0")
```

## Solution Options

### Option 1: Use the Skeleton Flavor (FASTEST - Recommended for Development)
The skeleton flavor skips SWIG compilation. This is the quickest fix:

```powershell
cd C:\Users\Admin\StudioProjects\lmaovpn
./gradlew.bat build -x assembleDebug --no-daemon -DskeletonFlavor=true
# Or for specific task
./gradlew.bat :app:build -Pskeletonbuild=true
```

**Build Command with Skeleton Flavor:**
```powershell
./gradlew assembleSkeletonDebug
```

---

### Option 2: Install SWIG on Windows (RECOMMENDED for Production)

#### Step 1: Download SWIG for Windows
```powershell
# Option A: Download pre-built binary
# Visit: https://sourceforge.net/projects/swig/files/
# Download: swigwin-4.2.1.zip (or latest version)
# Extract to: C:\swigwin-4.2.1
```

#### Step 2: Add SWIG to System PATH
```powershell
# Using PowerShell (Run as Administrator)
$swigPath = "C:\swigwin-4.2.1"
[Environment]::SetEnvironmentVariable("Path", $env:Path + ";$swigPath", [EnvironmentVariableTarget]::Machine)

# Verify installation
swig -version
```

#### Step 3: Restart Android Studio/Gradle Daemon
```powershell
./gradlew --stop
# Then rebuild
./gradlew clean build
```

---

### Option 3: Install SWIG via Package Manager

#### Using Chocolatey (if installed):
```powershell
choco install swig
```

#### Using MSYS2:
```powershell
pacman -S mingw-w64-x86_64-swig
```

---

### Option 4: Modify CMakeLists.txt to Make SWIG Optional

Edit `openvpn/src/main/cpp/CMakeLists.txt` and change:
```cmake
# FROM:
if (NOT ${CMAKE_LIBRARY_OUTPUT_DIRECTORY} MATCHES "build/intermediates/cmake/.*skeleton.*/")
    FIND_PACKAGE(SWIG 3.0 REQUIRED)

# TO:
if (NOT ${CMAKE_LIBRARY_OUTPUT_DIRECTORY} MATCHES "build/intermediates/cmake/.*skeleton.*/")
    FIND_PACKAGE(SWIG 3.0 QUIET)
    if(NOT SWIG_FOUND)
        message(WARNING "SWIG not found. Using skeleton build without Java bindings.")
    endif()
```

---

## Immediate Fix (Development)

**Run this command immediately:**
```powershell
cd C:\Users\Admin\StudioProjects\lmaovpn
./gradlew.bat clean :app:assembleSkeletonDebug -x test
```

This will:
1. Skip SWIG requirement
2. Use skeleton flavor
3. Build without unit tests
4. Generate a working APK for testing

---

## Full Installation Steps (Production Ready)

### For Windows 10/11:

1. **Download SWIG:**
   - Visit: https://sourceforge.net/projects/swig/files/
   - Download: `swigwin-4.2.1.zip`
   - Extract to: `C:\swigwin-4.2.1`

2. **Add to Environment Variables:**
   - Press `Win + X` → System
   - Advanced system settings → Environment Variables
   - Add `C:\swigwin-4.2.1` to `Path` variable
   - Click OK

3. **Restart Gradle Daemon:**
   ```powershell
   cd C:\Users\Admin\StudioProjects\lmaovpn
   ./gradlew.bat --stop
   ./gradlew.bat clean build
   ```

4. **Verify SWIG Installation:**
   ```powershell
   swig -version
   # Should show: SWIG Version 4.2.1
   ```

---

## Quick Troubleshooting

**If still failing after installation:**
1. Restart Android Studio completely
2. Invalidate caches: File → Invalidate Caches → Invalidate and Restart
3. Clean build: `./gradlew clean`
4. Delete `.cxx` folder: `openvpn/.cxx/`
5. Rebuild: `./gradlew build`

**Check SWIG is in PATH:**
```powershell
where swig
# Should show: C:\swigwin-4.2.1\swig.exe
```

---

## Recommended Action

Choose one:
- **For immediate testing:** Use Option 1 (Skeleton Flavor)
- **For production builds:** Use Option 2 (Install SWIG)
- **For development convenience:** Use Option 4 (Make SWIG optional)

