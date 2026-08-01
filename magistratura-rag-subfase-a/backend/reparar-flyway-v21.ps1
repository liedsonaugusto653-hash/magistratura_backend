# Corre na pasta backend ou ajusta o nome do contentor
$container = $env:PG_CONTAINER
if (-not $container) { $container = "magistratura-postgres" }
Write-Host "A limpar flyway_schema_history versão 21 falhada no contentor $container ..."
docker exec -i $container psql -U magistratura -d magistratura -c "DELETE FROM flyway_schema_history WHERE version = '21' AND success = false;"
if ($LASTEXITCODE -ne 0) {
  Write-Host "Falhou docker exec. Corre manualmente no psql:"
  Write-Host "DELETE FROM flyway_schema_history WHERE version = '21' AND success = false;"
  exit 1
}
Write-Host "OK. Agora: mvn spring-boot:run"
