#!/usr/bin/env bash
set -euo pipefail

# Ejecuta el HealthChecker que supervisa al Servidor Central
# Uso: ./health_check.sh <ipServidor> [puerto]

IP="${1:-localhost}"
PUERTO="${2:-5555}"
IP_LOCAL="DEFINIR_IP_LOCAL_MANUALMENTE"

# Navegar a la raíz del proyecto (donde está pom.xml)
cd "$(dirname "$0")/../.."

echo "[health_check] IP local: $IP_LOCAL"
echo "[health_check] Verificando conectividad a $IP:$PUERTO..."

# Compatibilidad con Linux (ping -c) y Git Bash (ping -n)
if command -v ping &> /dev/null; then
  if [[ "$(uname -s)" == "Linux" ]]; then
    ping -c 1 "$IP" > /dev/null || {
      echo "❌ No se puede contactar al servidor en $IP"
      exit 1
    }
  else
    ping -n 1 "$IP" > /dev/null || {
      echo "❌ No se puede contactar al servidor en $IP"
      exit 1
    }
  fi
fi

echo "[health_check] Compilando proyecto..."
mvn compile

echo "[health_check] Iniciando HealthChecker contra $IP:$PUERTO..."
mvn exec:java \
  -Dexec.mainClass="tolerancia.HealthChecker" \
  -Dexec.args="$IP $PUERTO" \
  -Dexec.cleanupDaemonThreads=false
