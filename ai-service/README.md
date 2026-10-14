# ReconPilot Analytics (Python 3.10)

Microservico FastAPI auxiliar e nao decisorio. Recebe apenas candidatos ja produzidos pelo Java, calcula um score explicavel e devolve flags para revisao. Nao altera pares, transacoes ou decisoes humanas.

Requer Python 3.10. Sem venv obrigatoria; no Windows (validado) use o executavel direto:

```powershell
$Py310 = "$env:LOCALAPPDATA\Programs\Python\Python310\python.exe"
& $Py310 -m pip install -r requirements-dev.txt   # inclui pytest; o runtime usa so requirements.txt
& $Py310 -m pytest -q

$env:AI_INTERNAL_TOKEN = 'segredo-local'
& $Py310 -m uvicorn app.main:app --host 127.0.0.1 --port 8000
```

Em Linux/macOS os comandos equivalentes (`python3.10 -m ...`) nao foram verificados nesta revisao. Imagem Docker: `docker build -t reconpilot-ai ai-service` (usuario nao-root, `HEALTHCHECK` em `/healthz`).

Endpoint interno: `POST /v1/anomalies/analyze`, com `X-Internal-Token`. Sem token configurado o servico retorna 503. O `risk_score` e nao calibrado e nao representa probabilidade de fraude.

