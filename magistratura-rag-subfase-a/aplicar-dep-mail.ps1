# Corre na pasta backend (onde está o pom.xml)
$pom = "pom.xml"
if (-not (Test-Path $pom)) {
  Write-Error "Corre este script dentro de backend\ (pasta com pom.xml)"
  exit 1
}
$dep = @"
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-mail</artifactId>
        </dependency>

"@
$content = Get-Content $pom -Raw -Encoding UTF8
if ($content -match "spring-boot-starter-mail") {
  Write-Host "Dependencia spring-boot-starter-mail ja existe no pom.xml"
  exit 0
}
# Insert after starter-validation block
$pattern = '(?s)(<artifactId>spring-boot-starter-validation</artifactId>\s*</dependency>)'
$replacement = "`$1`r`n`r`n$dep"
$newContent = [regex]::Replace($content, $pattern, $replacement, 1)
if ($newContent -eq $content) {
  Write-Error "Nao encontrei spring-boot-starter-validation no pom para inserir a seguir. Adiciona a dependencia manualmente."
  exit 1
}
Set-Content -Path $pom -Value $newContent -Encoding UTF8
Write-Host "OK: spring-boot-starter-mail adicionado ao pom.xml"
Write-Host "Seguinte: mvn spring-boot:run"
