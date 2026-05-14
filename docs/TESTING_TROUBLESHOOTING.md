# Spring Boot Testing Troubleshooting Guide

> **Project**: Order Processing System  
> **Spring Boot**: 3.2.5 | **Java**: 17 | **Maven**: 3.9.9  
> **Last Updated**: 2026-05-15

---

## Issue 1: `@MockBean` — Package Location Changes Across Versions

### Symptom

```
ERROR: package org.springframework.boot.test.mock.bean does not exist
ERROR: cannot find symbol — class MockBean
```

### Root Cause

The `@MockBean` annotation has **moved packages across Spring Boot versions**. Many online tutorials and AI-generated code reference the wrong package for your specific version.

| Spring Boot Version | Correct Import |
|---|---|
| **2.x** | `org.springframework.boot.test.mock.bean.MockBean` |
| **3.0 – 3.3** | `org.springframework.boot.test.mock.mockito.MockBean` |
| **3.4+** | `org.springframework.test.context.bean.definition.MockBean` *(migrated to Spring Framework)* |

### How We Diagnosed It

We inspected the actual JAR on disk to find the class:

```cmd
jar tf %USERPROFILE%\.m2\repository\org\springframework\boot\spring-boot-test\3.2.5\spring-boot-test-3.2.5.jar | findstr MockBean
```

**Output:**
```
org/springframework/boot/test/mock/mockito/MockBean.class
org/springframework/boot/test/mock/mockito/MockBeans.class
```

This proves that in **Spring Boot 3.2.5**, the class lives at `org.springframework.boot.test.mock.mockito.MockBean`.

### Fix

```java
// ❌ WRONG for Spring Boot 3.2.5
import org.springframework.boot.test.mock.bean.MockBean;

// ❌ WRONG for Spring Boot 3.2.5 (this is for 3.4+)
import org.springframework.test.context.bean.definition.MockBean;

// ✅ CORRECT for Spring Boot 3.2.5
import org.springframework.boot.test.mock.mockito.MockBean;
```

### Why This Happens

Spring Boot 3.4 deprecated `@MockBean` from its own package and moved it into the Spring Framework core (`spring-test`). If you upgrade to Spring Boot 3.4+, you'll need to update the import again. The deprecation notice in 3.4 reads:

> *"@MockBean and @SpyBean are now available in spring-test as @MockitoBean and @MockitoSpyBean."*

### Prevention

- **Always check the JAR** when import errors occur — don't trust blog posts or AI suggestions blindly.
- **Pin your Spring Boot version** in `pom.xml` and verify imports match that version.
- Add a comment in your test base class documenting the import source.

---

## Issue 2: `.env` File Committed to Git History

### Symptom

Credentials (`DB_USERNAME`, `DB_PASSWORD`) were committed to the git repository via the `.env` file. Even after deleting the file, the credentials remain visible in past commit diffs via `git log -p -- .env`.

### Root Cause

The `.env` file was created (`copy .env.example .env`) and committed before `.env` was added to `.gitignore`. Git tracks files at the point of `git add` — adding to `.gitignore` later does NOT retroactively untrack files or erase them from history.

### Fix Applied

**Step 1: Add `.env` to `.gitignore`**
```gitignore
# Environment variables (contains secrets — NEVER commit)
.env
```

**Step 2: Remove from tracking (but keep the file on disk)**
```cmd
git rm --cached .env
git commit -m "security: remove .env from tracking"
```

**Step 3: Purge from entire git history**
```cmd
git filter-branch --force --index-filter "git rm --cached --ignore-unmatch .env" --prune-empty -- --all
```

**Step 4: Clean up backup refs and garbage collect**
```cmd
git for-each-ref --format="delete %(refname)" refs/original | git update-ref --stdin
git reflog expire --expire=now --all
git gc --prune=now --aggressive
```

**Step 5: Force push to remote (if applicable)**
```cmd
git push --force --all
```

### Verification

```cmd
REM Should return EMPTY (no output = success)
git log --all -p -- .env

REM Should return EMPTY (file not tracked)
git ls-files .env

REM Should return EMPTY (no matching commits)
git log --all -S "DB_PASSWORD=password" --oneline
```

### Prevention

- **Always add `.env` to `.gitignore` BEFORE creating the file.**
- **Commit `.env.example`** as a template (with placeholder values only).
- **Use environment variable placeholders** in YAML configs:
  ```yaml
  # ✅ SAFE — placeholder with default for dev only
  username: ${DB_USERNAME:sa}
  password: ${DB_PASSWORD:password}
  ```
- **Never hardcode real credentials** in any file that gets committed.

---

## Issue 3: Lombok Dependency Scope — `<optional>` vs `<scope>provided</scope>`

### Symptom

No direct build error, but inconsistent behavior: the Lombok dependency was declared as `<optional>true</optional>` in some commits and `<scope>provided</scope>` in others.

### Explanation

| Attribute | Meaning | Use Case |
|---|---|---|
| `<optional>true</optional>` | "Don't include transitively in dependent projects" | Libraries that other projects depend on |
| `<scope>provided</scope>` | "Available at compile time, NOT included in the runtime classpath or final JAR" | Tools like Lombok that generate code at compile time |

### Why `<scope>provided</scope>` is Correct for Lombok

Lombok is an **annotation processor**. It generates Java bytecode during compilation (`javac`), then it's no longer needed at runtime. Using `provided` explicitly communicates this intent:

1. **Compile time**: Lombok JAR is on the classpath → `javac` runs annotation processing → generates getters/setters/builders as bytecode.
2. **Runtime**: Lombok JAR is NOT in the final `target/*.jar` → reduces artifact size, prevents classpath conflicts.

### Fix

```xml
<!-- ❌ WORKS but semantically wrong for this project -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<!-- ✅ CORRECT — standard Spring Boot convention -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <scope>provided</scope>
</dependency>
```

### Note on `annotationProcessorPaths`

Even with `<scope>provided</scope>`, Maven 3.9+ requires **explicit annotation processor registration** in the `maven-compiler-plugin`. See [LOMBOK_TROUBLESHOOTING.md](./LOMBOK_TROUBLESHOOTING.md) for the full explanation of that issue.

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

---

## Quick Reference: All Known Build/Test Issues

| Issue | Root Cause | Fix | Doc |
|---|---|---|---|
| `cannot find symbol` (getters/setters) | Maven 3.9+ annotation processor discovery | Add `annotationProcessorPaths` to `maven-compiler-plugin` | [LOMBOK_TROUBLESHOOTING.md](./LOMBOK_TROUBLESHOOTING.md) |
| `MockBean` import error | Package moved between Spring Boot versions | Use `o.s.b.test.mock.mockito.MockBean` for 3.2.x | This file |
| `.env` in git history | File committed before `.gitignore` entry | `git filter-branch` + `gc --prune=now` | This file |
| Lombok scope inconsistency | `<optional>` vs `<scope>provided</scope>` | Use `<scope>provided</scope>` | This file |
